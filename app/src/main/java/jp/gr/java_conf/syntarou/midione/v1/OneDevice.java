package jp.gr.java_conf.syntarou.midione.v1;

import androidx.annotation.NonNull;

import jp.gr.java_conf.syntarou.midione.drivers.bluetooth.OneDeviceBle;
import jp.gr.java_conf.syntarou.midione.drivers.bluetooth.peripheral.OneDevicePeripheral;
import jp.gr.java_conf.syntarou.midione.ui.userchoice.IUserChoiceElement;

public abstract class OneDevice implements Comparable<OneDevice>, IUserChoiceElement {
    public OneDevice(@NonNull OneDriver driver, @NonNull String name, @NonNull String uuid) {
        _driver = driver;
        _name = name;
        if (name == null) {
            throw new NullPointerException();
        }
        _uuid = uuid;
        _status = new OneDeviceStatus(this);
    }
    final OneDeviceStatus _status;
    public OneDriver getDriver() {
        return _driver;
    }

    protected String _name;
    protected OneDriver _driver;
    protected String _uuid;

    public void setName(String name) {
        _name = name;
    }

    public void openDevice() {
        if (getDeviceStatus().isConnected()) {
            return;
        }
        getDeviceStatus().countIt(OneDeviceStatus.STATUS_START_CONNECT);
        startOpenDeviceImpl(() -> {
            if (_listInput == null) {
                _listInput = new OneInput[countInput()];
            }
            if (_listOutput == null) {
                _listOutput = new OneOutput[countOutput()];
            }
            getDeviceStatus().countIt(OneDeviceStatus.STATUS_CONNECTED);
        }, () -> {
            if (this instanceof OneDeviceBle || this instanceof OneDevicePeripheral) {
                closeDevice();
                getDeviceStatus().countIt(OneDeviceStatus.STATUS_LOST);
            }
            else {
                getDeviceStatus().countIt(OneDeviceStatus.STATUS_JUST_AVAIL);
            }
        });
    }

    public void closeDevice() {
        startCloseDeviceImpl(() -> {
            _listInput = null;
            _listOutput = null;
            if (this instanceof OneDeviceBle || this instanceof OneDevicePeripheral) {
                getDeviceStatus().countIt(OneDeviceStatus.STATUS_LOST);
            }
            else {
                getDeviceStatus().countIt(OneDeviceStatus.STATUS_JUST_AVAIL);
            }
        });
    }
    protected OneOutput[] _listOutput = null;
    protected OneInput[] _listInput = null;
    public synchronized OneOutput getOutput(int track) {
        if (_listOutput == null) {
            _listOutput = new OneOutput[countOutput()];
        }
        if (_listOutput[track] == null) {
            OneOutput out = allocateOutput(track);
            _listOutput[track] = out;
        }
        return _listOutput[track];
    }

    public synchronized OneInput getInput(int track) {
        if (_listInput == null) {
            _listInput = new OneInput[countInput()];
        }
        if (_listInput[track] == null) {
            OneInput in = allocateInput(track);
            _listInput[track] = in;
        }
        return _listInput[track];
    }

    public String toString() {
        return _name;
    }

    public abstract int getSortOrder();
    protected abstract void startOpenDeviceImpl(Runnable whenSuccess, Runnable whenFail);
    protected abstract void startCloseDeviceImpl(Runnable whenDone);
    public OneDeviceStatus getDeviceStatus() {
        return _status;
    }
    public abstract int countOutput();
    public abstract int countInput();
    protected abstract OneOutput allocateOutput(int track);
    protected abstract OneInput allocateInput(int track);
    @Override
    public int compareTo(OneDevice x) {
        return _name.compareTo(x._name);
    }

    public int getNameRes() {
        return 0;
    }

    public int getSubLabel() {
        return 0;

    }

    public String getNameText() {
        return toString();
    }

    public String getSubLabelText() {
        return null;
    }
}