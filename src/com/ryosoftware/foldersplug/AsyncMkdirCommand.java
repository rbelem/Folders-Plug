
package com.ryosoftware.foldersplug;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class AsyncMkdirCommand extends AsyncTask<String, Void, Boolean> {
    public interface AsyncMkdirCommandListener {
        public void onCommandCompleted(Boolean result);
    }

    private Activity m_activity;
    private AsyncMkdirCommandListener m_listener;
    private ProgressDialog m_dialog = null;

    public AsyncMkdirCommand() {
        this.m_activity = null;
        this.m_listener = null;
    }

    public AsyncMkdirCommand(Activity activity) {
        this.m_activity = activity;
    }

    public void setListener(AsyncMkdirCommandListener listener) {
        this.m_listener = listener;
    }

    @Override
    protected void onPreExecute() {
        if (m_activity == null) {
            return;
        }

        m_dialog = new ProgressDialog(m_activity);
        m_dialog.setTitle("Wait...");
        m_dialog.setMessage("Preparing the source dir");
        m_dialog.setIndeterminate(true);
        m_dialog.setCancelable(false);
        m_dialog.show();
    }

    @Override
    protected Boolean doInBackground(String... params) {

        if (Shell.SU.available()) {
            String path = params[0];
            String parent = getValidDirectory(path);
            String baseDir;

            if (path.startsWith("/mnt") || path.startsWith("/media") || path.startsWith("/storage")
                || path.startsWith(Environment.getExternalStorageDirectory().getPath())) {
                baseDir = Environment.getExternalStorageDirectory().getPath();
            } else {
                baseDir = parent;
            }

            List<String> resultStatAccessRights = Shell.SU.run(String.format("busybox stat -L -c \"%%a\" \"%s\"", baseDir));
            String baseDirAccessRights = resultStatAccessRights.get(0);
            Shell.SU.run(String.format("busybox mkdir -p -m %s \"%s\"", baseDirAccessRights, path));

            List<String> resultStatUserAndGroup = Shell.SU.run(String.format("busybox stat -L -c \"%%u:%%g\" \"%s\"", baseDir));
            String userAndGroup = resultStatUserAndGroup.get(0);
            String basePath = path;
            if (!path.equals(parent)) {
                basePath = parent;
            }
            Shell.SU.run(String.format("busybox chown -R %s \"%s\"", userAndGroup, basePath));

            return true;
        }

        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (m_activity != null) {
            m_dialog.dismiss();
        }
        if (m_listener != null) {
            m_listener.onCommandCompleted(result);
        }
    }

    private String getValidDirectory(String path) {
        File filePath = new File(path);
        if (filePath.exists()) {
            if (filePath.isDirectory()) {
                return path;
            } else {
                return filePath.getParent();
            }
        }

        return getValidDirectory(filePath.getParent());
    }
}

