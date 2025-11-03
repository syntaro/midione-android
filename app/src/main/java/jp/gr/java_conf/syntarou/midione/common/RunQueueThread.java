package jp.gr.java_conf.syntarou.midione.common;

import android.util.Log;

import jp.gr.java_conf.syntarou.midione.AppConstant;

import java.util.LinkedList;

public class RunQueueThread {
    public RunQueueThread() {
        launchThread();
    }

    public synchronized void launchThread() {
        if (_thread == null && !_freezed) {
            OneHelper.startThread(this::infinityLoop);
            while (_thread == null) {
                synchronized (this) {
                    try {
                        wait(100);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
    }

    MXQueue<Runnable> _queue = new MXQueue<>();
    Thread _thread = null;
    LinkedList<Runnable> _validDelay = new LinkedList<>();
    boolean _exchanging = true;
    long _maxTimeForExchange = 1000;
    boolean _freezed = false;

    public synchronized void reserveExchange(int maxExchangeTime) {
        _exchanging = true;
        _maxTimeForExchange = maxExchangeTime;
    }

    public void freeze(boolean flag) {
        if (flag) {
            _freezed = true;
        } else {
            _freezed = false;
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public void doneExchange() {
        _exchanging = false;
        synchronized (this) {
            notifyAll();
        }
    }

    public void postDelayed(Runnable run, long time) {
        synchronized (_validDelay) {
            _validDelay.add(run);
        }
        OneHelper.startThread(() -> {
            try {
                Thread.sleep(time);
            } catch (InterruptedException ex) {
                Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
            }
            synchronized (_validDelay) {
                if (_validDelay.contains(run) == false) {
                    return;
                }
            }
            _validDelay.remove(run);
            post(run);
        });
    }

    public void post(Runnable run) {
        _queue.push(run);
        launchThread();
    }
    public void postIfNotLastPost(Runnable run) {
        if (_queue.pushIfNotLast(run)) {
            launchThread();
        }
    }

    Runnable _currentRun;
    public boolean _runningTask = false;
    public synchronized void sync() {
        while(_queue.isEmpty() == false || _currentRun != null) {
            try {
                wait(100);
            } catch (InterruptedException ex) {

            }
        }
    }

    public void infinityLoop() {
        try {
            long prevtime = System.currentTimeMillis();
            synchronized (this) {
                _thread = Thread.currentThread();
                notifyAll();
            }
            while (true) {
                Runnable next = _queue.pop();
                if (next == null) {
                    break;
                }
                if (_freezed) {
                    _queue.back(next);
                    break;
                }
                if (_exchanging) {
                    //最大1秒? 待機する、なぜなら、デッドロックはさけたい
                    synchronized (this) {
                        try {
                            wait(_maxTimeForExchange);
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }
                    _exchanging = false;
                }
                try {
                    _currentRun = next;
                    next.run();
                } catch (Throwable ex) {
                    Log.e(AppConstant.MidiOneTag, ex.getMessage(), ex);
                }
                finally {
                    _currentRun = null;
                }
                if (_queue.isEmpty()) {
                    synchronized (this) {
                        notifyAll();
                    }
                }
                if (true) { // Exchange 使うべきだけど 保険のような
                    long stepTime = System.currentTimeMillis();
                    long time1 = 10 - (stepTime - prevtime);
                    if (time1 < 2) {
                        time1 = 2;
                    } else if (time1 >= 10) {
                        time1 = 10;
                    }
                    OneHelper.Thread_sleep(time1);
                    prevtime = stepTime;
                }
            }
        } finally {
            _thread = null;
        }
    }
}
