package jp.gr.java_conf.syntarou.midione.drivers.bluetooth;

public class MABStreamForParser {
    public MABStreamForParser() {

    }

    byte[] _data;
    int _offset;
    int _length;
    int _pos;
    int _time0;
    int _time1;
    int _timestamp;
    int _command;

    public void set(byte[] data, int offset, int length) {
        _data = data;
        _offset = offset;
        _length = length;
        _time0 = data[offset] & 0xff;
        _time1 = 0;
        _pos = 1;
        _command = 0;
    }

    public int readTimestamp() {
        int ch = peek(0);
        if ((ch & 0x80) != 0) {
            _time1 = ch;
            read();
        }

        _timestamp = ((_time0 & 0x3f) << 7) | (_time1 & 0x7f);
        return _timestamp;
    }

    public int lastTimestamp() {
        return _timestamp;
    }

    public int readCommand() {
        int ch = peek(0);
        if ((ch & 0x80) != 0) {
            _command = ch;
            read();
        }
        return _command;
    }

    public void back() {
        if (_pos > 0) {
            _pos--;
        }
    }

    public int read() {
        if (_offset + _pos >= _data.length) {
            return -1;
        }
        if (_pos >= _length) {
            return -1;
        }
        return _data[_offset + (_pos++)] & 0xff;
    }

    public boolean isNextBig() {
        if (_offset + _pos >= _data.length) {
            return false;
        }
        if (_pos >= _length) {
            return false;
        }
        return (_data[_offset + _pos] & 0x80) != 0;
    }

    public boolean isNextSmall(int x) {
        return (peek(x) & 0x80) == 0;
    }

    public int leftSize() {
        if (_pos >= _length) {
            return 0;
        }
        return _length - _pos;
    }

    public int peek(int x) {
        if (_offset + _pos + x >= _data.length) {
            return -1;
        }
        if (_pos + x >= _length) {
            return -1;
        }
        return _data[_offset + _pos + x] & 0xff;
    }
}
