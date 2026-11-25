package com.smartscan.pro.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BrandingWatermark
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartscan.pro.BuildConfig
import com.smartscan.pro.ui.theme.CyanAccent
import com.smartscan.pro.ui.theme.EmeraldSuccess
import com.smartscan.pro.ui.theme.IndigoPrimary
import com.smartscan.pro.ui.theme.ThemeMode

@Composable
fun HomeScreen(
    onOpenScanTab: () -> Unit,
    onOpenToolsTab: () -> Unit,
    onQuickAction: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Smart Scan",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "AI Document Scanner & PDF Studio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = EmeraldSuccess.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Offline Ready",
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search scans, PDFs, OCR text...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }

        item {
            HeroScanBanner(onStartScan = onOpenScanTab)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = onOpenToolsTab) {
                    Text("View All Tools")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "ID Card",
                    icon = Icons.Rounded.Badge,
                    tint = IndigoPrimary,
                    onClick = { onQuickAction("ID Card Scan") }
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Image to PDF",
                    icon = Icons.Rounded.PictureAsPdf,
                    tint = Color(0xFFEF4444),
                    onClick = { onQuickAction("Image to PDF") }
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Offline OCR",
                    icon = Icons.Rounded.TextFields,
                    tint = CyanAccent,
                    onClick = { onQuickAction("Offline OCR") }
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Compress",
                    icon = Icons.Rounded.Compress,
                    tint = EmeraldSuccess,
                    onClick = { onQuickAction("PDF Compress") }
                )
            }
        }

        item {
            Text(
                text = "Recent Documents",
                style = MaterialTheme.typography.titleLarge
            )
        }

        item {
            EmptyStateCard(
                icon = Icons.Rounded.Folder,
                title = "No Scanned Documents Yet",
                subtitle = "Aapka document library abhi khaali hai. Real camera se scan karne ke liye neeche Scan button par tap karein.",
                buttonLabel = "Start First Scan",
                onAction = onOpenScanTab
            )
        }
    }
}

@Composable
private fun HeroScanBanner(onStartScan: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(IndigoPrimary, Color(0xFF2563EB), CyanAccent)
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "HD Smart Scanner",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Auto edge crop, magic filter, OCR text aur high-quality PDF — bilkul offline.",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onStartScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = IndigoPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Now", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clickable(onClick = onStartScan)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                )
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DocumentScanner,
                        contentDescription = "Scan",
                        tint = IndigoPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ScanScreen(
    onLaunchCamera: (String) -> Unit,
    onImportGallery: () -> Unit
) {
    val scanModes = listOf("Document", "Batch Multi-Page", "ID Card", "Book Mode", "QR / Barcode")
    var selectedMode by remember { mutableStateOf(scanModes.first()) }

    val pulse = rememberInfiniteTransition(label = "cameraPulse")
    val ringScale by pulse.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Smart Scanner Studio",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Mode select karein aur CameraX / ML Kit Scanner launch karein.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                scanModes.forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        label = { Text(mode) }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .clickable { onLaunchCamera(selectedMode) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .scale(ringScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Open Camera",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = "Mode: $selectedMode",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Auto border detection, perspective correction aur glare removal ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onLaunchCamera(selectedMode) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Camera")
                    }
                    OutlinedButton(
                        onClick = onImportGallery,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun DocumentsScreen(
    onOpenScanTab: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filters = listOf("All", "PDF", "Scanned Images", "OCR Text", "Locked Vault")
    var selectedFilter by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Documents Library",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by file name, tag or date...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        EmptyStateCard(
            icon = Icons.Rounded.Description,
            title = "No Files in \"$selectedFilter\"",
            subtitle = "Room offline database Phase 3 mein connect hoga. Uske baad aapke saare scanned PDFs, folders aur OCR history yahan save rahenge.",
            buttonLabel = "Scan New Document",
            onAction = onOpenScanTab
        )
    }
}

@Composable
fun AiAssistantScreen(
    hasApiKey: Boolean,
    onOpenSettings: () -> Unit,
    onRunAiAction: (String) -> Unit
) {
    var promptText by remember { mutableStateOf("") }
    val actions = listOf(
        "Summarize Document" to Icons.Rounded.AutoAwesome,
        "Translate Text" to Icons.Rounded.Translate,
        "Extract Key Points" to Icons.Rounded.Description,
        "Smart Rewrite" to Icons.Rounded.AutoFixHigh
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "AI Document Assistant",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasApiKey) {
                    EmeraldSuccess.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (hasApiKey) "AI API Key Configured" else "Online AI Setup Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hasApiKey) EmeraldSuccess else MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (hasApiKey) {
                        "Aapki API key saved hai. Phase 5 mein live document analysis aur chat active ho jayega."
                    } else {
                        "Real AI results ke liye Settings mein apni Gemini / OpenAI API key daalein. Offline OCR bina internet ke chalega."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!hasApiKey) {
                    OutlinedButton(onClick = onOpenSettings, shape = RoundedCornerShape(12.dp)) {
                        Text("Configure API Key in Settings")
                    }
                }
            }
        }

        Text(
            text = "AI Document Actions",
            style = MaterialTheme.typography.titleMedium
        )

        actions.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { (label, icon) ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onRunAiAction(label) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask AI about a scanned document...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Button(
                onClick = {
                    if (promptText.isNotBlank()) {
                        onRunAiAction(promptText)
                        promptText = ""
                    }
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Send")
            }
        }
    }
}

