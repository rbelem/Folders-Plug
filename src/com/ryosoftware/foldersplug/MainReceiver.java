package com.ryosoftware.foldersplug;

import com.ryosoftware.objects.Utilities;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MainReceiver extends BroadcastReceiver {
    private static final String LOG_SUBTITLE = "MainReceiver";

    public void onReceive(Context context, Intent intent) {
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Event '%s' received", intent.getAction()));
        MainService.deviceStateChanged(context, intent);
    }
}

