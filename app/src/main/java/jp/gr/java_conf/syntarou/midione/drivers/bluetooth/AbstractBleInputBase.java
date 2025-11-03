package jp.gr.java_conf.syntarou.midione.drivers.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import jp.gr.java_conf.syntarou.midione.v1.IOneDispatcher;
import jp.gr.java_conf.syntarou.midione.v1.OneInput;

public class AbstractBleInputBase extends OneInput {
    protected BluetoothDevice _bluetoothDevice;
    protected OneBleParser _midiParser = new OneBleParser();
    BlePartnerInfo _info;

    public AbstractBleInputBase(BlePartnerInfo info, int track, IOneDispatcher onParsed) {
        super(info._oneDevice, track);
        _info = info;
        _onParsed = onParsed;
        _midiParser.bindOnParsed(_onParsed);
    }

    @Override
    public void startParser() {
        _midiParser.start();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void stopParserAndThread() {
        _midiParser.stop();
    }

    @Override
    public int getNameRes() {
        return 0;
    }

    @Override
    public String getNameText() {
        String name = null;
        try {
            name = _info._bleDevice.getName();
            if (name != null && name.length() == 0) {
                name = null;
            }
        } catch (Throwable ex) {

        }
        if (name == null) {
            name = _info._bleDevice.getAddress();
        }
        return name != null ? name : "???";
    }

    @Override
    public boolean isParserRunning() {
        return _midiParser.isRunning();
    }

    public void incomingData(@NonNull byte[] data) {
        _midiParser.incommingData(data);
    }

    public String getAddress() {
        return _bluetoothDevice.getAddress();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void unconfigure() {
        if (_info._inputCharacteristic != null) {
            if (_info._connectedGatt != null) {
                _info._connectedGatt.setCharacteristicNotification(_info._inputCharacteristic, false);
            }
            _info._inputCharacteristic = null;
        }
    }
}
