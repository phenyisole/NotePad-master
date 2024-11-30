
# NotepadMaster - 安卓笔记应用

NotepadMaster 是一款基于 Google Notepad Master 开发的安卓笔记应用，旨在帮助用户高效管理个人笔记。应用主要提供了笔记的基本管理功能，包括记录笔记时间戳、搜索功能、笔记分类和标签管理等，便于用户查看历史记录并快速定位需要的信息。

---

## 功能模块

### 基础功能
1. **笔记显示时间戳**  
   每个笔记在创建或编辑时都会自动记录时间，并在笔记列表中展示，帮助用户追踪笔记的创建和更新信息。

2. **搜索笔记功能**  
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
   - 获取当前时间戳并格式化为用户友好的日期时间格式。  
   - 使用 SQLite 存储笔记内容和时间戳信息，并在笔记列表中动态加载显示。

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
   - 搜索结果会实时更新，并通过 Cursor 加载数据到 UI 层展示。

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
   - 提供界面供用户为笔记添加和编辑分类，支持按分类进行筛选。

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
    <img src="https://zhy-149.oss-cn-fuzhou.aliyuncs.com/Notepad/category1.png" width="23%" />
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
   - 使用 Android 文件系统 API，将文本内容保存为 `.txt` 文件。

   **代码示例**：
   ```java
   // 将笔记导出为txt文件
   String fileName = "note_export.txt";
   String content = "笔记内容...";
   
   FileOutputStream fos = openFileOutput(fileName, Context.MODE_PRIVATE);
   fos.write(content.getBytes());
   fos.close();
   ```

   **功能截图**：  
   ![导出功能截图](你的图片路径/导出功能截图.png)  
   展示了笔记导出功能的界面，用户可以选择导出所需的笔记。

---

## 界面展示

### 主界面 - 笔记列表
![主界面截图](你的图片路径/主界面截图.png)  
该截图展示了应用的主界面，用户可以在此查看所有笔记及其时间戳。

### 搜索笔记
![搜索界面截图](你的图片路径/搜索界面截图.png)  
展示了搜索功能，用户可以输入关键字搜索笔记。

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
│   │   │   │   │   │  

 │   │   ├── CategoryList.java          # 分类列表
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
