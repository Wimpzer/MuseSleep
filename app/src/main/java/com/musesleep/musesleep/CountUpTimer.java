package com.musesleep.musesleep;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

public abstract class CountUpTimer {

    private final long interval;
    private long base;
    private long pausedElapsedTime;
    private boolean haveResumed;

    public CountUpTimer(long interval) {
        this.interval = interval;
        haveResumed = false;
    }

    public void start() {
        base = SystemClock.elapsedRealtime();
        handler.sendMessage(handler.obtainMessage(MSG));
    }

    public void stop() {
        handler.removeMessages(MSG);
        haveResumed = false;
    }

    public void reset() {
        synchronized (this) {
            base = SystemClock.elapsedRealtime();
            haveResumed = false;
        }
    }

    public void pause() {
        pausedElapsedTime = SystemClock.elapsedRealtime() - base;
        stop();
    }

    public void resume() {
        haveResumed = true;
        start();
    }

    abstract public void onTick(long elapsedTime);

    private static final int MSG = 1;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            synchronized (CountUpTimer.this) {
                long elapsedTime = SystemClock.elapsedRealtime() - base;
                if(haveResumed)
                    elapsedTime += pausedElapsedTime;
                onTick(elapsedTime);
                sendMessageDelayed(obtainMessage(MSG), interval);
            }
        }
    };
}