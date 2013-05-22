package com.ryosoftware.foldersplug;

import com.ryosoftware.foldersplug.R;
import com.ryosoftware.objects.Utilities;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class CreateFolder extends Dialog implements OnClickListener {
    private static final String LOG_SUBTITLE = "CreateFolder";

    public static final String FOLDER_NAME = "folder";

    public interface CreateFolderCallback {
        abstract void onCreateFolderDialogEnded(int result_code, Intent intent);
    };

    private CreateFolderCallback iCallback;

    public CreateFolder(Context context, CreateFolderCallback callback) {
        super(context);
        iCallback = callback;
    }

    public void onCreate(Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);
        setContentView(R.layout.create_folder_dialog);
        findViewById(R.id.button_ok).setOnClickListener(this);
        findViewById(R.id.button_cancel).setOnClickListener(this);
        setTitle(R.string.new_folder_title);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class created");
    }

    public void finalize() throws Throwable {
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class destroyed");
    }

    public void onClick(View view) {
        if (view.getId() == R.id.button_ok) {
            Intent intent;
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Positive button clicked");
            intent = new Intent();
            intent.putExtra(FOLDER_NAME, ((EditText) findViewById(R.id.folder_name)).getText().toString());
            dismiss();
            if (iCallback != null) {
                iCallback.onCreateFolderDialogEnded(Activity.RESULT_OK, intent);
            }
        } else if ((view.getId() == R.id.button_cancel) && (iCallback != null)) {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Negative button clicked");
            dismiss();
            iCallback.onCreateFolderDialogEnded(Activity.RESULT_CANCELED, null);
        }
    }
}
