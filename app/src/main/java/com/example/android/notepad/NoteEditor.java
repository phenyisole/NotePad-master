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
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NoteEditor extends Activity {
    // For logging and debugging purposes
    private static final String TAG = "NoteEditor";

    /*
     * Creates a projection that returns the note ID and the note contents.
     */
    private static final String[] PROJECTION =
        new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_BACKGROUND_IMAGE,
            NotePad.Notes.COLUMN_NAME_FONT_COLOR,
    };

    // A label for the saved state of the activity
    private static final String ORIGINAL_CONTENT = "origContent";

    // This Activity can be started by more than one action. Each action is represented
    // as a "state" constant
    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;

    // Global mutable variables
    private int mState;
    private Uri mUri;
    private Cursor mCursor;
    private EditText mText;
    private String mOriginalContent;

    /**
     * Defines a custom EditText View that draws lines between each line of text that is displayed.
     */
    public static class LinedEditText extends EditText {
        private Rect mRect;
        private Paint mPaint;

        // This constructor is used by LayoutInflater
        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);

            // Creates a Rect and a Paint object, and sets the style and color of the Paint object.
            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }

        /**
         * This is called to draw the LinedEditText object
         * @param canvas The canvas on which the background is drawn.
         */
        @Override
        protected void onDraw(Canvas canvas) {

            // Gets the number of lines of text in the View.
            int count = getLineCount();

            // Gets the global Rect and Paint objects
            Rect r = mRect;
            Paint paint = mPaint;

            /*
             * Draws one line in the rectangle for every line of text in the EditText
             */
            for (int i = 0; i < count; i++) {

                // Gets the baseline coordinates for the current line of text
                int baseline = getLineBounds(i, r);

                /*
                 * Draws a line in the background from the left of the rectangle to the right,
                 * at a vertical position one dip below the baseline, using the "paint" object
                 * for details.
                 */
                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }

            // Finishes up by calling the parent method
            super.onDraw(canvas);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        /*
         * Creates an Intent to use when the Activity object's result is sent back to the
         * caller.
         */
        final Intent intent = getIntent();

        /*
         *  Sets up for the edit, based on the action specified for the incoming Intent.
         */

        // Gets the action that triggered the intent filter for this Activity
        final String action = intent.getAction();

        // For an edit action:
        if (Intent.ACTION_EDIT.equals(action)) {

            // Sets the Activity state to EDIT, and gets the URI for the data to be edited.
            mState = STATE_EDIT;
            mUri = intent.getData();

            // For an insert or paste action:
        } else if (Intent.ACTION_INSERT.equals(action)
                || Intent.ACTION_PASTE.equals(action)) {

            // Sets the Activity state to INSERT, gets the general note URI, and inserts an
            // empty record in the provider
            mState = STATE_INSERT;
            mUri = getContentResolver().insert(intent.getData(), null);

            /*
             * If the attempt to insert the new note fails, shuts down this Activity. The
             * originating Activity receives back RESULT_CANCELED if it requested a result.
             * Logs that the insert failed.
             */
            if (mUri == null) {

                // Writes the log identifier, a message, and the URI that failed.
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());

                // Closes the activity.
                finish();
                return;
            }

            // Since the new entry was created, this sets the result to be returned
            // set the result to be returned.
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

        // If the action was other than EDIT or INSERT:
        } else {

            // Logs an error that the action was not understood, finishes the Activity, and
            // returns RESULT_CANCELED to an originating Activity.
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        mCursor = managedQuery(
            mUri,         // The URI that gets multiple notes from the provider.
            PROJECTION,   // A projection that returns the note ID and note content for each note.
            null,         // No "where" clause selection criteria.
            null,         // No "where" clause selection values.
            null          // Use the default sort order (modification date, descending)
        );

        // For a paste, initializes the data from clipboard.
        // (Must be done after mCursor is initialized.)
        if (Intent.ACTION_PASTE.equals(action)) {
            // Does the paste
            performPaste();
            // Switches the state to EDIT so the title can be modified.
            mState = STATE_EDIT;
        }

        // Sets the layout for this Activity. See res/layout/note_editor.xml
        setContentView(R.layout.note_editor);

        // Gets a handle to the EditText in the the layout.
        mText = (EditText) findViewById(R.id.note);
        EditText mTitle = (EditText) findViewById(R.id.note_title);

// If the cursor is not null and it contains data, set the title and content
        if (mCursor != null && mCursor.moveToFirst()) {
            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
            String noteContent = mCursor.getString(colNoteIndex);
            String noteTitle = mCursor.getString(colTitleIndex);

            // Set the content and title in the respective EditText components
            mText.setText(noteContent);
            mTitle.setText(noteTitle); // 设置标题内容
        }

        /*
         * If this Activity had stopped previously, its state was written the ORIGINAL_CONTENT
         * location in the saved Instance state. This gets the state.
         */
        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        /*
         * mCursor is initialized, since onCreate() always precedes onResume for any running
         * process. This tests that it's not null, since it should always contain data.
         */
        if (mCursor != null) {
            // Requery in case something changed while paused (such as the title)
            mCursor.requery();

            mCursor.moveToFirst();

            // Modifies the window title for the Activity according to the current Activity state.
            if (mState == STATE_EDIT) {
                // Set the title of the Activity to include the note title
                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                Resources res = getResources();
                String text = String.format(title);
                setTitle("编辑");
            // Sets the title to "create" for inserts
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }

            /*
             * onResume() may have been called after the Activity lost focus (was paused).
             * The user was either editing or creating a note when the Activity paused.
             * The Activity should re-display the text that had been retrieved previously, but
             * it should not move the cursor. This helps the user to continue editing or entering.
             */

            // Gets the note text from the Cursor and puts it in the TextView, but doesn't change
            // the text cursor's position.
            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            String note = mCursor.getString(colNoteIndex);
            mText.setTextKeepState(note);

            int colFontColorIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_FONT_COLOR);
            String fontColorHex = mCursor.getString(colFontColorIndex);
            if (fontColorHex != null) {
                try {
                    int fontColor = Color.parseColor(fontColorHex);
                    mText.setTextColor(fontColor);
                } catch (IllegalArgumentException e) {
                    // 如果颜色格式不正确，则使用默认颜色
                    mText.setTextColor(Color.BLACK);
                }
            }
            int colBackgroundIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_BACKGROUND_IMAGE);
            String backgroundResIdStr = mCursor.getString(colBackgroundIndex);
            if (backgroundResIdStr != null) {
                try {
                    int backgroundResId = Integer.parseInt(backgroundResIdStr);

                    LinearLayout mainLayout = findViewById(R.id.main_layout);
                    mainLayout.setBackgroundResource(backgroundResId);

                } catch (NumberFormatException e) {
                    // 资源ID格式不正确，使用默认背景
//                    mText.setBackgroundResource(android.R.drawable.editbox_background_normal);
                }
            }
            // Stores the original note text, to allow the user to revert changes.
            if (mOriginalContent == null) {
                mOriginalContent = note;
            }

        /*
         * Something is wrong. The Cursor should always contain data. Report an error in the
         * note.
         */
        } else {
            setTitle(getText(R.string.error_title));
            mText.setText(getText(R.string.error_message));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (mCursor != null) {
            // Get the current note text
            String text = mText.getText().toString();
            String title = ((EditText) findViewById(R.id.note_title)).getText().toString(); // 获取标题内容
            int length = text.length();

            if (isFinishing() && (length == 0)) {
                setResult(RESULT_CANCELED);
                deleteNote();

            } else if (mState == STATE_EDIT) {
                // Updates the note text and title
                updateNote(text, title);
            } else if (mState == STATE_INSERT) {
                // Updates the note text and title
                updateNote(text, title);
                mState = STATE_EDIT;
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_options_menu, menu);

        // Only add extra menu items for a saved note 
        if (mState == STATE_EDIT) {

            Intent intent = new Intent(null, mUri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                    new ComponentName(this, NoteEditor.class), null, intent, 0, null);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Check if note has changed and enable/disable the revert option
        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
        String savedNote = mCursor.getString(colNoteIndex);
        String currentNote = mText.getText().toString();
        if (savedNote.equals(currentNote)) {
            menu.findItem(R.id.menu_revert).setVisible(false);
        } else {
            menu.findItem(R.id.menu_revert).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.
        switch (item.getItemId()) {
        case R.id.menu_save:
            String text = mText.getText().toString();
            String title = ((EditText) findViewById(R.id.note_title)).getText().toString(); // 获取标题内容
            updateNote(text, title); // 保存标题和内容
            finish();
            break;

        case R.id.menu_delete:
            deleteNote();
            finish();
            break;
        case R.id.menu_revert:
            cancelNote();
            break;
        case R.id.menu_edit_font_color:
            showFontPickerDialog();
            break;
        case R.id.menu_edit_background:
            changeBackground();
            break;
        case R.id.cancel:
            cancelNote();
            break;
        case R.id.menu_edit_category:
            editCategory();
            break;
    }
        return super.onOptionsItemSelected(item);
    }

    private void editCategory() {
        // 查询所有的分类
        Cursor cursor = getContentResolver().query(
                Uri.parse("content://com.google.provider.NotePad/categoryinfo"), // CategoryInfo 表的 URI
                new String[]{"category"}, // 查询的列
                null, null, null // 无筛选条件
        );

        if (cursor != null && cursor.getCount() > 0) {
            // 创建一个列表存储所有分类
            List<String> categories = new ArrayList<>();
            while (cursor.moveToNext()) {
                categories.add(cursor.getString(cursor.getColumnIndex("category")));
            }

            // 使用 AlertDialog 显示所有分类
            new AlertDialog.Builder(this)
                    .setTitle("选择分类")
                    .setItems(categories.toArray(new String[0]), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 获取选择的分类
                            String selectedCategory = categories.get(which);

                            // 更新当前笔记的分类
                            updateCategory(selectedCategory);
                        }
                    })
                    .show();
        } else {
            // 如果没有分类，提示用户
            new AlertDialog.Builder(this)
                    .setMessage("没有可用的分类，请先添加分类。")
                    .setPositiveButton("OK", null)
                    .show();
        }

        // 关闭 Cursor
        if (cursor != null) {
            cursor.close();
        }
    }

    private void updateCategory(String selectedCategory) {
        // 获取当前笔记的 URI（在 onCreate 中已经通过 mUri 获取）
        ContentValues values = new ContentValues();
        values.put("category", selectedCategory); // 更新 category 字段

        // 更新数据库中的分类
        int rowsUpdated = getContentResolver().update(mUri, values, null, null);

        if (rowsUpdated > 0) {
            // 更新成功，显示消息
            Toast.makeText(this, "分类更新成功", Toast.LENGTH_SHORT).show();
        } else {
            // 更新失败，显示错误消息
            Toast.makeText(this, "分类更新失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void changeBackground() {
        // 定义可选的背景图片资源
        final int[] backgrounds = {
                R.drawable.background_1,
                R.drawable.background_2,
                R.drawable.background_3,
                R.drawable.background_4,
                R.drawable.background_5,
                R.drawable.ycb
        };

        // 创建 HorizontalScrollView 和 LinearLayout
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setPadding(16, 16, 16, 16); // 添加内边距

        // 为每个背景图片创建 ImageView 并添加到 LinearLayout
        for (int background : backgrounds) {
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(background);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(200, 200);
            layoutParams.setMargins(0, 0, 16, 0); // 在每个图片之间添加 16 像素的间距
            imageView.setLayoutParams(layoutParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int selectedBackground = (int) v.getTag();

                    // 设置背景图片到 EditText
                    LinearLayout mainLayout = findViewById(R.id.main_layout);
                    mainLayout.setBackgroundResource(selectedBackground);


                    // 将选择的背景图片信息同步到数据库
                    updateBackground(selectedBackground);
                }
            });
            imageView.setTag(background);
            linearLayout.addView(imageView);
        }

        horizontalScrollView.addView(linearLayout);

        // 创建一个对话框，让用户选择背景图片
        new AlertDialog.Builder(this)
                .setTitle("选择背景图片")
                .setView(horizontalScrollView)
                .setNegativeButton("ok", null)
                .show();
    }


    private void updateBackground(int backgroundResId) {
        // 创建 ContentValues 对象以存储要更新的值
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_BACKGROUND_IMAGE, String.valueOf(backgroundResId));

        // 更新数据库中对应的记录
        getContentResolver().update(
                mUri,         // 要更新的 URI
                values,       // 要更新的值
                null,         // 不需要选择条件
                null          // 不需要选择参数
        );
    }

    // 自定义适配器用于显示背景图片
    private static class BackgroundAdapter extends BaseAdapter {
        private Context context;
        private int[] backgrounds;

        public BackgroundAdapter(Context context, int[] backgrounds) {
            this.context = context;
            this.backgrounds = backgrounds;
        }

        @Override
        public int getCount() {
            return backgrounds.length;
        }

        @Override
        public Object getItem(int position) {
            return backgrounds[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(context);
                imageView.setLayoutParams(new GridView.LayoutParams(200, 200));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imageView = (ImageView) convertView;
            }
            imageView.setImageResource(backgrounds[position]);
            return imageView;
        }
    }
    private void showFontPickerDialog() {
        // 创建颜色列表
        List<ColorItem> colorItems = Arrays.asList(
                new ColorItem(Color.BLACK, "黑色"),
                new ColorItem(Color.RED, "红色"),
                new ColorItem(Color.GREEN, "绿色"),
                new ColorItem(Color.BLUE, "蓝色"),
                new ColorItem(Color.YELLOW, "黄色"),
                new ColorItem(Color.MAGENTA, "洋红"),
                new ColorItem(Color.CYAN, "青色"),
                new ColorItem(Color.GRAY, "灰色")
        );

        // 创建适配器
        ColorAdapter adapter = new ColorAdapter(this, colorItems);

        // 创建对话框
        new AlertDialog.Builder(this)
                .setTitle("选择字体颜色")
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 获取用户选择的颜色
                        int selectedColor = colorItems.get(which).getColor();
                        // 更新 EditText 的字体颜色
                        mText.setTextColor(selectedColor);
                        // 将选择的颜色同步到数据库
                        updateFontColor(selectedColor);
                    }
                })
                .show();
    }


    private void updateFontColor(int color) {
        // 创建 ContentValues 对象以存储要更新的值
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_FONT_COLOR, String.format("#%06X", (0xFFFFFF & color))); // 将颜色转换为字符串

