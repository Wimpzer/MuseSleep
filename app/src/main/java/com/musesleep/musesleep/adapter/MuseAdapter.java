package com.musesleep.musesleep.adapter;

import android.content.Context;

import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;
import com.musesleep.musesleep.MainActivity;

import java.util.List;

public class MuseAdapter {

    private static MuseAdapter instance;
    private static Context context = MainActivity.getAppContext();
    private List<Muse> museList;

    private MuseAdapter() {
    }

    public static MuseAdapter getInstance() { // Double checked Thread safe implementation
        if(instance == null) {
            synchronized (MuseAdapter.class) {
                if(instance == null) {
                    instance = new MuseAdapter();
                }
            }
        }
        return instance;
    }

    private MuseManagerAndroid manager = null;

    public MuseManagerAndroid getManager() {
        return manager;
    }

    // XX this must come before other libmuse API calls; it loads the
    // library.
    public void setContext() {
        manager = MuseManagerAndroid.getInstance();
        manager.setContext(context);
    }

    public void startListening() {
        manager.startListening();
    }

    public void stopListening() {
        manager.stopListening();
    }

    public void setMuseListener(MuseListener museListener) {
        manager.setMuseListener(museListener);
    }

    public List<Muse> getMuseList() {
        return museList;
    }

    public void setMuseList(List<Muse> museList) {
        this.museList = museList;
    }
}
