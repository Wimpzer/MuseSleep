package com.musesleep.musesleep;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.musesleep.musesleep.object.FirebaseTimeObject;

public class PastSessionActivity extends AppCompatActivity implements View.OnClickListener {
    private String FIREBASE_TIME_TAG;
    private String FIREBASE_STAGE_TIME_TAG;

    private FirebaseDatabase myFirebaseInstance;
    private DatabaseReference myFirebaseTimeRef;
    private DatabaseReference myFirebaseStageTimeRef;
    private String firebaseSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.past_session_activity);

        setTags();

        // Get the values from intent
        Intent intent = getIntent();
        firebaseSessionId = intent.getExtras().getString("sessionId");
        boolean isCrashed = intent.getExtras().getBoolean("isCrashed");

        // Sets the Firebase references to the current sessionId
        myFirebaseInstance = FirebaseDatabase.getInstance();

        // .setPersistenceEnalbed(true) needs to added for offline use
        myFirebaseTimeRef = myFirebaseInstance.getReference(FIREBASE_TIME_TAG).child(firebaseSessionId);
        myFirebaseStageTimeRef = myFirebaseInstance.getReference(FIREBASE_STAGE_TIME_TAG).child(firebaseSessionId);

        // Initiates the views
        final TextView timeTextView = (TextView) findViewById(R.id.timeTextView);
        ImageView graphImageView = (ImageView) findViewById(R.id.graphImageView);
        final TextView stageOneTextView = (TextView) findViewById(R.id.stageOneTextView);
        final TextView stageTwoTextView = (TextView) findViewById(R.id.stageTwoTextView);
        final TextView stageThreeTextView = (TextView) findViewById(R.id.stageThreeTextView);
        final TextView stageFourTextView = (TextView) findViewById(R.id.stageFourTextView);
        final TextView sleepQualityTextView = (TextView) findViewById(R.id.sleepQualityTextView);
        Button backButton = (Button) findViewById(R.id.backButton);

        myFirebaseTimeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                FirebaseTimeObject timeObject = dataSnapshot.getValue(FirebaseTimeObject.class);

                String startTime = timeObject.getStartTime().substring(11,16);
                String startDay = timeObject.getStartDay().substring(0,3);
                String endTime = timeObject.getEndTime().substring(11,16);
                String endDay = timeObject.getEndDay().substring(0,3);

                String timeText = startTime + " " + startDay + " - " + endTime + " " + endDay;
                timeTextView.setText(timeText);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("PastSessionActivity", "onCancelled", databaseError.toException());
            }
        });

        // TODO: Lav en Listener som overstående bare med sleepStages.
        myFirebaseStageTimeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                int secondsInStageOne = 0;
                double stageOneProportion = 0;
                int secondsInStageTwo = 0;
                double stageTwoProportion = 0;
                int secondsInStageThree = 0;
                double stageThreeProportion = 0;
                int secondsInStageFour = 0;
                double stageFourProportion = 0;

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    int stageTime = Integer.parseInt(child.getKey());
                    int valueInSeconds = child.getValue(Integer.class);
                    long valueInHours = (long) (valueInSeconds/60)/60;
                    long valueInMinutes = (long) (valueInSeconds-(valueInHours*60*60))/60;
                    String secondsOfValue = Integer.toString(valueInSeconds % 60);
                    String minutesOfValue = String.valueOf(valueInMinutes);
                    String hoursOfValue = String.valueOf(valueInHours);
                    if(secondsOfValue.length() == 1) {
                        secondsOfValue = "0" + secondsOfValue;
                    }
                    if(minutesOfValue.length() == 1) {
                        minutesOfValue = "0" + minutesOfValue;
                    }
                    if(hoursOfValue.length() == 1) {
                        hoursOfValue = "0" + hoursOfValue;
                    }
                    String value = hoursOfValue + ":" + minutesOfValue + ":" + secondsOfValue;
                    if(stageTime == 1) {
                        stageOneTextView.setText(value);
                        secondsInStageOne = valueInSeconds;
                    }else if(stageTime == 2) {
                        stageTwoTextView.setText(value);
                        secondsInStageTwo = valueInSeconds;
                    }else if(stageTime == 3) {
                        stageThreeTextView.setText(value);
                        secondsInStageThree = valueInSeconds;
                    }else if(stageTime == 4) {
                        stageFourTextView.setText(value);
                        secondsInStageFour = valueInSeconds;
                    }
                }

                int totalTime = secondsInStageOne + secondsInStageTwo + secondsInStageThree + secondsInStageFour;
                if(totalTime != 0) {
                    stageOneProportion = secondsInStageOne / totalTime;
                    stageTwoProportion = secondsInStageTwo / totalTime;
                    stageThreeProportion = secondsInStageThree / totalTime;
                    stageFourProportion = secondsInStageFour / totalTime;
                }

                String sleepQuality = "";
                // TODO: Rate sleep quality baseret på tid i sleep stages
                if(stageOneProportion == 1)
                    sleepQuality = "Bad";
                sleepQualityTextView.setText(sleepQuality);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("PastSessionActivity", "onCancelled", databaseError.toException());
            }
        });
        backButton.setOnClickListener(this);

        if(isCrashed) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Lost the connection to the muse and therefore ended the session");
            builder.setNeutralButton("Okay", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.create().show();
        }
    }

    private void setTags() {
        FIREBASE_TIME_TAG = getResources().getString(R.string.firebase_time_tag);
        FIREBASE_STAGE_TIME_TAG = getResources().getString(R.string.firebase_stage_time_tag);
    }

    @Override
    public void onClick(View v) {
        finish();
    }
}
