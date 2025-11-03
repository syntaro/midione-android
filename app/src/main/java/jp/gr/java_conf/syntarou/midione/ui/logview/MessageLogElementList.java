package jp.gr.java_conf.syntarou.midione.ui.logview;

import android.util.Log;

import jp.gr.java_conf.syntarou.midione.AppConstant;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class MessageLogElementList {
    public MessageLogElementList(int capacity) {
        _list = new String[capacity];
        _quque = new LinkedList<>();
        try {
            _format = new SimpleDateFormat("H:mm:ss");
        } catch (Throwable ex) {

        }
    }

    public void setAdapter(MessageLogAdapter adapter) {
        _adapter = adapter;
    }

    String[] _list;
    int _readPosition;
    int _size;
    MessageLogAdapter _adapter;

    LinkedList<String> _quque;
    boolean _processingQueue = false;

    public int size() {
        return _size;
    }

    public String get(int x) {
        synchronized (_quque) {

        if (x >= _size) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int x2 = _readPosition + x;
        while (x2 >= _list.length) {
            x2 -= _list.length;
        }
        return _list[x2];
        }
    }

    SimpleDateFormat _format = null;

    private void add(String elem) {
        synchronized (_quque) {
            int writePos = _readPosition + _size;
            while (writePos >= _list.length) {
                writePos -= _list.length;
            }
            if (_size >= _list.length) {
                _readPosition++;
                _size--;
                if (!_processingQueue) {
                    fireRemoved(1);
                }
            }
            _size++;
            _list[writePos] = elem;
            if (!_processingQueue) {
                fireAdded(1);
            }
        }
    }

    public void addPool(String e) {
        String time2 = "";
        if (_format != null) {
            time2 = _format.format(new Date(System.currentTimeMillis()));
        }
        synchronized (_quque) {
            _quque.addLast(time2 + ":" + e);
            if (_quque.size() >= _list.length) {
                _quque.removeFirst();
            }
        }
    }

    public int flushPool() {
        synchronized (_quque) {
            int count = _quque.size();
            if (_quque.isEmpty()) {
                return count;
            }
            _processingQueue = true;
            try {
                int newSize = _quque.size();
                if (newSize >= _list.length) {
                    while (newSize > _list.length) {
                        _quque.removeFirst();
                        newSize--;
                    }
                    for (int i = 0; i < newSize; ++i) {
                        _list[i] = _quque.removeFirst();
                    }
                    fireAllChange();
                    ;
                } else {
                    int skip = newSize - _list.length;

                    for (String seek : _quque) {
                        if (skip > 0) {
                            skip--;
                        } else {
                            add(seek);
                        }
                    }
                    int overflow = (_size + newSize) - _list.length;
                    if (overflow > 0) {
                        if (overflow > _list.length) {
                            overflow = _list.length;
                        }
                        fireRemoved(overflow);
                    }
                    if (newSize > _list.length) {
                        newSize = _list.length;
                    }
                    fireAdded(newSize);
                    _quque.clear();
                }
            } finally {
                _processingQueue = false;
            }
            return count;
        }
    }

    protected void fireRemoved(int length) {
        if (_adapter == null) {
            Log.e(AppConstant.MidiOneTag, "fireRemoved " + length);
        } else {
            if (length == 1) {
                _adapter.notifyItemRemoved(0);
            } else if (length > 0) {
                _adapter.notifyItemRangeRemoved(0, length);
            }
        }
    }

    protected void fireAdded(int length) {
        if (_adapter == null) {
            Log.e(AppConstant.MidiOneTag, "fireAdded " + length);
        } else {
            if (length == 1) {
                _adapter.notifyItemInserted(size() - 1);
            } else if (length > 1) {
                _adapter.notifyItemRangeInserted(size() - length, length);
            }
        }
    }

    protected void fireAllChange() {
        if (_adapter == null) {
            Log.e(AppConstant.MidiOneTag, "fireAllChanged");
        } else {
            _adapter.notifyItemRangeChanged(0, size());
        }
    }
}
