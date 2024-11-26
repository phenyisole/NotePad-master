package com.example.android.notepad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CategoryList extends Activity {
    private ExpandableListView expandableListView;
    private CategoryAdapter adapter;
    private List<String> groupList; // 分类名
    private HashMap<String, List<HashMap<String, String>>> childList; // 每个分类对应的笔记信息
    private SQLiteDatabase db; // 数据库对象

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category_list);

        expandableListView = findViewById(R.id.expandable_list);
        groupList = new ArrayList<>();
        childList = new HashMap<>();

        // 打开数据库
        NotePadProvider.DatabaseHelper dbHelper = new NotePadProvider.DatabaseHelper(this);
        db = dbHelper.getReadableDatabase();

        // 从数据库加载分类和笔记数据
        loadCategoriesAndNotes();

        // 设置适配器
        adapter = new CategoryAdapter(this, groupList, childList);
        expandableListView.setAdapter(adapter);

        // 添加长按操作
        expandableListView.setOnItemLongClickListener((parent, view, position, id) -> {
            showCategoryOptionsDialog(position);
            return true;
        });

        // 设置左边按钮的点击事件
        Button leftButton = findViewById(R.id.button_left);
        leftButton.setOnClickListener(v -> finish());

        // 设置 add_button 的点击事件
        Button addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(v -> showAddCategoryDialog());
    }

    /**
     * 从数据库加载分类和对应的笔记
     */
    private void loadCategoriesAndNotes() {
        // 添加未归档分组
        groupList.add("未归档");
        List<HashMap<String, String>> unarchivedNotes = new ArrayList<>();
        childList.put("未归档", unarchivedNotes);

        // 查询 CategoryInfo 表获取分类
        Cursor categoryCursor = db.query(
                "CategoryInfo", // 表名
                new String[]{"category"}, // 查询的列
                null, null, null, null, null // 无筛选条件
        );

        // 遍历分类
        if (categoryCursor.moveToFirst()) {
            do {
                String category = categoryCursor.getString(0); // 获取分类名
                groupList.add(category); // 添加到 groupList

                // 查询 Notes 表，查找该分类的笔记
                Cursor notesCursor = db.query(
                        "Notes", // 表名
                        new String[]{"_id", "title", "modified", "note"}, // 查询的列
                        "category = ?", // WHERE 子句
                        new String[]{category}, // WHERE 参数
                        null, null, "modified DESC" // 按修改时间降序
                );

                List<HashMap<String, String>> notes = new ArrayList<>();
                if (notesCursor.moveToFirst()) {
                    do {
                        // 获取笔记的 ID、标题、时间和内容
                        String id = notesCursor.getString(0);
                        String title = notesCursor.getString(1);
                        String modifyTime = notesCursor.getString(2);
                        String note = notesCursor.getString(3);

                        // 将数据存储到 HashMap
                        HashMap<String, String> noteMap = new HashMap<>();
                        noteMap.put("id", id);
                        noteMap.put("title", title);
                        noteMap.put("modified", modifyTime);
                        noteMap.put("note", note);

                        // 添加到笔记列表
                        notes.add(noteMap);
                    } while (notesCursor.moveToNext());
                }
                notesCursor.close();

                // 将分类与笔记对应起来
                childList.put(category, notes);
            } while (categoryCursor.moveToNext());
        }
        categoryCursor.close();

        // 加载未归档笔记
        Cursor unarchivedCursor = db.query(
                "Notes", // 表名
                new String[]{"_id", "title", "modified", "note"}, // 查询的列
                "category = 0", // 未归档的条件
                null, null, null, "modified DESC" // 按修改时间降序
        );
        if (unarchivedCursor.moveToFirst()) {
            do {
                String id = unarchivedCursor.getString(0);
                String title = unarchivedCursor.getString(1);
                String modifyTime = unarchivedCursor.getString(2);
                String note = unarchivedCursor.getString(3);

                HashMap<String, String> noteMap = new HashMap<>();
                noteMap.put("id", id);
                noteMap.put("title", title);
                noteMap.put("modified", modifyTime);
                noteMap.put("note", note);

                unarchivedNotes.add(noteMap);
            } while (unarchivedCursor.moveToNext());
        }
        unarchivedCursor.close();
    }

    /**
     * 显示分类的长按选项对话框
     */
    private void showCategoryOptionsDialog(int position) {
        String category = groupList.get(position);
        if ("未归档".equals(category)) {
            // 禁止对未归档分组操作
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("分类操作")
                .setItems(new String[]{"修改名称", "删除分类"}, (dialog, which) -> {
                    if (which == 0) {
                        showRenameCategoryDialog(category);
                    } else if (which == 1) {
                        deleteCategory(category);
                    }
                })
                .show();
    }

    /**
     * 显示修改分类名称的对话框
     */
    private void showRenameCategoryDialog(String oldCategory) {
        final EditText input = new EditText(this);
        input.setText(oldCategory);

        new AlertDialog.Builder(this)
                .setTitle("修改分类名称")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newCategory = input.getText().toString().trim();
                    if (!newCategory.isEmpty()) {
                        renameCategoryInDatabase(oldCategory, newCategory);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 修改分类名称
     */
    private void renameCategoryInDatabase(String oldCategory, String newCategory) {
        ContentValues values = new ContentValues();
        values.put("category", newCategory);
        db.update("CategoryInfo", values, "category = ?", new String[]{oldCategory});

        // 更新界面
        int index = groupList.indexOf(oldCategory);
        groupList.set(index, newCategory);
        List<HashMap<String, String>> notes = childList.remove(oldCategory);
        childList.put(newCategory, notes);
        adapter.notifyDataSetChanged();
    }

    /**
     * 删除分类
     */
    private void deleteCategory(String category) {
        // 将分类中的笔记归档到未归档
        ContentValues values = new ContentValues();
        values.put("category", 0);
        db.update("Notes", values, "category = ?", new String[]{category});

        // 从数据库中删除分类
        db.delete("CategoryInfo", "category = ?", new String[]{category});

        // 更新界面
        groupList.remove(category);
        List<HashMap<String, String>> notes = childList.remove(category);
        if (notes != null) {
            childList.get("未归档").addAll(notes);
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * 显示添加分类的对话框
     */
    private void showAddCategoryDialog() {
        final EditText input = new EditText(this);
        input.setHint("请输入新分类名称");

        new AlertDialog.Builder(this)
                .setTitle("添加新分类")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newCategory = input.getText().toString().trim();
                    if (!newCategory.isEmpty()) {
                        addCategoryToDatabase(newCategory);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 将新分类插入到数据库中
     */
    private void addCategoryToDatabase(String newCategory) {
        ContentValues values = new ContentValues();
        values.put("category", newCategory);

        long rowId = db.insert("CategoryInfo", null, values);
        if (rowId > 0) {
            groupList.add(newCategory);
            childList.put(newCategory, new ArrayList<>());
            adapter.notifyDataSetChanged();
        }
    }
}

