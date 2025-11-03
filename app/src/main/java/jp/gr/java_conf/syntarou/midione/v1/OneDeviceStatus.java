package jp.gr.java_conf.syntarou.midione.v1;

import jp.gr.java_conf.syntarou.midione.drivers.bluetooth.OneDeviceBle;
import jp.gr.java_conf.syntarou.midione.drivers.bluetooth.peripheral.OneDevicePeripheral;
import jp.gr.java_conf.syntarou.midione.ui.logview.MessageLogger;

import java.util.ArrayList;

public class OneDeviceStatus {
    public OneDeviceStatus(OneDevice device) {
        _driver = device.getDriver();
        _device = device;
        _status = STATUS_JUST_AVAIL;
    }
    OneDriver _driver;
    OneDevice _device;
    int _status;
    public int getStatus() {
        return _status;
    }
    public boolean isConnected() {
        boolean conn = (_status == STATUS_CONNECTED);
        return conn;
    }
    public static final int STATUS_LOST = 0;
    public static final int STATUS_JUST_AVAIL = 1;
    public static final int STATUS_START_CONNECT = 2;
    public static final int STATUS_CONNECTED = 3;
    public static final int STATUS_CANCEL_CONNECT = 4;
    public static final int ON_CONNECT_ERROR = 6;
    public static final int ON_TRANSFER_ERROR = 7;
    public static final int ON_RECEIVE_ERROR = 8;

    public void countIt(int event) {
        String message = "";
        if (_status == event) {
            return;
        }
        switch (event) {
            case STATUS_JUST_AVAIL:
                if (_status == STATUS_CONNECTED) {
                    _countDisconnected ++;
                }
                _status = event;
                message = "Avail";
                break;
            case STATUS_LOST:
                _status = event;
                message = "Lost";
                break;
            case STATUS_START_CONNECT:
                _status = event;
                message = "Start Connect";
                break;
            case STATUS_CONNECTED:
                _status = event;
                message = "Connected";
                ++_countConnected;
                break;
            case ON_CONNECT_ERROR:
                message = "Error Connect";
                _status = STATUS_CANCEL_CONNECT;
                ++_errCountConnect;
                break;
            case ON_TRANSFER_ERROR:
                message = "Error Transfer";
                ++_errCountTransfer;
                break;
            case ON_RECEIVE_ERROR:
                message = "Error Receive";
                ++_errCountReceive;
                break;
            default:
                return;
        }
        MessageLogger.getInstance().log("Done [" + message + "]  " + _device.getDeviceStatus() + ":" + _device._uuid);
        _driver._one.onDeviceStatusChanged(_device, event);
    }
    public int _countConnected;
    public int _countDisconnected;
    public int _errCountConnect;
    public int _errCountTransfer;
    public int _errCountReceive;
    public String createSubLabelText() {
        StringBuilder builder = new StringBuilder();
        String alive = isConnected() ? "Open" : "Closed";
        String addr = "";

        if (_device != null) {
            if (_device instanceof OneDeviceBle) {
                OneDeviceBle btle = (OneDeviceBle) _device;
                addr = btle.getPartnerInfo()._model + "(" + btle.getPartnerInfo()._bleDevice.getAddress() + ")";
            }
            if (_device instanceof OneDevicePeripheral) {
                OneDevicePeripheral btleP = (OneDevicePeripheral) _device;
                if (btleP.getPartnerInfo()._bleDevice != null) {
                    addr = btleP.getPartnerInfo()._model + "(" + btleP.getPartnerInfo()._bleDevice.getAddress() + ")";
                } else {
                    addr = btleP.getPartnerInfo()._model;
                }
            }
        }

        builder.append(alive);
        if (addr != null && addr.length() > 0) {
            builder.append(":");
            builder.append(addr);
        }
        ArrayList<String> errText = new ArrayList<>();
        if (_errCountConnect > 0) {
            errText.add("connect=" + _errCountConnect);
        }
        if (_errCountTransfer > 0) {
            errText.add("transfer=" + _errCountTransfer);
        }
        if (_errCountReceive > 0) {
            errText.add("receive=" + _errCountReceive);
        }
        if (errText.size() > 0) {
            builder.append("(error = ");
            builder.append(errText.toString());
            builder.append(")");
        }
        return builder.toString();
    }
}
