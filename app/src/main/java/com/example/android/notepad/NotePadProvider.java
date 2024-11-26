/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.ContentProvider.PipeDataWriter;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class NotePadProvider extends ContentProvider implements PipeDataWriter<Cursor> {
    // Used for debugging and logging
    private static final String TAG = "NotePadProvider";

    /**
     * The database that the provider uses as its underlying data store
     */
    private static final String DATABASE_NAME = "note_pad.db";

    /**
     * The database version
     */
    private static final int DATABASE_VERSION = 5;

    /**
     * A projection map used to select columns from the database
     */
    private static HashMap<String, String> sNotesProjectionMap;

    /**
     * A projection map used to select columns from the database
     */
    private static HashMap<String, String> sLiveFolderProjectionMap;
    private static HashMap<String, String> sCategoryProjectionMap;

    static {
        // 创建 CategoryInfo 的映射表
        sCategoryProjectionMap = new HashMap<>();
        sCategoryProjectionMap.put(NotePad.CategoryInfo._ID, NotePad.CategoryInfo._ID);
        sCategoryProjectionMap.put(NotePad.CategoryInfo.COLUMN_NAME_CATEGORY, NotePad.CategoryInfo.COLUMN_NAME_CATEGORY);
    }

    /**
     * Standard projection for the interesting columns of a normal note.
     */
    private static final String[] READ_NOTE_PROJECTION = new String[] {
            NotePad.Notes._ID,               // Projection position 0, the note's id
            NotePad.Notes.COLUMN_NAME_NOTE,  // Projection position 1, the note's content
            NotePad.Notes.COLUMN_NAME_TITLE, // Projection position 2, the note's title
//            NotePad.Notes.COLUMN_NAME_TIME,
    };
    private static final int READ_NOTE_NOTE_INDEX = 1;
    private static final int READ_NOTE_TITLE_INDEX = 2;
//    private static final int READ_NOTE_TIME_INDEX = 3;


    /*
     * Constants used by the Uri matcher to choose an action based on the pattern
     * of the incoming URI
     */
    // The incoming URI matches the Notes URI pattern
    private static final int NOTES = 1;

    // The incoming URI matches the Note ID URI pattern
    private static final int NOTE_ID = 2;

    // The incoming URI matches the Live Folder URI pattern
    private static final int LIVE_FOLDER_NOTES = 3;
    private static final int CATEGORY_INFO = 4;
    /**
     * A UriMatcher instance
     */
    private static final UriMatcher sUriMatcher;

    // Handle to a new DatabaseHelper.
    private DatabaseHelper mOpenHelper;



    /**
     * A block that instantiates and sets static objects
     */
    static {

        /*
         * Creates and initializes the URI matcher
         */
        // Create a new instance
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // Add a pattern that routes URIs terminated with "notes" to a NOTES operation
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);

        // Add a pattern that routes URIs terminated with "notes" plus an integer
        // to a note ID operation
        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);

        // Add a pattern that routes URIs terminated with live_folders/notes to a
        // live folder operation
        sUriMatcher.addURI(NotePad.AUTHORITY, "live_folders/notes", LIVE_FOLDER_NOTES);
        sUriMatcher.addURI(NotePad.AUTHORITY, "categoryinfo", CATEGORY_INFO);
        /*
         * Creates and initializes a projection map that returns all columns
         */

        // Creates a new projection map instance. The map returns a column name
        // given a string. The two are usually equal.
        sNotesProjectionMap = new HashMap<String, String>();

        // Maps the string "_ID" to the column name "_ID"
        sNotesProjectionMap.put(NotePad.Notes._ID, NotePad.Notes._ID);

        // Maps "title" to "title"
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_TITLE);
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_CATEGORY, NotePad.Notes.COLUMN_NAME_CATEGORY);
        // Maps "note" to "note"
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_NOTE);

//        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_TIME, NotePad.Notes.COLUMN_NAME_TIME);
        // Maps "created" to "created"
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE,
                NotePad.Notes.COLUMN_NAME_CREATE_DATE);

        // Maps "modified" to "modified"
        sNotesProjectionMap.put(
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);

        /*
         * Creates an initializes a projection map for handling Live Folders
         */
// Maps "background_image" to "background_image"
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_BACKGROUND_IMAGE, NotePad.Notes.COLUMN_NAME_BACKGROUND_IMAGE);

