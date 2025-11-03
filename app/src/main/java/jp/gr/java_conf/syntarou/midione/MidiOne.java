package jp.gr.java_conf.syntarou.midione;

import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import jp.gr.java_conf.syntarou.midione.common.MXQueue;
import jp.gr.java_conf.syntarou.midione.common.OneHelper;
import jp.gr.java_conf.syntarou.midione.drivers.android.OneDriverDefault;
import jp.gr.java_conf.syntarou.midione.drivers.bluetooth.OneBleCore;
import jp.gr.java_conf.syntarou.midione.drivers.bluetooth.OneDriverBle;
import jp.gr.java_conf.syntarou.midione.drivers.bluetooth.peripheral.OneDriverPeripheral;
import jp.gr.java_conf.syntarou.midione.v1.IOneConnectionListener;
import jp.gr.java_conf.syntarou.midione.v1.IOneDispatcher;
import jp.gr.java_conf.syntarou.midione.v1.OneDevice;
import jp.gr.java_conf.syntarou.midione.v1.OneDeviceStatus;
import jp.gr.java_conf.syntarou.midione.v1.OneDriver;
import jp.gr.java_conf.syntarou.midione.v1.OneMessage;
import jp.gr.java_conf.syntarou.midione.v1.OneMessageFactory;
import jp.gr.java_conf.syntarou.midione.v1.OneOutput;

import java.util.ArrayList;
import java.util.LinkedList;

/*
 * Copyright 2023- Syntarou YOSHIDA.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */



public class MidiOne implements IOneConnectionListener {

    Context _midiContext;

