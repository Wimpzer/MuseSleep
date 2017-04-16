package com.musesleep.musesleep;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

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
import java.util.concurrent.atomic.AtomicReference;

public class SessionActivity extends AppCompatActivity {
    private final String TAG = "MUSESLEEP";

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

    private DatabaseReference myFirebaseRef;
    private String DATABASE_URL = "blazing-heat-9566";
    private String sessionId;


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

        myFirebaseRef = FirebaseDatabase.getInstance().getReference(DATABASE_URL);
        sessionId = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        myFirebaseRef.removeValue();

//        muse = manager.getMuses().get(musePosition);
        muse = MuseManager.getInstance().getMuseList().get(musePosition); //TODO: Check this hax
        muse.unregisterAllListeners();
        muse.registerConnectionListener(connectionListener);
        muse.registerDataListener(dataListener, MuseDataPacketType.EEG);
        muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_ABSOLUTE);
        muse.registerDataListener(dataListener, MuseDataPacketType.BETA_ABSOLUTE);
        muse.registerDataListener(dataListener, MuseDataPacketType.DELTA_ABSOLUTE);
        muse.registerDataListener(dataListener, MuseDataPacketType.GAMMA_ABSOLUTE);
        muse.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
        muse.runAsynchronously();

        handler.post(tickUi);
    }

    // helper methods to get different packet values
    private void getEegChannelValues(double[] buffer, MuseDataPacket p) {
        buffer[0] = p.getEegChannelValue(Eeg.EEG1);
        buffer[1] = p.getEegChannelValue(Eeg.EEG2);
        buffer[2] = p.getEegChannelValue(Eeg.EEG3);
        buffer[3] = p.getEegChannelValue(Eeg.EEG4);
        buffer[4] = p.getEegChannelValue(Eeg.AUX_LEFT);
        buffer[5] = p.getEegChannelValue(Eeg.AUX_RIGHT);
    }

    private final Handler handler = new Handler();

    // We update the UI from this Runnable instead of in packet handlers
    // because packets come in at high frequency -- 220Hz or more for raw EEG
    // -- and it only makes sense to update the UI at about 60fps. The update
    // functions do some string allocation, so this reduces our memory
    // footprint and makes GC pauses less frequent/noticeable.
    private final Runnable tickUi = new Runnable() {
        @Override
        public void run() {
            if (eegStale) {
                updateEeg();
            }
            handler.postDelayed(tickUi, 1000);
        }
    };

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
            default:
                break;
        }
    }

    public void receiveMuseArtifactPacket(final MuseArtifactPacket p) {

    }

    private void updateEeg() {
        double alphaWave = setAverageAlphaBuffer();
        double alphaWave100 = alphaWave * 100;
        Log.d(TAG, "updateAlpha - alphaWave 100: " + alphaWave100);

        double betaWave = setAverageBetaBuffer();
        double betaWave100 = betaWave * 100;
        Log.d(TAG, "updateBeta - Betawave 100: " + betaWave100);

        double deltaWave = setAverageDeltaBuffer();
        double deltaWave100 = deltaWave * 100;
        Log.d(TAG, "updateDelta - deltaWave 100: " + deltaWave100);

        double gammaWave = setAverageGammaBuffer();
        double gammaWave100 = gammaWave * 100;
        Log.d(TAG, "updateGamma - gammaWave 100: " + gammaWave100);

        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:S").format(new Date());
        myFirebaseRef.child(sessionId).child(currentTime).child("alphaWave").setValue(alphaWave100);
        myFirebaseRef.child(sessionId).child(currentTime).child("betaWave").setValue(betaWave100);
        myFirebaseRef.child(sessionId).child(currentTime).child("deltaWave").setValue(deltaWave100);
        myFirebaseRef.child(sessionId).child(currentTime).child("gammaWave").setValue(gammaWave100);
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
