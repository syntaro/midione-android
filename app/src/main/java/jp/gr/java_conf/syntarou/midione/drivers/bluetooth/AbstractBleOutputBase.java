package jp.gr.java_conf.syntarou.midione.drivers.bluetooth;

import android.Manifest;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import jp.gr.java_conf.syntarou.midione.AppConstant;
import jp.gr.java_conf.syntarou.midione.MXMidiStatic;
import jp.gr.java_conf.syntarou.midione.common.MXQueue;
import jp.gr.java_conf.syntarou.midione.common.OneHelper;
import jp.gr.java_conf.syntarou.midione.common.RunQueueThread;
import jp.gr.java_conf.syntarou.midione.MidiOne;
import jp.gr.java_conf.syntarou.midione.v1.OneAbstractByteBuilder;
import jp.gr.java_conf.syntarou.midione.v1.OneDevice;
import jp.gr.java_conf.syntarou.midione.v1.OneDeviceStatus;
import jp.gr.java_conf.syntarou.midione.v1.OneMessage;
import jp.gr.java_conf.syntarou.midione.v1.OneOutput;
import jp.gr.java_conf.syntarou.midione.ui.logview.MessageLogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;


public abstract class AbstractBleOutputBase extends OneOutput {
    MXQueue<OneMessage> _queue = new MXQueue<>();
    boolean _transmitterStarted = false;
    BlePartnerInfo _info;

    RunQueueThread _thread;

    public AbstractBleOutputBase(OneDevice device, BlePartnerInfo info, int track) {
        super(device, track);
        _thread = MidiOne.getInstance().getOneBleCore().getThread();
        _info = info;
    }

    /*
     * Transfer data
     *
     * @param writeBuffer byte array to write
     * @return true if transfer succeed
     */
    protected abstract boolean transferData(@NonNull byte[] writeBuffer);

    public synchronized boolean transferStream(OneAbstractByteBuilder stream) {
        long start = System.currentTimeMillis();
        byte[] data2 = stream.toCachedBuffer();
        if (data2.length == 0) {
            return true;
        }
        while (!transferData(data2)) {
            OneDevice d = _info._oneDevice;
            if (d == null) {
                MessageLogger.getInstance().log("no device");
                return false;
            }
            OneHelper.Thread_sleep(66);
            if (System.currentTimeMillis() >= start + 300) {
                d.getDeviceStatus().countIt(OneDeviceStatus.ON_TRANSFER_ERROR);
                if (MidiOne.isDebug) {
                    MessageLogger.getInstance().log("err transfer " + d.getNameText() + " :" + OneHelper.dumpHex(data2));
                }
                getDevice().closeDevice();
                return false;
            }
        }
        return true;
    }

    /**
     * Obtains buffer size
     *
     * @return buffer size
     */
    int _transferBufferSize = 517;

    public int getBufferSize() {
        return _transferBufferSize;
    }

    public void setBufferSize(int bufferSize) {
        _transferBufferSize = bufferSize;
    }

    @NonNull

