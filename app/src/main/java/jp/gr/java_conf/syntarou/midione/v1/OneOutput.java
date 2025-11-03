package jp.gr.java_conf.syntarou.midione.v1;

import jp.gr.java_conf.syntarou.midione.ui.userchoice.IUserChoiceElement;

public abstract class OneOutput implements IOneDispatcher, IUserChoiceElement, Comparable<OneOutput> {
    boolean _transmitterRunning;
    OneDevice _device;
    int _cable;

    public OneOutput(OneDevice device, int track) {
        _device = device;
        _cable = track;
        _transmitterRunning = true;
    }

    @Override
    public abstract boolean dispatchOne(OneMessage one);

    @Override
    public abstract int getNameRes();

    @Override
    public abstract String getNameText();

    public String getDeviceAddress() {
        return null;
    }

    @Override
    public int getSubLabel() {
        return 0;
    }

    @Override
    public String getSubLabelText() {
        OneDeviceStatus counter = _device.getDeviceStatus();
        return "(" + getDriver()._prefix + ")" + counter.createSubLabelText();
    }

    public boolean isTransmitterRunning() {
        return _transmitterRunning;
    }

    public void startTransmitter() {
        _transmitterRunning = true;
        synchronized (this) {
            notifyAll();
        }
    }

    public void stopTransmitterAndThread() {
        _transmitterRunning = false;
        synchronized (this) {
            notifyAll();
        }
    }
    public abstract void onClose();

    public int getCable() { return _cable; }

    public OneDriver getDriver() { return _device._driver; }

    public OneDevice getDevice() { return _device; }

    @Override
    public int compareTo(OneOutput x) {
        int n = getDevice().compareTo(x.getDevice());
        if (n == 0) {
            return  _cable - x._cable;
        }
        return n;
    }
}
