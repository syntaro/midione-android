package jp.gr.java_conf.syntarou.midione.drivers.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import jp.gr.java_conf.syntarou.midione.MidiOne;
import jp.gr.java_conf.syntarou.midione.v1.IOneDispatcher;
import jp.gr.java_conf.syntarou.midione.v1.OneDevice;
import jp.gr.java_conf.syntarou.midione.v1.OneDriver;

public class OneDriverBle extends OneDriver {
    boolean _usable = false;
    MidiOne _one;
    public OneDriverBle(Context context, MidiOne manager) {
        super("BLE", manager);
        _one = manager;
        _usable = false;
        try {
            _usable = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        }catch (Throwable ex) {

        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void startScanDevices(Context context) {
        if (!_usable) {
            return;
        }

        try {
            OneBleCore mab = MidiOne.getInstance().getOneBleCore();
            if (!mab.isBluetoothEnabled(context)) {
                mab.enableBluetooth((Activity) context);
                return;
            }
        } catch (Throwable ex) {
        }

        BleDevicePicker picker = new BleDevicePicker(_one.getOneBleCore());
        picker.launch(context);
    }

    @Override
    public void stopScanDevices(Context context) {

    }

    @Override
    public void startDriver(Context context) {
        /*
        if (_alreadyEnum) {
            return;
        }*/
        if (!_usable) {
            return;
        }
        _one.getOneBleCore().addAllBondedDevices(context);
    }

    IOneDispatcher _onRead = null;

    public OneDeviceBle addBluetoothDevice(@NonNull BluetoothDevice bleDevice) {
        OneDeviceBle found = null;
        String address = bleDevice.getAddress();
        if (found == null) {
            found = findByBle(bleDevice);
        }
        if (found == null) {
            found = findByAddress(address);
        }
        String name = bleDevice.getAddress();
        try {
            String text = bleDevice.getName();
            if (text != null && text.length() > 0) {
                name = text;
            }
        } catch (Throwable ex) {

        }

        if (found == null) {
            found = new OneDeviceBle(this, bleDevice, name);
            addDevice(found);
            found.openDevice();;
            return found;
        }

        boolean changed = false;
        if (name != null && !name.equals(found.getNameText())) {
            changed = true;
            found.setName(name);
        }
        if (bleDevice != null && found.getPartnerInfo()._bleDevice != bleDevice) {
            changed = true;
            found.getPartnerInfo()._bleDevice = bleDevice;
        }
        return found;
    }


    protected synchronized OneDeviceBle findByAddress(String address) {
        for (OneDevice seek : _listDevices) {
            OneDeviceBle ble = (OneDeviceBle) seek;
            if (address.equals(ble.getPartnerInfo()._bleDevice.getAddress())) {
                return ble;
            }
        }
        return null;
    }

    protected synchronized OneDeviceBle findByBle(BluetoothDevice device) {
        if (device != null) {
            for (OneDevice seek : _listDevices) {
                OneDeviceBle btle = (OneDeviceBle) seek;
                if (btle.getPartnerInfo()._bleDevice == device) {
                    return btle;
                }
            }
            OneDeviceBle btle = findByAddress(device.getAddress());
            if (btle != null) {
                btle.getPartnerInfo()._bleDevice = device;
                return btle;
            }
        }
        return null;
    }
}
