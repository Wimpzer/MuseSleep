package com.musesleep.musesleep;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseFileWriter;
import com.choosemuse.libmuse.MuseVersion;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.musesleep.musesleep.adapter.MuseAdapter;
import com.musesleep.musesleep.object.StopWatchObject;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SessionActivity extends AppCompatActivity implements OnClickListener {
    private final String TAG = "MUSESLEEP";
    private final String FIREBASE_WAVE_TAG = "EEGs";
    private final String FIREBASE_TIME_TAG = "Time";
    private final String FIREBASE_SLEEP_STAGE_TAG = "Stage";
    private final String FIREBASE_STAGE_TIME_TAG = "TimeInStage";
    private final int EEGTICKTIMER = 1000/2;
    private final int ALARMBUFFERTICKTIMER = 1000*60;
    private final int SLEEPSTAGETICKTIMER = 1000*5;
    private final SimpleDateFormat standardDateFormat =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS");

    private Muse muse = null;
    private ConnectionListener connectionListener = null;
    private DataListener dataListener = null;

    // Note: the array lengths here are taken from the comments in
    // MuseDataPacketType, which specify 6 values for EEG and EEG-derived packets.
    private final double[] eegBuffer = new double[6];
    private boolean eegStale = false;
    private final double[] alphaBuffer = new double[6];
    private boolean alphaStale = false;
    private final double[] betaBuffer = new double[6];
    private boolean betaStale = false;
    private final double[] deltaBuffer = new double[6];
    private boolean deltaStale = false;
    private final double[] gammaBuffer = new double[6];
    private boolean gammaStale = false;
    private final double[] thetaBuffer = new double[6];
    private boolean thetaStale = false;

    private FirebaseDatabase myFirebaseInstance;
    private DatabaseReference myFirebaseBaseRef;
    private DatabaseReference myFirebaseEEGRef;
    private DatabaseReference myFirebaseTimeRef;
    private DatabaseReference myFirebaseSleepStageRef;
    private DatabaseReference myFirebaseStageTimeRef;
    private String firebaseSessionId;

    private StopWatchObject stopWatchObject;
    private Button pauseButton;

    private int timeBuffer;
    private int sleepStage = 1;
    private String alarmSound;
    private boolean isFirstTime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.session_activity);

        // Gets extras
        Intent intent = getIntent();
        int musePosition = intent.getIntExtra("MusePosition", 0);
        timeBuffer = intent.getBundleExtra("startValues").getInt("timeBuffer", 0);
        alarmSound = intent.getBundleExtra("startValues").getString("alarmSound");

        WeakReference<SessionActivity> weakActivity =
                new WeakReference<>(this);
        connectionListener = new ConnectionListener(weakActivity);
        dataListener = new DataListener(weakActivity);

        // Sets the Firebase references to the current sessionId
        myFirebaseInstance = FirebaseDatabase.getInstance();
        myFirebaseBaseRef = myFirebaseInstance.getReference();
//        myFirebaseBaseRef.removeValue(); // Deletes the entire Firebase database
        firebaseSessionId = myFirebaseBaseRef.push().getKey();

        // .setPersistenceEnabled(true) needs to added for offline use
        myFirebaseEEGRef = myFirebaseInstance.getReference(FIREBASE_WAVE_TAG).child(firebaseSessionId);
        myFirebaseTimeRef = myFirebaseInstance.getReference(FIREBASE_TIME_TAG).child(firebaseSessionId);
        myFirebaseSleepStageRef = myFirebaseInstance.getReference(FIREBASE_SLEEP_STAGE_TAG).child(firebaseSessionId);
        myFirebaseStageTimeRef = myFirebaseInstance.getReference(FIREBASE_STAGE_TIME_TAG).child(firebaseSessionId);

        // Sets the start time and start day for the current session
        Date date = new Date();
        String startTime = standardDateFormat.format(date);
        myFirebaseTimeRef.child("startTime").setValue(startTime);
        String dayOfWeek = getDayOfWeekFromCalendar(date.getDay());
        myFirebaseTimeRef.child("startDay").setValue(dayOfWeek);

