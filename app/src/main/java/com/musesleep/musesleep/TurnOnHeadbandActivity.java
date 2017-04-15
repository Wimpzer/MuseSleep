package com.musesleep.musesleep;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseManagerAndroid;

import java.util.List;

public class TurnOnHeadbandActivity extends AppCompatActivity {

    private MuseManagerAndroid manager = null;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.turn_on_headband_activity);

        Fragment disconnectedFragment = new TurnOnHeadbandDisconnectedFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.turnOnFragment, disconnectedFragment);
        fragmentTransaction.commit();

        // XX this must come before other libmuse API calls; it loads the
        // library.
        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);

        manager.startListening();

        handler.post(tickUi);
    }

    @Override
    protected void onPause() {
        super.onPause();
        manager.stopListening();
        handler.removeCallbacks(tickUi);
    }

    @Override
    protected void onResume() {
        super.onResume();
        manager.startListening();
        handler.post(tickUi);
    }

    private final Runnable tickUi = new Runnable() {
        @Override
        public void run() {
            List<Muse> pairedMuses = manager.getMuses();

            if (pairedMuses.size() < 1) {

            } else {
                Fragment connectedFragment = new TurnOnHeadbandConnectedFragment();
                PairedMuses pairedMusesObject = new PairedMuses(pairedMuses);
                Bundle args = new Bundle();
                args.putSerializable("pairedMuses", pairedMusesObject);
                connectedFragment.setArguments(args);
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.turnOnFragment, connectedFragment);
                fragmentTransaction.commit();
            }

            handler.postDelayed(tickUi, 1000 / 60);
        }
    };
}