// Maps "font_color" to "font_color"
        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_FONT_COLOR, NotePad.Notes.COLUMN_NAME_FONT_COLOR);

        // Creates a new projection map instance
        sLiveFolderProjectionMap = new HashMap<String, String>();

        // Maps "_ID" to "_ID AS _ID" for a live folder
        sLiveFolderProjectionMap.put(LiveFolders._ID, NotePad.Notes._ID + " AS " + LiveFolders._ID);

        // Maps "NAME" to "title AS NAME"
        sLiveFolderProjectionMap.put(LiveFolders.NAME, NotePad.Notes.COLUMN_NAME_TITLE + " AS " +
            LiveFolders.NAME);
    }

    /**
    *
    * This class helps open, create, and upgrade the database file. Set to package visibility
    * for testing purposes.
    */
   static class DatabaseHelper extends SQLiteOpenHelper {

       DatabaseHelper(Context context) {

           // calls the super constructor, requesting the default cursor factory.
           super(context, DATABASE_NAME, null, DATABASE_VERSION);
       }

       /**
        *
        * Creates the underlying database with table name and column names taken from the
        * NotePad class.
        */
       public void onCreate(SQLiteDatabase db) {
           String CREATE_TABLE_NOTES = "CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
                   + NotePad.Notes._ID + " INTEGER PRIMARY KEY, "
                   + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT, "
                   + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT, "
                   + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER, "
                   + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER, "
                   + NotePad.Notes.COLUMN_NAME_BACKGROUND_IMAGE + " TEXT, "
                   + NotePad.Notes.COLUMN_NAME_FONT_COLOR + " TEXT, "
                   + NotePad.Notes.COLUMN_NAME_CATEGORY + " INTEGER"
                   + ");";

           db.execSQL(CREATE_TABLE_NOTES);

           String CREATE_TABLE_CATEGORY_INFO = "CREATE TABLE " + NotePad.CategoryInfo.TABLE_NAME + " ("
                   + NotePad.CategoryInfo._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                   + NotePad.CategoryInfo.COLUMN_NAME_CATEGORY + " TEXT NOT NULL"
                   + ");";
           db.execSQL(CREATE_TABLE_CATEGORY_INFO);
       }


        /**
        *
        * Demonstrates that the provider must consider what happens when the
        * underlying datastore is changed. In this sample, the database is upgraded the database
        * by destroying the existing data.
        * A real application should upgrade the database in place.
        */
       @Override
       public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

           // Logs that the database is being upgraded
           Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                   + newVersion + ", which will destroy all old data");

           // Kills the table and existing data
           db.execSQL("DROP TABLE IF EXISTS notes");
           db.execSQL("DROP TABLE IF EXISTS " + NotePad.CategoryInfo.TABLE_NAME);
           // Recreates the database with a new version
           onCreate(db);
       }
   }

   /**
    *
    * Initializes the provider by creating a new DatabaseHelper. onCreate() is called
    * automatically when Android creates the provider in response to a resolver request from a
    * client.
    */
   @Override
   public boolean onCreate() {

       // Creates a new helper object. Note that the database itself isn't opened until
       // something tries to access it, and it's only created if it doesn't already exist.
       mOpenHelper = new DatabaseHelper(getContext());

       // Assumes that any failures will be reported by a thrown exception.
       return true;
   }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        // Constructs a new query builder and sets its table name
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        // 根据 URI 模式选择不同的表和字段
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                qb.setTables(NotePad.Notes.TABLE_NAME);
                qb.setProjectionMap(sNotesProjectionMap);
                break;

            case NOTE_ID:
                qb.setTables(NotePad.Notes.TABLE_NAME);
                qb.setProjectionMap(sNotesProjectionMap);
                qb.appendWhere(
                        NotePad.Notes._ID + "=" +
                                uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION));
                break;

            case LIVE_FOLDER_NOTES:
                qb.setProjectionMap(sLiveFolderProjectionMap);
                break;

            case CATEGORY_INFO:
                qb.setTables(NotePad.CategoryInfo.TABLE_NAME);
                qb.setProjectionMap(sCategoryProjectionMap); // 设置 CategoryInfo 的映射表

                // 如果 sortOrder 未指定或无效，提供默认排序
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = NotePad.CategoryInfo._ID + " ASC"; // 默认按 ID 升序排列
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // Open the database in "read" mode
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        // Perform the query and return a cursor
        Cursor c = qb.query(
                db,            // The database to query
                projection,    // The columns to return from the query
                selection,     // The columns for the WHERE clause
                selectionArgs, // The values for the WHERE clause
                null,          // Don't group the rows
                null,          // Don't filter by row groups
                sortOrder      // The sort order
        );

        // Notify the content resolver if the data has changed
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }



    /**
    * This is called when a client calls {@link android.content.ContentResolver#getType(Uri)}.
    * Returns the MIME data type of the URI given as a parameter.
    *
    * @param uri The URI whose MIME type is desired.
    * @return The MIME type of the URI.
    * @throws IllegalArgumentException if the incoming URI pattern is invalid.
    */
   @Override
   public String getType(Uri uri) {

       /**
        * Chooses the MIME type based on the incoming URI pattern
        */
       switch (sUriMatcher.match(uri)) {

           // If the pattern is for notes or live folders, returns the general content type.
           case NOTES:
           case LIVE_FOLDER_NOTES:
               return NotePad.Notes.CONTENT_TYPE;

           // If the pattern is for note IDs, returns the note ID content type.
           case NOTE_ID:
               return NotePad.Notes.CONTENT_ITEM_TYPE;

           // If the URI pattern doesn't match any permitted patterns, throws an exception.
           default:
               throw new IllegalArgumentException("Unknown URI " + uri);
       }
    }

