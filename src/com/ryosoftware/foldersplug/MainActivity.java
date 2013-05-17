package com.ryosoftware.foldersplug;

import com.ryosoftware.foldersplug.R;
import com.ryosoftware.objects.Utilities;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;

public class MainActivity extends Activity
{
    private static final String LOG_SUBTITLE = "MainActivity";

    private class MainActivityBroadcastReceiver extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(MainService.ACTION_GET_MOUNT_STATES_ANSWER)) initializeMountPointElementCallback(intent);
        }
    }

    private MainActivityBroadcastReceiver iBroadcastReceiver;

    private int iMountedConnections;
    private int iEnabledConnections;

    public void onCreate(Bundle saved_instance_bundle)
    {
        super.onCreate(saved_instance_bundle);
        iBroadcastReceiver = new MainActivityBroadcastReceiver();
        setContentView(R.layout.main);
        registerReceiver(iBroadcastReceiver, new IntentFilter(MainService.ACTION_GET_MOUNT_STATES_ANSWER));
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class created");
        MainService.getMountPointsStates(this);
    }

    public void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(iBroadcastReceiver);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class destroyed");
    }

    private void showGraphicalConnectionState()
    {
        ImageView image_view = (ImageView) findViewById(R.id.connection_state_image);
        if (iMountedConnections == 0) image_view.setImageResource(R.drawable.none_connected);
        else if (iMountedConnections == iEnabledConnections) image_view.setImageResource(R.drawable.all_connected);
        else image_view.setImageResource(R.drawable.any_connected);
    }

    private void initializeMountPointElementCallback(Intent intent)
    {
        if (intent.getBooleanExtra(MainService.ACTION_GET_MOUNT_STATES_ANSWER_IS_FIRST_LIST_ELEMENT, false)) iMountedConnections = iEnabledConnections = 0;
        if (intent.getBooleanExtra(MainService.ACTION_GET_MOUNT_STATES_ANSWER_HAS_VALUES, false))
        {
            if (intent.getBooleanExtra(MainService.ACTION_GET_MOUNT_STATES_ANSWER_IS_MOUNTED, false)) iMountedConnections ++;
            if (intent.getBooleanExtra(MainService.ACTION_GET_MOUNT_STATES_ANSWER_IS_ENABLED, false)) iEnabledConnections ++;
        }
        if (intent.getBooleanExtra(MainService.ACTION_GET_MOUNT_STATES_ANSWER_IS_LAST_LIST_ELEMENT, false)) showGraphicalConnectionState();
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.preferences_menuitem) startActivityForResult(new Intent(this, Preferences.class), 0);
        else if (id == R.id.edit_mountpoints_menuitem) startActivityForResult(new Intent(this, MountPointsEdition.class), 1);
        return true;
    }

    protected void onActivityResult(int request_code, int result_code, Intent intent)
    {
        if (request_code == 0) MainService.reloadPreferences(this);
    }
 }
