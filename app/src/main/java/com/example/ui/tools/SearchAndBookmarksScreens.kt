package com.example.ui.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.BookmarkEntity
import com.example.data.local.BookmarkFolderEntity
import com.example.data.quran.QuranSearchResult
import com.example.ui.mushaf.toArabicDigits
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.LocalWirdColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    results: List<QuranSearchResult>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onOpenResult: (surahIndex: Int, ayahNumber: Int, pageNumber: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wirdColors = LocalWirdColors.current

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("البحث في القرآن الكريم", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("search_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("quran_search_input"),
                    placeholder = { Text("ابحث بكلمة أو عبارة أو اسم سورة أو 2:255...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Text(
                    text = "يعمل البحث دون إنترنت مع مراعاة الهمزات والتشكيل دون تغيير النص العثماني الأصلي.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                when {
                    isSearching -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    query.trim().length < 2 -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "أدخل كلمتين أو حرفين على الأقل للبحث في المصحف الشريف",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    results.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "لم يتم العثور على نتائج مطابقة",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "النتائج: ${results.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(results, key = { "${it.surahIndex}:${it.ayahNumber}" }) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onOpenResult(item.surahIndex, item.ayahNumber, item.pageNumber)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, wirdColors.subtleBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "سورة ${item.surahName} — آية ${item.ayahNumber} (ص ${item.pageNumber})",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${item.uthmaniText} ﴿${toArabicDigits(item.ayahNumber)}﴾",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = AmiriQuranFontFamily,
                                                fontSize = 20.sp,
                                                lineHeight = 36.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    folders: List<BookmarkFolderEntity>,
    bookmarks: List<BookmarkEntity>,
    onCreateFolder: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onOpenBookmark: (surahIndex: Int, ayahNumber: Int, pageNumber: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wirdColors = LocalWirdColors.current
    var selectedFolderId by remember { mutableLongStateOf(0L) } // 0 = All
    var showFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    val filtered = if (selectedFolderId == 0L) {
        bookmarks
    } else {
        bookmarks.filter { it.folderId == selectedFolderId }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("المفضلة والملاحظات", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("bookmarks_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            },
            actions = {
                IconButton(onClick = { showFolderDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "مجموعة جديدة")
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFolderId == 0L,
                            onClick = { selectedFolderId = 0L },
                            label = { Text("الكل (${bookmarks.size})") }
                        )
                    }
                    items(folders, key = { it.id }) { folder ->
                        FilterChip(
                            selected = selectedFolderId == folder.id,
                            onClick = { selectedFolderId = folder.id },
                            label = { Text(folder.name) }
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "لا توجد آيات محفوظة في هذه المجموعة بعد.\nاضغط مطولًا على أي آية في المصحف لحفظها.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { bm ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onOpenBookmark(bm.surahIndex, bm.ayahNumber, bm.pageNumber)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, wirdColors.subtleBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "سورة ${bm.surahName} — آية ${bm.ayahNumber} (ص ${bm.pageNumber})",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف")
                                        }
                                    }
                                    Text(
                                        text = bm.uthmaniSnippet,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = AmiriQuranFontFamily,
                                            fontSize = 19.sp,
                                            lineHeight = 34.sp
                                        )
                                    )
                                    if (bm.personalNote.isNotBlank()) {
                                        Text(
                                            text = "ملاحظتك الشخصية: ${bm.personalNote}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("إنشاء مجموعة مفضلة جديدة") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("اسم المجموعة") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateFolder(newFolderName)
                        newFolderName = ""
                        showFolderDialog = false
                    }
                ) { Text("إنشاء") }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
