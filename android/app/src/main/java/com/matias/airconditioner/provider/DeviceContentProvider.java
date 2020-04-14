package com.matias.airconditioner.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.util.HashMap;

/**
 * Created by Matias on 20/07/2016.
 */
public class DeviceContentProvider extends ContentProvider {
    static final String PROVIDER_NAME = "com.matias.airconditioner.provider.DeviceContentProvider";
    static final String URL = "content://" + PROVIDER_NAME + "/bt";
    public static final Uri CONTENT_URI = Uri.parse(URL);

    public static final String name = "name";
    public static final String mac = "mac";
    public static final String temp = "temp";

    static final int uriCode = 1;
    static final UriMatcher uriMatcher;
    private static HashMap<String, String> values;
    static {
        // Match the uri code to provider name
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "bt", uriCode);
        //uriMatcher.addURI(PROVIDER_NAME, "bt/*", uriCode);
    }
    private SQLiteDatabase db;
    private DatabaseHelper databaseHelper;
    static final String DATABASE_NAME = "center";// Database
    static final String TABLE_NAME = "center";// Table Name
    static final int DATABASE_VERSION = 1;// Database Version
    static final String CREATE_DB_TABLE = "CREATE TABLE " +
            TABLE_NAME + " (" + name + " TEXT, " + mac + " TEXT, " + temp + " INTEGER)"; // Create table query

    @Override
    public boolean onCreate() {
        databaseHelper = new DatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        String where = selection;
        if(uriMatcher.match(uri) == 0){
            where = name + uri.getLastPathSegment();
        }
        db = databaseHelper.getWritableDatabase();
        Cursor c = db.query(TABLE_NAME, projection, where, selectionArgs, null,
                null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case uriCode:
                return "vnd.android.cursor.dir/smarthouse";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long regId = 1;
        db = databaseHelper.getWritableDatabase();
        regId = db.insert(TABLE_NAME, null, values);
        Uri newUri = CONTENT_URI.withAppendedPath(CONTENT_URI, String.valueOf(regId));
        return newUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;// Count to tell how many rows deleted
        db = databaseHelper.getWritableDatabase();
        count = db.delete(TABLE_NAME,  selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int count = 0;// Count to tell number of rows updated
        String where = selection;
        if(uriMatcher.match(uri) == 0){
            where = name + uri.getLastPathSegment();
        }
        db = databaseHelper.getWritableDatabase();
        count = db.update(TABLE_NAME, values, where, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_DB_TABLE);// Create table
        }

        // On database version upgrade create new table
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }
}