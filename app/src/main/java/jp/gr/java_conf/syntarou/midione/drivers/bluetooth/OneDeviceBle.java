package jp.gr.java_conf.syntarou.midione.drivers.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import jp.gr.java_conf.syntarou.midione.MidiOne;
import jp.gr.java_conf.syntarou.midione.v1.OneDevice;
import jp.gr.java_conf.syntarou.midione.v1.OneInput;
import jp.gr.java_conf.syntarou.midione.v1.OneMessage;
import jp.gr.java_conf.syntarou.midione.v1.OneOutput;

public class OneDeviceBle extends OneDevice {
    private BlePartnerInfo _partner;

    public BlePartnerInfo getPartnerInfo() {
        return _partner;
    }

    private BleCentralCallback _callback = null;
    OneBleCore _bleCore;

    public OneDeviceBle(@NonNull OneDriverBle driver, @NonNull BluetoothDevice bleDevice,@NonNull String name) {
        super(driver, name, bleDevice.getAddress());
        _partner = new BlePartnerInfo();
        _partner._oneDevice = this;
        _partner._bleDevice = bleDevice;
        _bleCore = driver._one.getOneBleCore();;
        _name = name;
    }

    public String toString() {
        return "Ble1)" + _partner.toString();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void startConnectGatt(Context context) {
        if (_callback == null) {
            _callback = new BleCentralCallback(context, _bleCore, this);
        }
        _callback._terminate = false;
        _bleCore.getThread().post(() -> {
            _callback.reconnect();
        });
    }
    public void setBufferSize(int size) {
        if (_partner._internalOut != null) {
            ((BleCentralCallback.InternalMidiOutputDevice) _partner._internalOut).setBufferSize(size);
        }
    }

    public int getSortOrder() {
        if (_partner._bleDevice == null) {
            return 100;
        } else {
            return 101;
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void startOpenDeviceImpl(Runnable whenOpen, Runnable whenCancel) {
        _partner._whenOpen = whenOpen;
        _partner._whenCancel = whenCancel;
        startConnectGatt(MidiOne.getInstance().getMidiContext());
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void startCloseDeviceImpl(Runnable whenClose) {
        _bleCore.getThread().post(() -> {
            if (_callback != null) {
                _callback.terminate();
                _callback = null;
            }
            if (whenClose != null) {
                whenClose.run();
            }
        });
    }

    @Override
    public int countOutput() {
        return 1;
    }

    @Override
    public int countInput() {

        return 1;
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    protected OneOutput allocateOutput(int track) {
        return new OneOutput(this, track) {
            @Override
            public boolean dispatchOne(OneMessage one) {
                if (_partner._internalOut != null) {
                    _partner._internalOut.dispatchOne(one);
                }
                return false;
            }

            @Override
            public int getNameRes() {
                return 0;
            }

            public String getNameText() {
                return OneDeviceBle.this.toString();
            }

            @Override
            public void onClose() {

            }
        };
    }

    @Override
    protected OneInput allocateInput(int track) {
        OneInput oneInput = new OneInput(this, track) {
            @Override
            public int getNameRes() {
                return 0;
            }

            @Override
            public String getNameText() {
                return OneDeviceBle.this.toString();
            }
        };
        return oneInput;
    }
}

