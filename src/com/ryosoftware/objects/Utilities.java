package com.ryosoftware.objects;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

public class Utilities {
    public static final int GET_STRING_FROM_BOOLEAN_TYPE_IS_AVAILABILITY = 1;
    public static final int GET_STRING_FROM_BOOLEAN_TYPE_IS_STATE = 2;

    public static String getString(boolean value, int type) {
        if (type == GET_STRING_FROM_BOOLEAN_TYPE_IS_AVAILABILITY) {
            return value ? "enabled" : "disabled";
        }
        if (type == GET_STRING_FROM_BOOLEAN_TYPE_IS_STATE) {
            return value ? "ok" : "not ok";
        }
        return value ? "true" : "false";
    }

    public static ArrayList<String> convertVectorToList(String [] vector) {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < vector.length; i ++) {
            list.add(vector [i]);
        }
        return list;
    }

    public static int parseInt(String string, int default_value) {
        int value = default_value;
        try {
            value = Integer.parseInt(string);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static void sleep(Context context, long timeout) {
        synchronized (context) {
            try {
                context.wait(timeout);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String getSuffix(String string, String separator) {
        int index = string.lastIndexOf(separator);
        if (index != -1) {
            return string.substring(index + 1);
        }
        return "";
    }

    public static void log(String tag, String title, String description, boolean error) {
        String message = String.format("%s: %s", title, description);
        if (error) {
            Log.e(tag, message);
        } else {
            Log.d(tag, message);
        }
    }

    public static void log(String tag, String title, String description) {
        log(tag, title, description, false);
    }
}
