package com.musesleep.musesleep;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;
import com.musesleep.musesleep.Adapter.MuseAdapter;

import java.util.List;

public class TurnOnHeadbandActivity extends AppCompatActivity {

    private MuseManagerAndroid manager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.turn_on_headband_activity);

        Fragment disconnectedFragment = new TurnOnHeadbandDisconnectedFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.turnOnFragment, disconnectedFragment);
        fragmentTransaction.commit();

        MuseAdapter.getInstance().setContext();
        manager = MuseAdapter.getInstance().getManager();
        MuseListener museListener = new MuseListener() {
            @Override
            public void museListChanged() {
                List<Muse> pairedMuses = manager.getMuses();
                if (pairedMuses.size() >= 1) {
                    Fragment connectedFragment = new TurnOnHeadbandConnectedFragment();

                    manager.stopListening();

                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.replace(R.id.turnOnFragment, connectedFragment);
                    fragmentTransaction.commit();
                }
            }
        };
        manager.setMuseListener(museListener);

        manager.startListening();
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
        manager.stopListening();
        super.onDestroy();
    }

}
