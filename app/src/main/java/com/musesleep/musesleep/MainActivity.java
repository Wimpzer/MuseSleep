package com.musesleep.musesleep;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static Context appContext;
    protected DrawerLayout drawer;

    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fragment main = new MainFragment();
        fragmentManager = getSupportFragmentManager();
        replaceFragment(main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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

        // Saving the context for later use in MuseAdapter Singleton
        appContext = this;
    }

    private void replaceFragment(Fragment fragment) {
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainFragmentHolder, fragment);
        fragmentTransaction.commit();
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
            Fragment f = this.getSupportFragmentManager().findFragmentById(R.id.mainFragmentHolder);
            if(f instanceof MainFragment)
                super.onBackPressed();
            else {
                Fragment fragment = new MainFragment();
                replaceFragment(fragment);
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment = null;

        if (id == R.id.nav_session) {
            fragment = new MainFragment();
        } else if (id == R.id.nav_history) {
            fragment = new PastSessionsListFragment();
        } else if (id == R.id.nav_settings) {
//            Intent settingsIntent = new Intent(this, SettingsActivity.class);
//            startActivity(settingsIntent);
        } else if (id == R.id.nav_feedback) {
//            Intent feedbackIntent = new Intent(this, FeedbackActivity.class);
//            startActivity(feedbackIntent);
        } else if (id == R.id.nav_signout) {

        }

        replaceFragment(fragment);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
