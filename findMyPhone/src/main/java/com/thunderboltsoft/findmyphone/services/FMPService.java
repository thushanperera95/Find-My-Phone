/**
 * @README
 * @title: Find My Phone
 * @version: 1.0
 * @type: Service
 * @description: This is the service which will run in the background listening to incoming
 * SMSs which contain the words "fmp" or "stop". If it hears "fmp" the service
 * will switch on the strobe light and will ring the default ring tone at maximum
 * volume. If it hears "stop", the app will stop any ring tone ringing and strobe light.
 * @author: Thushan Perera
 * @publisher: ThunderboltSoft
 * @contact: kaozgamerdev@gmail.com
 */

package com.thunderboltsoft.findmyphone.services;

import java.io.IOException;
import java.util.Locale;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.thunderboltsoft.findmyphone.activites.FMPActivity;
import com.thunderboltsoft.ringmyphone.R;

public class FMPService extends Service {

    /*
     * This is 5 minutes in nanoseconds
     */
    long mDuration = 300000000000L;

    /*
     * The start time
     */
    long mStartTime;

    /*
     * Time that has passed
     */
    long mElapsedTime;

    /*
     * Sets up the intent filter
     */
    final IntentFilter mIntentFilter = new IntentFilter();

    /*
     * The mCommand word the service has to listen for
     */
    private String mCommand;

    /*
     * For use with playing the default ring tone of the device
     */
    private AudioManager mAudioManager;

    /*
     * Saves the current volume of the device so that app with return to the
     * device defaults
     */
    private int mCurrentVolume;

    /*
     * Contains info of default ring tone
     */
    private Uri mDefaultRingtoneNotification;

    /*
     * Holds the default ring tone of the device
     */
    private Ringtone mRingtone;

    /*
     * Specifies the intent filter
     */
    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

    // use this as an inner class like here or as a top-level class

    private Boolean PERMISSION_CAMERA_APPROVED = false;

    /*
     * Used to listen onto incoming sms
     */
    public BroadcastReceiver mSMSReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle bundle;
            Object[] pdusObj;
            SmsMessage[] messages;
            String message;
            String messAddr;

            // Bundles are used to pass data from a intent for proper use
            bundle = intent.getExtras();

