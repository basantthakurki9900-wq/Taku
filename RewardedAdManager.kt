package com.example.ads

import android.app.Activity
import android.content.Context
import com.example.BuildConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Asli Unity Ads (Rewarded) manager.
 * Unity Dashboard: App "Basat", Game ID 800360381.
 */
object RewardedAdManager {
    const val UNITY_GAME_ID = "800360381"
    const val REWARDED_PLACEMENT_ID = "BP_Rewarded_Android"
    const val AD_REWARD = 10

    // Debug build (jo APK tum test karoge) mein TEST ads aate hain. Apne hi live ads par
    // click mat karna, Unity account ban ho sakta hai. Release build mein live ads.
    val TEST_MODE: Boolean = BuildConfig.DEBUG

    private var initStarted = false
    private var isInitialized = false
    private var loading = false

    private val _adStatus = MutableStateFlow("Ad initialize ho raha hai...")
    val adStatus: StateFlow<String> = _adStatus.asStateFlow()

    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    fun initialize(context: Context) {
        if (initStarted) return
        initStarted = true
        UnityAds.initialize(
            context.applicationContext,
            UNITY_GAME_ID,
            TEST_MODE,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    isInitialized = true
                    _adStatus.value = "Unity Ads ready"
                    loadAd()
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError?,
                    message: String?
                ) {
                    initStarted = false
                    _adStatus.value = "Ad init fail: ${message ?: error?.name ?: "unknown"}"
                }
            }
        )
    }

    fun loadAd() {
        if (!isInitialized || loading) return
        loading = true
        _isAdLoaded.value = false
        UnityAds.load(REWARDED_PLACEMENT_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                loading = false
                _isAdLoaded.value = true
                _adStatus.value = "Ad ready"
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                loading = false
                _isAdLoaded.value = false
                _adStatus.value = "Ad load fail: ${message ?: error?.name ?: "unknown"}"
            }
        })
    }

    /**
     * Ad dikhata hai. [onRewarded] tabhi chalta hai jab user ne ad poora dekha ho.
     * [onFailed] mein user ko dikhane layak message aata hai.
     */
    fun showAd(activity: Activity, onRewarded: () -> Unit, onFailed: (String) -> Unit) {
        if (!isInitialized) {
            initialize(activity)
            onFailed("Ad abhi initialize ho raha hai, thodi der baad try karo.")
            return
        }
        if (!_isAdLoaded.value) {
            loadAd()
            onFailed("Ad abhi load ho raha hai, kuch second baad try karo.")
            return
        }
        _isAdLoaded.value = false
        UnityAds.show(
            activity,
            REWARDED_PLACEMENT_ID,
            UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(
                    placementId: String?,
                    error: UnityAds.UnityAdsShowError?,
                    message: String?
                ) {
                    loadAd()
                    onFailed("Ad nahi chala: ${message ?: error?.name ?: "unknown"}")
                }

                override fun onUnityAdsShowStart(placementId: String?) {}

                override fun onUnityAdsShowClick(placementId: String?) {}

                override fun onUnityAdsShowComplete(
                    placementId: String?,
                    state: UnityAds.UnityAdsShowCompletionState?
                ) {
                    loadAd() // agla ad pehle se ready rakho
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        onRewarded()
                    } else {
                        onFailed("Ad skip kiya, isliye coins nahi mile.")
                    }
                }
            }
        )
    }
}
