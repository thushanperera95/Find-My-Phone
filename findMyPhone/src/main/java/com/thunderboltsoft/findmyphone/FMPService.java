/**
 * @README
 *
 * @title: Find My Phone
 * @version: 1.0
 * @type: Service
 *
 * @description: This is the service which will run in the background listening to incoming
 * 					SMSs which contain the words "fmp" or "stop". If it hears "fmp" the service
 * 					will switch on the strobe light and will ring the default ring tone at maximum
 * 					volume. If it hears "stop", the app will stop any ring tone ringing and strobe light.
 *
 * @author: Thushan Perera
 * @publisher: ThunderboltSoft
 * @contact: kaozgamerdev@gmail.com
 */

package com.thunderboltsoft.findmyphone;

import java.util.Locale;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.thunderboltsoft.ringmyphone.R;

public class FMPService extends Service {

	/*
	 * This is 5 minutes in nanoseconds
	 */
	long duration = 300000000000L;

	/*
	 * The start time
	 */
	long startTime;

	/*
	 * Time that has passed
	 */
	long elapsedTime;

	/*
	 * Sets up the intent filter
	 */
	final IntentFilter theFilter = new IntentFilter();

	/*
	 * The command word the service has to listen for
	 */
	private String command;

	/*
	 * For use with playing the default ring tone of the device
	 */
	private AudioManager audioManager;

	/*
	 * Saves the current volume of the device so that app with return to the
	 * device defaults
	 */
	private int currentVolume;

	/*
	 * Contains info of default ring tone
	 */
	private Uri notification;

	/*
	 * Holds the default ring tone of the device
	 */
	private Ringtone r;

	/*
	 * Specifies the intent filter
	 */
	private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

	// use this as an inner class like here or as a top-level class


	/*
	 * Used to listen onto incoming sms
	 */
	public BroadcastReceiver yourReceiver = new BroadcastReceiver() {

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

	public FMPService() {}


	@Override
	public IBinder onBind(Intent arg0) {

		// We don't actually do any binding in this app
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Log.v("CHECK", "INside onCreate");

//		yourReceiver = new MyReceiver(this);

		Notification note;

        // adds filters to filter out everything except incoming sms
        theFilter.addAction(ACTION);

        // Priority is set high so that the incoming SMS is not "hijacked" by
        // other SMS clients
        theFilter.setPriority(1000);

        // Registers the receiver for use by the application
        this.registerReceiver(this.yourReceiver, theFilter);

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
		this.unregisterReceiver(this.yourReceiver);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		command = intent.getExtras().getString("command");

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

		Log.v("CHECK", "INside messageReceived");

		// Checks if the received sms is the command word
		if (commandReceived.equals(command)) {

			Toast.makeText(getApplicationContext(), "Test", Toast.LENGTH_SHORT).show();

//			SmsManager sms = SmsManager.getDefault();
//
//			sms.sendTextMessage(messAddr, null, coords, null, null);

			// Starts ringing the default ring tone and maximum volume
			startRingMyPhone(context);

			if (!startFlashLight(context)) {    // If flashlight not supported then stop ringing
				stopRingMyPhone();
			}
		}
//
//		// TODO Need to check if this is needed
		this.unregisterReceiver(this.yourReceiver);
		this.registerReceiver(this.yourReceiver, theFilter);
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
		while ((!isScreenOn) && (elapsedTime < duration)) {
			isScreenOn = powerm.isScreenOn();
		}

		// If ring tone is still playing, it will stop the ring tone
		if (r.isPlaying()) {
			r.stop();
		}

		// The audio level of the RING stream will be reset back to its original
		// volume
		audioManager.setStreamVolume(AudioManager.STREAM_RING, currentVolume,
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
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		// Saves the current audio volume
		currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);

		// Retrieves the maximum volume supported by the stream
		streamMaxVolume = audioManager
				.getStreamMaxVolume(AudioManager.STREAM_RING);

		// Sets the stream volume to the maximum
		audioManager.setStreamVolume(AudioManager.STREAM_RING, streamMaxVolume,
				AudioManager.FLAG_ALLOW_RINGER_MODES
						| AudioManager.FLAG_PLAY_SOUND);

		// Retrieves the default ring tone
		notification = RingtoneManager
				.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

		// Plays the default ring tone
		r = RingtoneManager.getRingtone(context, notification);
		r.play();

	}

	/*
	 * Starts the strobe light. Will continue until user switches screen on.
	 * 
	 * @param1: The context of the app
	 * 
	 * @return: If the method was successful
	 */
	private boolean startFlashLight(Context context) {

		boolean isSuccessful;
		PackageManager pm;
		Thread t1;

		// If the device has no camera then this will remain false
		// Thereby skipping the remaining code and jumping to the parent method
		isSuccessful = false;

		// Retrieves list of packages on the phone
		pm = context.getPackageManager();

		// Checks if the device has a camera
		if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			return isSuccessful;
		}

		// Creates a new thread t1
		// We will be making the thread sleep when the camera light is on and
		// off
		// This will simulate the "strobe light" effect
		t1 = new Thread(new Runnable() {

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
				boolean isScreenOn;
				Camera camera;
				final Parameters p;

				// Retrieves a list of hardware features on the device
				powerm = (PowerManager) getSystemService(Context.POWER_SERVICE);

				// Will check if the screen is switched on so that execution
				// will be stopped
				isScreenOn = false;

				// Opens camera and retrieves camera parameters
				camera = Camera.open();
				p = camera.getParameters();

				try {

					// Sets the timer
					startTime = System.nanoTime();
					elapsedTime = System.nanoTime() - startTime;

					// If screen is off and user hasn't sent "stop" command is
					// continue with strobe light
					// Continues as long as the elapsed time is less than the
					// time of 5 minutes
					while ((!isScreenOn) && (elapsedTime < duration)) {

						// The current elapsed time
						elapsedTime = System.nanoTime() - startTime;

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
				} catch (InterruptedException e) {

				}

				// Releases the camera so that other apps can use the camera
				camera.release();
			}
		});

		// Starts the thread
		t1.start();

		// Calls to stop phone ringing
		// Main thread will continue to even though another method is processed
		// in another thread
		// Therefore that is why stopRingMyPhone has a waiting while loop for
		// the user to turn screen on
		stopRingMyPhone();

		// Method completed without interruptions
		isSuccessful = true;

		return isSuccessful;
	}
}
