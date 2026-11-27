package com.smartscan.pro.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.adsbase.Ad
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener

class InterstitialAdManager(private val context: Context) {

    private val startAppAd = StartAppAd(context)
    private var lastShownTimeMs = 0L

    companion object {
        // Har 60 second se pehle dobara full-screen ad nahi dikhega
        private const val AD_COOLDOWN_MS = 60_000L
    }

    fun loadAd() {
        startAppAd.loadAd(StartAppAd.AdMode.AUTOMATIC)
    }

    fun showAdIfReady(
        activity: Activity,
        forceShow: Boolean = false,
        onComplete: () -> Unit
    ) {
        val now = SystemClock.elapsedRealtime()
        val cooldownPassed = (now - lastShownTimeMs) >= AD_COOLDOWN_MS

        if (forceShow || cooldownPassed) {
            startAppAd.showAd(object : AdDisplayListener {
                override fun adHidden(ad: Ad?) {
                    lastShownTimeMs = SystemClock.elapsedRealtime()
                    loadAd()
                    onComplete()
                }

                override fun adDisplayed(ad: Ad?) {
                    lastShownTimeMs = SystemClock.elapsedRealtime()
                }

                override fun adClicked(ad: Ad?) {}

                override fun adNotDisplayed(ad: Ad?) {
                    loadAd()
                    onComplete()
                }
            })
        } else {
            loadAd()
            onComplete()
        }
    }
}

@Composable
fun AdMobBannerBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                Banner(ctx)
            }
        )
    }
}