            if (bundle != null) {

                // Gets the PDUs from the bundle
                // PDU is a protocol description unit and is used for sms
                // messages
                pdusObj = (Object[]) bundle.get("pdus");

                // Creates array of sms messages
                messages = new SmsMessage[pdusObj.length];

                // Fills up the sms array by extracting from the pdu
                for (int i = 0; i < pdusObj.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                }

                // Goes through all the SMSs one by one
                for (SmsMessage currentMessage : messages) {
                    messAddr = currentMessage.getOriginatingAddress();
                    message = currentMessage.getDisplayMessageBody();

                    messageReceived(message, messAddr, context);
                }
            }
        }
    };

    public FMPService() {
    }


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Notification note;

        // adds filters to filter out everything except incoming sms
        mIntentFilter.addAction(ACTION);

        // Priority is set high so that the incoming SMS is not "hijacked" by
        // other SMS clients
        mIntentFilter.setPriority(1000);

        // Registers the receiver for use by the application
        this.registerReceiver(this.mSMSReceiver, mIntentFilter);

        Intent i = new Intent(this, FMPActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        note = builder.setContentIntent(pi)
                .setSmallIcon(R.mipmap.ic_launcher).setTicker("Find My Phone service is running").setWhen(System.currentTimeMillis())
                .setAutoCancel(false).setContentTitle("Find My Phone")
                .setContentText("Service is running").build();

        startForeground(1, note);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopForeground(true);
        this.unregisterReceiver(this.mSMSReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mCommand = intent.getExtras().getString("command");
        PERMISSION_CAMERA_APPROVED = intent.getExtras().getBoolean("camera_permission");

        if (PERMISSION_CAMERA_APPROVED) {
            Log.v("FMP", "Camera Approved");
        } else {
            Log.v("FMP", "Camera Denied");
        }

        return START_REDELIVER_INTENT;
    }

    /*
     * If message was "fmp" it will call methods to start strobe light and ring
     * the default ring tone at maximum volume
     *
     * @param1: The message that was received
     *
     * @param2: The context of the app
     */
    protected void messageReceived(String message, String messAddr, Context context) {
        String commandReceived = message.toLowerCase(Locale.getDefault());

        // Checks if the received sms is the mCommand word
        if (commandReceived.equals(mCommand)) {

            // Starts ringing the default ring tone and maximum volume
            startRingMyPhone(context);

            if (PERMISSION_CAMERA_APPROVED) {
                if (!startFlashLight(context)) {    // If flashlight not supported then stop ringing
                    stopRingMyPhone();
                }
            }
        }

        this.unregisterReceiver(this.mSMSReceiver);
        this.registerReceiver(this.mSMSReceiver, mIntentFilter);
    }

    /*
     * Stops any ring tone that is ringing, if there is a ring tone ringing
     */
    protected void stopRingMyPhone() {

        // Used to access power states of various hardware on the device
        PowerManager powerm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        // Flag for whether the device screen is switched on or off
        boolean isScreenOn = false;

        // It will wait until the user switched the screen before proceeding
        // further
        while ((!isScreenOn) && (mElapsedTime < mDuration)) {
            isScreenOn = powerm.isScreenOn();
        }

        // If ring tone is still playing, it will stop the ring tone
        if (mRingtone.isPlaying()) {
            mRingtone.stop();
        }

        // The audio level of the RING stream will be reset back to its original
        // volume
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, mCurrentVolume,
                AudioManager.FLAG_ALLOW_RINGER_MODES);
    }

    /*
     * Starts ringing the default ring tone at maximum volume
     *
     * @param1: The context of the app
     */
    private void startRingMyPhone(Context context) {

        int streamMaxVolume;

        // Retrieves audio service from the list of system services
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Saves the current audio volume
        mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);

        // Retrieves the maximum volume supported by the stream
        streamMaxVolume = mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_RING);

        // Sets the stream volume to the maximum
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, streamMaxVolume,
                AudioManager.FLAG_ALLOW_RINGER_MODES
                        | AudioManager.FLAG_PLAY_SOUND);

        // Retrieves the default ring tone
        mDefaultRingtoneNotification = RingtoneManager
                .getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        // Plays the default ring tone
        mRingtone = RingtoneManager.getRingtone(context, mDefaultRingtoneNotification);
        mRingtone.play();

    }

    /*
     * Starts the strobe light. Will continue until user switches screen on.
     *
     * @param1: The context of the app
     *
     * @return: If the method was successful
     */
    private boolean startFlashLight(Context context) {
        Thread workerCameraThread, workerThread;

        // Creates a new thread t1
        // We will be making the thread sleep when the camera light is on and
        // off
        // This will simulate the "strobe light" effect
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                PowerManager powerm;
                boolean isScreenOn = false;

                // Retrieves a list of hardware features on the device
                powerm = (PowerManager) getSystemService(Context.POWER_SERVICE);

                // Sets the timer
                mStartTime = System.nanoTime();
                mElapsedTime = System.nanoTime() - mStartTime;

                while ((!isScreenOn) && (mElapsedTime < mDuration)) {

                    // The current elapsed time
                    mElapsedTime = System.nanoTime() - mStartTime;
                    isScreenOn = powerm.isScreenOn();
                }

                stopRingMyPhone();
            }
        });

        // Creates a new thread t1
        // We will be making the thread sleep when the camera light is on and
        // off
        // This will simulate the "strobe light" effect
        workerCameraThread = new Thread(new Runnable() {

            /*
             * Time in milliseconds on how long to keep the led light on
             */
            private int delayOn = 250;

            /*
             * Time in milliseconds on how long to keep the led light off
             */
            private int delayOff = 250;

            @Override
            public void run() {

                PowerManager powerm;
                boolean isScreenOn = false;
                Camera camera;
                final Parameters p;

                // Retrieves a list of hardware features on the device
                powerm = (PowerManager) getSystemService(Context.POWER_SERVICE);

                // Opens camera and retrieves camera parameters
                camera = Camera.open();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    try {
                        Log.v("FMP", "About to setPreviewTexture");
                        camera.setPreviewTexture(new SurfaceTexture(0));
                    } catch (IOException e) {
                    }
                }
                camera.startPreview();
                p = camera.getParameters();

                try {

                    // Sets the timer
                    mStartTime = System.nanoTime();
                    mElapsedTime = System.nanoTime() - mStartTime;

                    // If screen is off and user hasn't sent "stop" mCommand is
                    // continue with strobe light
                    // Continues as long as the elapsed time is less than the
                    // time of 5 minutes
                    while ((!isScreenOn) && (mElapsedTime < mDuration)) {

                        // The current elapsed time
                        mElapsedTime = System.nanoTime() - mStartTime;

                        // Another check if screen is turned on
                        isScreenOn = powerm.isScreenOn();

                        // Sets led light on for "delayon" milliseconds
                        p.setFlashMode(Parameters.FLASH_MODE_TORCH);
                        camera.setParameters(p);
                        Thread.sleep(delayOn);

                        // //Sets led light off for "delayoff" milliseconds
                        p.setFlashMode(Parameters.FLASH_MODE_OFF);
                        camera.setParameters(p);
                        Thread.sleep(delayOff);
                    }

                    stopRingMyPhone();
                } catch (InterruptedException e) {

                }

                // Releases the camera so that other apps can use the camera
                camera.stopPreview();
                camera.release();
            }
        });


        if (PERMISSION_CAMERA_APPROVED) {
            // Starts the thread
            workerCameraThread.start();
        } else {
            workerThread.start();
        }

        return true;
    }
}
