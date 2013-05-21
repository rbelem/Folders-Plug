package com.ryosoftware.foldersplug;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import com.ryosoftware.objects.Utilities;

public class SuperuserManager {
    private static final String LOG_SUBTITLE = "SuperuserManager";

    private static final String COMMANDS_SEPARATOR = "\n";

    private static final String SNEAK_STRING = "<@@SNEAK-STRING@@>";
    private static final String [] AUTO_ADDED_EXECUTE_COMMANDS = { String.format("echo \"%s\" >&1", SNEAK_STRING), String.format("echo \"%s\" 1>&2", SNEAK_STRING) };

    private static final String SU_COMMAND = "su -c sh";
    private static final String GET_UID_COMMAND = "id";
    private static final String EXIT_COMMAND = "exit";

    private Process iProcess;

    private DataInputStream iDataInputStream;
    private DataOutputStream iDataOutputStream;
    private DataInputStream iDataErrorStream;

    private boolean iCanRunRootCommands;
    private boolean iDataInputStreamAvailable;
    private boolean iDataErrorStreamAvailable;

    SuperuserManager() {
        iProcess = null;
        iDataInputStream = null;
        iDataOutputStream = null;
        iDataErrorStream = null;
        iCanRunRootCommands = iDataInputStreamAvailable = iDataErrorStreamAvailable = false;
    }

    protected void finalize() throws Exception {
        closeSession();
    }

    public boolean beginSession() {
        if (iProcess == null) {
            boolean correct = false;
            try {
                iProcess = Runtime.getRuntime().exec(SU_COMMAND);
                iDataOutputStream = new DataOutputStream(iProcess.getOutputStream());
                iDataInputStream = new DataInputStream(iProcess.getInputStream());
                iDataErrorStream = new DataInputStream(iProcess.getErrorStream());
                if (isRoot()) {
                    iCanRunRootCommands = correct = true;
                }
            } catch (Exception e) {
                closeSession();
            }
            if (! correct) {
                try {
                    closeSession();
                } finally {
                    iProcess = null;
                }
            }
        }
        return iCanRunRootCommands;
    }

    public boolean closeSession() {
        boolean is_ok = true;
        if (iDataOutputStream != null) {
            try {
                if (iProcess != null) {
                    try {
                        iDataOutputStream.writeBytes(COMMANDS_SEPARATOR + EXIT_COMMAND + COMMANDS_SEPARATOR);
                        iDataOutputStream.flush();
                        is_ok = (iProcess.waitFor() != 255);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                iDataOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (iDataInputStream != null) {
            try {
                iDataInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (iDataErrorStream != null) {
            try {
                iDataErrorStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        iProcess = null;
        iDataInputStream = null;
        iDataOutputStream = null;
        iDataErrorStream = null;
        iCanRunRootCommands = false;
        return is_ok;
    }

    private boolean isRoot() {
        boolean is_root = false;
        try {
            if (iProcess != null) {
                try {
                    iDataOutputStream.writeBytes(GET_UID_COMMAND + COMMANDS_SEPARATOR);
                    iDataOutputStream.flush();
                    String uid = iDataInputStream.readLine();
                    if (uid == null) {
                        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Can't get root access or denied by user");
                    } else {
                        if (! uid.contains("uid=0")) {
                            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Root access rejected. I'm " + uid);
                        } else {
                            is_root = true;
                            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Root access granted");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return is_root;
    }

    public boolean execute(ArrayList<String> commands) {
        boolean is_ok = false;
        try {
            if (iProcess != null) {
                if ((commands != null) && (commands.size() > 0)) {
                    try {
                        getStandardOutput();
                        getErrorOutput();
                        for (int i = 0; i < AUTO_ADDED_EXECUTE_COMMANDS.length; i ++) {
                            commands.add(AUTO_ADDED_EXECUTE_COMMANDS [i]);
                        }
                        for (int i = 0; i < commands.size(); i ++) {
                            String command = commands.get(i) + COMMANDS_SEPARATOR;
                            iDataOutputStream.writeBytes(command);
                            iDataOutputStream.flush();
                        }
                        is_ok = iDataInputStreamAvailable = iDataErrorStreamAvailable = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    is_ok = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return is_ok;
    }

    public boolean execute(String command) {
        ArrayList<String> commands = new ArrayList<String>();
        commands.add(command);
        return execute(commands);
    }

    private ArrayList<String> getOutputStream(DataInputStream stream, boolean include_empty_lines) {
        ArrayList<String> data = new ArrayList<String>();
        if (stream != null) {
            try {
                String line;
                while (true) {
                    line = stream.readLine();
                    if ((line == null) || (line.equals(SNEAK_STRING))) {
                        break;
                    }
                    if ((include_empty_lines) || (line.length() > 0)) {
                        data.add(line);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    public ArrayList<String> getStandardOutput() {
        if (iDataInputStreamAvailable) {
            iDataInputStreamAvailable = false;
            return getOutputStream(iDataInputStream, true);
        }
        return null;
    }

    public ArrayList<String> getErrorOutput() {
        if (iDataErrorStreamAvailable) {
            iDataErrorStreamAvailable = false;
            return getOutputStream(iDataErrorStream, false);
        }
        return null;
    }

    public boolean canRunRootCommands() {
        return iCanRunRootCommands;
    }
}
