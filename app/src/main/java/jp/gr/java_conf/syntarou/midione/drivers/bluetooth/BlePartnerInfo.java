package jp.gr.java_conf.syntarou.midione.drivers.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import androidx.annotation.RequiresPermission;

import jp.gr.java_conf.syntarou.midione.v1.OneDevice;

public class BlePartnerInfo {
    public int PACKET_MAX_SYSEX = 16;
    public int PACKET_MAX = 16; // 16 = Recommend for MIDI. 23 was large, 100+ was not good. (For communicate with some vendors BLE-MIDI gear)
    public long CONNECTION_INTERVAL = 33; // 33 = Recommend for MIDI, 30 times / 1 sec
    // onConnectionUpdated will update it (very very important)
    // and onCharacteristicWrite timing make waitTillWrite() can skip this
    // (for make less waiting time and for more speedy communicate) <- it seems work but actually I don't know

    public OneDevice _oneDevice;
    public BluetoothDevice _bleDevice;
    public BluetoothGatt _connectedGatt;

    public BluetoothGattCharacteristic _inputCharacteristic;
    public BluetoothGattCharacteristic _outputCharacteristic;

    public BluetoothGattService _gattService;
    public AbstractBleInputBase _internalIn;
    public AbstractBleOutputBase _internalOut;
    public String _manufacture;
    public String _model;
    public Runnable _whenOpen;
    public Runnable _whenCancel;

    public String _connectingAddress;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public String toString() {
        String name = "???";
        if (_bleDevice != null) {
            String c = _bleDevice.getName();
            if (c != null && c.length() > 0) {
                name = c;
            }
            else {
                String a = _bleDevice.getAddress();
                if (a != null && a.length() > 0) {
                    name = a;
                }
            }
        }
        return name;
    }
}
