
package com.ryosoftware.foldersplug;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class AsyncRootCommand extends AsyncTask<String, Void, List<String>> {
    private Activity m_activity;
    private AsyncRootCommandListener m_listener;
    private ProgressDialog m_dialog = null;
    private List<String> m_result = null;
    private String m_dialogTitle = "";
    private String m_dialogMessage = "";

    public AsyncRootCommand() {
        this.m_activity = null;
        this.m_listener = null;
    }

    public AsyncRootCommand(Activity activity) {
        this.m_activity = activity;
    }

    public void setListener(AsyncRootCommandListener listener) {
        this.m_listener = listener;
    }

    public void setDialogTitle(String title) {
        this.m_dialogTitle = title;
    }

    public void setDialogMessage(String message) {
        this.m_dialogMessage = message;
    }

    public List<String> result() {
        return m_result;
    }

    @Override
    protected void onPreExecute() {
        if (m_activity == null) {
            return;
        }

        m_dialog = new ProgressDialog(m_activity);
        m_dialog.setTitle(m_dialogTitle);
        m_dialog.setMessage(m_dialogMessage);
        m_dialog.setIndeterminate(true);
        m_dialog.setCancelable(false);
        m_dialog.show();
    }

    @Override
    protected List<String> doInBackground(String... params) {
        if (Shell.SU.available()) {
            m_result = Shell.SU.run(params);
            return m_result;
        }

        return null;
    }

    @Override
    protected void onPostExecute(List<String> result) {
        if (m_activity != null) {
            m_dialog.dismiss();
        }
        if (m_listener != null) {
            m_listener.onCommandCompleted(result);
        }
    }
}
