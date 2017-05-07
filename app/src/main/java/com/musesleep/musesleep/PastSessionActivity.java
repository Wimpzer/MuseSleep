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

    private FirebaseDatabase myFirebaseInstance;
    private DatabaseReference myFirebaseBaseRef;
    private DatabaseReference myFirebaseTimeRef;
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
        myFirebaseBaseRef = myFirebaseInstance.getReference();

        // .setPersistenceEnalbed(true) needs to added for offline use
        myFirebaseTimeRef = myFirebaseInstance.getReference(FIREBASE_TIME_TAG).child(firebaseSessionId);

        // Initiates the views
        final TextView timeTextView = (TextView) findViewById(R.id.timeTextView);
        ImageView graphImageView = (ImageView) findViewById(R.id.graphImageView);
        TextView stageOneTextView = (TextView) findViewById(R.id.stageOneTextView);
        TextView stageTwoTextView = (TextView) findViewById(R.id.stageTwoTextView);
        TextView stageThreeTextView = (TextView) findViewById(R.id.stageThreeTextView);
        TextView stageFourTextView = (TextView) findViewById(R.id.stageFourTextView);
        TextView sleepQualityTextView = (TextView) findViewById(R.id.sleepQualityTextView);
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
        stageOneTextView.setText("5:00");
        stageTwoTextView.setText("5:00");
        stageThreeTextView.setText("5:00");
        stageFourTextView.setText("5:00");

        // TODO: Rate sleep quality baseret på tid i sleep stages
        sleepQualityTextView.setText("Good");

        backButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        finish(); //TODO: Gå til startskærmen hvis man kommer fra SessionActivity
    }
}
