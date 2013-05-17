package com.ryosoftware.foldersplug;

import java.util.ArrayList;
import com.ryosoftware.objects.DialogUtilities;
import com.ryosoftware.objects.ProcessUtilities;
import com.ryosoftware.objects.Utilities;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.IBinder;

public class MainService extends Service
{
    private static final String LOG_SUBTITLE = "MainService";

    private static final String ACTION_RELOAD_PREFERENCES = MainService.class.getName() + ".RELOAD_PREFERENCES";
    private static final String ACTION_GET_MOUNT_STATES = MainService.class.getName() + ".GET_MOUNT_STATES";
    private static final String ACTION_UPDATE_MOUNTPOINT = MainService.class.getName() + ".UPDATE_MOUNTPOINT";
    private static final String ACTION_REMOVE_MOUNTPOINT = MainService.class.getName() + ".REMOVE_MOUNTPOINT";
    private static final String ACTION_REMOVE_MOUNTPOINTS = MainService.class.getName() + ".REMOVE_MOUNTPOINTS";
    private static final String ACTION_DEVICE_STATE_CHANGED = MainService.class.getName() + ".DEVICE_STATE_CHANGED";

    public static final int UNKNOWN_MOUNTPOINT_IDENTIFIER = -1;

    private static final String DEVICE_STATE_CHANGED_VALUE = "value";

    private static final String MOUNT_STATE_IDENTIFIER = "identifier";
    private static final String MOUNT_STATE_SOURCE = "source";
    private static final String MOUNT_STATE_TARGET = "target";
    private static final String MOUNT_STATE_ENABLED = "enabled";
    private static final String MOUNT_STATE_MOUNTED = "mounted";

    public static final String ACTION_GET_MOUNT_STATES_ANSWER = MainService.class.getName() + ".GET_MOUNT_STATES_ANSWER";
    public static final String ACTION_GET_MOUNT_STATES_ANSWER_IS_FIRST_LIST_ELEMENT = "is_first";
    public static final String ACTION_GET_MOUNT_STATES_ANSWER_IS_LAST_LIST_ELEMENT = "is_last";
    public static final String ACTION_GET_MOUNT_STATES_ANSWER_HAS_VALUES = "has_values";
    public static final String ACTION_GET_MOUNT_STATES_ANSWER_IDENTIFIER = MOUNT_STATE_IDENTIFIER;
    public static final String ACTION_GET_MOUNT_STATES_ANSWER_SOURCE = MOUNT_STATE_SOURCE;
    public static final String ACTION_GET_MOUNT_STATES_ANSWER_TARGET = MOUNT_STATE_TARGET;
    public static final String ACTION_GET_MOUNT_STATES_ANSWER_IS_ENABLED = MOUNT_STATE_ENABLED;
    public static final String ACTION_GET_MOUNT_STATES_ANSWER_IS_MOUNTED = MOUNT_STATE_MOUNTED;

