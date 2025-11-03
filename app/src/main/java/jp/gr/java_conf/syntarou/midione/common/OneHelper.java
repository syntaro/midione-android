package jp.gr.java_conf.syntarou.midione.common;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import jp.gr.java_conf.syntarou.midione.AppConstant;
import jp.gr.java_conf.syntarou.midione.MidiOne;

import java.util.ArrayList;

public class OneHelper {

    public static String toHexString2(int i) {
        String str = Integer.toHexString(i).toUpperCase();
        if (str.length() == 1) {
            return "0" + str;
        }
        /*
        if (str.length() >= 3) {
            return str.substring(str.length() - 2, str.length());
        }*/
        return str;
    }

    public static String toHexString4(int i) {
        int hi = (i >> 7) & 0x7f;
        int lo = i & 0x7f;
        return toHexString2(hi) + ":" + toHexString2(lo);
    }

    public static String dumpHex(int[] data) {
        return dumpHex(data, 0, data.length);
    }

    public static String dumpHex(int[] data, int offset, int count) {
        StringBuffer str = new StringBuffer();
        for (int i = offset; i < offset + count; ++i) {
            if (i != 0) {
                str.append(", ");
            }
            str.append(Integer.toHexString(data[i]));
        }
        return str.toString();
    }

    public static String dumpHex(byte[] data) {
        if (data == null) {
            return "nullptr";
        }
        return dumpHex(data, 0, data.length);
    }

    public static String dumpHex(byte[] data, int offset, int count) {
        StringBuffer str = new StringBuffer();
        for (int i = offset; i < offset + count; ++i) {
            if (i != 0) {
                str.append(" ");
            }
            str.append(toHexString2(data[i] & 0xff));
        }
        return str.toString();
    }

    public static String dumpDword(int dword) {
        byte[] data = new byte[4];
        data[0] = (byte) ((dword >> 24) & 0xff);
        data[1] = (byte) ((dword >> 16) & 0xff);
        data[2] = (byte) ((dword >> 8) & 0xff);
        data[3] = (byte) ((dword) & 0xff);
        return dumpHex(data);
    }

    public static boolean isNumberFormat(String text) {
        try {
            OneHelper.numberFromText(text);
            return true;
        } catch (NumberFormatException e) {
        }
        return false;
    }

    public static final int numberFromText(String text) throws NumberFormatException {
        int mum = 10;
        boolean negative = false;

        if (text == null) {
            throw new NumberFormatException("text null cant be number");
        }
        if (text.startsWith("-")) {
            negative = true;
            text = text.substring(1);
        }
        if (text.startsWith("0x")) {
            text = text.substring(2);
            mum = 16;
        }
        if (text.endsWith("h") || text.endsWith("H")) {
            text = text.substring(0, text.length() - 1);
            mum = 16;
        }

        int start = 0;
        int end = text.length();

        if (start >= end) {
            throw new NumberFormatException("length 0");
        }

        int x = 0;
        for (int pos = start; pos < end; ++pos) {
            int ch = text.charAt(pos);
            if (ch >= '0' && ch <= '9') {
                x *= mum;
                x += ch - (char) '0';
            } else if (ch >= 'A' && ch <= 'F' && mum == 16) {
                x *= mum;
                x += ch - (char) 'A' + 10;
            } else if (ch >= 'a' && ch <= 'f' && mum == 16) {
                x *= mum;
                x += ch - (char) 'a' + 10;
            } else {
                throw new NumberFormatException("Format Error '" + text + "'");
            }
        }
        if (negative) {
            return -x;
        }
        return x;
    }

    public static void split(String str, ArrayList<String> list, char splitter) {
        list.clear();
        int len = str.length();
        int from = 0;
        for (int i = 0; i < len; ++i) {
            char ch = str.charAt(i);
            if (ch == splitter) {
                list.add(str.substring(from, i));
                from = i + 1;
                continue;
            }
        }
        if (from < len) {
            list.add(str.substring(from, len));
        }
    }

    public static boolean isShrinkTarget(char c) {
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
            return true;
        }
        return false;
    }

    public static String shrinkText(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() == 0) {
            return text;
        }
        int start = 0;
        int end = text.length() - 1;
        while (start <= end && isShrinkTarget(text.charAt(start))) {
            start++;
        }
        while (start <= end && isShrinkTarget(text.charAt(end))) {
            end--;
        }
        if (start > end) {
            return "";
        }
        return text.substring(start, end + 1);
    }

    public static void runOnUiThread(Runnable run) {
        runOnUiThread(run, 0);
    }

    static int count;
    static long count2;

    private static  void invokeProcess(Runnable run, Throwable background) {
        long from = 0;
        if (MidiOne.isDebug) {
            from = System.currentTimeMillis();
        }
        try {
            run.run();
        }catch(Throwable ex) {
            Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
        }
        if (MidiOne.isDebug) {
            long to = System.currentTimeMillis();
            if (to - from >= 200) {
                Log.e(AppConstant.MidiOneTag, "too slow" + (to - from), background);
            }else {
                count ++;
                if (count >= 3000) {
                    count2 ++ ;
                    count = 0;
                    //Log.e(AppConstant.TAG, "trace log " +count2, background);
                }
            }
        }
    }

    public static void Thread_sleep(long time) {
        synchronized (OneHelper.class) {
            try {
                MidiOne.class.wait(time);
            } catch (Throwable ex) {

            }
        }
    }

    public static void runOnUiThread(Runnable run, long time) {
        Throwable background = null;
        if (MidiOne.isDebug) {
            background = new Throwable();
        }

        if (Looper.myLooper() == Looper.getMainLooper() && time == 0) {
            invokeProcess(run, background);
            return;
        }
        else {
            Throwable back2 = background;
            Runnable wrap = () -> invokeProcess(run, back2);
            Handler _myLooper = new Handler(Looper.getMainLooper());
            if (time == 0) {
                _myLooper.post(wrap);
            }
            else {
                _myLooper.postDelayed(wrap, time);
            }
        }
    }

    private static ThreadGroup _tGroup = new ThreadGroup("MidiOne") {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Log.e(AppConstant.MidiOneTag, e.getMessage() + "@" + t, e);
        }
    };
    public static Thread startThread(Runnable run) {
        Thread t = new Thread(_tGroup, run);
        t.setDaemon(true);
        t.start();
        return t;
    }

    public static void stopAllThreads() {
        Log.e(AppConstant.MidiOneTag, "Thread Count = " + _tGroup.activeCount());
        _tGroup.interrupt();
    }
}
