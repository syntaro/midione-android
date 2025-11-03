package jp.gr.java_conf.syntarou.midione.v1;

import androidx.annotation.NonNull;

import jp.gr.java_conf.syntarou.midione.MidiOne;
import jp.gr.java_conf.syntarou.midione.ui.userchoice.IUserChoiceElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class OneInput implements IUserChoiceElement, IOneDispatcher, Comparable<OneInput> {
    protected IOneDispatcher _onParsed;
    boolean _parserRunning;
    OneDevice _device;

    int _cable;

    public OneInput(OneDevice device, int track) {
        _cable = track;
        _device = device;
        _parserRunning = true;
        _onParsed = null;
    }

    public void bindOnParsed(IOneDispatcher onRead) {
        _onParsed = onRead;
    }

    public boolean isParserRunning() {
        return _parserRunning;
    }

    public void startParser() {
        _parserRunning = true;
        synchronized (this) {
            notifyAll();
        }
    }

    public void stopParserAndThread() {
        _parserRunning = false;

        synchronized (this) {
            notifyAll();
        }
    }

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
        return null;
    }
    public boolean dispatchShortMessage(int status, int data1, int data2) {
        OneMessage one = OneMessage.thisCodes(0, status, data1, data2);
        return dispatchOne(one);
    }

    public boolean dispatchLongMessage(byte[] data) {
        OneMessage one = OneMessage.thisBuffer(0, data);
        return dispatchOne(one);
    }

    @Override
    public boolean dispatchOne(OneMessage one) {
        if (one == null) {
            return  false;
        }
        one._messageSource = this;
        if (_onParsed != null) {
            return _onParsed.dispatchOne(one);
        }else {
            MidiOne.getInstance().startDispatchWithSplit(one);
        }
        return false;
    }

    public void onClose() {

    }

    public int getCable() { return _cable; }

    public OneDriver getDriver() { return _device._driver; }

    public OneDevice getDevice() { return _device; }

    @Override
    public @NonNull String toString() {
        return getNameText() + "/" + getSubLabelText();
    }
    List<OneOutput> _routingOutput = Collections.synchronizedList(new ArrayList<>());
    public List<OneOutput> getRoutingOutput() {
        return _routingOutput;
    }

    @Override
    public int compareTo(OneInput x) {
        int n = getDevice().compareTo(x.getDevice());
        if (n == 0) {
            return  _cable - x._cable;
        }
        return n;
    }
}
