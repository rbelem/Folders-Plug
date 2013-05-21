package com.ryosoftware.foldersplug;

import com.ryosoftware.objects.Utilities;

public class MountPoint {
    private static final String LOG_SUBTITLE = "MountPoint";

    private int iId;
    private String iSource;
    private String iTarget;
    private boolean iMounted;
    private boolean iEnabled;
    private boolean iAutoUnmounted;

    MountPoint(int id, String source, String target, boolean enabled, boolean mounted) {
        iId = id;
        iSource = source;
        iTarget = target;
        iEnabled = enabled;
        iMounted = mounted;
        iAutoUnmounted = false;
    }

    public void logData() {
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Data logging startes");
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Identifier: " + iId);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Source: " + iSource);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Target: " + iTarget);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Enabled: " + iEnabled);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Mounted: " + iMounted);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Auto unmounted: " + iAutoUnmounted);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Data logging ended");
    }

    public int getId() {
        return iId;
    }

    public String getSource() {
        return iSource;
    }

    public void setSource(String source) {
        iSource = source;
    }

    public String getTarget() {
        return iTarget;
    }

    public void setTarget(String target) {
        iTarget = target;
    }

    public boolean getMounted() {
        return iMounted;
    }

    public void setMounted(boolean mounted) {
        iMounted = mounted;
    }

    public boolean getEnabled() {
        return iEnabled;
    }

    public void setEnabled(boolean enabled) {
        iEnabled = enabled;
    }

    public boolean getAutoUnmounted() {
        return iAutoUnmounted;
    }

    public void setAutoUnmounted(boolean auto_unmounted) {
        iAutoUnmounted = auto_unmounted;
    }
}