    public Context getMidiContext() {
        return _midiContext;
    }
    private Context createMidiContext(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context = context.getApplicationContext();
            context =  context.createAttributionContext("audioPlayback");
        }
        else {
            context = context.getApplicationContext();
        }
        _midiContext = context;
        return context;
    }

    public static final boolean isDebug = true;

    static MidiOne _instance = null;

    public static synchronized MidiOne getInstance() {
        if (_instance == null) {
            _instance = new MidiOne();
        }
        return _instance;
    }

    public static ParcelUuid MIDI_SERVICE = ParcelUuid.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700");

    MidiOne() {
    }
    OneDriverDefault _driverAndroid;
    OneDriverBle _driverBle;
    OneDriverPeripheral _driverBlePeripheral;
    OneBleCore _oneBle;

    public OneDriverBle getDriverBluetooth() {
        return _driverBle;
    }

    public OneDriverPeripheral getDriverPeripheral() {
        return _driverBlePeripheral;
    }


    public OneBleCore getOneBleCore() {
        return _oneBle;
    }
    boolean _initDone = false;

    public void fullInitialize(Context activity) {
        if (_initDone) {
            return;
        }
        _initDone = true;
        Context midiContext = createMidiContext(activity);

        _listDriver = new ArrayList<>();

        try {
            _driverAndroid = new OneDriverDefault(midiContext, this);
            installDriver(_driverAndroid);
        } catch (Throwable ex) {
            Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
        }
        try {
            _oneBle = new OneBleCore(this);
            _driverBle = new OneDriverBle(midiContext, this);
            _driverBlePeripheral = new OneDriverPeripheral(this);
            installDriver(_driverBle);
            installDriver(_driverBlePeripheral);
        } catch (Throwable ex) {
            Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
        }

        OneHelper.runOnUiThread(() -> {
            MidiOne.getInstance().startAllDrivers(midiContext);
        } , 500);
    }

    public ArrayList<OneDevice> listAllDevices() {
        ArrayList<OneDevice> result = new ArrayList<>();

        if (_listDriver == null) {
            return result;
        }

        for (OneDriver seek : _listDriver) {
            ArrayList<OneDevice> segment = new ArrayList<>();
            int cnt = seek.countDevices();
            for (int x = 0; x < cnt; ++x) {
                segment.add(seek.getDevice(x));
            }
            segment.sort((o1, o2) -> {
                int s1 = o1.getSortOrder();
                int s2 = o2.getSortOrder();
                if (s1 == s2) {
                    return o1.getNameText().compareTo(o2.getNameText());
                }
                return (s1 < s2) ? -1 : 1;
            });
            result.addAll(segment);
        }
        return result;
    }

    static boolean _useInfo = true;

    public void stopAllScan() {
        /*
        Log.e(AppConstant.TAG, "stopAllScan");
        synchronized (_installedServices) {
            for (MihMidiDriver driver : _installedServices) {
                Log.e(AppConstant.TAG, "stopAllScan1 - " + driver._prefix);
                //driver.stopDeepScan(_context);
            }
        }
        Log.e(AppConstant.TAG, "stopAllScan - 2");
         */
    }

    public void startAllDrivers(Context midiContext) {
        OneDriver[] copy;
        synchronized (this) {
            copy = new OneDriver[_listDriver.size()];
            _listDriver.toArray(copy);
        }
        for (OneDriver seek : copy) {
            try {
                seek.startDriver(midiContext);
            } catch (Throwable ex) {
                Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
            }
        }
    }

    public void stopAllDrivers() {
        for (OneDevice device : listAllDevices()) {
            try {
                device.closeDevice();
            }catch (Throwable ex) {
                Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
            }
        }
    }

    final LinkedList<IOneConnectionListener> _listeners = new LinkedList<>();
    IOneConnectionListener[] _dispatch;

    public synchronized void addConnectionListener(IOneConnectionListener listener) {
        if (_listeners.contains(listener)) {
            return;
        }
        _listeners.add(listener);
        _dispatch = null;
    }

    public synchronized void removeConnectionListener(IOneConnectionListener listener) {
        _listeners.remove(listener);
        _dispatch = null;
    }

    public void terminate() {
        stopThread();
        stopAllDrivers();
        _midiContext = null;
        _instance = null;
    }

    IOneDispatcher _next;/*
    MidiKeyboardView _keyboard = null;

    public void setKeyboard(MidiKeyboardView view) {
        _keyboard = view;
    }*/
    int lengthOfMessage(byte[] data, int offset, int count, int pos) {
        if (pos >= count) {
            return -1;
        }
        int ch = data[offset + pos] & 0xff;
        if (ch == 0xff) {
            int type = data[offset + pos + 1] & 0xff;
            int bodylen = data[offset + pos + 2] & 0xff;
            int totallen = 3 + bodylen;
            return totallen;
        }
        if (ch == 0xf0) {
            for (int i = pos + 1; i < count; ++i) {
                int ch2 = data[offset + i] & 0xff;
                if (ch2 == 0xf7) {
                    return i + 1 - pos;
                }
            }
            return count - pos;
        }
        return OneMessageFactory.getSuggestedLength(ch);
    }

    public synchronized boolean startDispatchWithSplit(OneMessage one) {
        try {
            byte[] data = one._data;
            int count = data.length;
            int start = 0;
            while (true) {
                int len = lengthOfMessage(data, 0, count, start);
                if (start == 0 && len == data.length) {
                    startDispatch(one);
                    break;
                }
                //Log.e(AppConstant.TAG, "length  " + len);
                if (len == 0) {
                    start++;
                    continue;
                }
                if (len < 0) {
                    break;
                }
                OneMessage seg = OneMessage.thisPart(one._tick, data, start, len);
                seg._messageSource = one._messageSource;
                if (seg.isMetaTempo()) {
                    startDispatch(seg);
                }
                else if (seg.isMetaMessage()) {
                    //Log.e(AppConstant.TAG, "Meta " + seg +" -> ignore");
                }
                else {
                    startDispatch(seg);
                }
                start += len;
            }
        } catch (Throwable ex) {
            Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
            return false;
        }
        return true;
    }

    public synchronized boolean startDispatch(OneMessage one) {
        int command = one.getCommand();
        if (command == MXMidiStatic.COMMAND_ACTIVESENSING
                || command == MXMidiStatic.COMMAND_MIDITIMECODE
                || command == MXMidiStatic.COMMAND_TRANSPORT_MIDICLOCK) {
            if (MidiOne.isDebug) {
                //Log.e(AppConstant.TAG, "quiet " + message);
            }
            return false;
        }
        tossThread(one);

        /*

        one._loggingDate = System.currentTimeMillis();
        MessageLogger.getInstance().log(one);
        return _next.dispatchOne(one);

         */
        return true;
    }

    ArrayList<OneDriver> _listDriver;
    public synchronized void installDriver(OneDriver driver) {
        if (_listDriver.contains(driver)) {
            return;
        }
        _listDriver.add(driver);
    }
    @Override
    public void onDeviceStatusChanged(OneDevice device, int event) {
        synchronized (_listeners) {
            if (_dispatch == null) {
                _dispatch = new IOneConnectionListener[_listeners.size()];
                _listeners.toArray(_dispatch);
            }
        }
        if (event == OneDeviceStatus.STATUS_LOST) {
            try {
                device.closeDevice();
            }catch (Throwable ex) {
                Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
            }
            device.getDriver().removeDevice(device);
        }
        for (IOneConnectionListener seek : _dispatch) {
            seek.onDeviceStatusChanged(device, event);
        }
    }

    public OneOutput findOutput(String driver, String device, int cable) {
        if (driver == null || device == null || cable < 0) {
            return null;
        }
        for (OneDriver seekDriver : _listDriver) {
            if (seekDriver.getPrefix().equalsIgnoreCase(driver)) {
                for (OneDevice seekDevice : seekDriver._one.listAllDevices()) {
                    if (seekDevice.getNameText().equalsIgnoreCase(device)) {
                        if (seekDevice.countOutput() < cable) {
                            return seekDevice.getOutput(cable);
                        }
                    }
                }
            }
        }
        return null;
    }

    Thread _thread;
    boolean _breakThread = false;

    MXQueue<OneMessage> _queue = new MXQueue<>();
    public synchronized void tossThread(OneMessage one) {
        if (_thread == null) {
            _breakThread = false;
            _thread = OneHelper.startThread(this::infinityLoop);
        }
        _queue.push(one);
    }

    public synchronized void stopThread() {
        _breakThread = true;
        while (_thread != null) {
            OneHelper.Thread_sleep(100);
        }
    }
    public void infinityLoop() {
        try {
            try {
                Thread.currentThread().setPriority(8);
            }
            catch (Throwable ex) {

            }
            while (!_breakThread) {
                OneMessage one = _queue.pop();
                if (one != null) {
                    one._loggingDate = System.currentTimeMillis();
                    //MessageLogger.getInstance().log(one);
                    _next.dispatchOne(one);
                }
            }
        }finally {
            _thread = null;
        }
    }
}
