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

package com.thunderboltsoft.findmyphone.activites;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.TextView;
import android.widget.Toast;

import com.revmob.RevMob;
import com.revmob.RevMobAdsListener;
import com.revmob.ads.banner.RevMobBanner;
import com.thunderboltsoft.findmyphone.services.FMPService;
import com.thunderboltsoft.ringmyphone.R;

import java.util.ArrayList;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
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

    private TextView mTxtStatus;

    public static final int MULTIPLE_PERMISSIONS = 10; // code you want.
    public static final int SMS_PERMISSIONS = 11;

    String[] permissionsList = new String[]{
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET
    };

    boolean[] permissionsResults = new boolean[]{
            true,
            true,
            true,
            true
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        if (checkPermissions()) {
//            // permissions granted.
//        } else {
//            // show dialog informing them that we lack certain permissions
//        }

        mBtnStop = (Button) findViewById(R.id.stop_button);
        mBtnStart = (Button) findViewById(R.id.start_button);
        mBtnSet = (Button) findViewById(R.id.set_button);
        mEditTxtFindPassword = (EditText) findViewById(R.id.command);
        mTxtStatus = (TextView) findViewById(R.id.txtStatus);

        Toolbar myToolBar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(myToolBar);

        ActionBar sActionBar = getSupportActionBar();
        if (sActionBar != null) {
            sActionBar.setTitle(getResources().getString(R.string.app_name));
            sActionBar.setIcon(R.mipmap.ic_launcher);
        }

//        mRevmob = RevMob.startWithListener(this, new RevMobAdsListener()) {
//            @Override
//            public void onRevMobSessionIsStarted() {
//                showAdBanner();
//            }
//        }, "53140b72bc653cfa52882e01");

        // Opens a file called "PREFERENCES" that can only be used by our
        // application in order to store data such as settings
        mPreferences = getSharedPreferences("PREFERENCES", MODE_PRIVATE);

        retrieveLastCommand();
        FMPActivityPermissionsDispatcher.startFMPServiceWithCheck(this);
        setUpListeners();
    }


//    private boolean checkPermissions() {
//        int result;
//        List<String> listPermissionsNeeded = new ArrayList<>();
//        for (String p : permissionsList) {
//            result = ContextCompat.checkSelfPermission(this, p);
//            if (result != PackageManager.PERMISSION_GRANTED) {
//                listPermissionsNeeded.add(p);
//            }
//        }
//        if (!listPermissionsNeeded.isEmpty()) {
//            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
//            return false;
//        }
//        return true;
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
//        switch (requestCode) {
//            case MULTIPLE_PERMISSIONS: {
//                for (int i = 0; i < permissionsList.length; i++) {
//                    permissionsResults[i] = ContextCompat.checkSelfPermission(this, permissionsList[i]) != PackageManager.PERMISSION_DENIED;
//                }
//
//                while ( (!permissionsResults[0]) || (!permissionsResults[1]) ) {
//                    new AlertDialog.Builder(this)
//                            .setTitle("Heads Up")
//                            .setMessage("This app need the send and receive sms permission in order to listen to the 'Find My Phone' command")
//                            .show();
//
//                    List<String> listPermissionsNeeded = new ArrayList<>();
//
//                    if (ContextCompat.checkSelfPermission(this, permissionsList[0]) != PackageManager.PERMISSION_GRANTED) {
//                        listPermissionsNeeded.add(permissionsList[0]);
//                    }
//
//                    if (ContextCompat.checkSelfPermission(this, permissionsList[1]) != PackageManager.PERMISSION_GRANTED) {
//                        listPermissionsNeeded.add(permissionsList[1]);
//                    }
//
//                    ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), SMS_PERMISSIONS);
//                }
//            }
//
//            case SMS_PERMISSIONS: {
//                if (ContextCompat.checkSelfPermission(this, permissionsList[0]) == PackageManager.PERMISSION_GRANTED) {
//                    permissionsResults[0] = true;
//                }
//
//                if (ContextCompat.checkSelfPermission(this, permissionsList[1]) == PackageManager.PERMISSION_GRANTED) {
//                    permissionsResults[1] = true;
//                }
//
//                while ( (!permissionsResults[0]) || (!permissionsResults[1]) ) {
//                    new AlertDialog.Builder(this)
//                            .setTitle("Heads Up")
//                            .setMessage("This app need the send and receive sms permission in order to listen to the 'Find My Phone' command")
//                            .show();
//
//                    List<String> listPermissionsNeeded = new ArrayList<>();
//
//                    if (ContextCompat.checkSelfPermission(this, permissionsList[0]) != PackageManager.PERMISSION_GRANTED) {
//                        listPermissionsNeeded.add(permissionsList[0]);
//                    }
//
//                    if (ContextCompat.checkSelfPermission(this, permissionsList[1]) != PackageManager.PERMISSION_GRANTED) {
//                        listPermissionsNeeded.add(permissionsList[1]);
//                    }
//
//                    ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), SMS_PERMISSIONS);
//                }
//            }
//
//            stopService(new Intent(this, FMPService.class));
//            startFMPService();
//        }
//    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) { // Check which button was clicked

            case R.id.set_button:
                startFMPService();
                Toast.makeText(this, "Passcode has been updated!", Toast.LENGTH_SHORT).show();
                break;

            case R.id.stop_button:
                stopFMPService();
                break;

            case R.id.start_button:
                startFMPService();
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
//            case R.id.action_settings:
//                Intent i = new Intent(getBaseContext(), SettingsActivity.class);
//                startActivity(i);
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Starts the "Find My Phone" service
     */
    @NeedsPermission(Manifest.permission.RECEIVE_SMS)
    public void startFMPService() {
        Intent serviceIntent;

        setCommand();

        serviceIntent = new Intent(this, FMPService.class);
        serviceIntent.putExtra("command", mFindPassword); // Add mFindPassword keyword info
        serviceIntent.putExtra("permissions_results", permissionsResults);
        // to the service intent

        stopFMPService();
        startService(serviceIntent);

        if (isMyServiceRunning()) { // Check then change button state
            mBtnStop.setVisibility(View.VISIBLE);
            mBtnStart.setVisibility(View.INVISIBLE);
            mTxtStatus.setText("RUNNING");
        }
    }

//    @OnShowRationale(Manifest.permission.CAMERA)
//    void showRationaleForReceiveSMS(final PermissionRequest request) {
//        new AlertDialog.Builder(this)
//                .setMessage("This app needs to listen to incoming SMS, so that on receiving the command code, the find my phone functions can be carried out.\n" +
//                        "Therefore the 'Request SMS' is required.\n" +
//                        "No personal data will be collected or stored.")
//                .setPositiveButton("Allow",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialogInterface, int i) {
//                                request.proceed();
//                            }
//                        })
//                .setNegativeButton("Deny",
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialogInterface, int i) {
//                                request.cancel();
//                            }
//                        })
//                .show();
//    }

    @OnPermissionDenied(Manifest.permission.RECEIVE_SMS)
    public void showDeniedForReceiveSMS() {
        new AlertDialog.Builder(this)
                .setTitle("Heads Up")
                .setMessage("Receive SMS is a required permission so that the app can listen to incoming SMS containing the command code. Please allow it.")
                .setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                startFMPService();
                            }
                        })
                .show();
    }

    @OnNeverAskAgain(Manifest.permission.RECEIVE_SMS)
    public void showNeverAskAgainReceiveSMS() {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Receive SMS is a required permission for hte core functionality of this app!\nWithout this the app is unable to function as intended.\nPlease go into system settings and allow the 'Receive SMS' permission for this app.")
                .setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Finish activity
                                finish();
                            }
                        })
                .show();
    }

    private void stopFMPService() {
        if (isMyServiceRunning()) {
            stopService(new Intent(this, FMPService.class));

            mBtnStart.setVisibility(View.VISIBLE);
            mBtnStop.setVisibility(View.INVISIBLE);

            mTxtStatus.setText("STOPPED");
        }


    }

    /**
     * Updates the mFindPassword variable by retrieving new word from the edit text
     * box
     */
    private void setCommand() {
        mFindPassword = mEditTxtFindPassword.getText().toString();

        // Saves the mFindPassword by writing it to the PREFERENCES file
        mPreferences.edit().putString("command", mFindPassword).apply();
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
