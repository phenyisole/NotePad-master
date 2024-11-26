package com.example.android.notepad;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class CategoryAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> groupList;
    private HashMap<String, List<HashMap<String, String>>> childList;

    public CategoryAdapter(Context context, List<String> groupList, HashMap<String, List<HashMap<String, String>>> childList) {
        this.context = context;
        this.groupList = groupList;
        this.childList = childList;
    }

    @Override
    public int getGroupCount() {
        return groupList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return childList.get(groupList.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return groupList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return childList.get(groupList.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String groupName = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.category_item, null);
        }
        TextView groupNameTextView = convertView.findViewById(R.id.category_name);
        groupNameTextView.setText(groupName);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        HashMap<String, String> childData = childList.get(groupList.get(groupPosition)).get(childPosition);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.noteslist_item, null);
        }

        // 设置子项数据
        String title = childData.get("title");
        String modifyTime = childData.get("modified");
        String note = childData.get("note");

        TextView titleTextView = convertView.findViewById(android.R.id.text1);
        titleTextView.setText(title);

        TextView timeTextView = convertView.findViewById(R.id.time_text);

        // 将时间戳转换为标准格式
        long timestamp = Long.parseLong(modifyTime); // 假设 modifyTime 是字符串类型的时间戳
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedTime = sdf.format(date);

        // 设置格式化后的时间
        timeTextView.setText(formattedTime);

        TextView noteTextView = convertView.findViewById(R.id.content_text);
        noteTextView.setText(note);

        // 设置点击事件，打开对应的笔记编辑界面
        convertView.setOnClickListener(v -> {
            String id = childData.get("id");
            Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, Long.parseLong(id));
            Intent intent = new Intent(Intent.ACTION_EDIT, noteUri);
            context.startActivity(intent);
        });

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
