package com.ryosoftware.foldersplug;
import java.util.ArrayList;

import android.content.Context;

import com.ryosoftware.objects.Utilities;


public class SuperuserCommandsExecutor {
    private static final String LOG_SUBTITLE = "SuperuserCommandsExecutor";

    private static final String REDIRECT_ERRORS_OUTPUT_TO_STDOUT = "%s 2>&1";

    private static final String GET_FOLDER_DATA = "ls -l -d \"%s\"";
    private static final String GET_FOLDER_CHILDS_DATA = "ls -l -a \"%s\"";
    private static final String MKDIR_COMMAND = "mkdir \"%s\"";
    private static final String RECURSIVE_REMOVE_COMMAND = "rm -R \"%s\"";
    private static final String MOUNT_FOLDER_COMMAND = "mount -o bind \"%s\" \"%s\"";
    private static final String UNMOUNT_COMMAND = "umount \"%s\"";
    private static final String [] RECURSIVE_MOVE_COMMAND = { "mv \"%s/\"* \"%s\"", "mv \"%s/\".[!.]* \"%s\"", "mv \"%s/\"..?* \"%s\"" };

    private static final String LS_DATA_SEPARATORS_REGEXP = "[ \t]+";

    private static boolean iLsFieldsInitializated = false;
    private static int iLsFields;
    private static boolean iLsPrefixed;

    private SuperuserManager iSuperuserManager;

    SuperuserCommandsExecutor() {
        iSuperuserManager = new SuperuserManager();
        iSuperuserManager.beginSession();
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class created");
    }

    protected void finalize() throws Throwable {
        closeSession();
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class destroyed");
    }

    private void closeSession() {
        iSuperuserManager.closeSession();
    }

    public boolean canRunRootCommands() {
        return iSuperuserManager.canRunRootCommands();
    }

