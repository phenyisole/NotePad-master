
# NotepadMaster - 安卓笔记应用

NotepadMaster 是一款基于 Google Notepad Master 开发的安卓笔记应用，旨在帮助用户高效管理个人笔记。应用主要提供了笔记的基本管理功能，包括记录笔记时间戳、搜索功能、笔记分类和标签管理等，便于用户查看历史记录并快速定位需要的信息。

---

## 功能模块

### 基础功能
- **笔记显示时间戳**  
   每个笔记在创建或编辑时都会自动记录时间，并在笔记列表中展示，帮助用户追踪笔记的创建和更新信息。

- **搜索笔记功能**  
   提供强大的搜索功能，支持根据标题或内容中的关键字进行模糊匹配，快速定位目标笔记。

### 附加功能
- **笔记分类管理**  
  支持为笔记添加分类标签，方便用户对笔记进行归类管理。  
- **笔记导出功能**  
  用户可以选择导出单条或批量导出笔记，便于备份和分享。  
- **UI美化功能**  
  采用 Material Design 风格，提供更流畅的交互体验和视觉效果。

---

## 技术栈
- **开发语言**：Java
- **框架与库**：
  - Android SDK
  - SQLite（本地存储）
- **IDE**：Android Studio
- **构建工具**：Gradle

---

## 环境配置

### 依赖与库
- **Android SDK**：需要至少安装 SDK 版本 30。
- **Gradle**：确保已安装支持的 Gradle 版本。

### Android Studio 配置
1. 使用 **Android Studio** 作为开发工具，确保安装了最新版本的 IDE。
2. 配置好 Gradle 构建工具，确保项目的依赖项正确同步。

---

## 安装与运行

### 安装步骤
1. 克隆项目：
   ```bash
   git clone https://github.com/phenyisole/NotePad-master.git
   ```
2. 导入到 Android Studio 中：
   - 打开 Android Studio，选择 "Open an existing Android Studio project"，选择克隆的项目文件夹。
3. 同步项目依赖：
   - 点击 "Sync Now" 按钮，确保所有依赖项被正确下载和配置。
4. 连接设备或启动模拟器，点击运行按钮 (`Run`) 安装并启动应用。

---

## 功能实现

### 1. **笔记显示时间戳**
   **功能描述**：  
   在用户创建或编辑笔记时，应用会自动记录当前的时间戳，并将该时间显示在笔记列表中。时间戳帮助用户快速了解笔记的创建时间和更新情况。  

   **实现原理**：  
   - 获取当前时间戳：在每次创建或更新笔记时，我们通过 System.currentTimeMillis() 获取当前系统时间戳，表示笔记的创建或修改时间。  
   - 使用 SQLite 存储笔记内容和时间戳信息，并在笔记列表中动态加载显示。
   - 显示时间戳：在加载笔记数据时，通过查询数据库中的时间戳字段，使用 SimpleDateFormat 类将时间戳转换为用户友好的日期格式，并将其显示在笔记列表的 UI 中。

   **代码示例**：
   ```java
   private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
            NotePad.Notes.COLUMN_NAME_NOTE
    };
   ```
   ```java
       long timestamp = cursor.getLong(cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE));
       SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
       String formattedDate = dateFormat.format(new Date(timestamp));
       TextView timeTextView = (TextView) view.findViewById(R.id.time_text);
       timeTextView.setText(formattedDate);
   ```

   **功能截图**：  
   ![笔记显示时间戳](https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/timestamp.png)  
   该截图展示了在笔记列表中，笔记与其时间戳的显示效果。

---

### 2. **搜索笔记功能**
   **功能描述**：  
   用户可以通过搜索框输入关键字，在笔记中进行快速查找。搜索功能支持对标题和内容进行模糊匹配，帮助用户快速定位目标笔记。

   **实现原理**：  
   - 在 SQLite 中使用 `LIKE` 语句进行关键字匹配查询。
   - 实时更新：每次用户输入新的查询关键字时，自动更新搜索结果，避免页面刷新，提升用户体验。
   - 数据更新：通过 Cursor 实时查询数据库，将查询结果加载到 UI 层中进行显示。

   **代码示例**：
   ```java
   // 查询数据库，进行模糊匹配
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
   ```
   ```xml
   <SearchView
            android:id="@+id/search_view"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:queryHint="搜索笔记"
            android:layout_marginLeft="15dp"
            android:layout_marginRight="15dp"
            android:layout_marginBottom="30dp"
            android:paddingBottom="5dp"
            android:iconifiedByDefault="false"
            android:background="@drawable/search_view_background" />  />
   ```

   **功能截图**：  
   <table>
     <tr>
       <td><img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/search1.png" width="300" /></td>
       <td><img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/search2.png" width="300" /></td>
     </tr>
   </table>

   该截图展示了搜索笔记功能的界面效果，用户可以输入关键词并快速显示匹配的笔记。

