package com.ryosoftware.foldersplug;

import java.util.ArrayList;
import com.ryosoftware.foldersplug.R;
import com.ryosoftware.objects.DialogUtilities;
import com.ryosoftware.objects.DialogUtilities.ButtonClickCallback;
import com.ryosoftware.objects.Utilities;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MountPointsEdition extends Activity {
    private static final String LOG_SUBTITLE = "MountPointsEdition";

    private class MountPointsListAdapter extends ArrayAdapter<MountPoint> implements OnClickListener, OnCreateContextMenuListener {
        private static final int DELETE_MOUNTPOINT_MENUITEM = 1;
        private static final int MOUNT_MOUNTPOINT_MENUITEM = 2;
        private static final int UMOUNT_MOUNTPOINT_MENUITEM = 3;

        private Activity iActivity;

        private ArrayList<MountPoint> iMountPoints;

        private int iContextualMenuOwner;

        MountPointsListAdapter(Activity activity, ArrayList<MountPoint> mount_points) {
            super(activity, R.layout.mountpoint_row, mount_points);
            iActivity = activity;
            iMountPoints = mount_points;
        }

        private MountPoint getMountPointByView(View view) {
            int position = (Integer) view.getTag();
            return (position < iMountPoints.size()) ? iMountPoints.get(position) : null;
        }

        private MountPoint getMountPointByIdentifier(int id) {
            MountPoint mountpoint;
            for (int i = 0; i < iMountPoints.size(); i ++) {
                mountpoint = iMountPoints.get(i);
                if (mountpoint.getId() == id) {
                    return mountpoint;
                }
            }
            return null;
        }

        public void onClick(View view) {
            MountPoint mountpoint = getMountPointByView(view);
            if (mountpoint != null) {
                if (mountpoint.getEnabled()) {
                    mountpoint.setEnabled(false);
                    MainService.updateMountPoint(iActivity, mountpoint);
                } else {
                    if (isMountPointEnableable(mountpoint.getId(), mountpoint.getTarget())) {
                        mountpoint.setEnabled(true);
                        MainService.updateMountPoint(iActivity, mountpoint);
                    } else {
                        ((CheckBox) view).setChecked(false);
                        DialogUtilities.showAlertDialog(iActivity, R.string.cannot_enable_two_mountpoint_with_same_target, null);
                    }
                }
            }
        }

        public void notifyDataSetChanged(ArrayList<MountPoint> mount_points) {
            iMountPoints = mount_points;
            super.notifyDataSetChanged();
        }

        public View getView(int position, View convert_view, ViewGroup parent) {
            MountPoint mountpoint = (position < iMountPoints.size()) ? iMountPoints.get(position) : null;
            if (convert_view == null) {
                LayoutInflater layout_inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convert_view = layout_inflater.inflate(R.layout.mountpoint_row, null);
            }
            if (mountpoint != null) {
                convert_view.setTag(position);
                convert_view.setOnCreateContextMenuListener(this);
                ((ImageView) convert_view.findViewById(R.id.state_icon)).setImageResource(mountpoint.getMounted() ? R.drawable.connected : R.drawable.disconnected);
                ((CheckBox) convert_view.findViewById(R.id.state_checkbox)).setChecked(mountpoint.getEnabled());
                convert_view.findViewById(R.id.state_checkbox).setOnClickListener(this);
                convert_view.findViewById(R.id.state_checkbox).setTag(position);
                ((TextView) convert_view.findViewById(R.id.source)).setText(mountpoint.getSource());
                ((TextView) convert_view.findViewById(R.id.target)).setText(mountpoint.getTarget());
            }
            return convert_view;
        }

        public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menu_info) {
            MountPoint mountpoint = getMountPointByView(view);
            iContextualMenuOwner = -1;
            if (mountpoint != null) {
                menu.add(ContextMenu.NONE, DELETE_MOUNTPOINT_MENUITEM, ContextMenu.NONE, R.string.delete_mountpoint_menuitem);
                if (mountpoint.getEnabled()) {
                    if (mountpoint.getMounted()) {
                        menu.add(ContextMenu.NONE, UMOUNT_MOUNTPOINT_MENUITEM, ContextMenu.NONE, R.string.unmount_mountpoint_menuitem);
                    } else {
                        menu.add(ContextMenu.NONE, MOUNT_MOUNTPOINT_MENUITEM, ContextMenu.NONE, R.string.mount_mountpoint_menuitem);
                    }
                }
                iContextualMenuOwner = (Integer) view.getTag();
            }
        }

        private class MountPointRemoveConfirmationCallback implements ButtonClickCallback {
            Activity iActivity;
            int iId;

            MountPointRemoveConfirmationCallback(Activity activity, int id) {
                iActivity = activity;
                iId = id;
            }

            public void onClick() {
                MountPoint mountpoint = getMountPointByIdentifier(iId);
                if (mountpoint != null) {
                    MainService.removeMountPoint(iActivity, iId);
                }
            }
        };

        private class MountPointUmountonfirmationCallback implements ButtonClickCallback {
            Activity iActivity;
            int iId;

            MountPointUmountonfirmationCallback(Activity activity, int id) {
                iActivity = activity;
                iId = id;
            }

            public void onClick() {
                MountPoint mountpoint = getMountPointByIdentifier(iId);
                if (mountpoint != null) {
                    mountpoint.setMounted(false);
                    MainService.updateMountPoint(iActivity, mountpoint);
                }
            }
        }

        private class MountPointDeleteConfirmationCallback implements ButtonClickCallback {
            Activity iActivity;
            int iId;

            MountPointDeleteConfirmationCallback(Activity activity, int id) {
                iActivity = activity;
                iId = id;
            }

            public void onClick() {
                MountPoint mountpoint = getMountPointByIdentifier(iId);
                if (mountpoint != null) {
                    MainService.removeMountPoint(iActivity, mountpoint.getId());
                }
            }
        }

        public boolean onContextItemSelected(MenuItem item) {
            MountPoint mountpoint = (iContextualMenuOwner != -1) ? iMountPoints.get(iContextualMenuOwner) : null;
            if (mountpoint != null) {
                switch (item.getItemId()) {
                case DELETE_MOUNTPOINT_MENUITEM:
                    if (mountpoint.getMounted()) {
                        MountPointRemoveConfirmationCallback callback = new MountPointRemoveConfirmationCallback(iActivity, mountpoint.getId());
                        DialogUtilities.showConfirmDialog(iActivity, R.string.mountpoint_mounted_are_you_sure_want_to_delete, callback, null, 0, null);
                    } else {
                        MountPointDeleteConfirmationCallback callback = new MountPointDeleteConfirmationCallback(iActivity, mountpoint.getId());
                        DialogUtilities.showConfirmDialog(iActivity, R.string.are_you_sure, callback, null, 0, null);
                    }
                    break;
                case MOUNT_MOUNTPOINT_MENUITEM:
                    if (! mountpoint.getMounted()) {
                        mountpoint.setMounted(true);
                        MainService.updateMountPoint(iActivity, mountpoint);
                    }
                    break;
                case UMOUNT_MOUNTPOINT_MENUITEM:
                    if (mountpoint.getMounted()) {
                        MountPointUmountonfirmationCallback callback = new MountPointUmountonfirmationCallback(iActivity, mountpoint.getId());
                        DialogUtilities.showConfirmDialog(iActivity, R.string.are_you_sure, callback, null, 0, null);
                    }
                    break;
                }
            }
            return false;
        }
    }

    private class FoldersEditionBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MainService.ACTION_GET_MOUNT_STATES_ANSWER)) {
                initializeMountPointElementCallback(intent);
            }
        }
    }

    private ArrayList<MountPoint> iMountPoints;
    private MountPointsListAdapter iMountPointsListAdapter;
    private FoldersEditionBroadcastReceiver iBroadcastReceiver;

    private int iEditedMountPoint;

    public void onCreate(Bundle saved_instance_bundle) {
        super.onCreate(saved_instance_bundle);
        iMountPoints = new ArrayList<MountPoint>();
        iMountPointsListAdapter = new MountPointsListAdapter(this, iMountPoints);
        setContentView(R.layout.mountpoints_list);
        ((ListView) findViewById(R.id.mount_points_list)).setAdapter(iMountPointsListAdapter);
        iBroadcastReceiver = new FoldersEditionBroadcastReceiver();
        registerReceiver(iBroadcastReceiver, new IntentFilter(MainService.ACTION_GET_MOUNT_STATES_ANSWER));
        MainService.getMountPointsStates(this);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class created");
    }

    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(iBroadcastReceiver);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class destroyed");
    }

    private void initializeMountPointElementCallback(Intent intent) {
        if (intent.getBooleanExtra(MainService.ACTION_GET_MOUNT_STATES_ANSWER_IS_FIRST_LIST_ELEMENT, false)) {
            iMountPoints.clear();
        }
        if (intent.getBooleanExtra(MainService.ACTION_GET_MOUNT_STATES_ANSWER_HAS_VALUES, false)) {
            iMountPoints.add(new MountPoint(intent.getIntExtra(MainService.ACTION_GET_MOUNT_STATES_ANSWER_IDENTIFIER, -1), intent.getStringExtra(MainService.ACTION_GET_MOUNT_STATES_ANSWER_SOURCE), intent.getStringExtra(MainService.ACTION_GET_MOUNT_STATES_ANSWER_TARGET), intent.getBooleanExtra(MainService.ACTION_GET_MOUNT_STATES_ANSWER_IS_ENABLED, false), intent.getBooleanExtra(MainService.ACTION_GET_MOUNT_STATES_ANSWER_IS_MOUNTED, false)));
        }
        if (intent.getBooleanExtra(MainService.ACTION_GET_MOUNT_STATES_ANSWER_IS_LAST_LIST_ELEMENT, false)) {
            iMountPointsListAdapter.notifyDataSetChanged(iMountPoints);
            findViewById(R.id.no_mountpoints_defined_layout).setVisibility(iMountPoints.size() == 0 ? View.VISIBLE : View.GONE);
            findViewById(R.id.mount_points_list).setVisibility(iMountPoints.size() == 0 ? View.GONE : View.VISIBLE);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mountpoints_edition, menu);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean enable_delete = (iMountPoints.size() > 0);
        if (enable_delete) for (int i = 0; i < iMountPoints.size(); i ++) if (iMountPoints.get(i).getMounted()) {
                    enable_delete = false;
                }
        menu.findItem(R.id.delete_mountpoints_menuitem).setEnabled(enable_delete);
        return true;
    }

    private class MountPointsDeleteConfirmationCallback implements ButtonClickCallback {
        Activity iActivity;

        MountPointsDeleteConfirmationCallback(Activity activity) {
            iActivity = activity;
        }

        public void onClick() {
            MainService.removeMountPoints(iActivity);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.insert_mountpoint_menuitem) {
            iEditedMountPoint = MainService.UNKNOWN_MOUNTPOINT_IDENTIFIER;
            startActivityForResult(new Intent(this, MountPointEdition.class), 0);
        } else if (id == R.id.delete_mountpoints_menuitem) {
            MountPointsDeleteConfirmationCallback callback = new MountPointsDeleteConfirmationCallback(this);
            DialogUtilities.showConfirmDialog(this, R.string.are_you_sure, callback, null, 0, null);
        }
        return true;
    }

    private boolean isMountPointEnableable(int id, String target) {
        MountPoint mountpoint;
        for (int i = 0; i < iMountPoints.size(); i ++) {
            mountpoint = iMountPoints.get(i);
            if (mountpoint.getId() == id) {
                if (mountpoint.getEnabled()) {
                    return true;
                }
            } else if ((mountpoint.getTarget().equals(target)) && (mountpoint.getEnabled())) {
                return false;
            }
        }
        return true;
    }

    protected void onActivityResult(int request_code, int result_code, Intent intent) {
        if ((request_code == 0) && (result_code == RESULT_OK)) {
            String source = intent.getStringExtra(MountPointEdition.SOURCE_PATH), target = intent.getStringExtra(MountPointEdition.TARGET_PATH);
            MountPoint mountpoint;
            boolean duplicated = false;
            for (int i = 0; i < iMountPoints.size(); i ++) {
                mountpoint = iMountPoints.get(i);
                if ((mountpoint.getSource().equals(source)) && (mountpoint.getTarget().equals(target))) {
                    duplicated = true;
                    Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Duplicated mountpoint");
                    DialogUtilities.showAlertDialog(this, R.string.duplicated_mountpoint, null);
                }
            }
            if ((! duplicated) && (iEditedMountPoint == MainService.UNKNOWN_MOUNTPOINT_IDENTIFIER)) {
                MainService.updateMountPoint(this, new MountPoint(iEditedMountPoint, source, target, isMountPointEnableable(iEditedMountPoint, target), false));
            }
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        return iMountPointsListAdapter.onContextItemSelected(item);
    }
}
