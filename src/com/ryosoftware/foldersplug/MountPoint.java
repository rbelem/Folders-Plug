package com.ryosoftware.foldersplug;

import com.ryosoftware.objects.Utilities;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MountPoint {
    private static final String LOG_SUBTITLE = "MountPoint";

    private int iId;
    private String iSource;
    private String iTarget;
    private boolean iEnabled;
    private boolean iAutoUnmounted;

    MountPoint(int id, String source, String target, boolean enabled) {
        iId = id;
        iSource = source;
        iTarget = target;
        iEnabled = enabled;
        iAutoUnmounted = false;
    }

    public void logData() {
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Data logging startes");
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Identifier: " + iId);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Source: " + iSource);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Target: " + iTarget);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Enabled: " + iEnabled);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Mounted: " + getMounted());
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
        return pathMounted(iTarget);
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

    private boolean pathMounted(String path) {
        Pattern p = Pattern.compile("^[\\w\\W]+\\s(/[\\w\\W]*)\\s[\\w\\d\\-]+\\s[\\w\\d,=\\-]+\\s\\d\\s\\d$");

        BufferedReader buf = null;

        try {
            buf = new BufferedReader(new FileReader("/proc/mounts"));
            String line = null;
            while ((line = buf.readLine()) != null) {
                Matcher matcher = p.matcher(line);
                if (matcher.find()) {
                    String linePath = matcher.group(1);
                    if (linePath.equals(path)) {
                        return true;
                    }
                }
            }
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (buf != null) {
                try {
                    buf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }
}