//BEGIN_INCLUDE(stream)
    /**
     * This describes the MIME types that are supported for opening a note
     * URI as a stream.
     */
    static ClipDescription NOTE_STREAM_TYPES = new ClipDescription(null,
            new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN });

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        /**
         *  Chooses the data stream type based on the incoming URI pattern.
         */
        switch (sUriMatcher.match(uri)) {

            // If the pattern is for notes or live folders, return null. Data streams are not
            // supported for this type of URI.
            case NOTES:
            case LIVE_FOLDER_NOTES:
                return null;

            // If the pattern is for note IDs and the MIME filter is text/plain, then return
            // text/plain
            case NOTE_ID:
                return NOTE_STREAM_TYPES.filterMimeTypes(mimeTypeFilter);

                // If the URI pattern doesn't match any permitted patterns, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
            }
    }



    @Override
    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts)
            throws FileNotFoundException {

        // Checks to see if the MIME type filter matches a supported MIME type.
        String[] mimeTypes = getStreamTypes(uri, mimeTypeFilter);

        // If the MIME type is supported
        if (mimeTypes != null) {

            // Retrieves the note for this URI. Uses the query method defined for this provider,
            // rather than using the database query method.
            Cursor c = query(
                    uri,                    // The URI of a note
                    READ_NOTE_PROJECTION,   // Gets a projection containing the note's ID, title,
                                            // and contents
                    null,                   // No WHERE clause, get all matching records
                    null,                   // Since there is no WHERE clause, no selection criteria
                    null                    // Use the default sort order (modification date,
                                            // descending
            );


            // If the query fails or the cursor is empty, stop
            if (c == null || !c.moveToFirst()) {

                // If the cursor is empty, simply close the cursor and return
                if (c != null) {
                    c.close();
                }

                // If the cursor is null, throw an exception
                throw new FileNotFoundException("Unable to query " + uri);
            }

            // Start a new thread that pipes the stream data back to the caller.
            return new AssetFileDescriptor(
                    openPipeHelper(uri, mimeTypes[0], opts, c, this), 0,
                    AssetFileDescriptor.UNKNOWN_LENGTH);
        }

        // If the MIME type is not supported, return a read-only handle to the file.
        return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType,
            Bundle opts, Cursor c) {
        // We currently only support conversion-to-text from a single note entry,
        // so no need for cursor data type checking here.
        FileOutputStream fout = new FileOutputStream(output.getFileDescriptor());
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new OutputStreamWriter(fout, "UTF-8"));
            pw.println(c.getString(READ_NOTE_TITLE_INDEX));
            pw.println("");
            pw.println(c.getString(READ_NOTE_NOTE_INDEX));
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Ooops", e);
        } finally {
            c.close();
            if (pw != null) {
                pw.flush();
            }
            try {
                fout.close();
            } catch (IOException e) {
            }
        }
    }
