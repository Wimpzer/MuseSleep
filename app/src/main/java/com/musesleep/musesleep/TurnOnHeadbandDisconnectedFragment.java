package com.musesleep.musesleep;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TurnOnHeadbandDisconnectedFragment extends Fragment {

    public TurnOnHeadbandDisconnectedFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.turn_on_headband_disconnected_fragment, container, false);
    }
}
