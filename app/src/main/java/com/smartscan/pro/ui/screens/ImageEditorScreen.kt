package com.smartscan.pro.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartscan.pro.engine.ImageEnhancer
import com.smartscan.pro.engine.ScanFilter
import com.smartscan.pro.ui.EditorSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class PageEditParams(
    val filter: ScanFilter = ScanFilter.MAGIC_COLOR,
    val rotation: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1.0f
) {
    fun isModified(): Boolean =
        filter != ScanFilter.ORIGINAL || rotation % 360f != 0f || brightness != 0f || contrast != 1.0f
}

@Composable
fun ImageEditorScreen(
    session: EditorSession,
    onCancel: () -> Unit,
    onSaveComplete: (
        finalTitle: String,
        finalFolder: String,
        finalPaths: List<String>,
        imagesModified: Boolean
    ) -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagePaths = session.pagePaths
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var docTitle by remember { mutableStateOf(session.initialTitle) }
    var docFolder by remember { mutableStateOf(session.folderName) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val editMap = remember {
        mutableStateMapOf<Int, PageEditParams>().apply {
            pagePaths.indices.forEach { idx ->
                put(
                    idx,
                    PageEditParams(
                        filter = if (session.existingDocument == null) ScanFilter.MAGIC_COLOR else ScanFilter.ORIGINAL
                    )
                )
            }
        }
    }

    val currentParams = editMap[currentPageIndex] ?: PageEditParams()
    val currentPath = pagePaths.getOrElse(currentPageIndex) { pagePaths.first() }

    var baseBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(currentPath) {
        previewBitmap = null
        baseBitmap = withContext(Dispatchers.IO) {
            ImageEnhancer.loadScaledBitmap(currentPath, maxDimension = 1000)
        }
    }

    // 45ms debounce avoids frame drops while dragging brightness/contrast sliders
    LaunchedEffect(baseBitmap, currentParams) {
        val src = baseBitmap ?: return@LaunchedEffect
        delay(45L)
        previewBitmap = withContext(Dispatchers.Default) {
            ImageEnhancer.applyEnhancements(
                source = src,
                filter = currentParams.filter,
                rotationDegrees = currentParams.rotation,
                brightness = currentParams.brightness,
                contrast = currentParams.contrast
            )
        }
    }

    fun updateCurrent(transform: (PageEditParams) -> PageEditParams) {
        editMap[currentPageIndex] = transform(currentParams)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel, enabled = !isSaving) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showRenameDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = docTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Rename",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Page ${currentPageIndex + 1} of ${pagePaths.size} • $docFolder",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        isSaving = true
                        scope.launch {
                            var anyModified = false
                            val savedPaths = withContext(Dispatchers.IO) {
                                pagePaths.mapIndexed { idx, path ->
                                    val params = editMap[idx] ?: PageEditParams(filter = ScanFilter.ORIGINAL)
                                    if (!params.isModified()) {
                                        path
                                    } else {
                                        anyModified = true
                                        val fullBmp = ImageEnhancer.loadScaledBitmap(path, maxDimension = 2200)
                                        if (fullBmp != null) {
                                            val enhanced = ImageEnhancer.applyEnhancements(
                                                source = fullBmp,
                                                filter = params.filter,
                                                rotationDegrees = params.rotation,
                                                brightness = params.brightness,
                                                contrast = params.contrast
                                            )
                                            ImageEnhancer.saveBitmapAsJpeg(enhanced, File(path), quality = 90)
                                            enhanced.recycle()
                                            fullBmp.recycle()
                                        }
                                        path
                                    }
                                }
                            }
                            isSaving = false
                            onSaveComplete(docTitle, docFolder, savedPaths, anyModified)
                        }
                    }
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0B0F19))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            val bmp = previewBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Document Preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                CircularProgressIndicator()
            }
        }

        if (pagePaths.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pagePaths.indices.forEach { idx ->
                    val selected = idx == currentPageIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (selected) 1.5.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { currentPageIndex = idx }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Page ${idx + 1}",
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScanFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = currentParams.filter == filter,
                            onClick = { updateCurrent { it.copy(filter = filter) } },
                            label = { Text(filter.label, fontSize = 12.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Brightness", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = currentParams.brightness,
                            onValueChange = { v -> updateCurrent { it.copy(brightness = v) } },
                            valueRange = -55f..55f,
                            modifier = Modifier.height(30.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Contrast", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = currentParams.contrast,
                            onValueChange = { v -> updateCurrent { it.copy(contrast = v) } },
                            valueRange = 0.6f..1.8f,
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { updateCurrent { it.copy(rotation = it.rotation - 90f) } }) {
                        Icon(Icons.Rounded.RotateLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rotate Left", fontSize = 12.sp)
                    }
                    TextButton(onClick = { updateCurrent { it.copy(rotation = it.rotation + 90f) } }) {
                        Icon(Icons.Rounded.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rotate Right", fontSize = 12.sp)
                    }
                    TextButton(onClick = {
                        updateCurrent { PageEditParams(filter = ScanFilter.ORIGINAL) }
                    }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        var tempTitle by remember(docTitle) { mutableStateOf(docTitle) }
        val folders = listOf("Scanned", "ID Cards", "Invoices", "Notes", "Personal")
        var tempFolder by remember(docFolder) { mutableStateOf(docFolder) }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Document Name & Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("File Name") },
                        singleLine = true
                    )
                    Text("Folder:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        folders.forEach { f ->
                            FilterChip(
                                selected = tempFolder == f,
                                onClick = { tempFolder = f },
                                label = { Text(f, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tempTitle.isNotBlank()) docTitle = tempTitle.trim()
                    docFolder = tempFolder
                    showRenameDialog = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