    private boolean initializeSuperuserFields() {
        if ((! iLsFieldsInitializated) && (iSuperuserManager.canRunRootCommands())) {
            iSuperuserManager.execute(String.format(GET_FOLDER_DATA, "/"));
            ArrayList<String> data = iSuperuserManager.getStandardOutput();
            if (data != null) {
                if (data.size() == 1) {
                    String [] fields = data.get(0).split(LS_DATA_SEPARATORS_REGEXP);
                    iLsFields = fields.length;
                    iLsPrefixed = true;
                    if (! fields [fields.length - 1].equals("/")) {
                        iLsFields ++;
                        iLsPrefixed = false;
                    }
                    iLsFieldsInitializated = true;
                    Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("LS command modifiers: fields=%d, prefixed=%s, data=%s", iLsFields, String.valueOf(iLsPrefixed), data.get(0)));
                } else {
                    Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "D: " + data.toString());
                }
            } else {
                Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Error getting data output");
            }
        }
        return iLsFieldsInitializated;
    }

    public ArrayList<String> getChildFolders(String pathname) {
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Trying to get contents of folder '%s'", pathname + "/"));
        if ((initializeSuperuserFields()) && (canRunRootCommands())) {
            iSuperuserManager.execute(String.format(GET_FOLDER_CHILDS_DATA, pathname + "/"));
            ArrayList<String> childs = iSuperuserManager.getStandardOutput();
            if (childs != null) {
                for (int i = childs.size() - 1; i >= 0; i --) {
                    String child = childs.get(i);
                    if (! child.startsWith("d")) {
                        childs.remove(i);
                        continue;
                    }
                    String [] data = child.split(LS_DATA_SEPARATORS_REGEXP, iLsFields);
                    String filename = data [iLsFields - 1];
                    if ((iLsPrefixed) && (filename.startsWith(pathname + "/"))) {
                        filename = filename.substring(pathname.length());
                    }
                    if ((filename.equals(".")) || (filename.equals(".."))) {
                        childs.remove(i);
                        continue;
                    }
                    childs.set(i, filename);
                }
                Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Folder contents has %d entries", (childs != null) ? childs.size() : 0));
                return childs;
            } else {
                Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Error getting data output");
            }
        } else {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, iLsFieldsInitializated ? "Can't get root privileges" : "LS fields not initializated");
        }
        return null;
    }

    public static ArrayList<String> getChildFolders(Context context, String pathname) {
        SuperuserCommandsExecutor superuser_commands_executor = new SuperuserCommandsExecutor();
        ArrayList<String> childs = superuser_commands_executor.getChildFolders(pathname);
        superuser_commands_executor.closeSession();
        return childs;
    }

    public boolean createFolder(String pathname) {
        boolean created = false;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Trying to create folder '%s'", pathname));
        if ((initializeSuperuserFields()) && (canRunRootCommands())) {
            iSuperuserManager.execute(String.format(MKDIR_COMMAND, pathname));
            created = isFolder(pathname);
        } else {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, iLsFieldsInitializated ? "Can't get root privileges" : "LS fields not initializated");
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Create folder returns: " + created);
        return created;
    }

    public static boolean createFolder(Context context, String pathname) {
        SuperuserCommandsExecutor superuser_commands_executor = new SuperuserCommandsExecutor();
        boolean created = superuser_commands_executor.createFolder(pathname);
        superuser_commands_executor.closeSession();
        return created;
    }

    public boolean isFolder(String pathname) {
        boolean is_folder = false;
        String real_pathname = (pathname.length() == 0) ? "/" : pathname;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Trying to test if '%s' is a folder", real_pathname));
        if ((initializeSuperuserFields()) && (canRunRootCommands())) {
            iSuperuserManager.execute(String.format(GET_FOLDER_DATA, real_pathname));
            ArrayList<String> data = iSuperuserManager.getStandardOutput();
            if (data != null) {
                if ((data.size() == 1) && (data.get(0).startsWith("d"))) {
                    is_folder = true;
                } else if (data.size() != 1) {
                    Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Unparseable data output: " + data.toString());
                }
            } else {
                Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Error getting data output");
            }
        } else {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, iLsFieldsInitializated ? "Can't get root privileges" : "LS fields not initializated");
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Is folder returns: " + is_folder);
        return is_folder;
    }

    public static boolean isFolder(Context context, String pathname) {
        SuperuserCommandsExecutor superuser_commands_executor = new SuperuserCommandsExecutor();
        boolean is_folder = superuser_commands_executor.isFolder(pathname);
        superuser_commands_executor.closeSession();
        return is_folder;
    }

    public boolean isEmptyFolder(String pathname) {
        boolean is_empty = false;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Trying to known is folder '%s' is empty", pathname + "/"));
        if ((initializeSuperuserFields()) && (canRunRootCommands())) {
            if (isFolder(pathname)) {
                iSuperuserManager.execute(String.format(GET_FOLDER_CHILDS_DATA, pathname + "/"));
                ArrayList<String> childs = iSuperuserManager.getStandardOutput();
                if (childs != null) {
                    is_empty = true;
                    for (int i = 0; i < childs.size(); i ++) {
                        String child = childs.get(i);
                        if (! child.startsWith("d")) {
                            is_empty = false;
                            break;
                        }
                        String [] data = child.split(LS_DATA_SEPARATORS_REGEXP, iLsFields);
                        String filename = data [iLsFields - 1];
                        if ((iLsPrefixed) && (filename.startsWith(pathname + "/"))) {
                            filename = filename.substring(pathname.length());
                        }
                        if ((! filename.equals(".")) && (! filename.equals(".."))) {
                            is_empty = false;
                            break;
                        }
                        childs.set(i, filename);
                    }
                } else {
                    Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Error getting data output");
                }
            } else {
                Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Isn't a folder");
            }
        } else {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, iLsFieldsInitializated ? "Can't get root privileges" : "LS fields not initializated");
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Is empty folder returns: " + is_empty);
        return is_empty;
    }

    public static boolean isEmptyFolder(Context context, String pathname) {
        SuperuserCommandsExecutor superuser_commands_executor = new SuperuserCommandsExecutor();
        boolean is_empty = superuser_commands_executor.isEmptyFolder(pathname);
        superuser_commands_executor.closeSession();
        return is_empty;
    }

    public boolean deleteFolderContents(String pathname) {
        boolean deleted = false;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Trying to delete contents for folder '%s'", pathname));
        if ((initializeSuperuserFields()) && (canRunRootCommands())) {
            ArrayList<String> commands = new ArrayList<String>();
            commands.add(String.format(RECURSIVE_REMOVE_COMMAND, pathname));
            commands.add(String.format(MKDIR_COMMAND, pathname));
            iSuperuserManager.execute(commands);
            deleted = isEmptyFolder(pathname);
        } else {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, iLsFieldsInitializated ? "Can't get root privileges" : "LS fields not initializated");
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Remove returns: " + deleted);
        return deleted;
    }

    public static boolean deleteFolderContents(Context context, String pathname) {
        SuperuserCommandsExecutor superuser_commands_executor = new SuperuserCommandsExecutor();
        boolean deleted = superuser_commands_executor.deleteFolderContents(pathname);
        superuser_commands_executor.closeSession();
        return deleted;
    }

    public boolean moveFolderContents(String source, String destination) {
        boolean moved = false;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Trying to move contents for folder '%s' to folder '%s'", source, destination));
        if ((initializeSuperuserFields()) && (canRunRootCommands())) {
            ArrayList<String> commands = new ArrayList<String>();
            for (int i = 0; i < RECURSIVE_MOVE_COMMAND.length; i ++) {
                commands.add(String.format(RECURSIVE_MOVE_COMMAND [i], source, destination));
            }
            iSuperuserManager.execute(commands);
            moved = isEmptyFolder(source);
        } else {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, iLsFieldsInitializated ? "Can't get root privileges" : "LS fields not initializated");
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Move returns: " + moved);
        return moved;
    }

    public static boolean moveFolderContents(Context context, String source, String destination) {
        SuperuserCommandsExecutor superuser_commands_executor = new SuperuserCommandsExecutor();
        boolean moved = superuser_commands_executor.moveFolderContents(source, destination);
        superuser_commands_executor.closeSession();
        return moved;
    }

    public boolean mountFolders(String source, String target) {
        boolean mounted = false;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Trying to mount '%s' into folder '%s'", source, target));
        if ((initializeSuperuserFields()) && (canRunRootCommands())) {
            ArrayList<String> data;
            iSuperuserManager.execute(String.format(MKDIR_COMMAND, target));
            iSuperuserManager.execute(String.format(REDIRECT_ERRORS_OUTPUT_TO_STDOUT, String.format(MOUNT_FOLDER_COMMAND, source, target)));
            data = iSuperuserManager.getStandardOutput();
            if (data != null) {
                if (data.size() == 0) {
                    mounted = true;
                } else {
                    Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, data.toString());
                }
            } else {
                Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Error getting data/errors output");
            }
        } else {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, iLsFieldsInitializated ? "Can't get root privileges" : "LS fields not initializated");
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Mount returns: " + mounted);
        return mounted;
    }

    public boolean mountFolders(Context context, String source, String target) {
        SuperuserCommandsExecutor superuser_commands_executor = new SuperuserCommandsExecutor();
        boolean mounted = superuser_commands_executor.mountFolders(source, target);
        superuser_commands_executor.closeSession();
        return mounted;
    }

    public boolean unmountFolder(String target) {
        boolean unmounted = false;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Trying to unmount '%s'", target));
        if ((initializeSuperuserFields()) && (canRunRootCommands())) {
            ArrayList<String> data;
            iSuperuserManager.execute(String.format(REDIRECT_ERRORS_OUTPUT_TO_STDOUT, String.format(UNMOUNT_COMMAND, target)));
            data = iSuperuserManager.getStandardOutput();
            if (data != null) {
                if (data.size() == 0) {
                    unmounted = true;
                } else {
                    Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, data.toString());
                }
            } else {
                Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Error getting data/errors output");
            }
        } else {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, iLsFieldsInitializated ? "Can't get root privileges" : "LS fields not initializated");
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Unmount returns: " + unmounted);
        return unmounted;
    }

    public boolean unmountFolders(Context context, String target) {
        SuperuserCommandsExecutor superuser_commands_executor = new SuperuserCommandsExecutor();
        boolean unmounted = superuser_commands_executor.unmountFolder(target);
        superuser_commands_executor.closeSession();
        return unmounted;
    }
}
