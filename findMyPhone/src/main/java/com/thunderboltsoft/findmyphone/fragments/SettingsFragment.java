package com.thunderboltsoft.findmyphone.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

import com.thunderboltsoft.findmyphone.activites.SettingsActivity;
import com.thunderboltsoft.ringmyphone.R;

/**
 * Created by Thushan on 04-Aug-16.
 */
public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_data_sync);
        setHasOptionsMenu(true);

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences
        // to their values. When their values change, their summaries are
        // updated to reflect the new value, per the Android Design
        // guidelines.
//        bindPreferenceSummaryToValue(findPreference("sync_frequency"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
