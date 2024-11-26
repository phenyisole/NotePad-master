package com.example.android.notepad;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotesList extends ListActivity {

    private static final String TAG = "NotesList";
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
            NotePad.Notes.COLUMN_NAME_NOTE
    };
    private static final int COLUMN_INDEX_TITLE = 1;

    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置新的布局文件
        setContentView(R.layout.activity_notes_list);

        // 初始化 SearchView
        searchView = (SearchView) findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // 处理搜索提交
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // 处理搜索文本变化
                performSearch(newText);
                return true;
            }

        });

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        getListView().setOnCreateContextMenuListener(this);

        Cursor cursor = managedQuery(
                getIntent().getData(),
                PROJECTION,
                null,
                null,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,NotePad.Notes.COLUMN_NAME_NOTE };
        int[] viewIDs = { android.R.id.text1, R.id.time_text,R.id.content_text };

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,
                cursor,
                dataColumns,
                viewIDs
        ) {
            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                super.bindView(view, context, cursor);

                long timestamp = cursor.getLong(cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE));
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String formattedDate = dateFormat.format(new Date(timestamp));
                TextView timeTextView = (TextView) view.findViewById(R.id.time_text);
                timeTextView.setText(formattedDate);

                String content = cursor.getString(cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE));
                if (!TextUtils.isEmpty(content)) {
                    int maxLength = 10; // 限制内容的最大长度
                    String previewContent = content.length() > maxLength ? content.substring(0, maxLength) + "..." : content;
                    TextView contentTextView = (TextView) view.findViewById(R.id.content_text);
                    contentTextView.setText(previewContent);
                }
            }
        };
        Button leftButton = findViewById(R.id.button_left);

        // 设置点击监听
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 点击后的逻辑处理
            }
        });
        Button addButton = findViewById(R.id.button_center);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
            }
        });
        Button rightButton = findViewById(R.id.button_right);
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 点击后的逻辑处理
                startActivity(new Intent(NotesList.this, CategoryList.class));
            }
        });
        Button exportButton = findViewById(R.id.export_button);
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 点击后的逻辑处理
                Button enterButton = findViewById(R.id.enter_button);
                Button cancelButton = findViewById(R.id.cancel_button);
                enterButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 获取列表视图
                        ListView listView = getListView();

                        // 遍历每个列表项
                        for (int i = 0; i < listView.getChildCount(); i++) {
                            View listItem = listView.getChildAt(i);

                            // 获取 CheckBox 和相关数据视图
                            CheckBox checkBox = listItem.findViewById(R.id.checkbox_select);
                            TextView contentTextView = listItem.findViewById(R.id.content_text);
                            TextView titleTextView = listItem.findViewById(android.R.id.text1); // 假设标题视图 ID 为 text1

                            if (checkBox != null && checkBox.isChecked()) {
                                // 获取标题和内容
                                String title = titleTextView.getText().toString().trim();
                                String content = contentTextView.getText().toString();

                                // 将勾选的项导出到单独的文件
                                writeToFile(title, content);
                                checkBox.setChecked(false);
                            }
                        }
                        enterButton.setVisibility(View.INVISIBLE);
                        cancelButton.setVisibility(View.INVISIBLE);
                        hideAllCheckboxes();
                    }
                });
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        enterButton.setVisibility(View.INVISIBLE);
                        cancelButton.setVisibility(View.INVISIBLE);
                        hideAllCheckboxes();
                    }
                });
                enterButton.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
                showAllCheckboxes();
            }
        });
        setListAdapter(adapter);
    }
    private void writeToFile(String title, String content) {
        try {
            // 替换非法字符，确保文件名合法
            String sanitizedTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_");
            String fileName = sanitizedTitle + ".txt";

            // 定义文件路径
            File file = new File(getExternalFilesDir(null), fileName);

            // 写入数据
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();

            // 提示用户导出成功
            Toast.makeText(this, "导出成功：" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导出失败：" + title, Toast.LENGTH_SHORT).show();
        }
    }
    private void showAllCheckboxes() {
        ListView listView = getListView();
        for (int i = 0; i < listView.getChildCount(); i++) {
            View listItem = listView.getChildAt(i);
            CheckBox checkBox = listItem.findViewById(R.id.checkbox_select);
            if (checkBox != null) {
                checkBox.setVisibility(View.VISIBLE);
            }
        }
    }
    private void hideAllCheckboxes() {
        ListView listView = getListView();
        for (int i = 0; i < listView.getChildCount(); i++) {
            View listItem = listView.getChildAt(i);
            CheckBox checkBox = listItem.findViewById(R.id.checkbox_select);
            if (checkBox != null) {
                checkBox.setVisibility(View.INVISIBLE);
            }
        }
    }
    private void performSearch(String query) {
        // 根据查询条件过滤数据
        Cursor cursor = managedQuery(
                NotePad.Notes.CONTENT_URI,
                PROJECTION,
                NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?",
                new String[]{"%" + query + "%"},
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
        adapter.changeCursor(cursor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            mPasteItem.setEnabled(false);
        }

        final boolean haveItems = getListAdapter().getCount() > 0;

        if (haveItems) {
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());
            Intent[] specifics = new Intent[1];
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);
            MenuItem[] items = new MenuItem[1];
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,
                    Menu.NONE,
                    Menu.NONE,
                    null,
                    specifics,
                    intent,
                    Menu.NONE,
                    items
            );

            if (items[0] != null) {
                items[0].setShortcut('1', 'e');
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        /*
         * Gets the extra info from the menu item. When an note in the Notes list is long-pressed, a
         * context menu appears. The menu items for the menu automatically get the data
         * associated with the note that was long-pressed. The data comes from the provider that
         * backs the list.
         *
         * The note's data is passed to the context menu creation routine in a ContextMenuInfo
         * object.
         *
         * When one of the context menu items is clicked, the same data is passed, along with the
         * note ID, to onContextItemSelected() via the item parameter.
         */
        try {
            // Casts the data object in the item into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // If the object can't be cast, logs an error
            Log.e(TAG, "bad menuInfo", e);

            // Triggers default processing of the menu item.
            return false;
        }
        // Appends the selected note's ID to the URI sent with the incoming Intent.
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         * Gets the menu item's ID and compares it to known actions.
         */
        switch (item.getItemId()) {
            case R.id.context_open:
                // Launch activity to view/edit the currently selected item
                startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
                return true;
//BEGIN_INCLUDE(copy)
//            case R.id.context_copy:
//                // Gets a handle to the clipboard service.
//                ClipboardManager clipboard = (ClipboardManager)
//                        getSystemService(Context.CLIPBOARD_SERVICE);
//
//                // Copies the notes URI to the clipboard. In effect, this copies the note itself
//                clipboard.setPrimaryClip(ClipData.newUri(   // new clipboard item holding a URI
//                        getContentResolver(),               // resolver to retrieve URI info
//                        "Note",                             // label for the clip
//                        noteUri)                            // the URI
//                );
//
//                // Returns to the caller and skips further processing.
//                return true;
//END_INCLUDE(copy)
            case R.id.context_delete:

                // Deletes the note from the provider by passing in a URI in note ID format.
                // Please see the introductory note about performing provider operations on the
                // UI thread.
                getContentResolver().delete(
                        noteUri,  // The URI of the provider
                        null,     // No where clause is needed, since only a single note ID is being
                        // passed in.
                        null      // No where clause is used, so no where arguments are needed.
                );

                // Returns to the caller and skips further processing.
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                /*
                 * Launches a new Activity using an Intent. The intent filter for the Activity
                 * has to have action ACTION_INSERT. No category is set, so DEFAULT is assumed.
                 * In effect, this starts the NoteEditor Activity in NotePad.
                 */
                startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
                return true;
            case R.id.menu_paste:
                /*
                 * Launches a new Activity using an Intent. The intent filter for the Activity
                 * has to have action ACTION_PASTE. No category is set, so DEFAULT is assumed.
                 * In effect, this starts the NoteEditor Activity in NotePad.
                 */
                startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
                return true;
            case R.id.menu_category:
                startActivity(new Intent(this, CategoryList.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        // Tries to get the position of the item in the ListView that was long-pressed.
        try {
            // Casts the incoming data object into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // If the menu object can't be cast, logs an error.
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        /*
         * Gets the data associated with the item at the selected position. getItem() returns
         * whatever the backing adapter of the ListView has associated with the item. In NotesList,
         * the adapter associated all of the data for a note with its list item. As a result,
         * getItem() returns that data as a Cursor.
         */
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        // If the cursor is empty, then for some reason the adapter can't get the data from the
        // provider, so returns null to the caller.
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // Sets the menu header to be the title of the selected note.
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Append to the
        // menu items for any other activities that can do stuff with it
        // as well.  This does a query on the system for any activities that
        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
        // for each one that is found.
        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id) ));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // Constructs a new URI from the incoming URI and the row ID
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
            // Intent's data is the note ID URI. The effect is to call NoteEdit.
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}
