package com.musesleep.musesleep.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

public class Mote extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String alarmSound = intent.getExtras().getString("alarmSound");

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        Ringtone ringtone = RingtoneManager.getRingtone(context, uri);
        ringtone.play();
        Log.d("TAG", "Ringtone is playing");

//        switch (alarmSound) {
//            case "Quiet":
//                break;
//            case "Increasing":
//                // Volumesteps / timeBuffer = increase 1 step / time
//                break;
//            case "Default":
//
//                break;
//        }
    }
}
