package jp.gr.java_conf.syntarou.midione.drivers.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import jp.gr.java_conf.syntarou.midione.AppConstant;
import jp.gr.java_conf.syntarou.midione.common.OneHelper;
import jp.gr.java_conf.syntarou.midione.common.RunQueueThread;
import jp.gr.java_conf.syntarou.midione.MidiOne;
import jp.gr.java_conf.syntarou.midione.util.BleMidiDeviceUtils;
import jp.gr.java_conf.syntarou.midione.util.BleUuidUtils;
import jp.gr.java_conf.syntarou.midione.v1.IOneDispatcher;
import jp.gr.java_conf.syntarou.midione.v1.OneDeviceStatus;
import jp.gr.java_conf.syntarou.midione.v1.OneMessage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class BleCentralCallback extends BluetoothGattCallback {
    OneDeviceBle _device;
    OneBleCore _bleCore;
    boolean _terminate = false;

    public BleCentralCallback(Context applicationContext, OneBleCore bleCore, OneDeviceBle device) {
        super();
        _device = device;
        _bleCore = bleCore;
        _connectContext = applicationContext;
    }

    long _fixLastScan = 0;

    long _fixLastConnect = 0;

    Context _connectContext;

    final ScanCallback _fixCallback = new ScanCallback() {
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice seek = result.getDevice();
            if (MidiOne.isDebug) {
                if (_terminate) {
                    if (MidiOne.isDebug) {
                        Log.e(AppConstant.MidiOneTag, "Fixing NG");
                    }
                    stopFixScan();
                    return;
                }
            }
            String addr1 = seek.getAddress();
            if (addr1 != null) {
                if (addr1.equals(_device.getPartnerInfo()._bleDevice.getAddress())) {
                    long cur = System.currentTimeMillis();
                    _fixLastConnect = cur;
                    _bleCore.getThread().post(() -> {
                        _workDiscover100 = 0;
                        _discoveryCount = 0;
                        if (_bleCore.getBluetoothAdapter().isDiscovering()) { //ADDED 2025/4/25
                            _bleCore.getBluetoothAdapter().cancelDiscovery();
                        }
                        _device.getPartnerInfo()._connectedGatt = seek.connectGatt(_connectContext, false, BleCentralCallback.this);
                        _device.getPartnerInfo()._connectedGatt.connect();
                    });
                } else {
                    if (MidiOne.isDebug) {
                        //Log.e(AppConstant.TAG, "fix scanned " + addr1 + " <> " + _device);
                    }
                }
            }
        }
    };
    BluetoothLeScanner _fixScanner = null;
    List<ScanFilter> _fixFilters = null;

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void stopFixScan() {
        if (_fixScanner != null) {
            if (MidiOne.isDebug) {
                Log.e(AppConstant.MidiOneTag, "stopFixScan");
            }
            _fixScanner.stopScan(_fixCallback);
        }
    }

    public synchronized void reconnect() throws SecurityException {
        if (_terminate) {
            return;
        }
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag, "reconnect " + _device);
        }

        if (_device.getPartnerInfo()._connectedGatt == null) {
            _device.getPartnerInfo()._connectedGatt = _device.getPartnerInfo()._bleDevice.connectGatt(_connectContext, false, this);
            _device.getPartnerInfo()._connectedGatt.connect();
            return;
        }

        if (_fixScanner == null) {
            _fixScanner = _bleCore._bluetoothAdapter.getBluetoothLeScanner();
            _fixFilters = BleMidiDeviceUtils.getBleMidiScanFilters(_connectContext);
        }

        if (_fixLastScan != 0 && _fixLastScan + 7000 > System.currentTimeMillis()) {
            return;
        } else if (_fixLastScan != 0) {
            _fixScanner.stopScan(_fixCallback);
            _bleCore.getThread().postDelayed(() -> {
                reconnect();
            }, 500);
            _fixLastScan = 0;
            return;
        }
        _fixLastScan = System.currentTimeMillis();

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();

        _fixScanner.flushPendingScanResults(_fixCallback);
        _fixScanner.startScan(_fixFilters, scanSettings, _fixCallback);
    }
    int _workDiscover100 = -1;

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) throws SecurityException {
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag, "onConnectionStateChange newState "+  newState + " / status " + status);
        }
        if (_terminate) {
            int state = _bleCore._bluetoothManager.getConnectionState(_device.getPartnerInfo()._bleDevice, BluetoothProfile.GATT);
            if (MidiOne.isDebug) {
                Log.e(AppConstant.MidiOneTag, "current State " + state);
            }
            if (state == BluetoothProfile.STATE_CONNECTED) {
                terminate();
                gatt.disconnect();
                return;
            }
            if (state ==  BluetoothProfile.STATE_DISCONNECTED) {
                terminate();
                gatt.close();
                return;
            }
            gatt.disconnect();
            return;
        }
        /*
        if (status == 0 && status == 0) {
            BluetoothDevice device = _device.getPartnerInfo()._bleDevice;
            if (device != null) {
                if (true) {
                }
                else {
                    String seekName = device.getName();
                    String seekAddr = device.getAddress();
                    Set<BluetoothDevice> listAlready = _device.getPartnerInfo()._oneBle.getBluetoothAdapter().getBondedDevices();
                    if (listAlready != null && listAlready.size() > 0) {
                        int hit = 0;
                        int err = 0;
                        Log.e(AppConstant.TAG, "seek = " + seekName + " addr " + seekAddr);
                        for (BluetoothDevice already : listAlready) {
                            String name = already.getName();
                            String addr = already.getAddress();
                            Log.e(AppConstant.TAG, "bonded = " + name + " addr " + addr);
                            hit++;
                            if (already != device && name.equals(seekName)) {
                                err++;
                            }
                        }
                        if (err >= 1) {
                            _device.getEventCounter().countIt(OneEventCounter.EVENT_ERR_CONNECT);
                            reconnect(_device);
                        }
                    }
                }
            }
        }
         */
        /*
        if (_device.getPartnerInfo()._gatt == null) {
            _device.getPartnerInfo()._gatt = gatt;
        }*/
        if (gatt == null) {
            //gatt = _device.getPartnerInfo()._connectedGatt;
        }
        /*
        if (_device.getPartnerInfo()._gatt == gatt) {
            if (MidiOne.isDebug) {
                Log.e(AppConstant.TAG, "gatt = 0");
            }
        }else {
            if (_device.getPartnerInfo()._gatt.getDevice() == gatt.getDevice()) {
                if (MidiOne.isDebug) {
                    Log.e(AppConstant.TAG, "gatt = 1");
                }
                //gatt = _device.getPartnerInfo()._gatt ;
                _device.getPartnerInfo()._gatt = gatt;
            }
            else if (_device.getPartnerInfo()._gatt.getDevice().getAddress().equals(gatt.getDevice().getAddress()) == true) {
                if (MidiOne.isDebug) {
                    Log.e(AppConstant.TAG, "gatt = 2");
                }
                //gatt = _device.getPartnerInfo()._gatt ;
                _device.getPartnerInfo()._gatt = gatt;
            }
            else {
                if (MidiOne.isDebug) {
                    Log.e(AppConstant.TAG, "gatt = 3");
                }
                _device.getPartnerInfo()._gatt = gatt;
            }
        }
        if (newState == BluetoothProfile.STATE_DISCONNECTED && status == 8) {
            reconnect(_device);
            return;
        }
        if (newState == BluetoothProfile.STATE_DISCONNECTED && status == 0) {
            reconnect(_device);
            return;
        }
        */
        if (newState == BluetoothProfile.STATE_DISCONNECTED && status == 133) {
            reconnect();
            return;
        }
        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            _workDiscover100 = 200;
            _device.getPartnerInfo()._whenOpen = null;
            terminate();
        }
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            startDiscovery(gatt);
        }
    }
    /*
    private final BluetoothGattCallback _disconnectCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) throws SecurityException {
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                MessageLogger.getInstance().log("forceClose");
                gatt.close();
                if (_device.getPartnerInfo()._whenCancel != null) {
                    _device.getPartnerInfo()._whenCancel.run();
                }
            }
            else {
                MessageLogger.getInstance().log("forceDisconnect");
                BlePartnerInfo info = _device.getPartnerInfo();
                info._terminate = true;

                if (_bleCore._bluetoothAdapter.isDiscovering()) { //ADDED 2025/4/25
                    _bleCore._bluetoothAdapter.cancelDiscovery();
                }
                if (info._internalIn != null) {
                    info._internalIn.unconfigure();
                    info._internalIn.stopParserAndThread();
                    info._internalIn = null;
                }
                if (info._internalOut != null) {
                    info._internalOut.unconfigure();
                    info._internalOut.stopTransmitterAndThread();
                    info._internalOut = null;
                }
                gatt.disconnect();
            }
        }
    };*/

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    public void startDiscovery(BluetoothGatt gatt) {
        if (_terminate) {
            gatt.disconnect();
            return;
        }
        _bleCore.getThread().post(() -> {
            if (_workDiscover100 < 100) {
                int state = _bleCore._bluetoothManager.getConnectionState(_device.getPartnerInfo()._bleDevice, BluetoothProfile.GATT);
                if (state == 0) {
                    _workDiscover100 = 200;
                    return;
                }
                _workDiscover100++;
                if (MidiOne.isDebug) {
                    Log.e(AppConstant.MidiOneTag, "discover challenge " + state + " -> " + _workDiscover100);
                }

/*
                if (_bleCore.getBluetoothAdapter().isDiscovering()) { //ADDED 2025/4/25
                    _bleCore.getBluetoothAdapter().cancelDiscovery();
                }
 */
                gatt.discoverServices();
                if (true) {
                    try {
                        _bleCore.getThread().postDelayed(() -> {
                            startDiscovery(gatt);
                        }, 500);
                    } catch (Throwable ex) {
                        Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
                    }
                }
            }
        });
    }

    int _discoveryCount = 0;
    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
    @Override
    public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
        BlePartnerInfo info = _device.getPartnerInfo();
        RunQueueThread thread = _bleCore.getThread();

        if (status != BluetoothGatt.GATT_SUCCESS) {
            return;
        }
        ++_discoveryCount;
        //info._connectedGatt = gatt;
        _workDiscover100 = 200;
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag, "onServicesDiscovered " + gatt);
        }
        /*
        if (gatt != null) {
            info._gatt = gatt;
        }*/
        //stopFixScan();

        BluetoothGattService deviceInformationService = BleMidiDeviceUtils.getDeviceInformationService(gatt);
        if (deviceInformationService != null) {

            final BluetoothGattCharacteristic manufacturerCharacteristic = BleMidiDeviceUtils.getManufacturerCharacteristic(deviceInformationService);
            if (manufacturerCharacteristic != null) {
                thread.post(() -> {
                    thread.reserveExchange(1000);
                    gatt.readCharacteristic(manufacturerCharacteristic);
                });
            }

            final BluetoothGattCharacteristic modelCharacteristic = BleMidiDeviceUtils.getModelCharacteristic(deviceInformationService);
            if (modelCharacteristic != null) {
                thread.post(() -> {
                    thread.reserveExchange(1000);
                    // this calls onCharacteristicRead after completed
                    gatt.readCharacteristic(modelCharacteristic);
                });
            }
        }
        // if the app is running on Meta/Oculus, don't set the mtu
        try {
            boolean isOculusDevices = false;
            isOculusDevices |= "miramar".equals(Build.DEVICE);
            isOculusDevices |= "hollywood".equals(Build.DEVICE);
            isOculusDevices |= "eureka".equals(Build.DEVICE);
            //isOculusDevices |= "raspite".equals(Build.DEVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE || isOculusDevices || true) {
                // Android 14: the default MTU size set to 517
                // https://developer.android.com/about/versions/14/behavior-changes-all#mtu-set-to-517
                _device.setBufferSize(64);
            } else {
                thread.post(() -> {
                    thread.reserveExchange(1000);
                    gatt.requestMtu(64);
                });
            }
        } catch (Throwable ex) {
            Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
        }
        if (info._internalIn != null) {
            info._internalIn.stopParserAndThread();
            info._internalIn = null;
        }
        if (info._internalOut != null) {
            info._internalOut.stopTransmitterAndThread();
            info._internalOut = null;
        }
        thread.post(() -> {
            try {
                info._gattService = BleMidiDeviceUtils.getMidiService(_connectContext, gatt);
                if (info._gattService == null) {

                    info._gattService = BleMidiDeviceUtils.getMidiService(_connectContext, info._connectedGatt);
                    if (info._gattService == null) {
                        OneHelper.runOnUiThread(() -> {
                            Toast.makeText(_connectContext, "No MIDI service on " + gatt, Toast.LENGTH_SHORT).show();
                        });
                        info._whenCancel.run();
                        info._whenOpen = null;
                        return;
                    }
                }

                if (info._internalIn == null) {
                    InternalMidiInputDevice in= new InternalMidiInputDevice(gatt, new IOneDispatcher() {
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
                    in.configureBleProtocol(gatt);
                    in.startParser();
                    info._internalIn = in;
                }
            } catch (IllegalArgumentException ex) {
                Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
            }
        });

        thread.post(() -> {
            try {
                info._gattService = BleMidiDeviceUtils.getMidiService(_connectContext, gatt);
                if (info._gattService == null) {
                    info._whenCancel.run();
                    info._whenOpen = null;
                    return;
                }
                if (info._internalOut == null) {
                    InternalMidiOutputDevice out = new InternalMidiOutputDevice(gatt, info);
                    out.configureBleProtocol();
                    info._internalOut = out;
                }
                info._internalOut.startTransmitter();
            } catch (IllegalArgumentException ex) {
                Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            thread.post(() -> {
                // Set the connection priority to high(for low latency)
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
            });
        }
        thread.post(() -> {
            if (_device.getPartnerInfo()._whenOpen != null) {
                _device.getPartnerInfo()._whenOpen.run();
            }
        });
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
        BlePartnerInfo info = _device.getPartnerInfo();
        if (_terminate) {
            gatt.disconnect();
            return;
        }
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag,"onCharacteristicChanged " + OneHelper.dumpHex(value));
        }
        if (info._internalIn != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                info._internalIn.incomingData(value);
            } else {
                info._internalIn.incomingData(characteristic.getValue());
            }
        }
    }

    @Override
    @Deprecated
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        } else {
            BlePartnerInfo info = _device.getPartnerInfo();
            if (info._internalIn != null) {
                info._internalIn.incomingData(characteristic.getValue());
            }
        }
    }

    @Override
    @Deprecated
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                     int status) {
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag,"onCharacteristicRead d " + OneHelper.dumpHex(characteristic.getValue()));
        }
        byte[] value = characteristic.getValue();
        BlePartnerInfo info = _device.getPartnerInfo();
        try {
            if (BleUuidUtils.matches(characteristic.getUuid(), BleMidiDeviceUtils.CHARACTERISTIC_MANUFACTURER_NAME) && value != null && value.length > 0) {
                String manufacturer = new String(value);

                info._manufacture = manufacturer;
                if (MidiOne.isDebug) {
                    Log.e(AppConstant.MidiOneTag, "manufacture = " + manufacturer);
                }
            }
        } catch (Throwable ex) {
            Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
        }
        try {
            if (BleUuidUtils.matches(characteristic.getUuid(), BleMidiDeviceUtils.CHARACTERISTIC_MODEL_NUMBER) && value != null && value.length > 0) {
                String model = new String(value);

                info._model = model;
                if (MidiOne.isDebug) {
                    Log.e(AppConstant.MidiOneTag,"model = " + model);
                }
            }

        } catch (Throwable ex) {
            Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
        }
        _bleCore.getThread().doneExchange();
    }

    @Override
    public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag,"onCharacteristicRead 2 " + OneHelper.dumpHex(value));
        }
        BlePartnerInfo info = _device.getPartnerInfo();
        try {
            if (BleUuidUtils.matches(characteristic.getUuid(), BleMidiDeviceUtils.CHARACTERISTIC_MANUFACTURER_NAME) && value != null && value.length > 0) {
                String manufacturer = new String(value);

                info._manufacture = manufacturer;
                if (MidiOne.isDebug) {
                    Log.e(AppConstant.MidiOneTag,"manufacture = " + manufacturer);
                }
            }
        } catch (Throwable ex) {
            Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
        }
        try {
            if (BleUuidUtils.matches(characteristic.getUuid(), BleMidiDeviceUtils.CHARACTERISTIC_MODEL_NUMBER) && value != null && value.length > 0) {
                String model = new String(value);

                info._model = model;
                if (MidiOne.isDebug) {
                    Log.e(AppConstant.MidiOneTag,"model = " + model);
                }
            }

        } catch (Throwable ex) {
            Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
        }
        _bleCore.getThread().doneExchange();
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag,"onMtuChanged " + mtu);
        }
        if (_device != null) {
            _device.setBufferSize(mtu < 23 ? 20 : mtu - 3);
        }
        BlePartnerInfo info = _device.getPartnerInfo();
        _bleCore.getThread().doneExchange();
    }

    /**
     * Terminates callback
     */
    public void terminate() throws SecurityException {
        _workDiscover100 = -1;

        BlePartnerInfo info = _device.getPartnerInfo();
        _terminate = true;

        if (_bleCore._bluetoothAdapter.isDiscovering()) { //ADDED 2025/4/25
            _bleCore._bluetoothAdapter.cancelDiscovery();
        }
        if (info._internalIn != null) {
            info._internalIn.unconfigure();
            info._internalIn.stopParserAndThread();
            info._internalIn = null;
        }
        if (info._internalOut != null) {
            info._internalOut.unconfigure();
            info._internalOut.stopTransmitterAndThread();
            info._internalOut = null;
        }
        if (info._connectedGatt != null) {
            info._connectedGatt.disconnect();
        }
        if (info._connectedGatt != null) {
            info._connectedGatt.close();
            info._connectedGatt = null;
        }
    }

    public class InternalMidiInputDevice extends AbstractBleInputBase {
        public InternalMidiInputDevice(BluetoothGatt gatt, IOneDispatcher onParsed) throws SecurityException {
            super(_device.getPartnerInfo(), 0, onParsed);
        }

        public void configureBleProtocol(BluetoothGatt gatt) throws SecurityException {
            BlePartnerInfo info = _device.getPartnerInfo();
            _bleCore.getThread().post(() -> {

                if (info._gattService == null) {
                    List<UUID> uuidList = new ArrayList<>();
                    for (BluetoothGattService service : gatt.getServices()) {
                        uuidList.add(service.getUuid());
                    }
                    throw new IllegalArgumentException("MIDI GattService not found from '" + info._bleDevice.getAddress() + "'. Service UUIDs:" + Arrays.toString(uuidList.toArray()));
                }

                info._inputCharacteristic = BleMidiDeviceUtils.getMidiInputCharacteristic(_connectContext, info._gattService);
                if (info._inputCharacteristic == null) {
                    throw new IllegalArgumentException("MIDI Input GattCharacteristic not found. Service UUID:" + info._gattService.getUuid());
                }

                gatt.setCharacteristicNotification(info._inputCharacteristic, true);
                List<BluetoothGattDescriptor> descriptors = info._inputCharacteristic.getDescriptors();
                RunQueueThread thread =  _bleCore.getThread();
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    if (BleUuidUtils.matches(BleUuidUtils.fromShortValue(0x2902), descriptor.getUuid())) {
                        thread.post(() -> {
                            thread.reserveExchange(1000);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            } else {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                            }
                        });
                    }
                }
                thread.post(() -> {
                    if (info._inputCharacteristic != null) {
                        thread.reserveExchange(1000);
                        gatt.readCharacteristic(info._inputCharacteristic);
                    }
                });
            });
        }


        @NonNull
        public String getAddress() {
            return _device.getPartnerInfo()._bleDevice.getAddress();
        }
    }

    public final class InternalMidiOutputDevice extends AbstractBleOutputBase {
        @Override
        public void waitTillWrite() {
            try {
                synchronized (_waitLock) {
                    if (_waitingWrite++ != 0) { // if....? because while -> (afraid dead lock) (but truth)
                        _waitLock.wait(_device.getPartnerInfo().CONNECTION_INTERVAL);
                    }
                }
            }catch(InterruptedException ex) {

            }
        }


        public InternalMidiOutputDevice(BluetoothGatt gatt, BlePartnerInfo info) throws IllegalArgumentException, SecurityException {
            super(info._oneDevice, info, 0);

            info._outputCharacteristic = BleMidiDeviceUtils.getMidiOutputCharacteristic(_connectContext, info._gattService);
            if (info._outputCharacteristic == null) {
                throw new IllegalArgumentException("MIDI Output GattCharacteristic not found. Service UUID:" + info._gattService.getUuid());
            }
        }

        /**
         * Configure the device as BLE Central
         */
        public void configureBleProtocol() {
            _info._outputCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        }

        @Override
        public boolean transferData(@NonNull byte[] writeBuffer) throws SecurityException {
            try {
                BlePartnerInfo info = _device.getPartnerInfo();
                if (info._connectedGatt == null) {
                    return false;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    _waitingWrite = 1;
                    int result = info._connectedGatt.writeCharacteristic(info._outputCharacteristic, writeBuffer, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    return result == BluetoothStatusCodes.SUCCESS;
                } else {
                    _waitingWrite = 1;
                    info._outputCharacteristic.setValue(writeBuffer);
                    boolean result = info._connectedGatt.writeCharacteristic(info._outputCharacteristic);
                    if (MidiOne.isDebug) {
//                      //Log.e(AppConstant.TAG, "notify4 " + result + " " + MXUtil.dumpHex(writeBuffer));
                    }

                    return result;
                }
            } catch (Throwable ex) {
                Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
                // android.os.DeadObjectException will be thrown
                // ignore it
                _device.closeDevice();
                return false;
            }
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (MidiOne.isDebug) {
            //MessageLogger.getInstance().log("onDescriptorWrite " + OneHelper.dumpHex(descriptor.getValue()));
        }
        _bleCore.getThread().doneExchange();
    }

    boolean rererefreshDeviceCache(BluetoothGatt gatt) {
        try {
            Method m = gatt.getClass().getMethod("refresh");
            Boolean b = (Boolean) m.invoke(gatt);
            return b.booleanValue();
        } catch (Throwable ex) {
            Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
        }
        return false;
    }

    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag,"onPhyUpdate " + gatt + "," + status);
        }
    }

    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag,"onPhyRead " + gatt + "," + status);
        }
    }
    Object _waitLock = new Object();
    int _waitingWrite = 0;
    public void onCharacteristicWrite(
            BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        //if (MidiOne.isDebug || true) {
        //    Log.e(AppConstant.TAG, "onCharacteristicWrite " + gatt + "," + status + ", "+  OneHelper.dumpHex(characteristic.getValue()), new Throwable());
        //}
        synchronized (_waitLock) {
            _waitingWrite = 0;
            _waitLock.notifyAll();
        }
        //super.onCharacteristicWrite(gatt, characteristic, status);
    }

    @Deprecated
    public void onDescriptorRead(
            BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag,"onDescriptorRead1 " + gatt + "," + status);
        }
        //super.onDescriptorRead(gatt, descriptor, status);
    }

    public void onDescriptorRead(
            @NonNull BluetoothGatt gatt,
            @NonNull BluetoothGattDescriptor descriptor,
            int status,
            byte[] value) {
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag,"onDescriptorRead2 " + gatt + "," + status);
        }
    }

    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag,"onReliableWriteCompleted " + gatt + "," + status);
        }
    }

    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag,"onReadRemoteRssi " + gatt + ", " + rssi + "," + status);
        }
    }

    public void onConnectionUpdated(
            BluetoothGatt gatt, int interval, int latency, int timeout, int status) {
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag,"onConnectionUpdated " + status + " interval " + interval);
        }
        if (interval >= 1 && interval <= 50) {
            _device.getPartnerInfo().CONNECTION_INTERVAL = interval;
        }
    }

    public void onConnectionUpdated(BluetoothDevice device, int interval, int latency, int timeout, int status) {
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag,"onConnectionUpdated " + status + " interval " + interval);
        }
        if (interval >= 1 && interval <= 50) {
            _device.getPartnerInfo().CONNECTION_INTERVAL = interval;
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void onServiceChanged(BluetoothGatt gatt) {
        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag,"onServiceChanged " + gatt);
        }
        _device.getDeviceStatus().countIt(OneDeviceStatus.STATUS_LOST);
    }

    public void onSubrateChange(
            BluetoothGatt gatt,
            int subrateFactor,
            int latency,
            int contNu,
            int timeout,
            int status) {

        if (MidiOne.isDebug) {
            Log.e(AppConstant.MidiOneTag, "onSubrateChange ");
        }
    }
}
