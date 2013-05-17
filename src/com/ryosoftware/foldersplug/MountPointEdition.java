package com.ryosoftware.foldersplug;

import com.ryosoftware.foldersplug.R;
import com.ryosoftware.objects.DialogUtilities;
import com.ryosoftware.objects.DialogUtilities.ButtonClickCallback;
import com.ryosoftware.objects.Utilities;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;


import ar.com.daidalos.afiledialog.FileChooserDialog;

public class MountPointEdition extends Activity implements OnClickListener {
    private static final String LOG_SUBTITLE = "MountPointEdition";

    public static final String SOURCE_PATH = "source";
    public static final String TARGET_PATH = "target";

    private static final int SOURCE_FILE_DIALOG = 1;
    private static final int TARGET_FILE_DIALOG = 2;

    private static final int START_ACCEPT_STATE = 1;
    private static final int TEST_TARGET_FOLDER = 2;
    private static final int FINISH_ACCEPT_STATE = 3;

    private Button iAcceptButton;
    private Button iCancelButton;
    private Button iSourceButton;
    private Button iTargetButton;
    private TextView iSourceText;
    private TextView iTargetText;

    public void onCreate(Bundle saved_instance_bundle) {
        super.onCreate(saved_instance_bundle);
        setContentView(R.layout.edit_mountpoint);
        findViewById(R.id.source_button).setOnClickListener(this);
        findViewById(R.id.target_button).setOnClickListener(this);
        (iAcceptButton = (Button) findViewById(R.id.accept)).setOnClickListener(this);
        (iCancelButton = (Button) findViewById(R.id.cancel)).setOnClickListener(this);
        (iSourceButton = (Button) findViewById(R.id.source_button)).setOnClickListener(this);
        iSourceText = (TextView) findViewById(R.id.source_text);
        (iTargetButton = (Button) findViewById(R.id.target_button)).setOnClickListener(this);
        iTargetText = (TextView) findViewById(R.id.target_text);
        Intent intent = getIntent();
        iSourceText.setText(intent.getStringExtra(SOURCE_PATH));
        iTargetText.setText(intent.getStringExtra(TARGET_PATH));
        setAcceptButtonState();
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class created");
    }

    public void onDestroy() {
        super.onDestroy();
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class destroyed");
    }

