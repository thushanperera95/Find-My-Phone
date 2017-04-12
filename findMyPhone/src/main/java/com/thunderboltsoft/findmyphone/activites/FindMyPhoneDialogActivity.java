package com.thunderboltsoft.findmyphone.activites;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by Thushan on 12-Apr-17.
 */

public class FindMyPhoneDialogActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setTitle("Find My Phone")
                .setMessage("Find My Phone service is currently active")
                .setCancelable(false)
                .setPositiveButton("Found My Phone!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
