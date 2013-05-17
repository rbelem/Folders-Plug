package com.ryosoftware.foldersplug;

import com.ryosoftware.foldersplug.R;
import com.ryosoftware.objects.Utilities;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity implements Preference.OnPreferenceChangeListener
{
    private static final String LOG_SUBTITLE = "Preferences";

    public void onCreate(Bundle saved_instance_bundle)
    {
        super.onCreate(saved_instance_bundle);
        addPreferencesFromResource(R.xml.preferences);
        findPreference(Constants.MOUNT_ON_BOOT_PREFERENCE_KEY).setOnPreferenceChangeListener(this);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class created");
    }

    public void onDestroy()
    {
        super.onDestroy();
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class destroyed");
    }

    public boolean onPreferenceChange(Preference preference, Object value)
    {
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Preference '" + preference.getKey() + "' changed. New value is '" + value.toString() + "'");
        SharedPreferences.Editor editor = Constants.getPreferences(this).edit();
        if (preference.getKey().equals(Constants.MOUNT_ON_BOOT_PREFERENCE_KEY)) editor.putBoolean(preference.getKey(), (Boolean) value);
        editor.commit();
        return true;
    }
}