private data class ToolModule(
    val category: String,
    val title: String,
    val subtitle: String,
    val isOffline: Boolean,
    val icon: ImageVector
)

@Composable
fun ToolsScreen(
    onToolClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val toolModules = remember {
        listOf(
            ToolModule("1. Smart Scanning", "Multi-Page & Auto Edge Scan", "Auto crop, perspective fix, ID & book scan", true, Icons.Rounded.DocumentScanner),
            ToolModule("2. AI Image Enhancement", "Magic Color, B&W & Shadow Fix", "Auto contrast, deskew, sharpen & denoise", true, Icons.Rounded.AutoFixHigh),
            ToolModule("3. Advanced Image Editing", "Crop, Rotate, Markup & Filters", "Brightness, highlight, blur sensitive areas", true, Icons.Rounded.Crop),
            ToolModule("4. PDF Creation & Conversion", "Image to PDF & PDF to Image", "A4/Letter sizing, margins, TXT & DOCX export", true, Icons.Rounded.PictureAsPdf),
            ToolModule("5. Advanced PDF Editor", "Merge, Split, Compress & Sign", "Reorder pages, watermark, digital signature", true, Icons.Rounded.CallMerge),
            ToolModule("5. Advanced PDF Editor", "Split & Extract PDF Pages", "Separate ranges or delete unwanted pages", true, Icons.Rounded.ContentCut),
            ToolModule("5. Advanced PDF Editor", "Add Watermark & Signature", "Custom stamp, hand-drawn e-signature", true, Icons.Rounded.BrandingWatermark),
            ToolModule("6. AI OCR & Text Recognition", "Offline Image to Text (ML Kit)", "Copy, edit, search & save extracted text", true, Icons.Rounded.TextFields),
            ToolModule("7. AI Document Assistant", "Summarize, Q&A & Translate", "Online AI powered document intelligence", false, Icons.Rounded.AutoAwesome),
            ToolModule("8. Document Management", "Folders, Tags & Smart Search", "Offline Room database, favorites & recycle bin", true, Icons.Rounded.Layers),
            ToolModule("9. Professional Tools", "QR / Barcode & E-Sign Pad", "Scan QR codes, create signatures & stamps", true, Icons.Rounded.QrCodeScanner),
            ToolModule("9. Professional Tools", "Digital Signature Maker", "Draw and save transparent signatures", true, Icons.Rounded.Draw),
            ToolModule("10. Security & Vault", "PDF Password & Biometric Lock", "AES Keystore encryption & fingerprint lock", true, Icons.Rounded.Lock),
            ToolModule("10. Security & Vault", "Batch Optimizer & Tuning", "Background WorkManager compression", true, Icons.Rounded.Tune)
        )
    }

    val filtered = toolModules.filter {
        searchQuery.isBlank() ||
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.subtitle.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Professional Tools Suite",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search PDF, OCR, Watermark, Lock...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }

        items(filtered) { tool ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToolClick(tool.title) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tool.category,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = tool.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = tool.subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (tool.isOffline) {
                            EmeraldSuccess.copy(alpha = 0.14f)
                        } else {
                            CyanAccent.copy(alpha = 0.14f)
                        }
                    ) {
                        Text(
                            text = if (tool.isOffline) "Offline" else "Online",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tool.isOffline) EmeraldSuccess else CyanAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    biometricEnabled: Boolean,
    apiKey: String,
    onThemeChange: (ThemeMode) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onSaveApiKey: (String) -> Unit
) {
    var showApiDialog by remember { mutableStateOf(false) }
    var tempKey by remember(apiKey) { mutableStateOf(apiKey) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Appearance (Dark / Light Theme)", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionButton(
                            modifier = Modifier.weight(1f),
                            label = "System",
                            icon = Icons.Rounded.BrightnessAuto,
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { onThemeChange(ThemeMode.SYSTEM) }
                        )
                        ThemeOptionButton(
                            modifier = Modifier.weight(1f),
                            label = "Dark",
                            icon = Icons.Rounded.DarkMode,
                            selected = themeMode == ThemeMode.DARK,
                            onClick = { onThemeChange(ThemeMode.DARK) }
                        )
                        ThemeOptionButton(
                            modifier = Modifier.weight(1f),
                            label = "Light",
                            icon = Icons.Rounded.LightMode,
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = { onThemeChange(ThemeMode.LIGHT) }
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("Fingerprint App Lock", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Secure private documents with Biometrics",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = onBiometricChange
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showApiDialog = true },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI Assistant API Key", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (apiKey.isBlank()) "Not configured — Tap to add key" else "Configured (Tap to update)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = EmeraldSuccess
                    )
                    Column {
                        Text("Smart Scan v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (BuildConfig.DEBUG) {
                                "AdMob Mode: Safe Test Ads (Debug Build)"
                            } else {
                                "AdMob Mode: Live Play Store Ads (Release Build)"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showApiDialog) {
        AlertDialog(
            onDismissRequest = { showApiDialog = false },
            title = { Text("Configure AI API Key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Online AI Document Assistant ke liye apni Gemini API key yahan paste karein. Yeh sirf aapke phone mein save rahegi.",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        singleLine = true,
                        placeholder = { Text("AIzaSy...") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveApiKey(tempKey)
                    showApiDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ThemeOptionButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(buttonLabel)
            }
        }
    }
}