    private class MainServiceBroadcastReceiver extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(ACTION_RELOAD_PREFERENCES)) reloadPreferences(intent);
            else if (action.equals(ACTION_GET_MOUNT_STATES)) getMountPointsStates(intent);
            else if (action.equals(ACTION_UPDATE_MOUNTPOINT)) updateMountPoint(intent);
            else if (action.equals(ACTION_REMOVE_MOUNTPOINT)) removeMountPoint(intent);
            else if (action.equals(ACTION_REMOVE_MOUNTPOINTS)) removeMountPoints(intent);
            else if (action.equals(ACTION_DEVICE_STATE_CHANGED)) deviceStateChanged(intent);
        }
    }

    private MainServiceBroadcastReceiver iBroadcastReceiver;
    private DatabaseContainer iDatabaseContainer;
    private ArrayList<MountPoint> iMountPoints;
    private SuperuserCommandsExecutor iSuperuserCommandsExecutor;
    private boolean iUmsConnected;

    public void onCreate()
    {
        super.onCreate();
        iDatabaseContainer = new DatabaseContainer(this);
        iDatabaseContainer.open();
        iMountPoints = new ArrayList<MountPoint>();
        iSuperuserCommandsExecutor = new SuperuserCommandsExecutor();
        iUmsConnected = false;
        loadMountPointsFromDatabase();
        iBroadcastReceiver = new MainServiceBroadcastReceiver();
        registerReceiver(iBroadcastReceiver, new IntentFilter(ACTION_RELOAD_PREFERENCES));
        registerReceiver(iBroadcastReceiver, new IntentFilter(ACTION_GET_MOUNT_STATES));
        registerReceiver(iBroadcastReceiver, new IntentFilter(ACTION_UPDATE_MOUNTPOINT));
        registerReceiver(iBroadcastReceiver, new IntentFilter(ACTION_REMOVE_MOUNTPOINT));
        registerReceiver(iBroadcastReceiver, new IntentFilter(ACTION_REMOVE_MOUNTPOINTS));
        registerReceiver(iBroadcastReceiver, new IntentFilter(ACTION_DEVICE_STATE_CHANGED));
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class created");
    }

    public void onDestroy()
    {
        unregisterReceiver(iBroadcastReceiver);
        iDatabaseContainer.close();
        super.onDestroy();
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class destroyed");
    }

    public int onStartCommand(Intent intent, int flags, int start_id)
    {
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "On start command procedure executed");
        if ((intent != null) && (intent.getAction() != null)) iBroadcastReceiver.onReceive(this, intent);
        return super.onStartCommand(intent, flags, start_id);
    }

    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private void loadMountPointsFromDatabase()
    {
        try
        {
            Cursor cursor = iDatabaseContainer.getRows();
            if (cursor != null)
            {
                try
                {
                    if (cursor.getCount() > 0)
                    {
                        int id;
                        String source, target;
                        boolean enabled;
                        cursor.moveToFirst();
                        while (true)
                        {
                            id = cursor.getInt(cursor.getColumnIndex(DatabaseContainer.FOLDERS_TABLE_ROWID_KEY));
                            source = cursor.getString(cursor.getColumnIndex(DatabaseContainer.FOLDERS_TABLE_SOURCE_KEY));
                            target = cursor.getString(cursor.getColumnIndex(DatabaseContainer.FOLDERS_TABLE_TARGET_KEY));
                            enabled = (cursor.getInt(cursor.getColumnIndex(DatabaseContainer.FOLDERS_TABLE_ENABLED_KEY)) > 0);
                            iMountPoints.add(new MountPoint(id, source, target, enabled, false));
                            if (cursor.isLast()) break;
                            cursor.moveToNext();
                        }
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
                cursor.close();
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private int createMountPointIntoDatabase(String source, String target, boolean enabled)
    {
        int id = UNKNOWN_MOUNTPOINT_IDENTIFIER;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Trying to create mountpoint");
        try
        {
            id = iDatabaseContainer.insert(source, target, enabled);
            if (id != DatabaseContainer.ROW_ID_ERROR) iMountPoints.add(new MountPoint(id, source, target, enabled, false));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Created mountpoint identifier is: " + id);
        return id;
    }

    private MountPoint updateMountPointIntoDatabase(int id, String source, String target, boolean enabled)
    {
        MountPoint mountpoint = null;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Trying to update mountpoint");
        try
        {
            if (iDatabaseContainer.update(id, source, target, enabled))
            {
                mountpoint = getMountPointByIdentifier(id);
                if (mountpoint == null)
                {
                    mountpoint = new MountPoint(id, source, target, enabled, false);
                    iMountPoints.add(mountpoint);
                }
                else
                {
                    mountpoint.setSource(source);
                    mountpoint.setTarget(target);
                    mountpoint.setEnabled(enabled);
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, (mountpoint == null) ? "Update wasn't completed" : "Update completed without errors");
        return mountpoint;
    }

    private boolean removeMountPointIntoDatabase(int id)
    {
        boolean deleted = false;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Trying to remove mountpoint");
        try
        {
            deleted = iDatabaseContainer.delete(id);
            if (deleted) for (int i = iMountPoints.size() - 1; i >= 0; i --) if (iMountPoints.get(i).getId() == id) iMountPoints.remove(i);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Remove mountpoint returns: " + deleted);
        return deleted;
    }

    private MountPoint getMountPointByIdentifier(int id)
    {
        MountPoint mountpoint;
        for (int i = 0; i < iMountPoints.size(); i ++)
        {
            mountpoint = iMountPoints.get(i);
            if (mountpoint.getId() == id) return mountpoint;
        }
        return null;
    }

    private void reloadPreferences(Intent intent)
    {
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Handling event 'preferences changed'");
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "No preferences afecting the service in this version");
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "'Preferences changed' event handled");
    }

    public static void reloadPreferences(Context context)
    {
        boolean service_running = ProcessUtilities.serviceRunning(MainService.class.getName(), context);
          Intent intent = service_running ? new Intent() : new Intent(context, MainService.class);
        intent.setAction(ACTION_RELOAD_PREFERENCES);
        if (service_running) context.sendBroadcast(intent);
    }

    private void getMountState(int index)
    {
        MountPoint mountpoint = (index < iMountPoints.size()) ? mountpoint = iMountPoints.get(index) : null;
        Intent intent = new Intent(ACTION_GET_MOUNT_STATES_ANSWER);
        intent.putExtra(ACTION_GET_MOUNT_STATES_ANSWER_IS_FIRST_LIST_ELEMENT, index == 0);
        intent.putExtra(ACTION_GET_MOUNT_STATES_ANSWER_IS_LAST_LIST_ELEMENT, index >= iMountPoints.size() - 1);
        intent.putExtra(ACTION_GET_MOUNT_STATES_ANSWER_HAS_VALUES, mountpoint != null);
        if (mountpoint != null)
        {
            mountpoint.logData();
            intent.putExtra(ACTION_GET_MOUNT_STATES_ANSWER_IDENTIFIER, mountpoint.getId());
            intent.putExtra(ACTION_GET_MOUNT_STATES_ANSWER_SOURCE, mountpoint.getSource());
            intent.putExtra(ACTION_GET_MOUNT_STATES_ANSWER_TARGET, mountpoint.getTarget());
            intent.putExtra(ACTION_GET_MOUNT_STATES_ANSWER_IS_ENABLED, mountpoint.getEnabled());
            intent.putExtra(ACTION_GET_MOUNT_STATES_ANSWER_IS_MOUNTED, mountpoint.getMounted());
        }
        sendBroadcast(intent);
    }

    private void getMountPointsStates(Intent intent)
    {
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Handling event 'get mount states'");
        getMountState(0);
        for (int i = 1; i < iMountPoints.size(); i ++) getMountState(i);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "'Get mount states' event handled");
    }

    public static void getMountPointsStates(Context context)
    {
        boolean service_running = ProcessUtilities.serviceRunning(MainService.class.getName(), context);
          Intent intent = service_running ? new Intent() : new Intent(context, MainService.class);
        intent.setAction(ACTION_GET_MOUNT_STATES);
        if (service_running) context.sendBroadcast(intent);
        else context.startService(intent);
    }

    private void executeMountPointCommands(MountPoint mountpoint, boolean mount)
    {
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Trying to mount/umount mountpoint");
        if (mountpoint.getMounted() != mount)
        {
            boolean correct;
            if (mount) correct = iSuperuserCommandsExecutor.mountFolders(mountpoint.getSource(), mountpoint.getTarget());
            else correct = iSuperuserCommandsExecutor.unmountFolder(mountpoint.getTarget());
            if (correct)
            {
                DialogUtilities.showToastMessage(this, mount ? R.string.mounting_mountpoint_success : R.string.unmounting_mountpoint_success);
                mountpoint.setMounted(mount);
                Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, mount ? "Mountpoint mounted" : "Mountpoint umounted");
            }
            else
            {
                   DialogUtilities.showToastMessage(this, mount ? R.string.error_mounting_mountpoint : R.string.error_unmounting_mountpoint);
                Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, mount ? "Mountpoint not mounted" : "Mountpoint not umounted");
            }
        }
        else Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Mount state is correct");
    }

    private void updateMountPoint(Intent intent)
    {
        int id;
        String source, target;
        boolean enabled, mounted;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Handling event 'update mountpoint'");
        id = intent.getIntExtra(MOUNT_STATE_IDENTIFIER, UNKNOWN_MOUNTPOINT_IDENTIFIER);
        source = intent.getStringExtra(MOUNT_STATE_SOURCE);
        target = intent.getStringExtra(MOUNT_STATE_TARGET);
        enabled = intent.getBooleanExtra(MOUNT_STATE_ENABLED, false);
        mounted = intent.getBooleanExtra(MOUNT_STATE_MOUNTED, false);
        if (id == UNKNOWN_MOUNTPOINT_IDENTIFIER) id = createMountPointIntoDatabase(source, target, enabled);
        if (id != UNKNOWN_MOUNTPOINT_IDENTIFIER)
        {
            MountPoint mountpoint = updateMountPointIntoDatabase(id, source, target, enabled);
            if (mountpoint != null)
            {
                if (mountpoint.getEnabled())
                {
                    if (mounted != mountpoint.getMounted())
                    {
                        if (mounted)
                        {
                            if (iUmsConnected)
                            {
                                DialogUtilities.showToastMessage(this, R.string.cannot_mount_mountpoint_while_ums_connection_active);
                                mountpoint.setAutoUnmounted(true);
                            }
                            else executeMountPointCommands(mountpoint, true);
                        }
                        else executeMountPointCommands(mountpoint, false);
                    }
                }
                else
                {
                    if (mountpoint.getMounted()) executeMountPointCommands(mountpoint, false);
                }
            }
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "'Update mountpoint' event handled");
        getMountPointsStates(this);
    }

    public static void updateMountPoint(Context context, MountPoint mountpoint)
    {
        boolean service_running = ProcessUtilities.serviceRunning(MainService.class.getName(), context);
          Intent intent = service_running ? new Intent() : new Intent(context, MainService.class);
          intent.setAction(ACTION_UPDATE_MOUNTPOINT);
        intent.putExtra(MOUNT_STATE_IDENTIFIER, mountpoint.getId());
        intent.putExtra(MOUNT_STATE_SOURCE, mountpoint.getSource());
        intent.putExtra(MOUNT_STATE_TARGET, mountpoint.getTarget());
        intent.putExtra(MOUNT_STATE_ENABLED, mountpoint.getEnabled());
        intent.putExtra(MOUNT_STATE_MOUNTED, mountpoint.getMounted());
        if (service_running) context.sendBroadcast(intent);
        else context.startService(intent);
    }

    private void removeMountPoint(Intent intent)
    {
        MountPoint mountpoint;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Handling event 'remove mountpoint'");
        mountpoint = getMountPointByIdentifier(intent.getIntExtra(MOUNT_STATE_IDENTIFIER, UNKNOWN_MOUNTPOINT_IDENTIFIER));
        if (mountpoint != null)
        {
               if (mountpoint.getMounted()) executeMountPointCommands(mountpoint, false);
               removeMountPointIntoDatabase(mountpoint.getId());
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "'Remove mountpoint' event handled");
        getMountPointsStates(this);
    }

    public static void removeMountPoint(Context context, int id)
    {
        boolean service_running = ProcessUtilities.serviceRunning(MainService.class.getName(), context);
          Intent intent = service_running ? new Intent() : new Intent(context, MainService.class);
        intent.setAction(ACTION_REMOVE_MOUNTPOINT);
        intent.putExtra(MOUNT_STATE_IDENTIFIER, id);
        if (service_running) context.sendBroadcast(intent);
        else context.startService(intent);
    }

    private void removeMountPoints(Intent intent)
    {
        MountPoint mountpoint;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Handling event 'remove mountpoints'");
        for (int i = iMountPoints.size() - 1; i >= 0; i --)
        {
            mountpoint = iMountPoints.get(i);
            if (mountpoint.getMounted()) executeMountPointCommands(mountpoint, false);
            removeMountPointIntoDatabase(mountpoint.getId());
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "'Remove mountpoints' event handled");
        getMountPointsStates(this);
    }

    public static void removeMountPoints(Context context)
    {
        boolean service_running = ProcessUtilities.serviceRunning(MainService.class.getName(), context);
          Intent intent = service_running ? new Intent() : new Intent(context, MainService.class);
        intent.setAction(ACTION_REMOVE_MOUNTPOINTS);
        if (service_running) context.sendBroadcast(intent);
        else context.startService(intent);
    }

    private void deviceBootCompleted()
    {
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Received device boot completed event");
        if (Constants.getPreferences(this).getBoolean(Constants.MOUNT_ON_BOOT_PREFERENCE_KEY, Constants.MOUNT_ON_BOOT_PREFERENCE_DEFAULT))
        {
            MountPoint mountpoint;
            boolean toast_showed = false;
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Automounts when device boots is enabled");
            for (int i = 0; i < iMountPoints.size(); i ++)
            {
                mountpoint = iMountPoints.get(i);
                if (mountpoint.getEnabled())
                {
                    if (! toast_showed)
                    {
                        DialogUtilities.showToastMessage(this, R.string.device_bootup_completed_automounting);
                        toast_showed = true;
                    }
                    executeMountPointCommands(mountpoint, true);
                }
            }
        }
        else Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Automounts when device boots is disabled");
    }

    private void deviceShutdownStarted()
    {
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Received device shutdown/reboot event");
        for (int i = 0; i < iMountPoints.size(); i ++) executeMountPointCommands(iMountPoints.get(i), false);
    }

    private void deviceUmsConnected()
    {
        if (! iUmsConnected)
        {
            MountPoint mountpoint;
            boolean toast_showed = false;
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Received device ums connected event");
            for (int i = 0; i < iMountPoints.size(); i ++)
            {
                mountpoint = iMountPoints.get(i);
                if (mountpoint.getMounted())
                {
                       if (! toast_showed)
                       {
                           DialogUtilities.showToastMessage(this, R.string.ums_connection_activated);
                           toast_showed = true;
                       }
                    mountpoint.setAutoUnmounted(true);
                    executeMountPointCommands(iMountPoints.get(i), false);
                }
            }
            iUmsConnected = true;
        }
    }

    private void deviceUmsDisconnected()
    {
        if (iUmsConnected)
        {
            MountPoint mountpoint;
            boolean toast_showed = false;
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Received device ums disconnected event");
            for (int i = 0; i < iMountPoints.size(); i ++)
            {
                mountpoint = iMountPoints.get(i);
                if (mountpoint.getAutoUnmounted())
                {
                    mountpoint.setAutoUnmounted(false);
                    if (mountpoint.getEnabled())
                    {
                        if (! toast_showed)
                        {
                            DialogUtilities.showToastMessage(this, R.string.ums_connection_deactivated);
                            toast_showed = true;
                        }
                        executeMountPointCommands(mountpoint, true);
                    }
                }
            }
            iUmsConnected = false;
        }
    }

    private void deviceStateChanged(Intent intent)
    {
        String action = intent.getStringExtra(DEVICE_STATE_CHANGED_VALUE);
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) deviceBootCompleted();
        else if ((action.equals(Intent.ACTION_SHUTDOWN)) || (action.equals(Intent.ACTION_REBOOT))) deviceShutdownStarted();
        else if (action.equals(Intent.ACTION_UMS_CONNECTED)) deviceUmsConnected();
        else if (action.equals(Intent.ACTION_UMS_DISCONNECTED)) deviceUmsDisconnected();
        getMountPointsStates(this);
    }

    public static void deviceStateChanged(Context context, Intent intent)
    {
        boolean service_running = ProcessUtilities.serviceRunning(MainService.class.getName(), context);
          Intent new_intent = service_running ? new Intent() : new Intent(context, MainService.class);
        new_intent.setAction(ACTION_DEVICE_STATE_CHANGED);
        new_intent.putExtra(DEVICE_STATE_CHANGED_VALUE, intent.getAction());
        context.sendBroadcast(new_intent);
    }
}
