package com.ryosoftware.objects;

import com.ryosoftware.foldersplug.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.widget.Toast;

public class DialogUtilities {
    public interface ButtonClickCallback {
        public abstract void onClick();
    };

    public static void showAlertDialog(Activity activity, String title, String message, String button, final ButtonClickCallback button_click_callback) {
        AlertDialog dialog = new AlertDialog.Builder(activity).create();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setButton(Dialog.BUTTON_NEUTRAL, (CharSequence) button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (button_click_callback != null) {
                    button_click_callback.onClick();
                }
            }
        });
        dialog.show();
    }

    public static void showAlertDialog(Activity activity, int message, final ButtonClickCallback button_click_callback) {
        Resources resources = activity.getResources();
        showAlertDialog(activity, resources.getString(R.string.error_title), resources.getString(message), resources.getString(R.string.accept_button), button_click_callback);
    }

    public static void showConfirmDialog(Activity activity, String title, String message, String button_one, String button_two, String button_three, final ButtonClickCallback button_one_click_callback, final ButtonClickCallback button_two_click_callback, final ButtonClickCallback button_three_click_callback) {
        AlertDialog dialog = new AlertDialog.Builder(activity).create();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setButton(Dialog.BUTTON_POSITIVE, (CharSequence) button_one, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (button_one_click_callback != null) {
                    button_one_click_callback.onClick();
                }
            }
        });
        dialog.setButton(Dialog.BUTTON_NEGATIVE, (CharSequence) button_two, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (button_two_click_callback != null) {
                    button_two_click_callback.onClick();
                }
            }
        });
        dialog.setButton(Dialog.BUTTON_NEUTRAL, (CharSequence) button_three, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (button_three_click_callback != null) {
                    button_three_click_callback.onClick();
                }
            }
        });
        dialog.show();
    }

    public static void showConfirmDialog(Activity activity, int message, final ButtonClickCallback button_one_click_callback, final ButtonClickCallback button_two_click_callback, int button_three, final ButtonClickCallback button_three_click_callback) {
        Resources resources = activity.getResources();
        showConfirmDialog(activity, resources.getString(R.string.warning_title), resources.getString(message), resources.getString(R.string.accept_button), resources.getString(R.string.cancel_button), (button_three != 0) ? resources.getString(button_three) : null, button_one_click_callback, button_two_click_callback, button_three_click_callback);
    }

    public static ProgressDialog showProgressDialog(Activity activity, String title, String message) {
        return ProgressDialog.show(activity, title, message);
    }

    public static ProgressDialog showProgressDialog(Activity activity, int message) {
        Resources resources = activity.getResources();
        return showProgressDialog(activity, resources.getString(R.string.please_wait), resources.getString(message));
    }

    public static void showToastMessage(Context context, int message) {
        Toast.makeText(context.getApplicationContext(), context.getResources().getString(message), Toast.LENGTH_LONG).show();
    }
}



