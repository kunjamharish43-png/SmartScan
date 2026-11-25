package com.smartscan.pro

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.smartscan.pro.ads.AdMobBannerBar
import com.smartscan.pro.ads.InterstitialAdManager
import com.smartscan.pro.ui.SmartScanViewModel
import com.smartscan.pro.ui.navigation.AppTab
import com.smartscan.pro.ui.navigation.SmartScanBottomBar
import com.smartscan.pro.ui.screens.AiAssistantScreen
import com.smartscan.pro.ui.screens.DocumentsScreen
import com.smartscan.pro.ui.screens.HomeScreen
import com.smartscan.pro.ui.screens.ImageEditorScreen
import com.smartscan.pro.ui.screens.ScanScreen
import com.smartscan.pro.ui.screens.SettingsScreen
import com.smartscan.pro.ui.screens.ToolsScreen
import com.smartscan.pro.ui.theme.SmartScanPrefs
import com.smartscan.pro.ui.theme.SmartScanTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var interstitialAdManager: InterstitialAdManager
    private lateinit var viewModel: SmartScanViewModel
    private var activeScanMode: String = "Document"

    private val documentScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pageUris = scanResult?.pages?.map { it.imageUri }.orEmpty()
            val pdfUri = scanResult?.pdf?.uri
            if (pageUris.isNotEmpty()) {
                viewModel.startEditorFromUris(
                    imageUris = pageUris,
                    pdfUri = pdfUri,
                    modeLabel = activeScanMode,
                    onError = { msg -> showToast(msg) }
                )
            }
        }
    }

    private val galleryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.startEditorFromUris(
                imageUris = uris,
                pdfUri = null,
                modeLabel = "Gallery",
                onError = { msg -> showToast(msg) }
            )
        }
    }

    private fun launchRealScanner(mode: String) {
        activeScanMode = mode
        val pageLimit = when (mode) {
            "ID Card" -> 2
            "Batch Multi-Page" -> 50
            else -> 20
        }

        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(pageLimit)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)
        scanner.getStartScanIntent(this)
            .addOnSuccessListener { intentSender ->
                documentScannerLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
            .addOnFailureListener { e ->
                showToast("Scanner start nahi hua (Play Services check karein): ${e.localizedMessage}")
            }
    }

    private fun launchGalleryImport() {
        galleryPickerLauncher.launch("image/*")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[SmartScanViewModel::class.java]
        interstitialAdManager = InterstitialAdManager(applicationContext).apply {
            loadAd()
        }

        setContent {
            var themeMode by remember { mutableStateOf(SmartScanPrefs.getThemeMode(this)) }
            var biometricEnabled by remember { mutableStateOf(SmartScanPrefs.isBiometricEnabled(this)) }
            var apiKey by remember { mutableStateOf(SmartScanPrefs.getAiApiKey(this)) }

            SmartScanTheme(themeMode = themeMode) {
                SmartScanRootApp(
                    viewModel = viewModel,
                    themeMode = themeMode,
                    biometricEnabled = biometricEnabled,
                    apiKey = apiKey,
                    onThemeChange = { newMode ->
                        themeMode = newMode
                        SmartScanPrefs.setThemeMode(this, newMode)
                    },
                    onBiometricChange = { enabled ->
                        biometricEnabled = enabled
                        SmartScanPrefs.setBiometricEnabled(this, enabled)
                    },
                    onSaveApiKey = { key ->
                        apiKey = key
                        SmartScanPrefs.setAiApiKey(this, key)
                    },
                    onLaunchScanner = { mode -> launchRealScanner(mode) },
                    onImportGallery = { launchGalleryImport() },
                    onTriggerAdThenAction = { action ->
                        interstitialAdManager.showAdIfReady(this) {
                            action()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SmartScanRootApp(
    viewModel: SmartScanViewModel,
    themeMode: com.smartscan.pro.ui.theme.ThemeMode,
    biometricEnabled: Boolean,
    apiKey: String,
    onThemeChange: (com.smartscan.pro.ui.theme.ThemeMode) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onLaunchScanner: (String) -> Unit,
    onImportGallery: () -> Unit,
    onTriggerAdThenAction: (action: () -> Unit) -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val documents by viewModel.documents.collectAsState()
    val editorSession by viewModel.editorSession.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    fun navigateToTab(tab: AppTab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun showNotice(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (editorSession != null) {
            val activeSession = editorSession!!
            ImageEditorScreen(
                session = activeSession,
                onCancel = { viewModel.closeEditor() },
                onSaveComplete = { title, folder, paths ->
                    viewModel.saveEditorResult(activeSession, title, folder, paths) { msg ->
                        onTriggerAdThenAction {
                            showNotice(msg)
                            navigateToTab(AppTab.DOCUMENTS)
                        }
                    }
                }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    Column {
                        AdMobBannerBar()
                        SmartScanBottomBar(
                            currentRoute = currentRoute,
                            onTabSelected = { navigateToTab(it) }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = AppTab.HOME.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    enterTransition = { fadeIn(animationSpec = tween(220)) },
                    exitTransition = { fadeOut(animationSpec = tween(180)) }
                ) {
                    composable(AppTab.HOME.route) {
                        HomeScreen(
                            documents = documents,
                            onOpenScanTab = { navigateToTab(AppTab.SCAN) },
                            onOpenToolsTab = { navigateToTab(AppTab.TOOLS) },
                            onQuickAction = { actionName ->
                                when (actionName) {
                                    "ID Card" -> onLaunchScanner("ID Card")
                                    "Gallery Import" -> onImportGallery()
                                    "PDF Tools" -> navigateToTab(AppTab.TOOLS)
                                    else -> showNotice("$actionName engine agle phase mein connect hoga.")
                                }
                            },
                            onOpenDocument = { doc -> viewModel.openExistingDocumentInEditor(doc) },
                            onToggleFavorite = { doc -> viewModel.toggleFavorite(doc) },
                            onRenameDocument = { doc, name -> viewModel.renameDocument(doc, name) },
                            onDeleteDocument = { doc ->
                                viewModel.deleteDocument(doc)
                                showNotice("Document delete ho gaya.")
                            }
                        )
                    }

                    composable(AppTab.SCAN.route) {
                        ScanScreen(
                            onLaunchCamera = { mode -> onLaunchScanner(mode) },
                            onImportGallery = { onImportGallery() }
                        )
                    }

                    composable(AppTab.DOCUMENTS.route) {
                        DocumentsScreen(
                            documents = documents,
                            onOpenScanTab = { navigateToTab(AppTab.SCAN) },
                            onOpenDocument = { doc -> viewModel.openExistingDocumentInEditor(doc) },
                            onToggleFavorite = { doc -> viewModel.toggleFavorite(doc) },
                            onRenameDocument = { doc, name -> viewModel.renameDocument(doc, name) },
                            onDeleteDocument = { doc ->
                                viewModel.deleteDocument(doc)
                                showNotice("Document delete ho gaya.")
                            }
                        )
                    }

                    composable(AppTab.AI_ASSISTANT.route) {
                        AiAssistantScreen(
                            hasApiKey = apiKey.isNotBlank(),
                            onOpenSettings = { navigateToTab(AppTab.SETTINGS) },
                            onRunAiAction = { query ->
                                if (apiKey.isBlank()) {
                                    showNotice("Pehle Settings mein apni AI API Key save karein.")
                                } else {
                                    showNotice("AI engine Phase 5 mein connect hoga: $query")
                                }
                            }
                        )
                    }

                    composable(AppTab.TOOLS.route) {
                        ToolsScreen(
                            onToolClick = { toolTitle ->
                                when {
                                    toolTitle.contains("Auto Edge", ignoreCase = true) -> onLaunchScanner("Batch Multi-Page")
                                    toolTitle.contains("Gallery Import", ignoreCase = true) -> onImportGallery()
                                    toolTitle.contains("Magic Color", ignoreCase = true) -> {
                                        val latest = documents.firstOrNull()
                                        if (latest != null) {
                                            viewModel.openExistingDocumentInEditor(latest)
                                        } else {
                                            onImportGallery()
                                        }
                                    }
                                    else -> onTriggerAdThenAction {
                                        showNotice("$toolTitle module Phase 4-5 mein connect hoga.")
                                    }
                                }
                            }
                        )
                    }

                    composable(AppTab.SETTINGS.route) {
                        SettingsScreen(
                            themeMode = themeMode,
                            biometricEnabled = biometricEnabled,
                            apiKey = apiKey,
                            onThemeChange = onThemeChange,
                            onBiometricChange = { enabled ->
                                onBiometricChange(enabled)
                                showNotice(if (enabled) "Biometric lock ON saved." else "Biometric lock OFF.")
                            },
                            onSaveApiKey = { savedKey ->
                                onSaveApiKey(savedKey)
                                showNotice("AI API Key securely saved.")
                            }
                        )
                    }
                }
            }
        }

        if (isBusy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
