package com.musesleep.musesleep;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseListener;
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
        manager.setMuseListener(new MuseListener() {
            @Override
            public void museListChanged() {
                List<Muse> pairedMuses = manager.getMuses();
                if (pairedMuses.size() >= 1) {
                    Fragment connectedFragment = new TurnOnHeadbandConnectedFragment();

                    // Fetch the names and MacAddresses of connected muses and store them in string array
                    int musesAmount = pairedMuses.size();
                    String[] pairedMusesNames = new String[musesAmount];
                    for (int i = 0; i < musesAmount; i++) {
                        pairedMusesNames[i] = String.valueOf(pairedMuses.get(i).getName().concat(pairedMuses.get(i).getMacAddress()));
                    }

                    Bundle args = new Bundle();
                    args.putStringArray("pairedMuses", pairedMusesNames);
                    connectedFragment.setArguments(args);

                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.replace(R.id.turnOnFragment, connectedFragment);
                    fragmentTransaction.commit();
                }
            }
        });

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
        super.onDestroy();
        manager.stopListening();
    }

}