//END_INCLUDE(stream)

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId; // 插入后的行 ID
        Uri resultUri; // 返回的 URI

        // 根据 URI 进行匹配
        switch (sUriMatcher.match(uri)) {
            case NOTES:
                // 处理 NOTES 表的插入
                ContentValues values;
                if (initialValues != null) {
                    values = new ContentValues(initialValues);
                } else {
                    values = new ContentValues();
                }

                // 如果插入的数据缺少某些列，填充默认值
                Long now = System.currentTimeMillis();
                if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE)) {
                    values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now);
                }
                if (!values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
                    values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
                }
                if (!values.containsKey(NotePad.Notes.COLUMN_NAME_TITLE)) {
                    Resources r = Resources.getSystem();
                    values.put(NotePad.Notes.COLUMN_NAME_TITLE, r.getString(android.R.string.untitled));
                }
                if (!values.containsKey(NotePad.Notes.COLUMN_NAME_NOTE)) {
                    values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");
                }
                if (!values.containsKey(NotePad.Notes.COLUMN_NAME_BACKGROUND_IMAGE)) {
                    values.put(NotePad.Notes.COLUMN_NAME_BACKGROUND_IMAGE, "@android:color/transparent");
                }
                if (!values.containsKey(NotePad.Notes.COLUMN_NAME_FONT_COLOR)) {
                    values.put(NotePad.Notes.COLUMN_NAME_FONT_COLOR, "#000000");
                }
                if(!values.containsKey(NotePad.Notes.COLUMN_NAME_CATEGORY)){
                    values.put(NotePad.Notes.COLUMN_NAME_CATEGORY, 0);
                }
                // 插入数据到 NOTES 表
                rowId = db.insert(NotePad.Notes.TABLE_NAME, null, values);
                if (rowId > 0) {
                    resultUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, rowId);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;

            case CATEGORY_INFO: // 新增 CATEGORY_INFO 的插入逻辑
                // 直接使用 initialValues，不需要填充默认值
                rowId = db.insert(NotePad.CategoryInfo.TABLE_NAME, null, initialValues);
                if (rowId > 0) {
                    resultUri = ContentUris.withAppendedId(Uri.parse("content://" + NotePad.AUTHORITY + "/categoryinfo"), rowId);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // 通知观察者数据已更改
        getContext().getContentResolver().notifyChange(resultUri, null);
        return resultUri;
    }



    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalWhere;
        int count;

        // Does the delete based on the incoming URI pattern.
        switch (sUriMatcher.match(uri)) {

            // If the incoming pattern matches the general pattern for notes, does a delete
            // based on the incoming "where" columns and arguments.
            case NOTES:
                count = db.delete(
                        NotePad.Notes.TABLE_NAME,  // The database table name
                        where,                     // The incoming where clause column names
                        whereArgs                  // The incoming where clause values
                );
                break;

            // If the incoming URI matches a single note ID, does the delete based on the
            // incoming data, but modifies the where clause to restrict it to the
            // particular note ID.
            case NOTE_ID:
                /*
                 * Starts a final WHERE clause by restricting it to the
                 * desired note ID.
                 */
                finalWhere =
                        NotePad.Notes._ID +                              // The ID column name
                                " = " +                                          // test for equality
                                uri.getPathSegments().                           // the incoming note ID
                                        get(NotePad.Notes.NOTE_ID_PATH_POSITION)
                ;

                // If there were additional selection criteria, append them to the final
                // WHERE clause
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                // Performs the delete.
                count = db.delete(
                        NotePad.Notes.TABLE_NAME,  // The database table name.
                        finalWhere,                // The final WHERE clause
                        whereArgs                  // The incoming where clause values.
                );
                break;

            // 添加 CATEGORY_INFO 的删除逻辑
            case CATEGORY_INFO:
                count = db.delete(
                        NotePad.CategoryInfo.TABLE_NAME,  // The database table name
                        where,                            // The incoming where clause column names
                        whereArgs                         // The incoming where clause values
                );
                break;

            // If the URI pattern is invalid, throws an exception.
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        /* Gets a handle to the content resolver object for the current context, and notifies it
         * that the incoming URI changed. The object passes this along to the resolver framework,
         * and observers that have registered themselves for the provider are notified.
         */
        getContext().getContentResolver().notifyChange(uri, null);

        // Returns the number of rows deleted.
        return count;
    }



    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {

        // Opens the database object in "write" mode.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;

        // 根据 URI 模式执行不同的更新操作
        switch (sUriMatcher.match(uri)) {

            // NOTES 表的更新逻辑
            case NOTES:
                count = db.update(
                        NotePad.Notes.TABLE_NAME, // 数据库表名
                        values,                   // 列名和值的映射
                        where,                    // WHERE 子句的列
                        whereArgs                 // WHERE 子句的值
                );
                break;

            // 针对特定 NOTE_ID 的更新逻辑
            case NOTE_ID:
                // 获取 URI 中的 Note ID
                String noteId = uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION);

                // 构建针对特定 ID 的 WHERE 子句
                finalWhere = NotePad.Notes._ID + " = " + noteId;

                // 如果 WHERE 子句非空，则拼接附加条件
                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                count = db.update(
                        NotePad.Notes.TABLE_NAME, // 数据库表名
                        values,                   // 列名和值的映射
                        finalWhere,               // 最终 WHERE 子句
                        whereArgs                 // WHERE 子句的值
                );
                break;

            // CATEGORY_INFO 表的更新逻辑
            case CATEGORY_INFO:
                count = db.update(
                        NotePad.CategoryInfo.TABLE_NAME, // 数据库表名
                        values,                          // 列名和值的映射
                        where,                           // WHERE 子句的列
                        whereArgs                        // WHERE 子句的值
                );
                break;

            // 如果 URI 不匹配任何已定义模式，则抛出异常
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // 通知观察者数据已更改
        getContext().getContentResolver().notifyChange(uri, null);

        // 返回更新的行数
        return count;
    }



    DatabaseHelper getOpenHelperForTest() {
        return mOpenHelper;
    }
}
