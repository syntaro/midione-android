package jp.gr.java_conf.syntarou.midione.drivers.bluetooth.peripheral;

import jp.gr.java_conf.syntarou.midione.common.RunQueueThread;
import jp.gr.java_conf.syntarou.midione.MidiOne;
import jp.gr.java_conf.syntarou.midione.drivers.bluetooth.BlePartnerInfo;
import jp.gr.java_conf.syntarou.midione.v1.OneDevice;
import jp.gr.java_conf.syntarou.midione.v1.OneInput;
import jp.gr.java_conf.syntarou.midione.v1.OneMessage;
import jp.gr.java_conf.syntarou.midione.v1.OneOutput;

public class OneDevicePeripheral extends OneDevice {
    final BlePartnerInfo _partner;
    public BlePartnerInfo getPartnerInfo() {
        return _partner;
    }

    public OneDevicePeripheral(OneDriverPeripheral driver, String name) {
        super(driver, "BeBLE:" + name, "");
        _partner = new BlePartnerInfo();
    }

    public void ensureInput() {
        if (_listInput == null){
            _listInput = new OneInput[1];
        }
    }
    public void ensureOutput() {
        if (_listOutput == null){
            _listOutput = new OneOutput[1];
        }
    }
    @Override
    public int getSortOrder() {
        return 0;
    }

    @Override
    public int countOutput() {
        return 1;
    }

    @Override
    public int countInput() {
        return 1;
    }

    @Override
    protected OneOutput allocateOutput(int track) {
        OneOutput out = new OneOutput(this, track) {
            @Override
            public boolean dispatchOne(OneMessage one) {
                if (MidiOne.isDebug) {
                    //Log.e(AppConstant.TAG, "dispatchOne " + one + "->" + _partner._internalOut);
                }
                if (_partner._internalOut != null) {
                    _partner._internalOut.dispatchOne(one);
                }
                return true;
            }

            @Override
            public int getNameRes() {
                return 0;
            }

            @Override
            public String getNameText() {
                return OneDevicePeripheral.this.toString();
            }

            @Override
            public void onClose() {

            }
        };

        return out;
    }

    @Override
    protected OneInput allocateInput(int track) {
        OneInput oneInput = new OneInput(this, track) {
            @Override
            public int getNameRes() {
                return 0;
            }

            public String getNameText() {
                return OneDevicePeripheral.this.toString();
            }
        };
        return oneInput;
    }
    @Override
    protected void startOpenDeviceImpl(Runnable whenSuccess, Runnable whenFail) {
        OneDriverPeripheral driver = (OneDriverPeripheral) _driver;
        RunQueueThread thread = driver._thread;
        thread.post(() -> {
            driver.startScanDevices(driver._one.getMidiContext());
        });
    }

    @Override
    protected void startCloseDeviceImpl(Runnable whenDone) {
        ((OneDriverPeripheral)_driver).terminate();
        whenDone.run();
    }

    @Override
    public String toString() {
        return  "Ble2)" + _partner.toString();
    }
}