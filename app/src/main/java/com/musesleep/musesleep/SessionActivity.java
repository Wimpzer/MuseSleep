package com.musesleep.musesleep;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.musesleep.musesleep.Adapter.MuseAdapter;
import com.musesleep.musesleep.Object.StopWatchObject;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SessionActivity extends AppCompatActivity implements OnClickListener {
    private final String TAG = "MUSESLEEP";
    private final String FIREBASE_WAVE_TAG = "EEGs";
    private final String FIREBASE_TIME_TAG = "Time";
    private final int TICKTIMER = 1000/2;

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
    private DatabaseReference myFirebaseRef;
    private DatabaseReference myFirebaseEEGRef;
    private DatabaseReference myFirebaseTimeRef;
    private String firebaseSessionId;

    private StopWatchObject stopWatchObject;
    private Button pauseButton;
    private SimpleDateFormat standardDateFormat =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.session_activity);

        Intent intent = getIntent();
        int musePosition = intent.getIntExtra("MusePosition", 0);

        WeakReference<SessionActivity> weakActivity =
                new WeakReference<>(this);
        connectionListener = new ConnectionListener(weakActivity);
        dataListener = new DataListener(weakActivity);

        // Sets the Firebase references to the current sessionId
        myFirebaseInstance = FirebaseDatabase.getInstance();
        myFirebaseBaseRef = myFirebaseInstance.getReference();
//        myFirebaseBaseRef.removeValue(); // Deletes the entire Firebase database
        firebaseSessionId = myFirebaseBaseRef.push().getKey();

        // .setPersistenceEnalbed(true) needs to added for offline use
        myFirebaseEEGRef = myFirebaseInstance.getReference(FIREBASE_WAVE_TAG).child(firebaseSessionId);
        myFirebaseTimeRef = myFirebaseInstance.getReference(FIREBASE_TIME_TAG).child(firebaseSessionId);

        // Sets the start time variable for the current session
        String startTime = standardDateFormat.format(new Date());
        myFirebaseTimeRef.child("startTime").setValue(startTime);

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
        String endTime = standardDateFormat.format(new Date());
        myFirebaseTimeRef.child("endTime").setValue(endTime);
    }

    @Override
    protected void onResume() {
        super.onResume();
        muse.unregisterAllListeners();
        registerMuseListeners();
        handler.post(tickUi);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        String endTime = standardDateFormat.format(new Date());
        myFirebaseTimeRef.child("endTime").setValue(endTime);
        muse.unregisterAllListeners();
        handler.removeCallbacks(tickUi);
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

    private final Runnable tickUi = new Runnable() {
        @Override
        public void run() {
            updateEeg();
            handler.postDelayed(tickUi, TICKTIMER);
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

        String currentTime = standardDateFormat.format(new Date());
        myFirebaseEEGRef.child("alpha").child(currentTime).setValue(alphaCollection);
        myFirebaseEEGRef.child("beta").child(currentTime).setValue(betaCollection);
        myFirebaseEEGRef.child("delta").child(currentTime).setValue(deltaCollection);
        myFirebaseEEGRef.child("gamma").child(currentTime).setValue(gammaCollection);
        myFirebaseEEGRef.child("theta").child(currentTime).setValue(thetaCollection);
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
                handler.removeCallbacks(tickUi);

                String endTime = standardDateFormat.format(new Date());
                myFirebaseTimeRef.child("endTime").setValue(endTime);
            }else{
                pauseButton.setText("Pause");
                stopWatchObject.resume();
                registerMuseListeners();
                handler.post(tickUi);
            }
        }else if(v.getId() == R.id.sessionStopButton) {
            stopWatchObject.stop();
            muse.unregisterAllListeners();
            handler.removeCallbacks(tickUi);

            String endTime = standardDateFormat.format(new Date());
            myFirebaseTimeRef.child("endTime").setValue(endTime);
        }
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
