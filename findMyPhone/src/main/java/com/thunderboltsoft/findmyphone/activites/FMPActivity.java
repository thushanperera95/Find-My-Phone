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
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.revmob.RevMob;
import com.revmob.RevMobAdsListener;
import com.revmob.ads.banner.RevMobBanner;
import com.thunderboltsoft.findmyphone.services.FMPService;
import com.thunderboltsoft.ringmyphone.R;

import java.util.List;

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

    private boolean mCameraPermissionCheck = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnStop = (Button) findViewById(R.id.stop_button);
        mBtnStart = (Button) findViewById(R.id.start_button);
        mBtnSet = (Button) findViewById(R.id.set_button);
        mEditTxtFindPassword = (EditText) findViewById(R.id.command);
        mTxtStatus = (TextView) findViewById(R.id.txtStatus);

        stopFMPService();

        Dexter.initialize(getApplication());

        Toolbar myToolBar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(myToolBar);

        ActionBar sActionBar = getSupportActionBar();
        if (sActionBar != null) {
            sActionBar.setTitle(getResources().getString(R.string.app_name));
            sActionBar.setIcon(R.mipmap.ic_launcher);
        }

        mRevmob = RevMob.startWithListener(this, new RevMobAdsListener() {
            @Override
            public void onRevMobSessionIsStarted() {
                loadBanner();
            }

            @Override
            public void onRevMobSessionNotStarted(String s) {
                Log.i("Revmob", "Session failed to start");
            }
        }, "53140b72bc653cfa52882e01");

        // Opens a file called "PREFERENCES" that can only be used by our
        // application in order to store data such as settings
        mPreferences = getSharedPreferences("PREFERENCES", MODE_PRIVATE);

        retrieveLastCommand();

        checkPermissions();

        setUpListeners();
    }

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
            case R.id.action_privacy_policy:
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Privacy Policy");

                WebView wv = new WebView(this);
                wv.loadUrl("http://thushanperera95.com/privacy_policy_android.html");
                wv.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        view.loadUrl(url);

                        return true;
                    }
                });

                alert.setView(wv);
                alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                alert.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Starts the "Find My Phone" service
     */
    public void startFMPService() {
        Intent serviceIntent;

        setCommand();

        serviceIntent = new Intent(this, FMPService.class);
        serviceIntent.putExtra("command", mFindPassword); // Add mFindPassword keyword info
        serviceIntent.putExtra("camera_permission", mCameraPermissionCheck);
        // to the service intent

        stopFMPService();
        startService(serviceIntent);

        if (isMyServiceRunning()) { // Check then change button state
            mBtnStop.setVisibility(View.VISIBLE);
            mBtnStart.setVisibility(View.INVISIBLE);
            mTxtStatus.setText(R.string.status_running);
        }
    }

    private void stopFMPService() {
        if (isMyServiceRunning()) {
            stopService(new Intent(this, FMPService.class));

            mBtnStart.setVisibility(View.VISIBLE);
            mBtnStop.setVisibility(View.INVISIBLE);

            mTxtStatus.setText(R.string.status_stopped);
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

        mPreferences.edit().apply();
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

    public void loadBanner() {
        mBannerAd = mRevmob.preLoadBanner(this, new RevMobAdsListener() {
            @Override
            public void onRevMobAdReceived() {
                showAdBanner();
                Log.i("RevMob", "Banner Ready to be Displayed"); //At this point, the banner is ready to be displayed.
            }

            @Override
            public void onRevMobAdNotReceived(String message) {
                Log.i("RevMob", "Banner Not Failed to Load");
            }

            @Override
            public void onRevMobAdDisplayed() {
                Log.i("RevMob", "Banner Displayed");
            }
        });
    }

    /**
     * Creates a banner ad and displays it
     */
    private void showAdBanner() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup view = (ViewGroup) findViewById(R.id.bannerLayout);
                view.addView(mBannerAd);
                mBannerAd.show();
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

    private void checkPermissions() {
        Dexter.checkPermissionsOnSameThread(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {

                List<PermissionDeniedResponse> deniedPermissions = report.getDeniedPermissionResponses();
                for (PermissionDeniedResponse deniedPermission : deniedPermissions) {
                    if (deniedPermission.getPermissionName().equals(Manifest.permission.RECEIVE_SMS)) {
                        if (deniedPermission.isPermanentlyDenied()) {
                            String message = "Receive SMS is a required permission for the core functionality of this app!\nWithout this the app is unable to function as intended.\n\nPlease use the Application Manager under System Settings and allow the 'SMS' permission for this app.";

                            new AlertDialog.Builder(FMPActivity.this)
                                    .setTitle("Heads up")
                                    .setMessage(message)
                                    .setPositiveButton("Ok",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Finish activity
                                                    finish();
                                                }
                                            })
                                    .setNegativeButton("Settings",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Finish activity
                                                    startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
                                                    finish();
                                                }
                                            })
                                    .setCancelable(false)
                                    .show();
                        } else {
                            String message = "Receive SMS is a required permission for the core functionality of this app!\n" +
                                    "Without this the app is unable to function as intended.\n\n" +
                                    "Please re-run this app and allow this permission.";

                            new AlertDialog.Builder(FMPActivity.this)
                                    .setTitle("Heads up")
                                    .setMessage(message)
                                    .setPositiveButton("Ok",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    // Finish activity
                                                    finish();
                                                }
                                            })
                                    .setCancelable(false)
                                    .show();
                        }
                    } else if (deniedPermission.getPermissionName().equals(Manifest.permission.CAMERA)) {
                        Toast.makeText(getApplicationContext(), "Flashlight will not flash when trying to locate your phone", Toast.LENGTH_SHORT).show();
                        mCameraPermissionCheck = false;
                    }
                }

                boolean proceed = false;

                List<PermissionGrantedResponse> grantedPermissions = report.getGrantedPermissionResponses();
                for (PermissionGrantedResponse grantedPermission : grantedPermissions) {
                    if (grantedPermission.getPermissionName().equals(Manifest.permission.CAMERA)) {
                        mCameraPermissionCheck = true;
                    } else if (grantedPermission.getPermissionName().equals(Manifest.permission.RECEIVE_SMS)) {
                        proceed = true;
                    }
                }

                if (proceed) {
                    startFMPService();
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<com.karumi.dexter.listener.PermissionRequest> permissions, final PermissionToken token) {
                token.continuePermissionRequest(); // Just continue I guess, I dunno wtf else to do here
            }
        }, Manifest.permission.RECEIVE_SMS, Manifest.permission.CAMERA);
    }
}
