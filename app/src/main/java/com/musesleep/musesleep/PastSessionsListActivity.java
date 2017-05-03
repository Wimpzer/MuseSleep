package com.musesleep.musesleep;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.musesleep.musesleep.Adapter.RecyclerViewAdapter;
import com.musesleep.musesleep.Object.FirebaseTimeObject;
import com.musesleep.musesleep.Object.PastSessionObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class PastSessionsListActivity extends MainActivity {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<PastSessionObject> pastSessions;

    private FirebaseDatabase myFirebaseInstance;
    private DatabaseReference myFirebaseBaseRef;
    private DatabaseReference myFirebaseTimeRef;

    private int amountLoaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.past_sessions_activity);

        // Sets the Firebase references
        myFirebaseInstance = FirebaseDatabase.getInstance();
        myFirebaseBaseRef = myFirebaseInstance.getReference();
        myFirebaseTimeRef = myFirebaseBaseRef.child("Time");

        addEventsToRecyclerView();

        recyclerView = (RecyclerView) findViewById(R.id.pastSessionsRecyclerView);

        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        amountLoaded = 0;
    }

    private void addEventsToRecyclerView() {
        myFirebaseTimeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<FirebaseTimeObject> timeObjects = new ArrayList<FirebaseTimeObject>();

                for(DataSnapshot child : dataSnapshot.getChildren()) {
                    FirebaseTimeObject timeObject = child.getValue(FirebaseTimeObject.class);
                    timeObjects.add(timeObject);
                }

                pastSessions = new ArrayList<>();
                adapter = new RecyclerViewAdapter(pastSessions);
                recyclerView.setAdapter(adapter);

                for(FirebaseTimeObject timeObject : timeObjects) {
                    //TODO: Create images according to timeOfDay and add to the PastSessionObject
                    String dateString = timeObject.getStartTime();

                    // Creates the time of day
                    int time = Integer.parseInt(dateString.substring(11,13));
                    String timeOfDay = "";
                    Drawable timeOfDayPicture = null;
                    if(0 <= time && time < 6) {
                        timeOfDay = "night";
                        timeOfDayPicture = ContextCompat.getDrawable(getAppContext(), R.drawable.night);
                    }else if(6 <= time && time < 12) {
                        timeOfDay = "morning";
                        timeOfDayPicture = ContextCompat.getDrawable(getAppContext(), R.drawable.morning);
                    }else if(12 <= time && time < 18) {
                        timeOfDay = "afternoon";
                        timeOfDayPicture = ContextCompat.getDrawable(getAppContext(), R.drawable.afternoon);
                    }else if(18 <= time && time < 24) {
                        timeOfDay = "evening";
                        timeOfDayPicture = ContextCompat.getDrawable(getAppContext(), R.drawable.evening);
                    }

                    // Creates the day of the week
                    Calendar calendar = new GregorianCalendar();
                    Date date = new Date();
                    SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS");
                    try {
                        date = date_format.parse(dateString);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    calendar.setTime(date);
                    int result = calendar.get(Calendar.DAY_OF_WEEK);
                    String dayOfWeek = "";
                    switch (result) {
                        case Calendar.MONDAY:
                            dayOfWeek = "Monday";
                            break;
                        case Calendar.TUESDAY:
                            dayOfWeek = "Tuesday";
                            break;
                        case Calendar.WEDNESDAY:
                            dayOfWeek = "Wednesday";
                            break;
                        case Calendar.THURSDAY:
                            dayOfWeek = "Thursday";
                            break;
                        case Calendar.FRIDAY:
                            dayOfWeek = "Friday";
                            break;
                        case Calendar.SATURDAY:
                            dayOfWeek = "Saturday";
                            break;
                        case Calendar.SUNDAY:
                            dayOfWeek = "Sunday";
                            break;
                    }

                    // Creates short date
                    String shortDate = dateString.substring(0, 10);

                    // Collects the above into a PastSessionObject
                    String upperText = dayOfWeek + " " + timeOfDay;
                    pastSessions.add(new PastSessionObject(upperText, shortDate, timeOfDayPicture));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("PastSessionsListAct", "onCancelled", databaseError.toException());
            }
        });
    }
}