    OneAbstractByteBuilder _stream = null;
    public void dequeAndSend() {
        int packetMax = getBufferSize();
        if (packetMax > _info.PACKET_MAX) {
            packetMax = _info.PACKET_MAX;
        }
        if (_stream == null || _stream.getRawData().length > packetMax) {
            _stream = new OneAbstractByteBuilder(packetMax) {

                @Override
                public boolean append(OneMessage one) {
                    try {
                        if (size() == 0) {
                            if (size() + 2 + one._data.length >= _rawData.length) {
                                return false;
                            }
                            write((byte) (0x80 | ((one._tick >> 7) & 0x3f)));
                            write((byte) (0x80 | (one._tick & 0x7f)));
                            write(one._data, 0, one._data.length);
                            return true;
                        } else {
                            if (size() + 1 + one._data.length >= _rawData.length) {
                                return false;
                            }
                            write((byte) (0x80 | (one._tick & 0x7f)));
                            write(one._data, 0, one._data.length);
                            return true;
                        }
                    } catch (IOException ex) {
                        Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
                        return false;
                    }
                }
            };
            _stream.setAutoExtendable(false);//for META message
        }
        _stream.reset();
        while (_queue.isEmpty() == false) {
            if (getDevice().getDeviceStatus().isConnected() == false) {
                return;
            }
            OneMessage packet0 = _queue.pop();
            if (packet0 == null) {
                continue;
            }
            if (packet0._data == null || packet0._data.length == 0) {
                continue;
            }
            //Log.e("*****", "One " + packet0);

            int isLong = packet0._data[0] & 0xff;

            if (isLong == 0xf0 || isLong == 0xf7) {
                sendMidiSystemExclusive(packet0);
                _stream.reset();
                OneHelper.Thread_sleep(_info.CONNECTION_INTERVAL);
                continue;
            }

            if (_stream.append(packet0) == false) {
                if (packet0._data.length + 2 < packetMax) {
                    if (MidiOne.isDebug) {
                        Log.d(AppConstant.MidiOneTag, "packet back " + packetMax + " > " + packet0._data.length + "+2");
                    }
                    _queue.back(packet0);
                    continue;
                } else {
                    if (MidiOne.isDebug) {
                        Log.i(AppConstant.MidiOneTag, "packet too large " + packet0 + " size " + packet0._data.length + "+2 (limit " + packetMax);
                    }
                    continue;
                }
            }
            while (!_queue.isEmpty()) {
                OneMessage packet = _queue.pop();
                boolean sysex2 = (packet._data[0] & 0xff) == 0xf0;
                if (sysex2) {
                    _queue.back(packet);
                    break;
                }
                if (!_stream.append(packet)) {
                    _queue.back(packet);
                    break;
                }
            }
            //Log.e("**********", "stream = " + _stream.size());

            if (_stream.size() > 0) {
                /*
                if (_stream.size() == 5 && packet0.getCommand() == MXMidiStatic.COMMAND_CH_NOTEON) {
                    int ch = packet0.getChannel();
                    OneMessage noteOff = OneMessage.thisCodes(0, MXMidiStatic.COMMAND_CH_NOTEOFF + ch, packet0.getData1(), packet0.getData2());
                    noteOff._tick = packet0._tick;
                    packet0._tick = getTimestamp();
                    _stream.reset();
                    _stream.append(noteOff);
                    _stream.append(packet0);
                }
                if (_stream.size() == 5 && packet0.getCommand() == MXMidiStatic.COMMAND_CH_NOTEOFF) {
                    OneMessage revenge = OneMessage.thisCodes(0, packet0.getStatus(), packet0.getData1(), packet0.getData2());
                    revenge._tick = getTimestamp();
                    _stream.append(revenge);
                }
                if (_stream.size() == 4 && packet0.getCommand() == MXMidiStatic.COMMAND_CH_PROGRAMCHANGE) {
                    OneMessage revenge = OneMessage.thisCodes(0, packet0.getStatus(), packet0.getData1(), packet0.getData2());
                    revenge._tick = getTimestamp();
                    _stream.append(revenge);
                }
                 */
                if (transferStream(_stream)) {
                    waitTillWrite();
                }
                _stream.reset();
            }
        }
    }

    /**
     * Starts using the device
     */
    public synchronized void startTransmitter() {
        _transmitterStarted = true;
        notifyAll();
        //_lastActiveSensing = 0;
    }

    /**
     * Stops using the device
     */
    public void stopTransmitterAndThread() {
        _transmitterStarted = false;
        if (_sensingThread != null) {
            _sensingThread.interrupt();
            _sensingThread = null;
        }
    }

    /**
     * Terminates the device instance
     */
    public synchronized final void terminate() {
    }

    public boolean isRunning() {
        return _transmitterStarted;
    }

    @Override
    public boolean dispatchOne(OneMessage one) {
        if (one._data.length > 0 && _transmitterStarted)  {
            synchronized (_queue) {
                one._tick = getTimestamp();
                _queue.push(one);
                /*
                OneMessage bulk = OneMessage.thisCodes(0, MXMidiStatic.COMMAND_ACTIVESENSING, 0, 0);
                bulk._tick = getTimestamp();
                _queue.push(bulk);*/
            }
            _thread.postIfNotLastPost(this::dequeAndSend);
            if (_sensingThread == null) {
                //sendActiveSensing();
            }
        }
        return true;
    }

