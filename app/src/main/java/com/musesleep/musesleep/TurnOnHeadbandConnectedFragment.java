package com.musesleep.musesleep;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseManagerAndroid;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class TurnOnHeadbandConnectedFragment extends Fragment implements OnItemClickListener {

    private ListView museListView;

    public TurnOnHeadbandConnectedFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.turn_on_headband_connected_fragment, container, false);

        // Fetch the names and MacAddresses of connected muses and store them in string array
        MuseManagerAndroid manager = MuseManager.getInstance().getManager();
        List<Muse> pairedMuses = manager.getMuses();

        int musesAmount = pairedMuses.size();
        String[] pairedMusesNames = new String[musesAmount];
        for (int i = 0; i < musesAmount; i++) {
            pairedMusesNames[i] = String.valueOf(pairedMuses.get(i).getName().concat(pairedMuses.get(i).getMacAddress()));
        }

        museListView = (ListView) rootView.findViewById(R.id.museListView);
        ArrayAdapter<String> listViewAdapter = new ArrayAdapter<>(getActivity(), R.layout.listview_item, pairedMusesNames);
        museListView.setAdapter(listViewAdapter);
        museListView.setOnItemClickListener(this);

        MuseManager.getInstance().setMuseList(pairedMuses);

        return rootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), SessionActivity.class);
        intent.putExtra("MusePosition", position);

        startActivity(intent);
    }
}
