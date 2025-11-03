package jp.gr.java_conf.syntarou.midione.drivers.android;

import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.util.Log;

import androidx.annotation.NonNull;

import jp.gr.java_conf.syntarou.midione.AppConstant;
import jp.gr.java_conf.syntarou.midione.v1.OneDevice;
import jp.gr.java_conf.syntarou.midione.v1.OneInput;
import jp.gr.java_conf.syntarou.midione.v1.OneMessage;
import jp.gr.java_conf.syntarou.midione.v1.OneOutput;

import java.io.IOException;

public class OneDeviceDefault extends OneDevice {
    MidiDeviceInfo _info;
    MidiDevice _connected;
    public OneDeviceDefault(@NonNull OneDriverDefault driver, @NonNull String name, @NonNull String uuid, MidiDeviceInfo info) {
        super(driver, name, uuid);
        _info = info;
        _connected = null;
    }

    MidiInputPort[] _listSystemOutput;
    MidiOutputPort[] _listSystemInput;

    @Override
    public int getSortOrder() {
        return 0;
    }

    @Override
    protected void startOpenDeviceImpl(Runnable whenSuccess, Runnable whenFail) {
        if (_connected != null) {
            return;
        }
        try {
            MidiManager manager = ((OneDriverDefault) _driver)._midiMan;
            manager.openDevice(_info, new MidiManager.OnDeviceOpenedListener() {
                @Override
                public void onDeviceOpened(MidiDevice device) {
                    try {
                        _connected = device;
                        _info = device.getInfo();
                        _listSystemInput = new MidiOutputPort[_info.getOutputPortCount()];
                        _listSystemOutput = new MidiInputPort[_info.getInputPortCount()];
                        _listInput = new OneInput[_info.getOutputPortCount()];
                        _listOutput = new OneOutput[_info.getInputPortCount()];
                        for (int i = 0; i < _info.getInputPortCount(); ++i) {
                            OneOutput out = getOutput(i);
                            _listSystemOutput[i] = device.openInputPort(i);
                        }
                        for (int i = 0; i < _info.getOutputPortCount(); ++i) {
                            OneInput in = getInput(i);
                            _listSystemInput[i] = device.openOutputPort(i);
                            if (_listSystemInput[i] != null) {
                                _listSystemInput[i].connect(new MidiReceiver() {
                                    @Override
                                    public void onSend(byte[] msg, int offset, int count, long timestamp) throws IOException {
                                    OneMessage one = OneMessage.thisPart(0, msg, offset, count);
                                    one._messageSource = in;
                                    in.dispatchOne(one);
                                    }
                                });
                            }
                        }
                        whenSuccess.run();
                    } catch (Throwable ex) {
                        Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
                        whenFail.run();
                    }
                }
            }, null);
        } catch (Throwable ex) {
            Log.e(AppConstant.MidiOneTag, ex.getMessage());
            whenFail.run();
        }
    }

    @Override
    protected void startCloseDeviceImpl(Runnable whenDone) {
        if (_connected == null) {
            return;
        }
        if (_listSystemInput != null) {
            for (MidiOutputPort seek : _listSystemInput) {
                try {
                    seek.close();
                }catch(IOException ex) {
                    Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
                }
            }
            _listSystemInput = null;
        }
        if (_listSystemOutput != null) {
            for (MidiInputPort seek : _listSystemOutput) {
                try {
                    seek.close();
                }catch(IOException ex) {
                    Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
                }
            }
            _listSystemOutput = null;
        }
        try {
            if (_connected != null) {
                _connected.close();
                _connected = null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        whenDone.run();
    }

    @Override
    public int countOutput() {
        return _info.getInputPortCount();
    }

    @Override
    public int countInput() {
        return _info.getOutputPortCount();
    }

    @Override
    protected OneOutput allocateOutput(int track) {
        return new OneOutput(this, track) {
            @Override
            public boolean dispatchOne(OneMessage one) {
                try {
                    if (_listSystemOutput != null && _listSystemOutput[track] != null) {
                        _listSystemOutput[track].send(one._data, 0, one._data.length);
                    }
                    return true;
                } catch (IOException ex) {
                }
                return false;
            }

            @Override
            public int getNameRes() {
                return 0;
            }

            @Override
            public String getNameText() {
                if (countOutput() >= 2) {
                    return _name + "(" + _info.getPorts()[track].getName() + ")";
                } else {
                    return _name;
                }
            }

            @Override
            public void onClose() {

            }
        };
    }

    @Override
    public OneInput allocateInput(int track) {
        OneInput oneInput = new OneInput(this, track) {

            @Override
            public int getNameRes() {
                return 0;
            }

            @Override
            public String getNameText() {
                if (countInput() >= 2) {
                    return _name + "(" + _info.getPorts()[track].getName() + ")";
                } else {
                    return _name;
                }
            }
        };
        return oneInput;
    }
}
