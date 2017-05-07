package com.thunderboltsoft.findmyphone.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.thunderboltsoft.findmyphone.activites.FindMyPhoneDialogActivity;

public class PopupBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("testingkaozgamer")) {

            Toast.makeText(context, "Testing Broadcast", Toast.LENGTH_LONG).show();
            Intent i = new Intent(context, FindMyPhoneDialogActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
