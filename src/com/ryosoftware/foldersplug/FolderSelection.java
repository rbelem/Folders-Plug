package com.ryosoftware.foldersplug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import com.ryosoftware.foldersplug.R;
import com.ryosoftware.foldersplug.CreateFolder.CreateFolderCallback;
import com.ryosoftware.objects.DialogUtilities;
import com.ryosoftware.objects.DialogUtilities.ButtonClickCallback;
import com.ryosoftware.objects.Utilities;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class FolderSelection extends Activity implements OnClickListener, OnItemClickListener, CreateFolderCallback
{
    private static final String LOG_SUBTITLE = "FolderSelection";

    private static final String ROOT_DIRECTORY = "";

    public static final String START_PATH = "start";
    public static final String SELECTED_PATH = "result";

    private static final String ITEM_KEY = "key";
    private static final String ITEM_IMAGE = "image";

    private TextView iFolderNameText;
    private Button iSelectButton;
    private Button iCancelButton;
    private Button iNewButton;
    private ListView iListView;
    private ArrayList<HashMap<String, Object>> iFilesList;
    private SimpleAdapter iAdapterList;
    private SuperuserCommandsExecutor iSuperuserCommandsExecutor;
    private String iCurrentPath;

    public void onCreate(Bundle saved_instance_state)
    {
        super.onCreate(saved_instance_state);
        setContentView(R.layout.files_list);
        iFolderNameText = (TextView) findViewById(R.id.folder_name);
        (iSelectButton = (Button) findViewById(R.id.select)).setOnClickListener(this);
        (iCancelButton = (Button) findViewById(R.id.cancel)).setOnClickListener(this);
        (iNewButton = (Button) findViewById(R.id.create)).setOnClickListener(this);
        (iListView = (ListView) findViewById (R.id.files_listview)).setOnItemClickListener(this);
        iFilesList = new ArrayList<HashMap<String, Object>>();
        iAdapterList = new SimpleAdapter(this, iFilesList, R.layout.file_row, new String [] { ITEM_KEY, ITEM_IMAGE }, new int [] { R.id.name, R.id.icon });
        iSuperuserCommandsExecutor = new SuperuserCommandsExecutor();
        iCurrentPath = getIntent().getExtras().getString(START_PATH);
        if ((iCurrentPath == null) || (iCurrentPath.equals("")) || (! iSuperuserCommandsExecutor.isFolder(iCurrentPath))) iCurrentPath = ROOT_DIRECTORY;
        else if (iCurrentPath.equals("/")) iCurrentPath = "";
        setResult(RESULT_CANCELED);
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class created");
    }

    public void onDestroy()
    {
        super.onDestroy();
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class destroyed");
    }

    public void onResume()
    {
        super.onResume();
        if (! iSuperuserCommandsExecutor.canRunRootCommands()) DialogUtilities.showAlertDialog(this, R.string.initialization_error_possibly_cause_no_root_adquired, new ButtonClickCallback()
        {
            public void onClick()
            {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        else showDirectoryContents(iCurrentPath);
    }

    private String getParent(String pathname)
    {
        int index = pathname.lastIndexOf("/");
        if (index <= 0) return "";
        return pathname.substring(0, index);
    }

    public void onClick(View view)
    {
        int id = view.getId();
        if (id == iSelectButton.getId())
        {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Folder select button clicked");
            Intent intent = getIntent();
            intent.putExtra(SELECTED_PATH, iCurrentPath.equals("") ? "/" : iCurrentPath);
            setResult(RESULT_OK, intent);
            finish();
        }
        else if (id == iCancelButton.getId())
        {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Folder selection cancel button clicked");
            setResult(RESULT_CANCELED);
            finish();
        }
        else if (id == iNewButton.getId())
        {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Create folder button clicked");
            CreateFolder dialog = new CreateFolder(this, this);
            dialog.show();
        }
    }

    public void onCreateFolderDialogEnded(int result_code, Intent intent)
    {
        if (result_code == RESULT_OK)
        {
            String filename = intent.getStringExtra(CreateFolder.FOLDER_NAME);
            if (! iSuperuserCommandsExecutor.createFolder(String.format("%s/%s", iCurrentPath, filename))) DialogUtilities.showAlertDialog(this, R.string.cannot_create_folder, null);
            else iCurrentPath = String.format("%s/%s", iCurrentPath, filename);
            showDirectoryContents(iCurrentPath);
        }
    }

    private HashMap<String, Object> createListViewItem(String name, int image)
    {
        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put(ITEM_KEY, name);
        item.put(ITEM_IMAGE, image);
        return item;
    }

    private void showDirectoryContents(String pathname, ArrayList<String> childs)
    {
        iCurrentPath = pathname;
        iFolderNameText.setText(iCurrentPath + "/");
        iFilesList.clear();
        if (! iCurrentPath.equals("")) iFilesList.add(createListViewItem(getResources().getString(R.string.folder_up), R.drawable.folder_up));
        if (childs != null)
        {
            Collections.sort(childs);
            for (int i = 0; i < childs.size(); i ++) iFilesList.add(createListViewItem(childs.get(i), R.drawable.folder));
        }
        iAdapterList.notifyDataSetChanged();
        iListView.setAdapter(iAdapterList);
    }

    private class CannotReadFolderDialogButtonClickCallback implements ButtonClickCallback
    {
        private String iPathname;
        ArrayList<String> iChilds;

        CannotReadFolderDialogButtonClickCallback(String pathname, ArrayList<String> childs)
        {
            iPathname = pathname;
            iChilds = childs;
        }

        public void onClick()
        {
            showDirectoryContents(iPathname, iChilds);
        }
    }

    private void showDirectoryContents(String pathname, boolean inhibite_warnings)
    {
        boolean show_directory_contents = true;
        ArrayList<String> childs = iSuperuserCommandsExecutor.getChildFolders(pathname);
        if (childs == null)
        {
            while (true)
            {
                pathname = getParent(pathname);
                childs = iSuperuserCommandsExecutor.getChildFolders(pathname);
                if ((childs != null) || (pathname.equals(""))) break;
            }
            if (! inhibite_warnings)
            {
                DialogUtilities.showAlertDialog(this, R.string.cannot_read_folder, new CannotReadFolderDialogButtonClickCallback(pathname, childs));
                show_directory_contents = false;
            }
        }
        if (show_directory_contents) showDirectoryContents(pathname, childs);
    }

    private class FolderNotExistsDialogButtonClickCallback implements ButtonClickCallback
    {
        private String iPathname;

        FolderNotExistsDialogButtonClickCallback(String pathname)
        {
            iPathname = pathname;
        }

        public void onClick()
        {
            while ((! iSuperuserCommandsExecutor.isFolder(iPathname)) && (! iPathname.equals(""))) iPathname = getParent(iPathname);
            showDirectoryContents(iPathname, true);
        }
    }

    private void showDirectoryContents(String pathname)
    {
        boolean show_directory_contents = true;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Showing contents for folder '%s'", pathname));
        if (! iSuperuserCommandsExecutor.isFolder(pathname))
        {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Handling folder don\'t exists error");
            DialogUtilities.showAlertDialog(this, R.string.folder_do_not_exists, new FolderNotExistsDialogButtonClickCallback(getParent(pathname)));
            show_directory_contents = false;
        }
        if (show_directory_contents) showDirectoryContents(pathname, false);
    }

    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
    {
        String filename;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("List item clicked at position %d", position));
        if ((! iCurrentPath.equals("")) && (position == 0)) filename = getParent(iCurrentPath);
        else filename = String.format("%s/%s", iCurrentPath, ((HashMap<String, Object>) iFilesList.get(position)).get(ITEM_KEY).toString());
        showDirectoryContents(filename);
    }

    public boolean onKeyDown(int key_code, KeyEvent event)
    {
        if ((key_code == KeyEvent.KEYCODE_BACK) && (! iCurrentPath.equals("")))
        {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Handling back key click"));
            showDirectoryContents(getParent(iCurrentPath));
            return true;
        }
        else return super.onKeyDown(key_code, event);
    }
}
