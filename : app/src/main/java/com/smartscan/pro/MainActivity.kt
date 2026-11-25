package com.smartscan.pro

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartscan.pro.ads.AdMobBannerBar
import com.smartscan.pro.ads.InterstitialAdManager
import com.smartscan.pro.ui.navigation.AppTab
import com.smartscan.pro.ui.navigation.SmartScanBottomBar
import com.smartscan.pro.ui.screens.AiAssistantScreen
import com.smartscan.pro.ui.screens.DocumentsScreen
import com.smartscan.pro.ui.screens.HomeScreen
import com.smartscan.pro.ui.screens.ScanScreen
import com.smartscan.pro.ui.screens.SettingsScreen
import com.smartscan.pro.ui.screens.ToolsScreen
import com.smartscan.pro.ui.theme.SmartScanPrefs
import com.smartscan.pro.ui.theme.SmartScanTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var interstitialAdManager: InterstitialAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        interstitialAdManager = InterstitialAdManager(applicationContext).apply {
            loadAd()
        }

        setContent {
            var themeMode by remember { mutableStateOf(SmartScanPrefs.getThemeMode(this)) }
            var biometricEnabled by remember { mutableStateOf(SmartScanPrefs.isBiometricEnabled(this)) }
            var apiKey by remember { mutableStateOf(SmartScanPrefs.getAiApiKey(this)) }

            SmartScanTheme(themeMode = themeMode) {
                SmartScanRootApp(
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
    themeMode: com.smartscan.pro.ui.theme.ThemeMode,
    biometricEnabled: Boolean,
    apiKey: String,
    onThemeChange: (com.smartscan.pro.ui.theme.ThemeMode) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onTriggerAdThenAction: (action: () -> Unit) -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                    onOpenScanTab = { navigateToTab(AppTab.SCAN) },
                    onOpenToolsTab = { navigateToTab(AppTab.TOOLS) },
                    onQuickAction = { actionName ->
                        onTriggerAdThenAction {
                            showNotice("$actionName engine Phase 3-4 mein connect hoga.")
                        }
                    }
                )
            }

            composable(AppTab.SCAN.route) {
                ScanScreen(
                    onLaunchCamera = { mode ->
                        showNotice("CameraX + ML Kit ($mode) Phase 3 mein activate hoga.")
                    },
                    onImportGallery = {
                        showNotice("Gallery import + Auto Crop Phase 3 mein activate hoga.")
                    }
                )
            }

            composable(AppTab.DOCUMENTS.route) {
                DocumentsScreen(
                    onOpenScanTab = { navigateToTab(AppTab.SCAN) }
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
                        onTriggerAdThenAction {
                            showNotice("$toolTitle module agle phase mein live connect hoga.")
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
