
package com.ryosoftware.foldersplug;

import android.os.AsyncTask;

import eu.chainfire.libsuperuser.Shell;

public class CheckRootCommand extends AsyncTask<Void, Void, Boolean> {

    private Boolean result = null;

    public Boolean result() {
        return result;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        result = Shell.SU.available();
        return result;
    }
}
