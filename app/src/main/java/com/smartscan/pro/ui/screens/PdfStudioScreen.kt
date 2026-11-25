package com.smartscan.pro.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smartscan.pro.data.DocumentEntity
import com.smartscan.pro.engine.ImageEnhancer
import com.smartscan.pro.engine.PdfBuildOptions
import com.smartscan.pro.engine.PdfPageSize
import com.smartscan.pro.engine.PdfQuality
import com.smartscan.pro.engine.PdfStudioEngine
import com.smartscan.pro.ui.theme.EmeraldSuccess
import java.io.File

@Composable
fun PdfStudioScreen(
    document: DocumentEntity,
    onClose: () -> Unit,
    onBuildPdf: (PdfBuildOptions) -> Unit,
    onExtractPages: (selectedIndices: List<Int>, newTitle: String) -> Unit,
    onExportPdfToDevice: (File, String) -> Unit,
    onSharePdf: (File, String) -> Unit
) {
    val context = LocalContext.current
    val pages = remember(document.imagePaths) { document.pagePathList() }
    val selectedPageIndices = remember(document.id) {
        mutableStateListOf<Int>().apply { addAll(pages.indices) }
    }

    var pageSize by remember { mutableStateOf(PdfPageSize.A4) }
    var quality by remember { mutableStateOf(PdfQuality.MEDIUM) }
    var watermarkText by remember { mutableStateOf("") }
    var includePageNumbers by remember { mutableStateOf(true) }
    var attachSignature by remember { mutableStateOf(false) }
    var signatureAllPages by remember { mutableStateOf(false) }
    var hasSignature by remember { mutableStateOf(PdfStudioEngine.hasSavedSignature(context)) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }

    val existingPdfFile = remember(document.pdfPath) {
        document.pdfPath?.let { File(it) }?.takeIf { it.exists() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header
        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PDF Studio",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${document.title} •${pages.size} page(s)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Ready PDF Status Card
            if (existingPdfFile != null) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldSuccess.copy(alpha = 0.12f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PDF Ready for Export",
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                                Text(
                                    text = "File Size: ${ImageEnhancer.formatFileSize(existingPdfFile.length())}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onExportPdfToDevice(existingPdfFile, "${document.title}.pdf") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save to Phone")
                            }
                            OutlinedButton(
                                onClick = { onSharePdf(existingPdfFile, document.title) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }

            // Page Selection Strip (for Split/Extract)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pages (${selectedPageIndices.size}/${pages.size} selected)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (pages.size > 1) {
                            TextButton(
                                enabled = selectedPageIndices.isNotEmpty(),
                                onClick = { showSplitDialog = true }
                            ) {
                                Icon(Icons.Rounded.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Split / Extract")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pages.forEachIndexed { idx, path ->
                            val isSelected = selectedPageIndices.contains(idx)
                            Box(
                                modifier = Modifier
                                    .width(82.dp)
                                    .height(108.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isSelected) selectedPageIndices.remove(idx)
                                        else selectedPageIndices.add(idx)
                                    }
                            ) {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "Page ${idx + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .padding(vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isSelected) "✓ P${idx + 1}" else "P${idx + 1}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Page Size & Compression Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Page Size", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PdfPageSize.entries.forEach { size ->
                            FilterChip(
                                selected = pageSize == size,
                                onClick = { pageSize = size },
                                label = { Text(size.label) }
                            )
                        }
                    }

                    Text("PDF Quality / Compression", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PdfQuality.entries.forEach { q ->
                            FilterChip(
                                selected = quality == q,
                                onClick = { quality = q },
                                label = { Text(q.label) }
                            )
                        }
                    }
                }
            }

            // Watermark, Page Numbers & Digital Signature Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Watermark & E-Signature", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = watermarkText,
                        onValueChange = { watermarkText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Watermark Text (Optional)") },
                        placeholder = { Text("Jaise: CONFIDENTIAL ya Smart Scan") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add Page Numbers", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = includePageNumbers,
                            onCheckedChange = { includePageNumbers = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Attach Digital Signature", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (hasSignature) "Saved signature ready" else "Draw signature first",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { showSignatureDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (hasSignature) "Redraw" else "Draw", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = attachSignature && hasSignature,
                                onCheckedChange = { checked ->
                                    if (checked && !hasSignature) {
                                        showSignatureDialog = true
                                    } else {
                                        attachSignature = checked
                                    }
                                }
                            )
                        }
                    }

                    if (attachSignature && hasSignature) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Stamp Signature on All Pages", fontSize = 13.sp)
                            Switch(
                                checked = signatureAllPages,
                                onCheckedChange = { signatureAllPages = it }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Primary Action Button
        Surface(tonalElevation = 4.dp) {
            Button(
                onClick = {
                    onBuildPdf(
                        PdfBuildOptions(
                            pageSize = pageSize,
                            quality = quality,
                            watermarkText = watermarkText,
                            includePageNumbers = includePageNumbers,
                            attachSignature = attachSignature && hasSignature,
                            signatureOnAllPages = signatureAllPages
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingPdfFile == null) "Create & Optimize PDF" else "Rebuild PDF with Changes",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showSignatureDialog) {
        SignaturePadDialog(
            onDismiss = { showSignatureDialog = false },
            onSaved = {
                hasSignature = true
                attachSignature = true
                showSignatureDialog = false
            }
        )
    }

    if (showSplitDialog) {
        var splitTitle by remember { mutableStateOf("${document.title}_Split") }
        AlertDialog(
            onDismissRequest = { showSplitDialog = false },
            title = { Text("Extract ${selectedPageIndices.size} Page(s)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chune huye pages se naya separate PDF document banega.", fontSize = 13.sp)
                    OutlinedTextField(
                        value = splitTitle,
                        onValueChange = { splitTitle = it },
                        label = { Text("New Document Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val sorted = selectedPageIndices.sorted()
                    onExtractPages(sorted, splitTitle)
                    showSplitDialog = false
                }) {
                    Text("Extract")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSplitDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    val activeStroke = remember { mutableStateListOf<Offset>() }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    var selectedInk by remember { mutableIntStateOf(AndroidColor.BLACK) }

    val inkOptions = listOf(
        "Black" to (AndroidColor.BLACK to Color.Black),
        "Blue" to (AndroidColor.rgb(20, 60, 180) to Color(0xFF143CB4)),
        "Red" to (AndroidColor.rgb(190, 24, 24) to Color(0xFFBE1818))
    )
    val composeInkColor = inkOptions.firstOrNull { it.second.first == selectedInk }?.second?.second ?: Color.Black

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Digital Signature Pad") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Neeche white box mein ungli se apna signature draw karein:", fontSize = 12.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    inkOptions.forEach { (name, pair) ->
                        val isChosen = selectedInk == pair.first
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(pair.second)
                                .border(
                                    width = if (isChosen) 3.dp else 1.dp,
                                    color = if (isChosen) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { selectedInk = pair.first }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        strokes.clear()
                        activeStroke.clear()
                    }) {
                        Text("Clear")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { start ->
                                    activeStroke.clear()
                                    activeStroke.add(start)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    activeStroke.add(change.position)
                                },
                                onDragEnd = {
                                    if (activeStroke.size >= 2) {
                                        strokes.add(activeStroke.toList())
                                    }
                                    activeStroke.clear()
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val allStrokes = strokes + listOf(activeStroke.toList())
                        allStrokes.forEach { pts ->
                            if (pts.size >= 2) {
                                val path = Path().apply {
                                    moveTo(pts[0].x, pts[0].y)
                                    for (i in 1 until pts.size) {
                                        lineTo(pts[i].x, pts[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = composeInkColor,
                                    style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = strokes.isNotEmpty(),
                onClick = {
                    val w = canvasSize.width.coerceAtLeast(1).toFloat()
                    val h = canvasSize.height.coerceAtLeast(1).toFloat()
                    val normalized = strokes.map { stroke ->
                        stroke.map { pt -> (pt.x / w) to (pt.y / h) }
                    }
                    PdfStudioEngine.saveNormalizedSignature(context, normalized, selectedInk)
                    onSaved()
                }
            ) {
                Text("Save Signature")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun MergeDocumentsDialog(
    documents: List<DocumentEntity>,
    onDismiss: () -> Unit,
    onMergeConfirm: (selectedDocs: List<DocumentEntity>, mergedTitle: String) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<Long>() }
    var mergedTitle by remember { mutableStateOf("Merged_SmartScan") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.CallMerge, contentDescription = null)
                Text("Merge PDFs")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = mergedTitle,
                    onValueChange = { mergedTitle = it },
                    label = { Text("Merged File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Kam se kam 2 documents select karein:", fontSize = 12.sp)

                if (documents.size < 2) {
                    Text(
                        "Merge karne ke liye library mein kam se kam 2 documents hone chahiye.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(220.dp)) {
                        items(documents, key = { it.id }) { doc ->
                            val checked = selectedIds.contains(doc.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selectedIds.remove(doc.id) else selectedIds.add(doc.id)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) selectedIds.add(doc.id) else selectedIds.remove(doc.id)
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                    Text("${doc.pageCount} page(s)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedIds.size >= 2,
                onClick = {
                    val chosen = selectedIds.mapNotNull { id -> documents.firstOrNull { it.id == id } }
                    onMergeConfirm(chosen, mergedTitle)
                }
            ) {
                Text("Merge (${selectedIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
