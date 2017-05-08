package com.musesleep.musesleep;

import android.content.Intent;
import android.os.Bundle;
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
    private final String FIREBASE_TIME_TAG = "Time";
    private final String FIREBASE_STAGE_TIME_TAG = "TimeInStage";

    private FirebaseDatabase myFirebaseInstance;
    private DatabaseReference myFirebaseTimeRef;
    private DatabaseReference myFirebaseStageTimeRef;
    private String firebaseSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.past_session_activity);

        // Get the values from intent
        Intent intent = getIntent();
        firebaseSessionId = intent.getExtras().getString("sessionId");

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
                    long minutesOfValue = (long) valueInSeconds/60;
                    int secondsOfValue = valueInSeconds % 60;
                    String value = minutesOfValue + ":" + secondsOfValue;
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
                stageOneProportion = secondsInStageOne/totalTime;
                stageTwoProportion = secondsInStageTwo/totalTime;
                stageThreeProportion = secondsInStageThree/totalTime;
                stageFourProportion = secondsInStageFour/totalTime;

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
    }

    @Override
    public void onClick(View v) {
        finish();
    }
}