    //long _lastActiveSensing = 0;
    Thread _sensingThread = null;
    public synchronized void sendActiveSensing() {
        if (_sensingThread == null) {
            OneMessage activeSensing = OneMessage.thisCodes(0, MXMidiStatic.COMMAND_MIDITIMECODE, 1, 0);
            _sensingThread = OneHelper.startThread(() -> {
                try {
                    while( isRunning()) {
                        synchronized (_sensingThread) {
                            _sensingThread.wait(50);
                        }
                        if (_transmitterStarted) {
                            dispatchOne(activeSensing);
                        }
                        else {
                            break;
                        }
                        Log.e(AppConstant.MidiOneTag, "ActiveSensing");
                    }
                } catch (InterruptedException e) {
                } finally {
                    _sensingThread = null;
                }
            });
        }
    }

    long pastTimestamp = -1;

    public int getTimestamp1st(long timestamp) {
        int forskip1 = (int) (0x80 | (timestamp & 0x7f)) & 0xff;
        return forskip1;
    }

    public int getTimestamp2nd(long timestamp) {
        int forskip2 = (int) (0x80 | ((timestamp >> 7) & 0x3f)) & 0xff;
        return forskip2;
    }

    protected long getTimestamp() {
        if (true) {
            return 0;
        }
        long milliseconds = System.currentTimeMillis() % 8192;
        if (pastTimestamp >= milliseconds) {
            milliseconds = pastTimestamp + 1;
        }
        pastTimestamp = milliseconds;

        int max = 0x80 * 0x40;
        int skip = 0x40 + 1;
        long timestamp = milliseconds & (max - skip);

        int forskip1 = getTimestamp1st(timestamp);
        int forskip2 = getTimestamp2nd(timestamp);

        if (forskip1 >= 0xf0) {
            pastTimestamp++;
            return getTimestamp();
        }
        if (forskip2 >= 0xf0) {
            pastTimestamp += 1 << 7;
            return getTimestamp();
        }
        return timestamp;
    }

    protected boolean sendMidiSystemExclusive(OneMessage data) {
        int bufferSize = getBufferSize();
        if (bufferSize >= _info.PACKET_MAX_SYSEX) {
            bufferSize = _info.PACKET_MAX_SYSEX;
        }
        OneAbstractByteBuilder sysexStream = new OneAbstractByteBuilder(bufferSize) {
            @Override
            public boolean append(OneMessage one) {
                return false;
            }
        };

        long timestamp = data._tick;
        ByteArrayInputStream in = new ByteArrayInputStream(data._data);

        sysexStream.reset();
        do {
            timestamp++;
        } while (getTimestamp1st(timestamp) == MXMidiStatic.COMMAND_SYSEX_END);
        boolean first = true;


        while (true) {
            if (first) {
                sysexStream.tryWrite(getTimestamp2nd(timestamp));
                sysexStream.tryWrite(getTimestamp1st(timestamp));
                first = false;
            } else {
                sysexStream.tryWrite(getTimestamp1st(timestamp));
            }

            int ch = 0;
            while (sysexStream.countSpace() >= 2) {
                ch = in.read();
                if (ch < 0 || ch == 0xf7) {
                    break;
                }
                sysexStream.tryWrite(ch);
            }
            if (in.available() >= 1 && sysexStream.countSpace() >= 1) {
                ch = in.read();
                sysexStream.tryWrite(ch);
            }
            if (sysexStream.size() <= 2) {
                break;
            }
            if (ch == 0xf7) {
                sysexStream.tryWrite(getTimestamp1st(timestamp));
                sysexStream.tryWrite(0xf7);
            }

            if (transferStream(sysexStream) == false) {
                break;
            }
            waitTillWrite();
            sysexStream.reset();
        }
        return true;
    }


    @Override
    public int getNameRes() {
        return 0;
    }

    @Override
    public String getNameText() {
        return "";
    }

    @Override
    public void onClose() {

    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void unconfigure() {
        if (_info._outputCharacteristic != null) {
            _info._connectedGatt.setCharacteristicNotification(_info._outputCharacteristic, false);
            _info._outputCharacteristic = null;
        }
    }

    public abstract void waitTillWrite();
}
