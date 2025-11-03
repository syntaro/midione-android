package jp.gr.java_conf.syntarou.midione.ui.logview;

import android.util.Log;

import androidx.recyclerview.widget.RecyclerView;

import jp.gr.java_conf.syntarou.midione.common.OneHelper;
import jp.gr.java_conf.syntarou.midione.MidiOne;

public class MessageLogger {
    public static synchronized MessageLogger getInstance() {
        if (_instance == null) {
            _instance = new MessageLogger();
        }
        return _instance;
    }

    static MessageLogger _instance = null;

    MessageLogElementList _list;
    private MessageLogger() {
        _list = new MessageLogElementList(100);
    }

    public synchronized void log(String one) {
        _list.addPool(one);
        if (MidiOne.isDebug) {
            Log.e("****************", one);
        }
    }
    RecyclerView _view;
    public void attachRecyclerView(RecyclerView view) {
        MessageLogAdapter adapter = new MessageLogAdapter(view.getContext(), _list);
        view.setAdapter(adapter);
        _list.setAdapter(adapter);
        _view = view;
        OneHelper.runOnUiThread(this::flush, 1500);
    }
    public void detachRecylverView() {
        //_view.setAdapter(null);
        _list.setAdapter(null);
        _view = null;
    }

    private void flush() {
        RecyclerView view = _view;
        if (view != null) {
            if (_list.flushPool() > 0) {
                int x = _list.size() - 1;
                view.scrollToPosition(x);
            }
            OneHelper.runOnUiThread(this::flush, 1500);
        }
    }
}
