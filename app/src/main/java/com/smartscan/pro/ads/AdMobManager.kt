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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.smartscan.pro.BuildConfig

class InterstitialAdManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var lastShownTimeMs = 0L

    companion object {
        // Policy safe gap: har 60 second se pehle dobara full-screen ad nahi dikhega
        private const val AD_COOLDOWN_MS = 60_000L
    }

    fun loadAd() {
        if (isLoading || interstitialAd != null) return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    fun showAdIfReady(
        activity: Activity,
        forceShow: Boolean = false,
        onComplete: () -> Unit
    ) {
        val now = SystemClock.elapsedRealtime()
        val cooldownPassed = (now - lastShownTimeMs) >= AD_COOLDOWN_MS
        val currentAd = interstitialAd

        if (currentAd != null && (forceShow || cooldownPassed)) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastShownTimeMs = SystemClock.elapsedRealtime()
                    loadAd()
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadAd()
                    onComplete()
                }
            }
            currentAd.show(activity)
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
                val metrics = ctx.resources.displayMetrics
                val adWidthDp = (metrics.widthPixels / metrics.density).toInt().coerceAtLeast(320)
                val adaptiveSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    ctx,
                    adWidthDp
                )

                AdView(ctx).apply {
                    adUnitId = BuildConfig.ADMOB_BANNER_ID
                    setAdSize(adaptiveSize)
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { adView ->
                adView.destroy()
            }
        )
    }
}
