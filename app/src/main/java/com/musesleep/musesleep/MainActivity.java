package com.musesleep.musesleep;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    private static final int TIME_BUFFER_RESULT_CODE = 1;
    private static final int ALARM_SOUND_RESULT_CODE = 2;
    private static Context appContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // The ACCESS_COARSE_LOCATION permission is required to use the
        // BlueTooth LE library and must be requested at runtime for Android 6.0+
        // On an Android 6.0 device, the following code will display 2 dialogs,
        // one to provide context and the second to request the permission.
        // On an Android device running an earlier version, nothing is displayed
        // as the permission is granted from the manifest.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            DialogInterface.OnClickListener buttonListener =
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which){
                            dialog.dismiss();
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                                    0);
                        }
                    };

            AlertDialog introDialog = new AlertDialog.Builder(this)
                    .setTitle("Muse Needs Your Permission")
                    .setMessage("Muse needs a few permissions to work properly. On the next screens, tap \"Allow\" to proceed. If you deny, Muse will not work properly until you go into your Android settings and allow.")
                    .setPositiveButton("I Understand", buttonListener)
                    .create();
            introDialog.show();
        }

        // Saving the context for later use in MuseManager Singleton
        appContext = this;

        // Initiating clickable views and sets OnClickListener
        Button startSessionButton = (Button) findViewById(R.id.startSessionButton);
        startSessionButton.setOnClickListener(this);

        ImageView timeBufferImageView = (ImageView) findViewById(R.id.timeBufferImageView);
        timeBufferImageView.setOnClickListener(this);

        ImageView alarmSoundImageView = (ImageView) findViewById(R.id.alarmSoundImageView);
        alarmSoundImageView.setOnClickListener(this);
    }

    public static Context getAppContext() {
        return appContext;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_session) {
//            Intent sessionActivity = new Intent(this, SessionActivity.class);
//            startActivity(sessionActivity);
        } else if (id == R.id.nav_history) {
//            Intent historyIntent = new Intent(this, HistoryActivity.class);
//            startActivity(historyIntent);
        } else if (id == R.id.nav_settings) {
//            Intent settingsIntent = new Intent(this, SettingsActivity.class);
//            startActivity(settingsIntent);
        } else if (id == R.id.nav_feedback) {
//            Intent feedbackIntent = new Intent(this, FeedbackActivity.class);
//            startActivity(feedbackIntent);
        } else if (id == R.id.nav_signout) {

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.startSessionButton) {
            Intent startSessionIntent = new Intent(this, TurnOnHeadbandActivity.class);
            startActivity(startSessionIntent);
        } else if (v.getId() == R.id.timeBufferImageView) {
            Intent timeBufferIntent = new Intent(this, ListViewActivity.class);
            timeBufferIntent.putExtra("headline", getString(R.string.time_buffer_headline));
            timeBufferIntent.putExtra("array", R.array.time_buffer_array);
            startActivityForResult(timeBufferIntent, TIME_BUFFER_RESULT_CODE);
        } else if (v.getId() == R.id.alarmSoundImageView) {
            Intent timeBufferIntent = new Intent(this, ListViewActivity.class);
            timeBufferIntent.putExtra("headline", getString(R.string.alarm_sound_headline));
            timeBufferIntent.putExtra("array", R.array.alarm_sound_array);
            startActivityForResult(timeBufferIntent, ALARM_SOUND_RESULT_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            String result = data.getStringExtra("selectedValue");
            if (requestCode == TIME_BUFFER_RESULT_CODE) {
                TextView timeBufferTextView = (TextView) findViewById(R.id.timeBufferTextView);
                timeBufferTextView.setText(result + " min");
            } else if (requestCode == ALARM_SOUND_RESULT_CODE) {
                TextView alarmSoundTextView = (TextView) findViewById(R.id.alarmSoundTextView);
                alarmSoundTextView.setText(result);
            }
        }
    }
}
