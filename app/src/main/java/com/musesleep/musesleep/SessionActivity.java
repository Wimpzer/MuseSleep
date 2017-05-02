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
import com.choosemuse.libmuse.MuseManagerAndroid;
import com.choosemuse.libmuse.MuseVersion;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SessionActivity extends AppCompatActivity implements OnClickListener {
    private final String TAG = "MUSESLEEP";
    private final String FIREBASE_WAVE_TAG = "EEGs";
    private final String FIREBASE_STARTTIME_TAG = "StartTime";

    private MuseManagerAndroid manager = null;
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
    private String firebaseSessionId;

    private CountUpTimer countUpTimer;
    private Button pauseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.session_activity);

        Intent intent = getIntent();
        int musePosition = intent.getIntExtra("MusePosition", 0);

        manager = MuseManager.getInstance().getManager();

        WeakReference<SessionActivity> weakActivity =
                new WeakReference<>(this);
        connectionListener = new ConnectionListener(weakActivity);
        dataListener = new DataListener(weakActivity);
        manager.startListening();

        // Sets the Firebase reference to the current sessionId
        myFirebaseInstance = FirebaseDatabase.getInstance();
        myFirebaseBaseRef = myFirebaseInstance.getReference();
//        myFirebaseBaseRef.removeValue(); // Deletes the entire Firebase database
        firebaseSessionId = myFirebaseBaseRef.push().getKey();
        myFirebaseRef = myFirebaseInstance.getReference(firebaseSessionId); // .setPersistenceEnalbed(true) hvis den skal virke offline

        // Sets the start time variable for the current session
        String startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        myFirebaseRef.child(FIREBASE_STARTTIME_TAG).setValue(startTime);

//        muse = manager.getMuses().get(musePosition);
        muse = MuseManager.getInstance().getMuseList().get(musePosition); // TODO: Redo this hack
        muse.unregisterAllListeners();
        registerMuseDataListeners();

        final TextView sessionTimerTextView = (TextView) findViewById(R.id.sessionTimerTextView);
        countUpTimer = new CountUpTimer(1000) {
            @Override
            public void onTick(long elapsedTime) {
                int totalTime = (int) (elapsedTime/1000);
                String minutes = String.format("%02d", totalTime/60);
                String seconds = String.format("%02d", totalTime - (Integer.parseInt(minutes)*60));
                sessionTimerTextView.setText(minutes + ":" + seconds);
            }
        };
        countUpTimer.start();

        // Sets OnClickListener on the buttons
        Button stopButton = (Button) findViewById(R.id.sessionStopButton);
        stopButton.setOnClickListener(this);
        pauseButton = (Button) findViewById(R.id.sessionPauseButton);
        pauseButton.setOnClickListener(this);
    }

    private void registerMuseDataListeners() {
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
        manager.stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        manager.startListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        manager.stopListening();
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
        updateEeg();
    }

    public void receiveMuseArtifactPacket(final MuseArtifactPacket p) {

    }

    private void updateEeg() {
        double alphaWave = setAverageAlphaBuffer();
        if(Double.isNaN(alphaWave))
            alphaWave = 0;
        Log.d(TAG, "updateAlpha - alphaWave: " + alphaWave);

        double betaWave = setAverageBetaBuffer();
        if(Double.isNaN(betaWave))
            betaWave = 0;
        Log.d(TAG, "updateBeta - betaWave: " + betaWave);

        double deltaWave = setAverageDeltaBuffer();
        if(Double.isNaN(deltaWave))
            deltaWave = 0;
        Log.d(TAG, "updateDelta - deltaWave: " + deltaWave);

        double gammaWave = setAverageGammaBuffer();
        if(Double.isNaN(gammaWave))
            gammaWave = 0;
        Log.d(TAG, "updateGamma - gammaWave: " + gammaWave);

        double thetaWave = setAverageThetaBuffer();
        if(Double.isNaN(thetaWave))
            thetaWave = 0;
        Log.d(TAG, "updateTheta - thetaWave: " + thetaWave);

        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS").format(new Date());
        Map<String, Double> waveCollection = new HashMap<>();
        waveCollection.put("alphaWave", alphaWave);
        waveCollection.put("betaWave", betaWave);
        waveCollection.put("deltaWave", deltaWave);
        waveCollection.put("gammaWave", gammaWave);
        waveCollection.put("thetaWave", thetaWave);
        myFirebaseRef.child(FIREBASE_WAVE_TAG).child(currentTime).setValue(waveCollection);
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
                countUpTimer.pause();
                manager.stopListening();
                muse.unregisterAllListeners();
            }else{
                pauseButton.setText("Pause");
                countUpTimer.resume();
                manager.startListening();
                registerMuseDataListeners();
            }
        }else if(v.getId() == R.id.sessionStopButton) {
            countUpTimer.stop();
            manager.stopListening();
            muse.unregisterAllListeners();
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
