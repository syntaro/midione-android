package jp.gr.java_conf.syntarou.midione.drivers.android;

import android.content.Context;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import jp.gr.java_conf.syntarou.midione.MidiOne;
import jp.gr.java_conf.syntarou.midione.v1.OneDevice;
import jp.gr.java_conf.syntarou.midione.v1.OneDriver;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
public class OneDriverDefault extends OneDriver  {
    MidiManager _midiMan;
    MidiManager.DeviceCallback _callback;

    public OneDriverDefault(Context context, MidiOne manager) {
        super("And", manager);
    }

    boolean _alreadyEnum = false;

    public void registerMidiDeviceInfo(MidiDeviceInfo info) {
        String name = info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME);
        if (name == null) {
            name = "?";
        }
        OneDevice one = findDeviceInfoByUUID(name);
        if (one == null) {
            one = new OneDeviceDefault(this, name, name, info);
            addDevice(one);
        }
    }
    @Override
    public void startDriver(Context context) {
        if (_alreadyEnum) {
            return;
        }
        _midiMan = (MidiManager) context.getSystemService(Context.MIDI_SERVICE);
        Set<MidiDeviceInfo> infoList;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            infoList = _midiMan.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM);
        } else {
            infoList = new HashSet<>();
            Collections.addAll(infoList, _midiMan.getDevices());
        }

        for (MidiDeviceInfo device : infoList) {
            registerMidiDeviceInfo(device);
        }
        _callback = new MidiManager.DeviceCallback() {
            @Override
            public void onDeviceAdded(MidiDeviceInfo device) {
                registerMidiDeviceInfo(device);
            }

            @Override
            public void onDeviceRemoved(MidiDeviceInfo info) {
                String name = info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME);
                if (name != null) {
                    for (int i = 0; i < countDevices(); ++i) {
                        OneDevice dev = getDevice(i);
                        if (dev.getNameText().equals(name)) {
                            removeDevice(dev);
                            return;
                        }
                    }
                }
            }

            @Override
            public void onDeviceStatusChanged(MidiDeviceStatus status) {
                super.onDeviceStatusChanged(status);
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _midiMan.registerDeviceCallback(MidiManager.TRANSPORT_MIDI_BYTE_STREAM, new Handler(Looper.getMainLooper())::post, _callback);
        }
        else {
            _midiMan.registerDeviceCallback(_callback, new Handler(Looper.getMainLooper()));
        }
        _alreadyEnum = true;
    }

    public void startScanDevices(Context context) {

    }

    @Override
    public void terminateDriver() {
        super.terminateDriver();
        _midiMan.unregisterDeviceCallback(_callback);
    }

    @Override
    public void stopScanDevices(Context context) {
    }
}
