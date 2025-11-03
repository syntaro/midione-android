package jp.gr.java_conf.syntarou.midione.drivers.bluetooth.peripheral;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import jp.gr.java_conf.syntarou.midione.AppConstant;
import jp.gr.java_conf.syntarou.midione.MidiOne;
import jp.gr.java_conf.syntarou.midione.common.OneHelper;
import jp.gr.java_conf.syntarou.midione.common.RunQueueThread;
import jp.gr.java_conf.syntarou.midione.drivers.bluetooth.AbstractBleInputBase;
import jp.gr.java_conf.syntarou.midione.drivers.bluetooth.AbstractBleOutputBase;
import jp.gr.java_conf.syntarou.midione.drivers.bluetooth.BlePartnerInfo;
import jp.gr.java_conf.syntarou.midione.util.BleUuidUtils;
import jp.gr.java_conf.syntarou.midione.v1.IOneDispatcher;
import jp.gr.java_conf.syntarou.midione.v1.OneDeviceStatus;
import jp.gr.java_conf.syntarou.midione.v1.OneDriver;
import jp.gr.java_conf.syntarou.midione.v1.OneMessage;
import jp.gr.java_conf.syntarou.midione.ui.logview.MessageLogger;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class OneDriverPeripheral extends OneDriver {
    OneDevicePeripheral _device;
    final RunQueueThread _thread;
    BluetoothLeAdvertiser bluetoothLeAdvertiser;
    BluetoothManager _manager;
    BluetoothAdapter _adapter;
    public OneDriverPeripheral(MidiOne one) {
        super("Be BLE", one);
        _manager = _one.getOneBleCore().getBluetoothManager();
        _adapter = _one.getOneBleCore().getBluetoothAdapter();
        _thread = _one.getOneBleCore().getThread();
    }

    @Override
    public void startDriver(Context context) {

        if (_adapter == null) {
            throw new UnsupportedOperationException("Bluetooth is not available.");
        }

        if (_adapter.isEnabled() == false) {
            throw new UnsupportedOperationException("Bluetooth is disabled.");
        }

        if (_adapter.isMultipleAdvertisementSupported() == false) {
            throw new UnsupportedOperationException("Bluetooth LE Advertising not supported on this device.");
        }

        bluetoothLeAdvertiser = _adapter.getBluetoothLeAdvertiser();
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag, "bluetoothLeAdvertiser: " + bluetoothLeAdvertiser);
        }
        if (bluetoothLeAdvertiser == null) {
            throw new UnsupportedOperationException("Bluetooth LE Advertising not supported on this device.");
        }
    }

    @Override
    public void startScanDevices(Context context) {
        if (_device == null) {
            _device = new OneDevicePeripheral(this, _prefix);
            _device.getDeviceStatus().countIt(OneDeviceStatus.STATUS_START_CONNECT);
            addDevice(_device);
        }
        _thread.post(() -> {
            startAdvertising();
        });
    }

    @Override
    public void stopScanDevices(Context context) {
        stopAdvertising();
    }
    private static final UUID SERVICE_DEVICE_INFORMATION = BleUuidUtils.fromShortValue(0x180A);
    private static final UUID SERVICE_BLE_MIDI = UUID.fromString("03b80e5a-ede8-4b33-a751-6ce34ec4c700");

    /**
     * Gatt Characteristics
     */
    private static final short MANUFACTURER_NAME = 0x2A29;
    private static final short MODEL_NUMBER = 0x2A24;

    private static final UUID CHARACTERISTIC_MANUFACTURER_NAME = BleUuidUtils.fromShortValue(MANUFACTURER_NAME);
    private static final UUID CHARACTERISTIC_MODEL_NUMBER = BleUuidUtils.fromShortValue(MODEL_NUMBER);
    private static final UUID CHARACTERISTIC_BLE_MIDI = UUID.fromString("7772e5db-3868-4112-a1a9-f2669d106bf3");

    private static final short SERIAL_NUMBER = 0x2A25;
    private static final short FIRMWARE_REVISION = 0x2A26;
    private static final short HARDWARE_REVISION = 0x2A27;
    private static final short SOFTWARE_REVISION = 0x2A28;
    private static final UUID CHARACTERISTIC_SERIAL_NUMBER = BleUuidUtils.fromShortValue(SERIAL_NUMBER);
    private static final UUID CHARACTERISTIC_FIRMWARE_REVISION = BleUuidUtils.fromShortValue(FIRMWARE_REVISION);
    private static final UUID CHARACTERISTIC_HARDWARE_REVISION = BleUuidUtils.fromShortValue(HARDWARE_REVISION);
    /**
     * Gatt Characteristic Descriptor
     */
    private static final UUID DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION = BleUuidUtils.fromShortValue(0x2902);

    private BluetoothGattService informationGattService;
    private BluetoothGattService midiGattService;
    private BluetoothGattCharacteristic midiCharacteristic;
    private BluetoothGattServer _gattServer;
    private boolean _gattServerConnectCanceled;

    private final String manufacturer = "1st Constellation";
    private final String modelNumber = "MidiOne";
    private String serialNumber = null;
    private final String firmwareRevision = "0001";
    private final String hardwareRevision = "0001";

    public long howLongAdvertinsing() {
        if (_runningFrom == 0) {
            return 0;
        }
        long tick = System.currentTimeMillis() - _runningFrom;
        if (tick >= 120 * 1000) {
            _runningFrom = 0;
            stopAdvertising();

            return 0;
        }
        return tick;
    }
    /**
     * Starts advertising
     */
    public synchronized void startAdvertising() throws SecurityException {
        // register Gatt service to Gatt server
        MessageLogger.getInstance().log("startAdvertising");
        long lastTime = howLongAdvertinsing();
        if (lastTime != 0) {
            if (lastTime >= 5000) {
                try {
                    Thread.sleep(500);
                } catch (Throwable ex) {

                }
                MessageLogger.getInstance().log("startAdvertising restart");
            } else {
                MessageLogger.getInstance().log("startAdvertising can't spam run");
                return;
            }
        }
        else {
            MessageLogger.getInstance().log("startAdvertising new");
        }
        if (_gattServer == null) {
            _gattServer = _manager.openGattServer(_one.getMidiContext(), gattServerCallback);

            if (serialNumber == null) {
                BluetoothAdapter bluetoothAdapter = _manager.getAdapter();
                if (bluetoothAdapter != null) {
                    String addr = bluetoothAdapter.getAddress();
                    if (addr != null) {
                        serialNumber = addr;
                    }
                }
                if (serialNumber == null) {
                    serialNumber = bluetoothAdapter.getName();
                }
                if (serialNumber == null) {
                    serialNumber = "1111";
                }
            }

            // Device information service
            if (informationGattService == null) {
                informationGattService = new BluetoothGattService(SERVICE_DEVICE_INFORMATION, BluetoothGattService.SERVICE_TYPE_PRIMARY);
                informationGattService.addCharacteristic(new BluetoothGattCharacteristic(CHARACTERISTIC_MANUFACTURER_NAME, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ));
                informationGattService.addCharacteristic(new BluetoothGattCharacteristic(CHARACTERISTIC_MODEL_NUMBER, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ));
                //informationGattService.addCharacteristic(new BluetoothGattCharacteristic(CHARACTERISTIC_SERIAL_NUMBER, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ));
                //informationGattService.addCharacteristic(new BluetoothGattCharacteristic(CHARACTERISTIC_HARDWARE_REVISION, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ));
                //informationGattService.addCharacteristic(new BluetoothGattCharacteristic(CHARACTERISTIC_FIRMWARE_REVISION, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ));
                addServiceAndWait(informationGattService);
            }
            // MIDI service
            if (midiCharacteristic == null) {
                midiCharacteristic = new BluetoothGattCharacteristic(CHARACTERISTIC_BLE_MIDI,
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY
                                | BluetoothGattCharacteristic.PROPERTY_READ
                                | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                        BluetoothGattCharacteristic.PERMISSION_READ
                                | BluetoothGattCharacteristic.PERMISSION_WRITE);/*
                                | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED //test
                                | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM//test
                                | BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED//test
                                | BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM);//test*/
                BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION, BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                midiCharacteristic.addDescriptor(descriptor);
                midiCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            }

            if (midiGattService == null) {
                midiGattService = new BluetoothGattService(SERVICE_BLE_MIDI, BluetoothGattService.SERVICE_TYPE_PRIMARY);
                midiGattService.addCharacteristic(midiCharacteristic);
                addServiceAndWait(midiGattService);
            }
        }

        if (_gattServer == null) {
            Log.e(AppConstant.MidiOneTag, "gattServer is null, check Bluetooth is ON.");
            return;
        }

        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        // set up advertising setting
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(120 * 1000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            builder.setDiscoverable(true);
        }
        /*
        Only System Service can use it
        boolean invoked = false;
        try {
            Method[] list = builder.getClass().getMethods();
            for (Method method : list) {
                Log.d("Reflection",method.getName());
                if (method.getName().equals("setOwnAddressType")) {
                    method.invoke(builder, 0);
                    invoked = true;
                    break;
                }
            }
        }catch(Throwable ex) {
            Log.e(AppConstant.TAG, ex.getMessage(), ex);
        }
        Log.e(AppConstant.TAG, "reflection invoked " + invoked);
            */
        AdvertiseSettings advertiseSettings = builder.build();

        // set up advertising data
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .setIncludeDeviceName(true)
                .build();

        // set up scan result
        AdvertiseData scanResult = new AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid.fromString(SERVICE_DEVICE_INFORMATION.toString()))
                .addServiceUuid(ParcelUuid.fromString(SERVICE_BLE_MIDI.toString()))
                .build();

        _runningFrom = System.currentTimeMillis();
        _device.getDeviceStatus().countIt(OneDeviceStatus.STATUS_START_CONNECT);
        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, scanResult, advertiseCallback);
    }

    long _runningFrom = 0;
    long _runStartTime;

    /**
     * Stops advertising
     */
    public synchronized void stopAdvertising() throws SecurityException {
        try {
            MessageLogger.getInstance().log("stopAdvertising");
            if (_runningFrom > 0) {
                _runningFrom = 0;
                bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
            }
        } catch (IllegalStateException ignored) {
            Log.e(AppConstant.MidiOneTag, "stopAdvertising", ignored);
        }
    }

    /**
     * Callback for BLE connection<br />
     * nothing to do.
     */
    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            if (MidiOne.isDebug) {
                Log.e(AppConstant.MidiOneTag, "onStartScuccess " + _runningFrom);
            }
            _thread.postDelayed(() -> {
                stopAdvertising();
            }, 120 * 1000);
        }

        @Override
        public void onStartFailure(int errorCode) {
            _device.getDeviceStatus().countIt(OneDeviceStatus.STATUS_LOST);
            Toast.makeText(_one.getMidiContext(), "Start Advertising Fail", Toast.LENGTH_LONG).show();
        }
    };
    private final BluetoothGattCallback disconnectCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) throws SecurityException {
            if (MidiOne.isDebug) {
                Log.e(AppConstant.MidiOneTag, "onConnectionStateChange disconnect status: " + status + ", newState: " + newState);
            }
            // disconnect the device
            if (gatt != null) {
                gatt.disconnect();
            }
        }
    };

    public void terminate() throws SecurityException {
        stopAdvertising();

        for (int i = 0; i < countDevices(); ++i) {
            OneDevicePeripheral seek = (OneDevicePeripheral) getDevice(i);
            if (seek._partner._internalOut != null) {
                seek._partner._internalOut.stopTransmitterAndThread();
                seek._partner._internalOut = null;
            }
            if (seek._partner._internalIn != null) {
                seek._partner._internalIn.stopParserAndThread();
                seek._partner._internalIn = null;
            }
        }

        if (_gattServer != null) {
            for (int i = 0; i < countDevices(); ++i) {
                OneDevicePeripheral seek = (OneDevicePeripheral) getDevice(i);
                _gattServer.cancelConnection(seek._partner._bleDevice);
                if (seek._partner._connectedGatt != null) {
                    seek._partner._connectedGatt.disconnect();
                    seek._partner._connectedGatt = null;
                }
                seek._partner._bleDevice.connectGatt(_one.getMidiContext(), true, disconnectCallback);
            }
            try {
                _gattServer.clearServices();
            } catch (Throwable ignored) {
                // android.os.DeadObjectException
            }
            try {
                _gattServer.close();
            } catch (Throwable ignored) {
                // android.os.DeadObjectException
            }
            _gattServer = null;
        }
    }

    boolean _didOnServiceAdded;
    int codeOnServiceAddred;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void addServiceAndWait(BluetoothGattService service) {
        if (_gattServer.getService(service.getUuid()) != null) {
            return;
        }
        _didOnServiceAdded = false;
        _gattServerConnectCanceled = false;
        _gattServer.addService(service);
        while (!_didOnServiceAdded && !_gattServerConnectCanceled) {
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException ex) {

                }
            }
        }
    }

    public void notifyDoneServiceAdded(int status) {
        _didOnServiceAdded = true;
        codeOnServiceAddred = status;
        synchronized (this) {
            notifyAll();
        }
    }


    /**
     * Callback for BLE data transfer
     */
    final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            if (MidiOne.isDebug) {
                Log.e(AppConstant.MidiOneTag, "onServiceAdded " + service);
            }
            notifyDoneServiceAdded(status);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            if (MidiOne.isDebug) {
                Log.e(AppConstant.MidiOneTag, "onMtuChanged " + mtu);
            }

            AbstractBleOutputBase midiOutputDevice = _device._partner._internalOut;
            if (midiOutputDevice != null) {
                int buf = mtu;
                if (buf <= 23) {
                    buf = 20;
                }
                if (buf >= 200) {
                    buf = 200;
                }
                ((InternalMidiOutputDevice) midiOutputDevice).setBufferSize(buf);
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (MidiOne.isDebug) {
                MessageLogger.getInstance().log("onConnectionStateChange " + status + " state " + newState);
            }

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    BlePartnerInfo partner = _device._partner;

                    if (partner._bleDevice == null) {
                        if (MidiOne.isDebug) {
                            MessageLogger.getInstance().log( "new connection");
                            MessageLogger.getInstance().log("dev1 = " + device);
                        }
                        partner._bleDevice = device;
                    }
                    else if (partner._bleDevice != device) {
                        if (partner._bleDevice != null
                                && partner._bleDevice.getAddress().equals(device.getAddress())) {
                            return;
                        } else {
                            MessageLogger.getInstance().log("another connected");
                            MessageLogger.getInstance().log("dev1 = " + device);
                            MessageLogger.getInstance().log("dev2 = " + _device._partner._bleDevice);
                            _device._partner._bleDevice = device;
                            if (partner._internalIn != null) {
                                partner._internalIn.stopParserAndThread();
                                partner._internalIn = null;
                            }
                            if (partner._internalOut != null) {
                                partner._internalOut.stopTransmitterAndThread();;
                                partner._internalOut = null;
                            }
                        }
                    } else {
                        if (MidiOne.isDebug) {
                            MessageLogger.getInstance().log("same connected");
                            MessageLogger.getInstance().log("dev1 = " + device);
                        }
                    }

                    if (partner._internalOut == null) {
                        partner._internalOut = new InternalMidiOutputDevice();
                    }
                    if (partner._internalIn == null) {
                        partner._internalIn = new InternalMidiInputDevice(new IOneDispatcher() {
                            @Override
                            public boolean dispatchOne(OneMessage one) {
                                try {
                                    return _device.getInput(0).dispatchOne(one);
                                }catch(NullPointerException ex) {
                                    Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
                                }
                                return false;
                            }

                            @Override
                            public String getNameText() {
                                return "";
                            }
                        });
                    }
                    _device._partner._internalIn.startParser();
                    _device._partner._internalOut.startTransmitter();
                    _device.ensureInput();
                    _device.ensureOutput();
                    _device.getDeviceStatus().countIt(OneDeviceStatus.STATUS_CONNECTED);
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    if (_device._partner._bleDevice != device) {
                        if (_device._partner._bleDevice == null || device == null) {
                            return;
                        }
                        if (_device._partner._bleDevice.getAddress().equals(device.getAddress()) == false) {
                            _device.closeDevice();
                            if (MidiOne.isDebug) {
                                MessageLogger.getInstance().log("another device DCed ");
                                MessageLogger.getInstance().log("dev1 = " + device);
                                MessageLogger.getInstance().log("dev2 = " + _device._partner._bleDevice);
                            }
                            return;
                        }
                    }

                    MessageLogger.getInstance().log("DC FROM " + status + " -> " + newState + " device " + _device);
                    if (_device._partner._connectedGatt != null) {
                        _device._partner._connectedGatt.close();
                        _device._partner._connectedGatt = null;
                    }
                    _gattServerConnectCanceled = true;
                    _device.getDeviceStatus().countIt(OneDeviceStatus.STATUS_LOST);
                    if (_device != null) {
                        _device.closeDevice();
                    }
                    synchronized (this) {
                        notifyAll();
                    }
                    break;
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) throws SecurityException {
            if (MidiOne.isDebug) {
                Log.e(AppConstant.MidiOneTag, "onCharacteristicReadRequest");
            }
            //OneDevicePeripheral target = _driver.getOneDevicPeripheral(device);
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            UUID characteristicUuid = characteristic.getUuid();

            if (BleUuidUtils.matches(CHARACTERISTIC_BLE_MIDI, characteristicUuid)) {
                //Log.e(AppConstant.TAG, "chara midi");
                _gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[]{1, 2, 3, 0});
            } else {
                switch (BleUuidUtils.toShortValue(characteristicUuid)) {
                    case MODEL_NUMBER:
                        //Log.e(AppConstant.TAG, "chara model");
                        _gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, modelNumber.getBytes(StandardCharsets.UTF_8));
                        break;

                    case MANUFACTURER_NAME:
                        //Log.e(AppConstant.TAG, "chara manu");
                        _gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, manufacturer.getBytes(StandardCharsets.UTF_8));
                        break;

                    case SERIAL_NUMBER:
                        //Log.e(AppConstant.TAG, "chara serial");
                        _gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, serialNumber.getBytes(StandardCharsets.UTF_8));
                        break;
                    case FIRMWARE_REVISION:
                        //Log.e(AppConstant.TAG, "chara firm");
                        _gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, firmwareRevision.getBytes(StandardCharsets.UTF_8));
                        break;
                    case HARDWARE_REVISION:
                        //Log.e(AppConstant.TAG, "chara hard");
                        _gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, hardwareRevision.getBytes(StandardCharsets.UTF_8));
                        break;
                    default:
                        // send empty
                        //Log.e(AppConstant.TAG, "chara empty");
                        _gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[]{});
                        break;
                }
            }
            _thread.doneExchange();
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) throws SecurityException {
            //super.onCharacteristicWriteRequest(device, requestId, characteristic,preparedWrite, responseNeeded, offset, value);
            if (MidiOne.isDebug) {
                MessageLogger.getInstance().log("onCharacteristicWriteRequest " + responseNeeded + " -> " + OneHelper.dumpHex(value));
            }

            if (BleUuidUtils.matches(characteristic.getUuid(), CHARACTERISTIC_BLE_MIDI)) {
                if (_device._partner._internalIn != null) {
                    _device._partner._internalIn.incomingData(value);
                }

                if (responseNeeded) {
                    // send empty
                    _gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[]{});
                }
            }
            _thread.doneExchange();
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) throws SecurityException {
            //super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            byte[] descriptorValue = descriptor.getValue();
            if (MidiOne.isDebug) {
                Log.e(AppConstant.MidiOneTag, "onDescriptorWriteRequest  " + OneHelper.dumpHex(descriptorValue));
            }
            try {
                System.arraycopy(value, 0, descriptorValue, offset, value.length);
                descriptor.setValue(descriptorValue);
            } catch (IndexOutOfBoundsException ignored) {
            }

            _gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[]{});
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) throws SecurityException {
            //super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            if (offset == 0) {
                _gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, descriptor.getValue());
            } else {
                final byte[] value = descriptor.getValue();
                byte[] result = new byte[value.length - offset];
                try {
                    System.arraycopy(value, offset, result, 0, result.length);
                } catch (IndexOutOfBoundsException ignored) {
                }
                _gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, result);
            }
        }
    };

    private class InternalMidiInputDevice extends AbstractBleInputBase {
        public InternalMidiInputDevice(IOneDispatcher onParsed) {
            super(_device.getPartnerInfo(), 0, onParsed);
        }
    }

    private class InternalMidiOutputDevice extends AbstractBleOutputBase {

        @Override
        public void waitTillWrite() {
            OneHelper.Thread_sleep(_device._partner.CONNECTION_INTERVAL);
        }

        public InternalMidiOutputDevice() {
            super(_device, _device.getPartnerInfo(), 0);
        }

        @Override
        public boolean transferData(@NonNull byte[] writeBuffer) throws SecurityException {
            try {
                BluetoothDevice ble = _device._partner._bleDevice;
                if (ble == null) {
                    MessageLogger.getInstance().log("ble = null");
                    return true;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    int result = _gattServer.notifyCharacteristicChanged(ble, midiCharacteristic, false, writeBuffer);
                    return result == BluetoothStatusCodes.SUCCESS;
                } else {
                    midiCharacteristic.setValue(writeBuffer);
                    boolean result = _gattServer.notifyCharacteristicChanged(ble, midiCharacteristic, false);
                    return result;
                }
            } catch (Throwable ignored) {
                Log.e(AppConstant.MidiOneTag, "taransfer error ", ignored);
                return false;
            }
        }
    }
}