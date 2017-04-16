package com.musesleep.musesleep;

import android.content.Context;

import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;

import java.util.List;

public class MuseManager {

    private static MuseManager instance;
    private static Context context = MainActivity.getAppContext();
    private List<Muse> museList;

    private MuseManager() {
    }

    public static MuseManager getInstance() { // Double checked Thread safe implementation
        if(instance == null) {
            synchronized (MuseManager.class) {
                if(instance == null) {
                    instance = new MuseManager();
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