    public void onClick(View view) {
        if (view.getId() == iSourceButton.getId()) {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Set source button clicked");

            FileChooserDialog dialog = new FileChooserDialog(this);
            dialog.setFolderMode(true);
            dialog.addListener(this.onSourceFolderSelectedListener);
            dialog.show();

            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Folder selection activity started");
        } else if (view.getId() == iTargetButton.getId()) {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Set target button clicked");

            FileChooserDialog dialog = new FileChooserDialog(this);
            dialog.setFolderMode(true);
            dialog.addListener(this.onTargetFolderSelectedListener);
            dialog.show();

            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Folder selection activity started");
        } else if (view.getId() == iAcceptButton.getId()) {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Accept button clicked");
            doAcceptActions(START_ACCEPT_STATE);
        } else if (view.getId() == iCancelButton.getId()) {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Cancel button clicked");
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private class SourceFolderDeletionConfirmButtonCallback implements ButtonClickCallback {
        private static final int DELETE_DESTINATION_ACTION = 1;
        private static final int COMBINE_ACTION = 2;
        private static final int BYPASS_ACTION = 3;

        private Activity iActivity;
        private String iSource;
        private String iTarget;
        private int iAction;

        private class DeleteFolderContentsInstructions extends Thread {
            private int OPERATION_COMPLETED_WITHOUT_ERRORS = 0;
            private int CANNOT_DELETE_FOLDER_CONTENTS = 1;
            private int CANNOT_MOVE_FOLDER_CONTENTS = 2;

            class DeleteFolderContentsInstructionsHandler extends Handler {
                public void handleMessage(Message message) {
                    if (iDialog != null) {
                        iDialog.dismiss();
                    }
                    if (message.what == CANNOT_DELETE_FOLDER_CONTENTS) {
                        DialogUtilities.showAlertDialog(iActivity, R.string.cannot_delete_files, null);
                    } else if (message.what == CANNOT_MOVE_FOLDER_CONTENTS) {
                        DialogUtilities.showAlertDialog(iActivity, R.string.cannot_move_files, null);
                    } else if (message.what == OPERATION_COMPLETED_WITHOUT_ERRORS) {
                        doAcceptActions(FINISH_ACCEPT_STATE);
                    }
                }
            }

            private ProgressDialog iDialog;
            private DeleteFolderContentsInstructionsHandler iHandler;

            DeleteFolderContentsInstructions(ProgressDialog dialog) {
                iDialog = dialog;
                iHandler = new DeleteFolderContentsInstructionsHandler();
            }

            public void run() {
                if (iAction == BYPASS_ACTION) {
                    iHandler.sendEmptyMessage(OPERATION_COMPLETED_WITHOUT_ERRORS);
                } else {
                    boolean allow_move = true;
                    if (iAction == DELETE_DESTINATION_ACTION) {
                        if (! SuperuserCommandsExecutor.deleteFolderContents(iActivity, iSource)) {
                            allow_move = false;
                            iHandler.sendEmptyMessage(CANNOT_DELETE_FOLDER_CONTENTS);
                        }
                    }
                    if (allow_move) {
                        iHandler.sendEmptyMessage(SuperuserCommandsExecutor.moveFolderContents(iActivity, iTarget, iSource) ? OPERATION_COMPLETED_WITHOUT_ERRORS : CANNOT_DELETE_FOLDER_CONTENTS);
                    }
                }
            }
        }

        SourceFolderDeletionConfirmButtonCallback(Activity activity, String source, String target, int action) {
            iActivity = activity;
            iSource = source;
            iTarget = target;
            iAction = action;
        }

        public void onClick() {
            ProgressDialog dialog = null;
            if (iAction != BYPASS_ACTION) {
                (dialog = DialogUtilities.showProgressDialog(iActivity, R.string.moving_files)).show();
            }
            new DeleteFolderContentsInstructions(dialog).start();
        }
    }

    private void doAcceptActions(int state) {
        final String source = iSourceText.getText().toString(), target = iTargetText.getText().toString();
        switch (state) {
        case START_ACCEPT_STATE:
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Accept algorithm started: State START_ACCEPT_STATE");
            if (source.equals(target)) {
                DialogUtilities.showAlertDialog(this, R.string.source_and_target_coincidence, null);
            } else {
                doAcceptActions(TEST_TARGET_FOLDER);
            }
            break;
        case TEST_TARGET_FOLDER:
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Accept algorithm started: State TEST_TARGET_FOLDER");

            File filePath = new File(target);
            if (!filePath.exists()) {
                String parent = getValidDirectory(target);
                File fileParent = new File(parent);
                if (fileParent.canRead() && fileParent.canWrite()) {
                    filePath.mkdirs();
                } else {
                    SuperuserCommandsExecutor.createFolder(this, target);
                }
            }

            if (SuperuserCommandsExecutor.isEmptyFolder(this, target)) {
                doAcceptActions(FINISH_ACCEPT_STATE);
            } else {
                SourceFolderDeletionConfirmButtonCallback deletion_callback = new SourceFolderDeletionConfirmButtonCallback(this, source, target, SourceFolderDeletionConfirmButtonCallback.DELETE_DESTINATION_ACTION);
                SourceFolderDeletionConfirmButtonCallback combine_callback = new SourceFolderDeletionConfirmButtonCallback(this, source, target, SourceFolderDeletionConfirmButtonCallback.COMBINE_ACTION);
                SourceFolderDeletionConfirmButtonCallback bypass_callback = new SourceFolderDeletionConfirmButtonCallback(this, source, target, SourceFolderDeletionConfirmButtonCallback.BYPASS_ACTION);
                Resources resources = getResources();
                DialogUtilities.showConfirmDialog(this, resources.getString(R.string.warning_title), resources.getString(R.string.target_folder_is_not_empty_need_action), resources.getString(R.string.accept_button), resources.getString(R.string.combine_button), resources.getString(R.string.bypass_button), deletion_callback, combine_callback, bypass_callback);
            }
            break;
        case FINISH_ACCEPT_STATE:
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Accept algorithm started: State FINISH_ACCEPT_STATE");
            Intent intent = getIntent();
            intent.putExtra(SOURCE_PATH, source);
            intent.putExtra(TARGET_PATH, target);
            setResult(RESULT_OK, intent);
            finish();
            break;
        }
    }

    private String getValidDirectory(String path) {
        File filePath = new File(path);
        if (filePath.exists())
            if (filePath.isDirectory()) {
                return path;
            } else {
                return filePath.getParent();
            }

        return getValidDirectory(filePath.getParent());
    }

    private void setAcceptButtonState() {
        iAcceptButton.setEnabled((iSourceText.getText().length() > 0) && (iTargetText.getText().length() > 0));
    }

    private FileChooserDialog.OnFileSelectedListener onSourceFolderSelectedListener = new FileChooserDialog.OnFileSelectedListener() {
        public void onFileSelected(Dialog source, File file) {
            source.hide();
            String path = file.getPath();
            iSourceText.setText(path);
            if (path.startsWith("/mnt") || path.startsWith("/media") || path.startsWith("/storage")
                    || path.startsWith(Environment.getExternalStorageDirectory().getPath())) {
                iTargetText.setText(path);
            }
            setAcceptButtonState();
        }
        public void onFileSelected(Dialog source, File folder, String name) {
            source.hide();
        }
    };

    private FileChooserDialog.OnFileSelectedListener onTargetFolderSelectedListener = new FileChooserDialog.OnFileSelectedListener() {
        public void onFileSelected(Dialog source, File file) {
            source.hide();
            iTargetText.setText(file.getPath());
            setAcceptButtonState();
        }
        public void onFileSelected(Dialog source, File folder, String name) {
            source.hide();
        }
    };
}
