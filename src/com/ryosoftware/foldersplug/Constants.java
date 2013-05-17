package com.ryosoftware.foldersplug;

import com.ryosoftware.foldersplug.R;
import com.ryosoftware.objects.Utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

public class Constants
{
    public static final String LOG_TITLE = "FoldersPlug";
    private static final String LOG_SUBTITLE = "Constants";

    public static final String MOUNT_ON_BOOT_PREFERENCE_KEY = "mount_on_boot_preference";

    public static boolean MOUNT_ON_BOOT_PREFERENCE_DEFAULT;

    private static boolean iInitializated = false;

    private static void initializeConstants(Context context)
    {
        if (! iInitializated)
        {
            Utilities.log(LOG_TITLE, LOG_SUBTITLE, "Initializating constants");
            Resources resources = context.getResources();
            MOUNT_ON_BOOT_PREFERENCE_DEFAULT = Boolean.parseBoolean(resources.getString(R.string.mount_on_boot_preference_default));
            iInitializated = true;
        }
    }

    public static SharedPreferences getPreferences(Context context)
    {
        Utilities.log(LOG_TITLE, LOG_SUBTITLE, "Getting preferences object");
        initializeConstants(context);
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
