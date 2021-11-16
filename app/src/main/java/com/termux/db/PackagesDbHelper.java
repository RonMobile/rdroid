package com.termux.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

public class PackagesDbHelper extends SQLiteOpenHelper {

    // SQLs
    private static final String SQL_CREATE_ENTRIES =
        "CREATE TABLE " + PackagesContract.PackageEntry.TABLE_NAME + " (" +
            PackagesContract.PackageEntry._ID + " INTEGER PRIMARY KEY," +
            PackagesContract.PackageEntry.COLUMN_NAME_NAME + " TEXT," +
            PackagesContract.PackageEntry.COLUMN_NAME_VERSION + " TEXT," +
            PackagesContract.PackageEntry.COLUMN_NAME_ACTION + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
        "DROP TABLE IF EXISTS " + PackagesContract.PackageEntry.TABLE_NAME;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Packages.db";

    public PackagesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public static String getPackageAction(Context context, String packageName){

        SQLiteOpenHelper dbHelper = new PackagesDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String[] projection = {
            BaseColumns._ID,
            PackagesContract.PackageEntry.COLUMN_NAME_NAME,
            PackagesContract.PackageEntry.COLUMN_NAME_VERSION,
            PackagesContract.PackageEntry.COLUMN_NAME_ACTION
        };

        // Filter results WHERE
        String selection = PackagesContract.PackageEntry.COLUMN_NAME_NAME + " = ?";
        String[] selectionArgs = { packageName };

        Cursor cursor = db.query(
            PackagesContract.PackageEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        );

        cursor.moveToNext();
        String action =
            cursor.getString(
                cursor.getColumnIndexOrThrow(PackagesContract.PackageEntry.COLUMN_NAME_ACTION)
            );

        cursor.close();

        return action;
    }

}
