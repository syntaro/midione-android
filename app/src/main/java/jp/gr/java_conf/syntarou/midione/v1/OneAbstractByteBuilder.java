package jp.gr.java_conf.syntarou.midione.v1;

import java.io.IOException;
import java.io.OutputStream;

public abstract class OneAbstractByteBuilder extends OutputStream {
    protected byte[] _rawData;
    protected int _usedLength;
    protected boolean _autoExtendable;

    public OneAbstractByteBuilder(int capacity) {
        _rawData = new byte[capacity];
        _usedLength = 0;
        _autoExtendable = false;
    }

    public void reset() {
        _usedLength = 0;
    }

    public void shrink() {
        if (_rawData.length > 512) {
            if (_usedLength <= 512) {
                byte[] newData = new byte[512];

                for (int i = 0; i < _usedLength; ++i) {
                    newData[i] = _rawData[i];
                }
                _rawData = newData;
            }
        }
    }

    public abstract boolean append(OneMessage one);

    public void extendBuffer(int newSize) throws IOException {
        if (newSize < _usedLength) {
            throw new IOException("newSize " + newSize + " is under usedLength " + _usedLength);
        }
        try {
            byte[] newData = new byte[newSize];

            for (int i = 0; i < _usedLength; ++i) {
                newData[i] = _rawData[i];
            }
            _rawData = newData;
        } catch (OutOfMemoryError ex) {
            throw new IOException("Failed to Make Packet", ex);
        }
    }

    @Override
    public void write(byte[] data, int offset, int count) throws IOException {
        if (_usedLength + count > _rawData.length) {
            if (_autoExtendable) {
                extendBuffer(_rawData.length + count + 512);
            } else {
                throw new IOException("Failed to Make Packet, Size Over " + _usedLength + "+" + count + " > " + _rawData.length);
            }
        }
        for (int i = 0; i < count; ++i) {
            _rawData[_usedLength++] = data[i + offset];
        }
    }

    public boolean tryWrite(int b) {
        try {
            write(b);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (_usedLength + 1 > _rawData.length) {
            if (_autoExtendable) {
                extendBuffer(_rawData.length + 1024);
            } else {
                throw new IOException("Failed to Make Packet, Size Over");
            }
        }
        _rawData[_usedLength++] = (byte) b;
    }

    public byte[] getRawData() {
        return _rawData;
    }

    public int size() {
        return _usedLength;
    }

    public int getLeftLength() {
        return _rawData.length - _usedLength;
    }

    public void setAutoExtendable(boolean auto) {
        _autoExtendable = auto;
    }

    public boolean isAutoExtendable() {
        return _autoExtendable;
    }

    byte[][] _cache = new byte[512][];

    public byte[] toCachedBuffer() {
        if (_rawData.length == _usedLength) {
            return _rawData;
        }
        byte[] buffer = null;
        if (_usedLength < 512) {
            if (_cache[_usedLength] == null) {
                _cache[_usedLength] = new byte[_usedLength];
            }
            buffer = _cache[_usedLength];
        } else {
            buffer = new byte[_usedLength];
        }
        for (int x = 0; x < _usedLength; ++x) {
            buffer[x] = _rawData[x];
        }
        return buffer;
    }

    public int countSpace() {
        return _rawData.length - _usedLength;
    }

    public void swapLast2() {
        byte last = _rawData[_usedLength - 1];
        byte last2 = _rawData[_usedLength - 2];
        _rawData[_usedLength - 1] = last2;
        _rawData[_usedLength - 2] = last;
    }
}