//         更新数据库中对应的记录
        getContentResolver().update(
                mUri,         // 要更新的 URI
                values,       // 要更新的值
                null,         // 不需要选择条件
                null          // 不需要选择参数
        );
    }
    private class ColorAdapter extends ArrayAdapter<ColorItem> {
        private Context context;
        private List<ColorItem> colorItems;

        public ColorAdapter(Context context, List<ColorItem> colorItems) {
            super(context, 0, colorItems);
            this.context = context;
            this.colorItems = colorItems;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.color_item, parent, false);
            }

            ColorItem colorItem = colorItems.get(position);

            View colorPreview = convertView.findViewById(R.id.color_preview);
            TextView colorName = convertView.findViewById(R.id.color_name);

            colorPreview.setBackgroundColor(colorItem.getColor());
            colorName.setText(colorItem.getName());

            return convertView;
        }
    }

    private static class ColorItem {
        private int color;
        private String name;

        public ColorItem(int color, String name) {
            this.color = color;
            this.name = name;
        }

        public int getColor() {
            return color;
        }

        public String getName() {
            return name;
        }
    }
//BEGIN_INCLUDE(paste)
    /**
     * A helper method that replaces the note's data with the contents of the clipboard.
     */
    private final void performPaste() {

        // Gets a handle to the Clipboard Manager
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        // Gets a content resolver instance
        ContentResolver cr = getContentResolver();

        // Gets the clipboard data from the clipboard
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {

            String text=null;
            String title=null;

            // Gets the first item from the clipboard data
            ClipData.Item item = clip.getItemAt(0);

            // Tries to get the item's contents as a URI pointing to a note
            Uri uri = item.getUri();

            if (uri != null && NotePad.Notes.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {

                // The clipboard holds a reference to data with a note MIME type. This copies it.
                Cursor orig = cr.query(
                        uri,            // URI for the content provider
                        PROJECTION,     // Get the columns referred to in the projection
                        null,           // No selection variables
                        null,           // No selection variables, so no criteria are needed
                        null            // Use the default sort order
                );

                // If the Cursor is not null, and it contains at least one record
                // (moveToFirst() returns true), then this gets the note data from it.
                if (orig != null) {
                    if (orig.moveToFirst()) {
                        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
                        int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                        text = orig.getString(colNoteIndex);
                        title = orig.getString(colTitleIndex);
                    }

                    // Closes the cursor.
                    orig.close();
                }
            }

            // If the contents of the clipboard wasn't a reference to a note, then
            // this converts whatever it is to text.
            if (text == null) {
                text = item.coerceToText(this).toString();
            }

            // Updates the current note with the retrieved title and text.
            updateNote(text, title);
        }
    }
//END_INCLUDE(paste)

    private final void updateNote(String text, String title) {

        // Sets up a map to contain values to be updated in the provider.
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

        // If the action is to insert a new note, this creates an initial title for it.
        if (mState == STATE_INSERT) {

            // If no title was provided as an argument, create one from the note text.
            if (title == null) {
  
                // Get the note's length
                int length = text.length();

                // Sets the title by getting a substring of the text that is 31 characters long
                // or the number of characters in the note plus one, whichever is smaller.
                title = text.substring(0, Math.min(30, length));
  
                // If the resulting length is more than 30 characters, chops off any
                // trailing spaces
                if (length > 30) {
                    int lastSpace = title.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        title = title.substring(0, lastSpace);
                    }
                }
            }
            // In the values map, sets the value of the title
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        } else if (title != null) {
            // In the values map, sets the value of the title
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
        }

        // This puts the desired notes text into the map.
        values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);


        getContentResolver().update(
                mUri,    // The URI for the record to update.
                values,  // The map of column names and new values to apply to them.
                null,    // No selection criteria are used, so no where columns are necessary.
                null     // No where columns are used, so no where arguments are necessary.
            );


    }

    /**
     * This helper method cancels the work done on a note.  It deletes the note if it was
     * newly created, or reverts to the original text of the note i
     */
    private final void cancelNote() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                // Put the original note text back into the database
                mCursor.close();
                mCursor = null;
                ContentValues values = new ContentValues();
                values.put(NotePad.Notes.COLUMN_NAME_NOTE, mOriginalContent);
                getContentResolver().update(mUri, values, null, null);
            } else if (mState == STATE_INSERT) {
                // We inserted an empty note, make sure to delete it
                deleteNote();
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Take care of deleting a note.  Simply deletes the entry.
     */
    private final void deleteNote() {
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            getContentResolver().delete(mUri, null, null);
            mText.setText("");
        }
    }
}