---

### 3. **笔记分类管理**
   **功能描述**：  
   为了帮助用户更好地管理笔记，应用支持为每个笔记添加自定义分类。用户可以根据这些分类对笔记进行组织和查找，提高管理效率。

   **实现原理**：  
   - 为每个笔记增加分类字段，存储到 SQLite 中。
   - 支持分类，修改，删除
   - UI 交互：通过弹出对话框或选择器让用户为笔记分配分类，支持多分类管理。
   - 提供界面供用户为笔记添加和编辑分类，支持按分类进行筛选。
   - 数据库更新：用户选择分类时，更新笔记的分类字段，通过 ContentValues 更新数据库中的分类信息。

   **代码示例**：
   ```java
   String CREATE_TABLE_CATEGORY_INFO = "CREATE TABLE " + NotePad.CategoryInfo.TABLE_NAME + " ("
                   + NotePad.CategoryInfo._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                   + NotePad.CategoryInfo.COLUMN_NAME_CATEGORY + " TEXT NOT NULL"
                   + ");";
           db.execSQL(CREATE_TABLE_CATEGORY_INFO);
   ```
   ```java
   //添加分类
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
   ```
   ```java
//将笔记绑定到分类
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
   ```

   **功能截图**：  
   <div align="center">
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/category11.png" width="23%" />
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/category2.png" width="23%" />
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/category3.png" width="23%" />
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/category4.png" width="23%" />
   </div>

   该截图展示了笔记添加标签的界面，用户可以通过标签进行筛选和管理。

---

### 4. **笔记导出功能**
   **功能描述**：  
   用户可以选择将笔记导出为 `.txt` 文件，便于备份和分享。支持导出单个笔记，也可以批量导出多个笔记。

   **实现原理**：  
   - 选择要导出的笔记内容，并将其转换为文本格式。  
   - 利用 Android 文件 API 创建文件并写入内容，导出为 .txt 文件。
     

   **代码示例**：
   ```java
      //单独导出
     try {
            // 获取笔记内容
            String noteContent = mText.getText().toString();

            // 打开输出流并写入数据
            try (OutputStream outputStream = getContentResolver().openOutputStream(fileUri)) {
                if (outputStream != null) {
                    outputStream.write(noteContent.getBytes());
                    outputStream.flush();
                    Toast.makeText(this, "笔记导出成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "无法打开文件", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
   ```
   ```java
      Uri fileUri = data.getData();

      if (fileUri != null) {
          saveNoteToFile(fileUri);
      } else {
          Toast.makeText(this, "文件创建失败", Toast.LENGTH_SHORT).show();
      }
   ```
   ```java
   //批量导出固定位置切默认导出为txt文件
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
   ```

   **功能截图**：  
   <div align="center">
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/export1.png" width="30%" />
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/export2.png" width="30%" />
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/export3.png" width="30%" />
   </div>
   
   <div align="center">
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/export4.png" width="30%" />
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/export5.png" width="30%" />
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/export6.png" width="30%" />
   </div>

   展示了笔记导出功能的界面，用户可以选择导出所需的笔记。  
   其中 批量导出的默认路径是 /storage/emulated/0/Android/data/com.example.android.notepad/files