//        muse = manager.getMuses().get(musePosition);
        muse = MuseAdapter.getInstance().getMuseList().get(musePosition); // TODO: Redo this hack

        final TextView sessionTimerTextView = (TextView) findViewById(R.id.sessionTimerTextView);
        stopWatchObject = new StopWatchObject(1000) {
            @Override
            public void onTick(long elapsedTime) {
                int totalTime = (int) (elapsedTime/1000);
                String minutes = String.format("%02d", totalTime/60);
                String seconds = String.format("%02d", totalTime - (Integer.parseInt(minutes)*60));
                sessionTimerTextView.setText(minutes + ":" + seconds);
            }
        };
        stopWatchObject.start();

        // Sets OnClickListener on the buttons
        Button stopButton = (Button) findViewById(R.id.sessionStopButton);
        stopButton.setOnClickListener(this);
        pauseButton = (Button) findViewById(R.id.sessionPauseButton);
        pauseButton.setOnClickListener(this);

        muse.unregisterAllListeners();
        registerMuseListeners();
        handler.post(tickEEG);
        handler.post(tickAlarmBuffer);
        handler.post(tickSleepStage);
    }

    private void registerMuseListeners() {
        muse.registerConnectionListener(connectionListener);
        muse.registerDataListener(dataListener, MuseDataPacketType.EEG);
        muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_ABSOLUTE);
        muse.registerDataListener(dataListener, MuseDataPacketType.BETA_ABSOLUTE);
        muse.registerDataListener(dataListener, MuseDataPacketType.DELTA_ABSOLUTE);
        muse.registerDataListener(dataListener, MuseDataPacketType.GAMMA_ABSOLUTE);
        muse.registerDataListener(dataListener, MuseDataPacketType.THETA_ABSOLUTE);
        muse.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
        muse.runAsynchronously();
    }

    @Override
    protected void onPause() {
        super.onPause();
        setEndTime();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setEndTime();
        muse.unregisterAllListeners();
        muse.disconnect(false);
        handler.removeCallbacks(tickEEG);
        handler.removeCallbacks(tickAlarmBuffer);
        handler.removeCallbacks(tickSleepStage);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    // Helper methods to get different packet values
    private void getEegChannelValues(double[] buffer, MuseDataPacket p) {
        buffer[0] = p.getEegChannelValue(Eeg.EEG1);
        buffer[1] = p.getEegChannelValue(Eeg.EEG2);
        buffer[2] = p.getEegChannelValue(Eeg.EEG3);
        buffer[3] = p.getEegChannelValue(Eeg.EEG4);
        buffer[4] = p.getEegChannelValue(Eeg.AUX_LEFT);
        buffer[5] = p.getEegChannelValue(Eeg.AUX_RIGHT);
    }

    private final Handler handler = new Handler();

    private final AtomicReference<MuseFileWriter> fileWriter = new AtomicReference<>();
    private final AtomicReference<Handler> fileHandler = new AtomicReference<>();

    static {
        // Try to load our own all-in-one JNI lib. If it fails, rely on libmuse
        // to load libmuse_android.so for us.
        try {
            System.loadLibrary("TestLibMuseAndroid");
        } catch (UnsatisfiedLinkError e) {
        }
    }

    public void receiveMuseConnectionPacket(final MuseConnectionPacket p) {
        final ConnectionState current = p.getCurrentConnectionState();
        final String status = p.getPreviousConnectionState().toString().
                concat(" -> ").
                concat(current.toString());
        Log.i(TAG, status);
        if (p.getCurrentConnectionState() == ConnectionState.DISCONNECTED) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (muse != null) {
                        muse.runAsynchronously();
                    }
                }
            }, 20);
        }
        handler.post(new Runnable() {
            @Override public void run() {
                if (current == ConnectionState.CONNECTED) {
                    final MuseVersion museVersion = muse.getMuseVersion();
                    final String version = museVersion.getFirmwareType().
                            concat(" - ").concat(museVersion.getFirmwareVersion()).
                            concat(" - ").concat(Integer.toString(museVersion.getProtocolVersion()));
                }
            }
        });
    }

    public void receiveMuseDataPacket(final MuseDataPacket p) {
        Handler h = fileHandler.get();
        if (h != null) {
            h.post(new Runnable() {
                @Override
                public void run() {
                    fileWriter.get().addDataPacket(0, p);
                }
            });
        }
        final long n = p.valuesSize();
        switch (p.packetType()) {
            case EEG:
                assert(eegBuffer.length >= n);
                getEegChannelValues(eegBuffer,p);
                eegStale = true;
                break;
            case ALPHA_ABSOLUTE:
                assert(alphaBuffer.length >= n);
                getEegChannelValues(alphaBuffer,p);
                alphaStale = true;
                break;
            case BETA_ABSOLUTE:
                assert(betaBuffer.length >= n);
                getEegChannelValues(betaBuffer, p);
                betaStale = true;
                break;
            case DELTA_ABSOLUTE:
                assert(deltaBuffer.length >= n);
                getEegChannelValues(deltaBuffer, p);
                deltaStale = true;
                break;
            case GAMMA_ABSOLUTE:
                assert(gammaBuffer.length >= n);
                getEegChannelValues(gammaBuffer, p);
                gammaStale = true;
                break;
            case THETA_ABSOLUTE:
                assert(thetaBuffer.length >= n);
                getEegChannelValues(thetaBuffer, p);
                thetaStale = true;
                break;
            default:
                break;
        }
    }

    public void receiveMuseArtifactPacket(final MuseArtifactPacket p) {

    }

    private final Runnable tickEEG = new Runnable() {
        @Override
        public void run() {
            updateEeg();
            handler.postDelayed(tickEEG, EEGTICKTIMER);
        }
    };

    private final Runnable tickAlarmBuffer = new Runnable() {
        @Override
        public void run() {
            wakeUpCheck();
            handler.postDelayed(tickAlarmBuffer, ALARMBUFFERTICKTIMER);
        }
    };

    private final Runnable tickSleepStage = new Runnable() {
        @Override
        public void run() {
            saveSleepStage();
            handler.postDelayed(tickSleepStage, SLEEPSTAGETICKTIMER);
        }
    };

    private void updateEeg() {
        double alphaWave = setAverageAlphaBuffer();
        if(Double.isNaN(alphaWave))
            alphaWave = 0;
        Log.d(TAG, "updateAlpha - alphaWave: " + alphaWave);
        Map<String, Double> alphaCollection = new HashMap<>();
        for(int i = 0; i < alphaBuffer.length; i++) {
            if(Double.isNaN(alphaBuffer[i]))
                alphaCollection.put("alpha" + i, 0.0);
            else
                alphaCollection.put("alpha" + i, alphaBuffer[i]);
        }
        alphaCollection.put("alphaAvg", alphaWave);

        double betaWave = setAverageBetaBuffer();
        if(Double.isNaN(betaWave))
            betaWave = 0;
        Log.d(TAG, "updateBeta - betaWave: " + betaWave);
        Map<String, Double> betaCollection = new HashMap<>();
        for(int i = 0; i < betaBuffer.length; i++) {
            if(Double.isNaN(betaBuffer[i]))
                betaCollection.put("beta" + i, 0.0);
            else
                betaCollection.put("beta" + i, betaBuffer[i]);
        }
        betaCollection.put("betaAvg", betaWave);

        double deltaWave = setAverageDeltaBuffer();
        if(Double.isNaN(deltaWave))
            deltaWave = 0;
        Log.d(TAG, "updateDelta - deltaWave: " + deltaWave);
        Map<String, Double> deltaCollection = new HashMap<>();
        for(int i = 0; i < deltaBuffer.length; i++) {
            if(Double.isNaN(deltaBuffer[i]))
                deltaCollection.put("delta" + i, 0.0);
            else
                deltaCollection.put("delta" + i, deltaBuffer[i]);
        }
        deltaCollection.put("deltaAvg", deltaWave);

        double gammaWave = setAverageGammaBuffer();
        if(Double.isNaN(gammaWave))
            gammaWave = 0;
        Log.d(TAG, "updateGamma - gammaWave: " + gammaWave);
        Map<String, Double> gammaCollection = new HashMap<>();
        for(int i = 0; i < gammaBuffer.length; i++) {
            if(Double.isNaN(gammaBuffer[i]))
                gammaCollection.put("gamma" + i, 0.0);
            else
                gammaCollection.put("gamma" + i, gammaBuffer[i]);
        }
        gammaCollection.put("gammaAvg", gammaWave);

        double thetaWave = setAverageThetaBuffer();
        if(Double.isNaN(thetaWave))
            thetaWave = 0;
        Log.d(TAG, "updateTheta - thetaWave: " + thetaWave);
        Map<String, Double> thetaCollection = new HashMap<>();
        for(int i = 0; i < thetaBuffer.length; i++) {
            if(Double.isNaN(thetaBuffer[i]))
                thetaCollection.put("theta" + i, 0.0);
            else
                thetaCollection.put("theta" + i, thetaBuffer[i]);
        }
        thetaCollection.put("thetaAvg", thetaWave);

        //This would be where the 'algorithm' would calculate the sleepStage
        sleepStage = 1;

        String currentTime = standardDateFormat.format(new Date());
        myFirebaseEEGRef.child("alpha").child(currentTime).setValue(alphaCollection);
        myFirebaseEEGRef.child("beta").child(currentTime).setValue(betaCollection);
        myFirebaseEEGRef.child("delta").child(currentTime).setValue(deltaCollection);
        myFirebaseEEGRef.child("gamma").child(currentTime).setValue(gammaCollection);
        myFirebaseEEGRef.child("theta").child(currentTime).setValue(thetaCollection);
    }

    private void wakeUpCheck() {
        try {
            String nextAlarmString = Settings.System.getString(getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
            Calendar gc = new GregorianCalendar();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE hh:mm a");

            Date nextAlarm = simpleDateFormat.parse(nextAlarmString);

            int nextAlarmHour = nextAlarm.getHours();
            int nextAlarmMinute = nextAlarm.getMinutes();
            if((nextAlarmHour < gc.get(Calendar.HOUR_OF_DAY)) || (nextAlarmHour == gc.get(Calendar.HOUR_OF_DAY) && nextAlarmMinute < gc.get(Calendar.MINUTE))) {
                gc.add(Calendar.DATE, 1);
            }
            gc.set(Calendar.HOUR_OF_DAY, nextAlarm.getHours());
            gc.set(Calendar.MINUTE, nextAlarm.getMinutes());
            gc.set(Calendar.SECOND, 0);
            nextAlarm = gc.getTime();
            gc.add(Calendar.MINUTE, -timeBuffer);

            Date earliestWakeUp = gc.getTime();
            Date currentTime = new Date();

            if (currentTime.after(earliestWakeUp) && currentTime.before(nextAlarm) && isFirstTime == true) {
                if(sleepStage == 1) {
                    isFirstTime = false;

                    Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);

                    switch (alarmSound) {
                        case "Quiet":
                            // Extra_RINGTONE
                            break;
                        case "Increasing":
                            // Volumesteps / timeBuffer = increase 1 step / time
                            break;
                        case "Default":

                            break;
                    }

                    handler.postDelayed(new Runnable() { //TODO: Hvorfor venter den ikke på delayed time?
                        @Override
                        public void run() {
                            startPastSessionActivity();
                        }
                    }, 60-currentTime.getSeconds());

                    intent.putExtra(AlarmClock.EXTRA_HOUR, currentTime.getHours());
                    intent.putExtra(AlarmClock.EXTRA_MINUTES, currentTime.getMinutes()+1);
                    startActivity(intent);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void saveSleepStage() {
        String currentTime = standardDateFormat.format(new Date());
        myFirebaseSleepStageRef.child(currentTime).setValue(sleepStage);

        myFirebaseStageTimeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() == null) { // Setting the value to 0 seconds for all four stages
                    dataSnapshot.getRef().child("1").setValue(0);
                    dataSnapshot.getRef().child("2").setValue(0);
                    dataSnapshot.getRef().child("3").setValue(0);
                    dataSnapshot.getRef().child("4").setValue(0);
                }else{
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        int stageTime = Integer.parseInt(child.getKey());
                        if (stageTime == sleepStage)
                            dataSnapshot.getRef().child(child.getKey()).setValue(child.getValue(Integer.class) + 5);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("saveSleepStage", "onCancelled", databaseError.toException());
            }
        });
    }

    private double setAverageAlphaBuffer() {
        double sum = 0;
        if(alphaBuffer.length > 0) {
            for (Double mark : alphaBuffer) {
                sum += mark;
            }
            return (sum / alphaBuffer.length);
        }
        return sum;
    }

    private double setAverageBetaBuffer() {
        double sum = 0;
        if(betaBuffer.length > 0) {
            for (Double mark : betaBuffer) {
                sum += mark;
            }
            return (sum / betaBuffer.length);
        }
        return sum;
    }

    private double setAverageDeltaBuffer() {
        double sum = 0;
        if(deltaBuffer.length > 0) {
            for (Double mark : deltaBuffer) {
                sum += mark;
            }
            return (sum / deltaBuffer.length);
        }
        return sum;
    }

    private double setAverageGammaBuffer() {
        double sum = 0;
        if(gammaBuffer.length > 0) {
            for (Double mark : gammaBuffer) {
                sum += mark;
            }
            return (sum / gammaBuffer.length);
        }
        return sum;
    }

    private double setAverageThetaBuffer() {
        double sum = 0;
        if(thetaBuffer.length > 0) {
            for (Double mark : thetaBuffer) {
                sum += mark;
            }
            return (sum / thetaBuffer.length);
        }
        return sum;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.sessionPauseButton) {
            if(pauseButton.getText().toString().toLowerCase().equals("pause")) {
                pauseButton.setText("Resume");
                stopWatchObject.pause();
                muse.unregisterAllListeners();
                handler.removeCallbacks(tickEEG);
                handler.removeCallbacks(tickAlarmBuffer);
                handler.removeCallbacks(tickSleepStage);

                setEndTime();
            }else{
                pauseButton.setText("Pause");
                stopWatchObject.resume();
                registerMuseListeners();
                handler.post(tickEEG);
                handler.post(tickAlarmBuffer);
                handler.post(tickSleepStage);
            }
        }else if(v.getId() == R.id.sessionStopButton) {
            stopWatchObject.stop();
            muse.unregisterAllListeners();
            muse.disconnect(false);
            handler.removeCallbacks(tickEEG);
            handler.removeCallbacks(tickAlarmBuffer);
            handler.removeCallbacks(tickSleepStage);

            setEndTime();

            startPastSessionActivity();
        }
    }

    private void startPastSessionActivity() {
        Intent pastSessionIntent = new Intent(this, PastSessionActivity.class);
        pastSessionIntent.putExtra("sessionId", firebaseSessionId);
        startActivity(pastSessionIntent);
        finish();
    }

    private void setEndTime() {
        Date date = new Date();
        String endTime = standardDateFormat.format(date);
        myFirebaseTimeRef.child("endTime").setValue(endTime);
        String dayOfWeek = getDayOfWeekFromCalendar(date.getDay());
        myFirebaseTimeRef.child("endDay").setValue(dayOfWeek);
    }

    private String getDayOfWeekFromCalendar(int dayInt) {
        dayInt++;
        String dayOfWeek = "";
        switch (dayInt) {
            case Calendar.MONDAY:
                dayOfWeek = "Monday";
                break;
            case Calendar.TUESDAY:
                dayOfWeek = "Tuesday";
                break;
            case Calendar.WEDNESDAY:
                dayOfWeek = "Wednesday";
                break;
            case Calendar.THURSDAY:
                dayOfWeek = "Thursday";
                break;
            case Calendar.FRIDAY:
                dayOfWeek = "Friday";
                break;
            case Calendar.SATURDAY:
                dayOfWeek = "Saturday";
                break;
            case Calendar.SUNDAY:
                dayOfWeek = "Sunday";
                break;
        }
        return dayOfWeek;
    }

    // Listener translators follow.
    class ConnectionListener extends MuseConnectionListener {
        final WeakReference<SessionActivity> activityRef;

        ConnectionListener(final WeakReference<SessionActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            activityRef.get().receiveMuseConnectionPacket(p);
        }
    }

    class DataListener extends MuseDataListener {
        final WeakReference<SessionActivity> activityRef;

        DataListener(final WeakReference<SessionActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            activityRef.get().receiveMuseDataPacket(p);
        }

        @Override
        public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
            activityRef.get().receiveMuseArtifactPacket(p);
        }
    }
}
