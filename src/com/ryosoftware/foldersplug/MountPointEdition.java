package com.ryosoftware.foldersplug;

import com.ryosoftware.foldersplug.AsyncMkdirCommand;
import com.ryosoftware.foldersplug.AsyncMkdirCommand.AsyncMkdirCommandListener;
import com.ryosoftware.foldersplug.AsyncRootCommand;
import com.ryosoftware.foldersplug.AsyncRootCommand.AsyncRootCommandListener;
import com.ryosoftware.foldersplug.R;
import com.ryosoftware.objects.DialogUtilities;
import com.ryosoftware.objects.DialogUtilities.ButtonClickCallback;
import com.ryosoftware.objects.Utilities;
import android.app.Activity;
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

import java.util.List;

public class MountPointEdition extends Activity implements OnClickListener {
    private static final String LOG_SUBTITLE = "MountPointEdition";

    public static final String SOURCE_PATH = "source";
    public static final String TARGET_PATH = "target";

    private static final int SOURCE_FILE_DIALOG = 1;
    private static final int TARGET_FILE_DIALOG = 2;

    private static final int START_ACCEPT_STATE = 1;
    private static final int TEST_TARGET_FOLDER = 2;
    private static final int COMBINE_CONTENTS = 3;
    private static final int CLEANUP_TARGET_CONTENTS = 4;
    private static final int CLEANUP_SOURCE_CONTENTS = 5;
    private static final int MOVE_TARGET_CONTENTS = 6;
    private static final int FINISH_ACCEPT_STATE = 7;

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
            Intent intent = new Intent(this, FolderSelection.class);
            intent.putExtra(FolderSelection.START_PATH, iSourceText.getText().toString());
            startActivityForResult(intent, SOURCE_FILE_DIALOG);
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Folder selection activity started");
        } else if (view.getId() == iTargetButton.getId()) {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Set target button clicked");
            Intent intent = new Intent(this, FolderSelection.class);
            intent.putExtra(FolderSelection.START_PATH, iTargetText.getText().toString());
            startActivityForResult(intent, TARGET_FILE_DIALOG);
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

    private List<String> runRootCommand(String command) {
        List<String> result = null;

        try {
            AsyncRootCommand rootCommand = new AsyncRootCommand();
            rootCommand.execute(command).get();
            result = rootCommand.result();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private ButtonClickCallback cleanupSourceCallback = new ButtonClickCallback() {
        @Override
        public void onClick() {
            doAcceptActions(CLEANUP_SOURCE_CONTENTS);
        }
    };

    private ButtonClickCallback combineContentsCallback = new ButtonClickCallback() {
        @Override
        public void onClick() {
            doAcceptActions(COMBINE_CONTENTS);
        }
    };

    private ButtonClickCallback bypassCallback = new ButtonClickCallback() {
        @Override
        public void onClick() {
            doAcceptActions(FINISH_ACCEPT_STATE);
        }
    };

    private AsyncMkdirCommandListener createSourceFolder = new AsyncMkdirCommandListener() {
        @Override
        public void onCommandCompleted(Boolean result) {
            doAcceptActions(TEST_TARGET_FOLDER);
        }
    };

    private AsyncRootCommandListener combineContentsListener = new AsyncRootCommandListener() {
        @Override
        public void onCommandCompleted(List<String> result) {
            doAcceptActions(CLEANUP_TARGET_CONTENTS);
        }
    };

    private AsyncRootCommandListener cleanupTargetListener = new AsyncRootCommandListener() {
        @Override
        public void onCommandCompleted(List<String> result) {
            doAcceptActions(FINISH_ACCEPT_STATE);
        }
    };

    private AsyncRootCommandListener cleanupSourceListener = new AsyncRootCommandListener() {
        @Override
        public void onCommandCompleted(List<String> result) {
            doAcceptActions(TEST_TARGET_FOLDER);
        }
    };

    private AsyncRootCommandListener moveTargetContentToSource = new AsyncRootCommandListener() {
        @Override
        public void onCommandCompleted(List<String> result) {
            doAcceptActions(FINISH_ACCEPT_STATE);
        }
    };

    private void doAcceptActions(int state) {
        final String source = iSourceText.getText().toString();
        final String target = iTargetText.getText().toString();

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

            int action = FINISH_ACCEPT_STATE;
            List<String> resultTarget = runRootCommand(String.format("busybox ls -a \"%s\"", target));
            if (resultTarget.isEmpty()) {
                try {
                    AsyncMkdirCommand command = new AsyncMkdirCommand(this);
                    command.execute(target);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                String[] contents = resultTarget.get(0).split("[\\s]+");
                if (contents.length > 2) {
                    action = MOVE_TARGET_CONTENTS;
                }
            }

            List<String> resultSource = runRootCommand(String.format("busybox ls -a \"%s\"", source));
            if (resultSource.isEmpty()) {
                try {
                    AsyncMkdirCommand command = new AsyncMkdirCommand(this);
                    command.setListener(createSourceFolder);
                    command.execute(source);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            } else {
                String[] contents = resultSource.get(0).split("[\\s]+");
                if (contents.length > 2) {
                    Resources resources = getResources();
                    DialogUtilities.showConfirmDialog(this, resources.getString(R.string.warning_title),
                            resources.getString(R.string.target_folder_is_not_empty_need_action),
                            resources.getString(R.string.accept_button),
                            resources.getString(R.string.combine_button),
                            resources.getString(R.string.bypass_button),
                            cleanupSourceCallback, combineContentsCallback, bypassCallback);
                    break;
                }
            }

            doAcceptActions(action);

            break;
        case COMBINE_CONTENTS:
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Accept algorithm started: State COMBINE_CONTENTS");

            try {
                AsyncRootCommand rootCommand = new AsyncRootCommand(this);
                rootCommand.setListener(combineContentsListener);
                rootCommand.setDialogTitle("Wait...");
                rootCommand.setDialogMessage("Combining files...");
                rootCommand.execute(String.format("busybox cp -a \"%s/\"* \"%s\"", target, source));
            } catch (Exception e) {
                e.printStackTrace();
            }

            break;
        case CLEANUP_TARGET_CONTENTS:
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Accept algorithm started: State CLEANUP_TARGET_CONTENTS");

            try {
                AsyncRootCommand rootCommand = new AsyncRootCommand(this);
                rootCommand.setListener(cleanupTargetListener);
                rootCommand.setDialogTitle("Wait...");
                rootCommand.setDialogMessage("Cleaning up target...");
                rootCommand.execute(String.format("busybox rm -rf \"%s/\"*", target));
            } catch (Exception e) {
                e.printStackTrace();
            }

            break;
        case CLEANUP_SOURCE_CONTENTS:
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Accept algorithm started: State CLEANUP_SOURCE_CONTENTS");

            try {
                AsyncRootCommand rootCommand = new AsyncRootCommand(this);
                rootCommand.setListener(cleanupSourceListener);
                rootCommand.setDialogTitle("Wait...");
                rootCommand.setDialogMessage("Cleaning up source...");
                rootCommand.execute(String.format("busybox rm -rf \"%s/\"*", source));
            } catch (Exception e) {
                e.printStackTrace();
            }

            break;
        case MOVE_TARGET_CONTENTS:
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Accept algorithm started: State MOVE_TARGET_CONTENTS");

            try {
                AsyncRootCommand rootCommand = new AsyncRootCommand(this);
                rootCommand.setListener(moveTargetContentToSource);
                rootCommand.setDialogTitle("Wait...");
                rootCommand.setDialogMessage("Moving files...");
                rootCommand.execute(String.format("busybox mv -f \"%s/\"* \"%s\"", target, source));
            } catch (Exception e) {
                e.printStackTrace();
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

    private void setAcceptButtonState() {
        iAcceptButton.setEnabled((iSourceText.getText().length() > 0) && (iTargetText.getText().length() > 0));
    }

    protected void onActivityResult(int request_code, int result_code, Intent intent) {
        if (result_code != RESULT_OK) {
            return;
        }

        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Retrieving mountpoint value");
        if (request_code == SOURCE_FILE_DIALOG) {
            iSourceText.setText(intent.getStringExtra(FolderSelection.SELECTED_PATH));
            String path = intent.getStringExtra(FolderSelection.SELECTED_PATH);
            if (path.startsWith("/mnt") || path.startsWith("/media") || path.startsWith("/storage")
                    || path.startsWith(Environment.getExternalStorageDirectory().getPath()))
                iTargetText.setText(intent.getStringExtra(FolderSelection.SELECTED_PATH));
        } else if (request_code == TARGET_FILE_DIALOG) {
            iTargetText.setText(intent.getStringExtra(FolderSelection.SELECTED_PATH));
        }

        setAcceptButtonState();
    }
}
