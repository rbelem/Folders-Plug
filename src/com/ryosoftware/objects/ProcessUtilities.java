package com.ryosoftware.objects;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;

public class ProcessUtilities {
    public static ArrayList<String> getForegroundApplications(Context context) {
        ArrayList<String> list = new ArrayList<String>();
        try {
            ActivityManager activity_manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (RunningAppProcessInfo process : activity_manager.getRunningAppProcesses()) if (process.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    list.add(process.processName);
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean applicationRunning(String class_name, Context context) {
        return getForegroundApplications(context).contains(class_name);
    }

    public static boolean processRunning(String process_name) {
        boolean running = false;
        Process process;
        try {
            process = Runtime.getRuntime().exec("sh");
            DataInputStream os_in = new DataInputStream(process.getInputStream());
            DataOutputStream os_out = null;
            try {
                os_out = new DataOutputStream(process.getOutputStream());
                if ((os_in != null) && (os_out != null)) {
                    String line;
                    os_out.writeBytes("ps\n");
                    os_out.writeBytes("exit\n");
                    os_out.flush();
                    while ((line = os_in.readLine()) != null) {
                        if (line.contains((CharSequence) process_name)) {
                            running = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (os_in != null) {
                os_in.close();
            }
            if (os_out != null) {
                os_out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return running;
    }

    public static boolean serviceRunning(String class_name, Context context) {
        try {
            ActivityManager activity_manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (RunningServiceInfo service : activity_manager.getRunningServices(Integer.MAX_VALUE)) if (service.service.getClassName().equals(class_name)) {
                    return true;
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