---
### 5. **UI美化功能**
   **功能描述**：  
   - 重新制作了notepad的主ui 模仿的是vivo手机的便签
   - 增加内容预览功能
   - 使用阿里矢量图标库，更新了原先的按钮
   - 在底部添加更现代的页面选择栏
   - 重绘笔记编辑功能，使标题的更改与显示更加直观
   - 增加添加背景功能
   - 增加更改字体颜色功能

   **功能实现**:
   - 动态界面：通过 RecyclerView 和 CardView 提供流畅的列表展示，支持动态添加、编辑、删除笔记。
   - 通过 SharedPreferences 存储用户的字体和背景设置，动态应用不同的视觉风格。
     
   **代码示例**：  
   ```
//主ui基本框架（缩略） 
      <?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="#FAFAFA">

    <!-- 便签标题和按钮 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="60dp"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:layout_marginTop="20dp">

        <TextView
            android:id="@+id/label"
            android:layout_width="wrap_content"
            android:layout_height="60dp"
            android:text="便签"
            android:textColor="#000000"
            android:textSize="30sp"
            android:layout_marginLeft="30dp" />

        <!-- 按钮区 -->
        <LinearLayout
            android:orientation="horizontal"
            android:layout_width="wrap_content"
            android:layout_marginLeft="170dp">
            <Button
                android:id="@+id/enter_button"
                android:layout_width="20dp"
                android:layout_height="60dp"
                android:text="&#xec9e;"  <!-- 图标 -->
                android:textColor="#000000" />
            <Button
                android:id="@+id/cancel_button"
                android:layout_width="20dp"
                android:layout_height="60dp"
                android:text="&#xe60e;"  <!-- 图标 -->
                android:textColor="#000000" />
            <Button
                android:id="@+id/export_button"
                android:layout_width="20dp"
                android:layout_height="60dp"
                android:text="&#xe627;"  <!-- 图标 -->
                android:textColor="#000000" />
        </LinearLayout>
    </LinearLayout>

    <!-- 搜索框和列表 -->
    <RelativeLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:padding="10dp">

        <SearchView
            android:id="@+id/search_view"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:queryHint="搜索笔记"
            android:layout_marginBottom="30dp" />

        <ListView
            android:id="@android:id/list"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:layout_below="@id/search_view"
            android:divider="@android:color/transparent" />

        <!-- 底部按钮 -->
        <androidx.constraintlayout.widget.ConstraintLayout
            android:layout_width="match_parent"
            android:layout_height="120dp"
            android:layout_alignParentBottom="true">

            <Button
                android:id="@+id/button_left"
                android:layout_width="50dp"
                android:layout_height="50dp"
                android:text="&#xe60f;"  <!-- 图标 -->
                android:textColor="#FFA500"
                android:textSize="32sp" />
                
            <Button
                android:id="@+id/button_center"
                android:layout_width="80dp"
                android:layout_height="80dp"
                android:text="&#xe70b;"  <!-- 图标 -->
                android:textColor="#FFA500"
                android:textSize="70sp" />

            <Button
                android:id="@+id/button_right"
                android:layout_width="50dp"
                android:layout_height="50dp"
                android:text="&#xe7a4;"  <!-- 图标 -->
                android:textColor="#ABAAA8"
                android:textSize="35sp" />
        </androidx.constraintlayout.widget.ConstraintLayout>

    </RelativeLayout>

</LinearLayout>

   ```

   **功能截图**：  
   
<div align="center">
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/ui.png" width="30%" />
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/ui2.png" width="30%" />
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/ui3.png" width="30%" />
   </div>
   
   <div align="center">
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/ui4.png" width="30%" />
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/ui5.png" width="30%" />
   </div>
   

## 界面展示

### 主界面 - 笔记列表
<table>
     <tr>
       <td><img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/ui.png" width="300" /></td>
       <td><img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/ui5.png" width="300" /></td>
     </tr>
   </table>
该截图展示了应用的主界面，用户可以在此查看所有笔记及其时间戳。

---

## 文件结构

```plaintext
NotepadMaster/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   ├── com/
│   │   │   │   │   ├── example/
│   │   │   │   │   │   ├── android/
│   │   │   │   │   │   │   ├── notepad/
│   │   │   │   │   │   │   │   ├── CategoryAdapter.java       # 分类适配器
│   │   │   │   │   │   |   |   ├── CategoryList.java          # 分类列表
│   │   │   │   │   │   │   │   ├── NoteEditor.java            # 笔记编辑器
│   │   │   │   │   │   │   │   ├── NotePad.java                # 笔记数据管理
│   │   │   │   │   │   │   │   ├── NotePadProvider.java        # 笔记内容提供者
│   │   │   │   │   │   │   │   ├── NotesList.java              # 笔记列表
│   │   │   │   │   │   │   │   ├── NotesLiveFolder.java        # 笔记存档
│   │   │   │   │   │   │   │   └── TitleEditor.java            # 标题编辑器
│   │   ├── res/
│   │   │   ├── drawable/
│   │   │   │   ├── add.png
│   │   │   │   ├── app_notes.png
│   │   │   │   ├── background_1.png
│   │   │   │   ├── background_2.png
│   │   │   │   ├── background_3.png
│   │   │   │   └── ...
│   │   │   ├── layout/
│   │   │   │   ├── activity_notes_list.xml
│   │   │   │   ├── category_item.xml
│   │   │   │   └── note_editor.xml
│   │   │   └── values/
│   │   │       ├── strings.xml
│   │   │       └── styles.xml
├── build.gradle
└── README.md
```

---

## 贡献指南
- Fork 本仓库，创建一个新的分支（`feature/your-feature`），在该分支上进行开发。
- 提交修改并发起 Pull Request。
- 提交前请确保通过了所有单元测试，并且代码符合项目的代码风格。

---

## 许可证
- 本项目采用 [MIT License](LICENSE)，你可以自由使用和修改，但需要保留原作者信息。

---

## 联系方式
- **维护者**：张鸿雨
