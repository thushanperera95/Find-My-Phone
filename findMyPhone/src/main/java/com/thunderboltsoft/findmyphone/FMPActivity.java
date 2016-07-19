/**
 * @README
 * @title: Find My Phone
 * @version: 1.0
 * @type: Main Activity
 * @description: Sending a mFindPassword word from another device will activate the ring tone and led flash
 * of the "missing" device so that the owner can locate the "missing" device
 * @author: Thushan Perera
 * @publisher: ThunderboltSoft
 * @contact: kaozgamerdev@gmail.com
 */

package com.thunderboltsoft.findmyphone;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.revmob.RevMob;
import com.revmob.RevMobAdsListener;
import com.revmob.ads.banner.RevMobBanner;
import com.thunderboltsoft.ringmyphone.R;

@SuppressWarnings("unused")
public class FMPActivity extends AppCompatActivity implements OnClickListener {

    // RevMob instance
    private RevMob mRevmob;

    // Reference to the banner ad
    private RevMobBanner mBannerAd;

    // Holds references to all the buttons in the activity
    private Button mBtnStop;
    private Button mBtnStart;
    private Button mBtnSet;

    // Holds references to the edit text box in the activity
    private EditText mEditTxtFindPassword;

    // Holds the current passcode of the app
    private String mFindPassword;

    // Stores the reference to the app's shared preferences instance
    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnStop = (Button) findViewById(R.id.stop_button);
        mBtnStart = (Button) findViewById(R.id.start_button);
        mBtnSet = (Button) findViewById(R.id.set_button);
        mEditTxtFindPassword = (EditText) findViewById(R.id.command);

        Toolbar myToolBar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(myToolBar);

        ActionBar sActionBar = getSupportActionBar();
        if (sActionBar != null) {
            sActionBar.setTitle(getResources().getString(R.string.app_name));
            sActionBar.setIcon(R.mipmap.ic_launcher);
        }

//        mRevmob = RevMob.startWithListener(this, new RevMobAdsListener() {
//            @Override
//            public void onRevMobSessionIsStarted() {
//                showAdBanner();
//            }
//        }, "53140b72bc653cfa52882e01");

        // Opens a file called "PREFERENCES" that can only be used by our
        // application in order to store data such as settings
        mPreferences = getSharedPreferences("PREFERENCES", MODE_PRIVATE);

        retrieveLastCommand();
        startFMPService();
        setUpListeners();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) { // Check which button was clicked

            case R.id.set_button:
                stopService(new Intent(this, FMPService.class));
                startFMPService();

                if (isMyServiceRunning()) { // Check then change button state
                    mBtnStop.setVisibility(View.VISIBLE);
                    mBtnStart.setVisibility(View.INVISIBLE);
                }

                Toast.makeText(this, "Passcode has been updated!", Toast.LENGTH_SHORT).show();
                break;

            case R.id.stop_button:
                stopService(new Intent(this, FMPService.class));

                if (!isMyServiceRunning()) { // Check then change button state
                    mBtnStart.setVisibility(View.VISIBLE);
                    mBtnStop.setVisibility(View.INVISIBLE);
                }
                break;

            case R.id.start_button:
                startFMPService();

                if (isMyServiceRunning()) { // Check then change button state
                    mBtnStop.setVisibility(View.VISIBLE);
                    mBtnStart.setVisibility(View.INVISIBLE);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_info:
                showHowtoDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Starts the "Find My Phone" service
     */
    private void startFMPService() {
        Intent serviceIntent;

        setCommand();

        serviceIntent = new Intent(this, FMPService.class);
        serviceIntent.putExtra("command", mFindPassword); // Add mFindPassword keyword info
        // to the service intent

        if (isMyServiceRunning()) { // Check if still running, true = stop
            // existing service
            stopService(new Intent(this, FMPService.class));
        }

        startService(serviceIntent);
    }

    /**
     * Updates the mFindPassword variable by retrieving new word from the edit text
     * box
     */
    private void setCommand() {
        mFindPassword = mEditTxtFindPassword.getText().toString();

        // Saves the mFindPassword by writing it to the PREFERENCES file
        mPreferences.edit().putString("command", mFindPassword).commit();
    }

    /**
     * Opens the "How to use" dialog box
     */
    public void showHowtoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("How to use...");

        // Sets the view of the dialog box
        builder.setView(LayoutInflater.from(this).inflate(R.layout.dialog_help,
                null));

        // Ads a "OK" button to close the dialog box and return to where the
        // user was
        builder.setPositiveButton("OK",
                new android.content.DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        // OK, go back to Main menu
                    }
                });

        // If the user clicks on anywhere outside the dialog box, it will return
        // the user to where the user was
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                // OK, go back to Main menu
            }
        });

        builder.show();
    }

    /**
     * Checks if the "Find My Phone" service is still running
     *
     * @return boolean True if the service is running, else false
     */
    private boolean isMyServiceRunning() {

        // Gets list of services running in the system
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        // Foreach loop going through all the services and find if service
        // exists
        for (RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (FMPService.class.getName().equals(
                    service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a banner ad and displays it
     */
    private void showAdBanner() {
        mBannerAd = mRevmob.createBanner(this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup view = (ViewGroup) findViewById(R.id.bannerLayout);
                view.addView(mBannerAd);
            }
        });
    }

    /**
     * Sets up on click listeners to all the buttons
     */
    private void setUpListeners() {
        mBtnStart.setOnClickListener(this);
        mBtnStop.setOnClickListener(this);
        mBtnSet.setOnClickListener(this);
    }

    /**
     * Retrieves the last mFindPassword word saved from the PREFERENCES FILE
     */
    private void retrieveLastCommand() {
        mFindPassword = mPreferences.getString("command", "");
        if ((mFindPassword.equals("")) || (mFindPassword.equals(null))) { // If Empty then
            // default to "fmp"
            mFindPassword = "fmp";
        }

        mEditTxtFindPassword.setText(mFindPassword);
    }
}
