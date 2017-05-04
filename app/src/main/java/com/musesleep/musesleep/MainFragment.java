package com.musesleep.musesleep;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import static android.app.Activity.RESULT_OK;

public class MainFragment extends Fragment implements View.OnClickListener {

    private static final int TIME_BUFFER_RESULT_CODE = 1;
    private static final int ALARM_SOUND_RESULT_CODE = 2;

    private View rootView;

    public MainFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.content_main, container, false);
        // Initiating clickable views and sets OnClickListener
        Button startSessionButton = (Button) rootView.findViewById(R.id.startSessionButton);
        startSessionButton.setOnClickListener(this);

        ImageView timeBufferImageView = (ImageView) rootView.findViewById(R.id.timeBufferImageView);
        timeBufferImageView.setOnClickListener(this);

        ImageView alarmSoundImageView = (ImageView) rootView.findViewById(R.id.alarmSoundImageView);
        alarmSoundImageView.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.startSessionButton) {
            Intent startSessionIntent = new Intent(getActivity(), TurnOnHeadbandActivity.class);
            startActivity(startSessionIntent);
        } else if (v.getId() == R.id.timeBufferImageView) {
            Intent timeBufferIntent = new Intent(getActivity(), ListViewActivity.class);
            timeBufferIntent.putExtra("headline", getString(R.string.time_buffer_headline));
            timeBufferIntent.putExtra("array", R.array.time_buffer_array);
            startActivityForResult(timeBufferIntent, TIME_BUFFER_RESULT_CODE);
        } else if (v.getId() == R.id.alarmSoundImageView) {
            Intent timeBufferIntent = new Intent(getActivity(), ListViewActivity.class);
            timeBufferIntent.putExtra("headline", getString(R.string.alarm_sound_headline));
            timeBufferIntent.putExtra("array", R.array.alarm_sound_array);
            startActivityForResult(timeBufferIntent, ALARM_SOUND_RESULT_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            String result = data.getStringExtra("selectedValue");
            if (requestCode == TIME_BUFFER_RESULT_CODE) {
                TextView timeBufferTextView = (TextView) rootView.findViewById(R.id.timeBufferTextView);
                timeBufferTextView.setText(result + " min");
            } else if (requestCode == ALARM_SOUND_RESULT_CODE) {
                TextView alarmSoundTextView = (TextView) rootView.findViewById(R.id.alarmSoundTextView);
                alarmSoundTextView.setText(result);
            }
        }
    }
}
