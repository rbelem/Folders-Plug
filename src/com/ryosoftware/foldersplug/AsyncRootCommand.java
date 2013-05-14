
package com.ryosoftware.foldersplug;

import android.os.AsyncTask;

import eu.chainfire.libsuperuser.Shell;

public class AsyncRootCommand extends AsyncTask<String, Void, Boolean> {

    @Override
    protected Boolean doInBackground(String... params) {
        if (Shell.SU.available()) {
            Shell.SU.run(params);
            return true;
        }

        return false;
    }
}
