package com.musesleep.musesleep;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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

        Bundle bundle = getArguments();
        PairedMuses pairedMuses = (PairedMuses) bundle.getSerializable("pairedMuses");

        // Fetch the names and MacAddresses of connected muses and store them in string array
        int musesAmount = pairedMuses.getPairedMuses().size();
        String[] musesMacAddresses = new String[musesAmount];
        for (int i = 0; i < musesAmount; i++) {
            musesMacAddresses[i] = String.valueOf(pairedMuses.getPairedMuses().get(i).getName().concat(pairedMuses.getPairedMuses().get(i).getMacAddress()));
        }

        museListView = (ListView) rootView.findViewById(R.id.museListView);
        ArrayAdapter<String> listViewAdapter = new ArrayAdapter<>(getActivity(), R.layout.listview_item, musesMacAddresses);
        museListView.setAdapter(listViewAdapter);
        museListView.setOnItemClickListener(this);

        return rootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.putExtra("id", id);
        startActivity(intent);
    }
}
