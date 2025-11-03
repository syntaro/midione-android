package jp.gr.java_conf.syntarou.midione.drivers.bluetooth;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import jp.gr.java_conf.syntarou.midione.AppConstant;
import jp.gr.java_conf.syntarou.midione.common.MXQueue;
import jp.gr.java_conf.syntarou.midione.common.OneHelper;
import jp.gr.java_conf.syntarou.midione.MidiOne;
import jp.gr.java_conf.syntarou.midione.v1.IOneDispatcher;
import jp.gr.java_conf.syntarou.midione.v1.OneMessage;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class OneBleParser {
    // for Timestamp
    private static final int MAX_TIMESTAMP = 8192;
    private static final int BUFFER_LENGTH_MILLIS = 50;
    private int lastTimestamp;
    private long lastTimestampRecorded = 0;
    private int zeroTimestampCount = 0;
    private Boolean isTimestampAlwaysZero = null;

    private IOneDispatcher _receiver = null;

    private final EventDequeueRunnable eventDequeueRunnable;
    private Thread _thread = null;

    private volatile boolean isRunning = false;
    private volatile boolean isTerminated = false;

    public OneBleParser() {

        eventDequeueRunnable = new EventDequeueRunnable();
    }

    public void bindOnParsed(@Nullable IOneDispatcher mihStream) {
        _receiver = mihStream;
    }

    /**
     * Stops the internal Thread
     */
    public void start() {
        if (isTerminated) {
            return;
        }
        if (_thread == null || _thread.isAlive() == false) {
            _thread = OneHelper.startThread(eventDequeueRunnable);
            while (!_thread.isAlive()) {
                synchronized (_queue) {
                    _queue.notifyAll();
                }
            }
        }
        isRunning = true;
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Stops the internal Thread
     */
    public void stop() {
        isRunning = false;
        if (eventDequeueRunnable != null) {
            synchronized (_queue) {
                _queue.notifyAll();
            }
        }
    }

    /**
     * Stops the internal Thread
     */
    public void terminate() {
        isTerminated = true;
        isRunning = false;
        synchronized (_queue) {
            _queue.notifyAll();
        }
    }

    /**
     * {@link Runnable} with MIDI event data, and firing timing
     */
    private class MidiEventWithTiming {
        private static final int INVALID = -1;

        private final long timing;
        private final byte[] array;

        /**
         * Calculate `time to wait` for the event's timestamp
         *
         * @param timestamp the event's timestamp
         * @return time to wait
         */
        private long calculateEventFireTime(final int timestamp) {
            final long currentTimeMillis = System.currentTimeMillis();

            // checks timestamp value is always zero
            if (isTimestampAlwaysZero != null) {
                if (isTimestampAlwaysZero) {
                    if (timestamp != 0) {
                        // timestamp comes with non-zero. prevent misdetection
                        isTimestampAlwaysZero = false;
                        zeroTimestampCount = 0;
                        lastTimestampRecorded = 0;
                    } else {
                        // event fires immediately
                        return currentTimeMillis;
                    }
                } else {
                    if (timestamp == 0) {
                        // recheck timestamp value on next time
                        isTimestampAlwaysZero = null;
                        zeroTimestampCount = 0;
                        // event fires immediately
                        return currentTimeMillis;
                    }
                }
            } else {
                if (timestamp == 0) {
                    if (zeroTimestampCount >= 3) {
                        // decides timestamp is always zero
                        isTimestampAlwaysZero = true;
                    } else {
                        zeroTimestampCount++;
                    }
                    // event fires immediately
                    return currentTimeMillis;
                } else {
                    isTimestampAlwaysZero = false;
                    zeroTimestampCount = 0;
                    lastTimestampRecorded = 0;
                }
            }

            if (lastTimestampRecorded == 0) {
                // first time: event fires immediately
                lastTimestamp = timestamp;
                lastTimestampRecorded = currentTimeMillis;
                return currentTimeMillis;
            }

            if (currentTimeMillis - lastTimestampRecorded >= MAX_TIMESTAMP) {
                // the event comes after long pause
                lastTimestamp = timestamp;
                lastTimestampRecorded = currentTimeMillis;
                return currentTimeMillis;
            }

            final long elapsedRealtime = currentTimeMillis - lastTimestampRecorded;
            // realTimestampPeriod: how many times MAX_TIMESTAMP passed
            long realTimestampPeriod = (lastTimestamp + elapsedRealtime) / MAX_TIMESTAMP;
            if (realTimestampPeriod > 0 && timestamp > 7000) {
                realTimestampPeriod--;
            }
            final long lastTimestampStarted = lastTimestampRecorded - lastTimestamp;
            // result: time to wait
            final long result = BUFFER_LENGTH_MILLIS // buffer
                    + lastTimestampStarted + realTimestampPeriod * MAX_TIMESTAMP + timestamp // time to fire event
                    - currentTimeMillis; // current time

            lastTimestamp = timestamp;
            lastTimestampRecorded = currentTimeMillis;
            return result;
        }

        /**
         * Constructor with no arguments
         *
         * @param timestamp BLE MIDI timestamp
         */
        MidiEventWithTiming(int timestamp) {
            timing = calculateEventFireTime(timestamp);
            array = new byte[0];
        }

        /**
         * Constructor with 1 argument
         *
         * @param arg1      argument 1
         * @param timestamp BLE MIDI timestamp
         */
        MidiEventWithTiming(int timestamp, int arg1) {
            timing = calculateEventFireTime(timestamp);
            array = new byte[]{
                    (byte) arg1
            };
        }

        /**
         * Constructor with 2 arguments
         *
         * @param arg1      argument 1
         * @param arg2      argument 2
         * @param timestamp BLE MIDI timestamp
         */
        MidiEventWithTiming(int timestamp, int arg1, int arg2) {
            timing = calculateEventFireTime(timestamp);
            array = new byte[]{
                    (byte) arg1,
                    (byte) arg2
            };
        }

        /**
         * Constructor with 3 arguments
         *
         * @param arg1      argument 1
         * @param arg2      argument 2
         * @param arg3      argument 3
         * @param timestamp BLE MIDI timestamp
         */
        MidiEventWithTiming(int timestamp, int arg1, int arg2, int arg3) {
            timing = calculateEventFireTime(timestamp);
            array = new byte[]{
                    (byte) arg1,
                    (byte) arg2,
                    (byte) arg3
            };
        }

        /**
         * Constructor with array
         *
         * @param array     data
         * @param timestamp BLE MIDI timestamp
         */
        MidiEventWithTiming(int timestamp, @NonNull byte[] array) {
            timing = calculateEventFireTime(timestamp);
            this.array = array;
        }

        MidiEventWithTiming(int timestamp, @NonNull byte[] array, int offset, int length) {
            timing = calculateEventFireTime(timestamp);
            if (offset == 0 && length == array.length) {
                this.array = array;
            } else {
                byte[] copy = new byte[length];
                for (int i = 0; i < copy.length; ++i) {
                    copy[i] = array[offset + i];
                }
                this.array = copy;
            }
        }

        public long getTiming() {
            return timing;
        }

        public byte[] getArray() {
            return array;
        }
    }

    public int getMessageLength(int midiEvent) {
        switch (midiEvent & 0xf0) {
            case 0xf0: {
                switch (midiEvent) {
                    case 0xf0: //sysex
                    case 0xf7: //sysex special
                        return -1;

                    case 0xf1: //midi time code
                    case 0xf3: //song select
                        return 2;

                    case 0xf2: //song position
                        return 3;

                    case 0xf6: //tune request
                    case 0xf8: //timeing clock
                    case 0xfa: //start
                    case 0xfb: //continue
                    case 0xfc: //stop
                    case 0xfe: //active sencing
                        return 1;

                    case 0xff: //system reset
                        return -2;
                }
            }
            return 1;
            case 0x80:
            case 0x90:
            case 0xa0:
            case 0xb0:
            case 0xe0:
                return 3;

            case 0xc0: // program change
            case 0xd0: // channel after-touch
                return 2;
        }
        return 0;
    }

    MABStreamForParser _stream = new MABStreamForParser();
    ByteArrayOutputStream _sysex = null;

    public void getTillSysexEnd() {
        if (_sysex == null) {
            return;
        }
        while (_stream.leftSize() > 0) {
            int ch = _stream.read();
            if (ch < 0) {
                break;
            }
            if ((ch & 0x80) == 0) {
                _sysex.write(ch);
                continue;
            }
            int ch2 = _stream.peek(0);
            if (ch2 == 0xf7) {
                //_sysex.write(ch);
                _sysex.write(ch2);
                _stream.read();
            } else if (ch2 >= 0) {
                _sysex.write(ch);
            }
            //flush sysex
            byte[] copy = _sysex.toByteArray();
            addEventToQueue(new MidiEventWithTiming(_stream.lastTimestamp(), copy, 0, copy.length));
            _sysex = null;
            break;
        }
    }

    int metaSkip = 0;

    /**
     * Updates incoming data
     *
     * @param data incoming data
     */
    public void incommingData(@NonNull byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        synchronized (_queue) {
            _stream.set(data, 0, data.length);

            if (_sysex != null) {
                getTillSysexEnd();
            }
            while (_stream.leftSize() > 0) {
                int timestamp = _stream.readTimestamp();
                if (metaSkip > 0) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    while (metaSkip > 0 && _stream.leftSize() > 0) {
                        out.write(_stream.read());
                        metaSkip--;
                    }
                    if (_stream.leftSize() == 0) {
                        break;
                    }
                    continue;
                }

                int command = _stream.readCommand();
                int length = getMessageLength(command);
                int data1, data2;


                switch (length) {
                    case 0:
                        continue;
                    case 1:
                        addEventToQueue(new MidiEventWithTiming(timestamp, command));
                        continue;
                    case 2:
                        if (_stream.isNextSmall(0)) {
                            data1 = _stream.read();
                            addEventToQueue(new MidiEventWithTiming(timestamp, command, data1));
                        } else {
                            //Log.e(AppConstant.TAG, "Unknown Data2 " + _stream._pos  +" of " + _stream.peek(0) + " -> " + _stream.isNextSmall(0));
                        }
                        continue;
                    case 3:
                        if (_stream.isNextSmall(0) && _stream.isNextSmall(1)) {
                            data1 = _stream.read();
                            data2 = _stream.read();
                            addEventToQueue(new MidiEventWithTiming(timestamp, command, data1, data2));
                        } else {
                            //Log.e(AppConstant.TAG, "Unknown Data3 " + _stream._pos  +" of " + _stream.peek(0) + " -> " + _stream.isNextSmall(0));
                            //Log.e(AppConstant.TAG, "Unknown Data3 " + (_stream._pos+1)  +" of " + _stream.peek(1) + " -> " + _stream.isNextSmall(1));
                        }
                        continue;
                    case -1:
                        _sysex = new ByteArrayOutputStream();
                        _sysex.write(command);
                        getTillSysexEnd();
                        break;
                    case -2:
                        int type = _stream.read();
                        switch (type) {

                        }
                        metaSkip = _stream.read();
                        break;
                }
            }
        }
    }

    private final MXQueue<MidiEventWithTiming> _queue = new MXQueue<>();

    private final Comparator<MidiEventWithTiming> midiTimerTaskComparator = new Comparator<MidiEventWithTiming>() {
        @Override
        public int compare(final MidiEventWithTiming lhs, final MidiEventWithTiming rhs) {
            // sort by tick
            int tickDifference = (int) (lhs.getTiming() - rhs.getTiming());
            if (tickDifference != 0) {
                return tickDifference * 256;
            }

            int lhsMessage = lhs.array.length > 0 ? lhs.getArray()[0] : 0;
            int rhsMessage = rhs.array.length > 0 ? rhs.getArray()[0] : 0;

            // same timing
            // sort by the MIDI data priority order, as:
            // system message > control messages > note on > note off
            // swap the priority of note on, and note off
            int lhsInt = lhsMessage & 0xf0;
            int rhsInt = rhsMessage & 0xf0;

            if (lhsInt == 0x90) {
                lhsInt = 0x80;
            } else if (lhsInt == 0x80) {
                lhsInt = 0x90;
            }
            if (rhsInt == 0x90) {
                rhsInt = 0x80;
            } else if (rhsInt == 0x80) {
                rhsInt = 0x90;
            }
            return -(lhsInt - rhsInt);
        }
    };

    private void addEventToQueue(MidiEventWithTiming event) {
        _queue.push(event, midiTimerTaskComparator);
    }

    /**
     * Runnable for MIDI event queueing
     */
    private class EventDequeueRunnable implements Runnable {
        private final List<MidiEventWithTiming> _dequeue = new ArrayList<>();

        @Override
        public void run() {
            while (true) {
                // running
                while (!isTerminated && isRunning) {
                    // deque events
                    _dequeue.clear();
                    final long currentTime = System.currentTimeMillis();
                    synchronized (_queue) {
                        while (_queue.isEmpty() == false) {
                            MidiEventWithTiming event = _queue.popAndNoRemove();
                            if (event.getTiming() <= currentTime) {
                                _dequeue.add(event);
                                _queue.pop();
                            } else {
                                break;
                            }
                        }
                    }

                    if (!_dequeue.isEmpty()) {
                        // fire events
                        for (MidiEventWithTiming event : _dequeue) {
                            byte[] data = event.array;
                            if (_receiver != null) {
                                _receiver.dispatchOne(OneMessage.thisPart(0, data, 0, data.length));
                            } else {
                                if (MidiOne.isDebug) {
                                    Log.e(AppConstant.MidiOneTag, "_receiver is null");
                                }
                            }
                        }
                    }
                }

                if (isTerminated) {
                    break;
                }
            }
        }
    }
}
