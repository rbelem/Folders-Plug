package com.ryosoftware.foldersplug;

import com.ryosoftware.objects.Utilities;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseContainer
{
    private static final String LOG_SUBTITLE = "DatabaseContainer";

    public static final String DATABASE_NAME = "mountpoints";
    public static final int DATABASE_VERSION = 1;

    public static final String FOLDERS_TABLE_NAME = "folders";

    public static final String FOLDERS_TABLE_ROWID_KEY = "_id";
    public static final String FOLDERS_TABLE_SOURCE_KEY = "source";
    public static final String FOLDERS_TABLE_TARGET_KEY = "target";
    public static final String FOLDERS_TABLE_ENABLED_KEY = "enabled";

    public static final int ROW_ID_ERROR = -1;

    private static final String DATABASE_CREATE = "CREATE TABLE folders (_id INTEGER PRIMARY KEY AUTOINCREMENT, source TEXT NOT NULL, target TEXT UNIQUE, enabled INTEGER)";

    private static class DatabaseHelper extends SQLiteOpenHelper
    {
        private static final String LOG_SUBTITLE = "DatabaseHelper";

        DatabaseHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class created");
        }

        public void onCreate(SQLiteDatabase sqlite_database)
        {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Creating database");
            sqlite_database.execSQL(DATABASE_CREATE);
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Database created");
        }

        public void onUpgrade(SQLiteDatabase sqlite_database, int old_version, int new_version)
        {
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Deleting database");
            sqlite_database.execSQL("DROP TABLE IF EXISTS titles");
            Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Database deleted");
            onCreate(sqlite_database);
        }
    }

    private Context iContext;
    private DatabaseHelper iDatabaseHelper;
    private SQLiteDatabase iSQLiteDatabase;

    public DatabaseContainer(Context context)
    {
        iContext = context;
        iDatabaseHelper = new DatabaseHelper(iContext);
        iSQLiteDatabase = null;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Class created");
    }

    public void open() throws SQLException
    {
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Opening database");
        if (iSQLiteDatabase == null)
        {
            try
            {
                iSQLiteDatabase = iDatabaseHelper.getWritableDatabase();
                Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Database opened");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Database is already opened");
    }

    public void close()
    {
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Closing database");
        if (iSQLiteDatabase != null)
        {
            try
            {
                iDatabaseHelper.close();
                iSQLiteDatabase = null;
                Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Database closed");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Database is not opened");
    }

    public int insert(String source, String destination, boolean enabled)
    {
        int id = ROW_ID_ERROR;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Trying to make insert (source=%s, destination=%s, enabled=%s)", source, destination, Utilities.getString(enabled, Utilities.GET_STRING_FROM_BOOLEAN_TYPE_IS_AVAILABILITY)));
        if (iSQLiteDatabase != null)
        {
            try
            {
                ContentValues values = new ContentValues();
                values.put(FOLDERS_TABLE_SOURCE_KEY, source);
                values.put(FOLDERS_TABLE_TARGET_KEY, destination);
                values.put(FOLDERS_TABLE_ENABLED_KEY, enabled);
                id = (int) iSQLiteDatabase.insert(FOLDERS_TABLE_NAME, null, values);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Insert identifier is: " + id);
        return id;
    }

    public boolean update(int row_id, String source, String destination, boolean enabled)
    {
        boolean updated = false;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Trying to make update (source=%s, destination=%s, enabled=%s)", source, destination, Utilities.getString(enabled, Utilities.GET_STRING_FROM_BOOLEAN_TYPE_IS_AVAILABILITY)));
        if (iSQLiteDatabase != null)
        {
            try
            {
                ContentValues values = new ContentValues();
                values.put(FOLDERS_TABLE_SOURCE_KEY, source);
                values.put(FOLDERS_TABLE_TARGET_KEY, destination);
                values.put(FOLDERS_TABLE_ENABLED_KEY, enabled);
                updated = (iSQLiteDatabase.update(FOLDERS_TABLE_NAME, values, String.format("%s=%d", FOLDERS_TABLE_ROWID_KEY, row_id), null) > 0);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Update result is: " + updated);
        return updated;
    }

    public boolean delete(int row_id)
    {
        boolean deleted = false;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Deleting row with id=%d", row_id));
        if ((iSQLiteDatabase != null) && (row_id != ROW_ID_ERROR))
        {
            try
            {
                deleted = (iSQLiteDatabase.delete(FOLDERS_TABLE_NAME, String.format("%s=%d", FOLDERS_TABLE_ROWID_KEY, row_id), null) > 0);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Delete result is: " + deleted);
        return deleted;
    }

    public Cursor getRows()
    {
        Cursor cursor = null;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, "Retrieving all rows data");
        if (iSQLiteDatabase != null)
        {
            String [] fields = { FOLDERS_TABLE_ROWID_KEY, FOLDERS_TABLE_SOURCE_KEY, FOLDERS_TABLE_TARGET_KEY, FOLDERS_TABLE_ENABLED_KEY };
            try
            {
                cursor = iSQLiteDatabase.query(FOLDERS_TABLE_NAME, fields, null, null, null, null, null);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, (cursor == null) ? "Returning a empty result" : "Returning a non empty result");
        return cursor;
    }

    public Cursor getRow(int row_id) throws SQLException
    {
        Cursor cursor = null;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Retrieving row with id=%d", row_id));
        if (iSQLiteDatabase != null)
        {
            String [] fields = { FOLDERS_TABLE_ROWID_KEY, FOLDERS_TABLE_SOURCE_KEY, FOLDERS_TABLE_TARGET_KEY, FOLDERS_TABLE_ENABLED_KEY };
            try
            {
                cursor = iSQLiteDatabase.query(true, FOLDERS_TABLE_NAME, fields, String.format("%s=%d", FOLDERS_TABLE_ROWID_KEY, row_id), null, null, null, null, null);
                if (cursor != null) cursor.moveToFirst();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, (cursor == null) ? "Returning a empty result" : "Returning a non empty result");
        return cursor;
    }

    public long getRowIdBySource(String source)
    {
        long id = ROW_ID_ERROR;
        Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Retrieving row with source='%s'", source));
        if (iSQLiteDatabase != null)
        {
            try
            {
                String [] fields = { FOLDERS_TABLE_ROWID_KEY };
                Cursor cursor = iSQLiteDatabase.query(true, FOLDERS_TABLE_NAME, fields, String.format("%s='%s'", FOLDERS_TABLE_SOURCE_KEY, source), null, null, null, null, null);
                if (cursor != null)
                {
                    try
                    {
                        if (cursor.getCount() > 0)
                        {
                            cursor.moveToFirst();
                            id = cursor.getLong(0);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    cursor.close();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        if (id == ROW_ID_ERROR) Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Id for row with source='%s' couldn't be retrieved", source));
        else Utilities.log(Constants.LOG_TITLE, LOG_SUBTITLE, String.format("Retrieved id for row with source='%s' is %d", source, id));
        return id;
    }
}

