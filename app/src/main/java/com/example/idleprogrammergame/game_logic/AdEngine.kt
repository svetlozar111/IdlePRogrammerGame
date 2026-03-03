package com.example.idleprogrammergame.game_logic

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.google.android.gms.ads.MobileAds

/**
 * Manages ad rewards and bonus activation using AdMob Rewarded Ads.
 * Uses test ad unit ID for development: ca-app-pub-3940256099942544/6300978111
 */
class AdEngine(private val gameEngine: GameEngine) {
    
    companion object {
        // Test ad unit ID for rewarded video ads
        const val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TAG = "AdEngine"
    }
    
    var currentRewardType by mutableStateOf(AdRewardType.DOUBLE_COINS_5_MIN)
    var isWatchingAd by mutableStateOf(false)
    var isAdLoading by mutableStateOf(false)
    var adError by mutableStateOf<String?>(null)
    
    private var rewardedAd: RewardedAd? = null
    private var pendingOnComplete: (() -> Unit)? = null
    private var activity: Activity? = null
    private var isAdLoaded = false
    
    fun initialize(context: Context) {
        MobileAds.initialize(context) { _ ->
            Log.d(TAG, "AdMob initialized")
            loadRewardedAd()
        }
    }
    
    fun setActivity(activity: Activity) {
        this.activity = activity
    }
    
    private fun loadRewardedAd() {
        val currentActivity = activity
        if (currentActivity == null) {
            Log.e(TAG, "Activity is null, cannot load ad")
            return
        }
        
        isAdLoading = true
        isAdLoaded = false
        
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            currentActivity,
            TEST_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Ad loaded successfully")
                    rewardedAd = ad
                    isAdLoading = false
                    isAdLoaded = true
                    adError = null
                    
                    setupFullScreenCallback(ad)
                }
                
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Ad failed to load: ${loadAdError.message}")
                    rewardedAd = null
                    isAdLoading = false
                    isAdLoaded = false
                    adError = loadAdError.message
                }
            }
        )
    }
    
    private fun setupFullScreenCallback(ad: RewardedAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed")
                rewardedAd = null
                isWatchingAd = false
                loadRewardedAd()
            }
            
            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "Ad failed to show: ${adError.message}")
                rewardedAd = null
                isWatchingAd = false
                // Give bonus anyway even if ad failed to show
                activateBonus()
                pendingOnComplete?.invoke()
                pendingOnComplete = null
                loadRewardedAd()
            }
            
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed full screen")
            }
        }
    }
    
    fun watchAd(onComplete: () -> Unit) {
        val currentActivity = activity
        
        if (isWatchingAd) {
            Log.d(TAG, "Already watching ad")
            return
        }
        
        if (currentActivity == null) {
            Log.e(TAG, "Activity is null")
            // Fallback
            activateBonus()
            onComplete()
            return
        }
        
        // Check if ad is already loaded
        if (rewardedAd != null && isAdLoaded) {
            Log.d(TAG, "Showing loaded ad")
            showAd(onComplete)
        } else {
            // Need to load ad first
            Log.d(TAG, "Loading ad first...")
            isAdLoading = true
            pendingOnComplete = onComplete
            
            val adRequest = AdRequest.Builder().build()
            
            RewardedAd.load(
                currentActivity,
                TEST_AD_UNIT_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Log.d(TAG, "Ad loaded after request")
                        rewardedAd = ad
                        isAdLoading = false
                        isAdLoaded = true
                        adError = null
                        
                        setupFullScreenCallback(ad)
                        showAd(pendingOnComplete ?: {})
                        pendingOnComplete = null
                    }
                    
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e(TAG, "Ad failed to load: ${loadAdError.message}")
                        rewardedAd = null
                        isAdLoading = false
                        isAdLoaded = false
                        adError = loadAdError.message
                        
                        // Fallback - give bonus anyway for testing
                        Log.d(TAG, "Giving fallback bonus")
                        activateBonus()
                        pendingOnComplete?.invoke()
                        pendingOnComplete = null
                    }
                }
            )
        }
    }
    
    private fun showAd(onComplete: () -> Unit) {
        val currentActivity = activity
        val ad = rewardedAd
        
        if (currentActivity == null || ad == null) {
            Log.e(TAG, "Cannot show ad - activity or ad is null")
            activateBonus()
            onComplete()
            return
        }
        
        isWatchingAd = true
        pendingOnComplete = onComplete
        
        ad.show(
            currentActivity,
            OnUserEarnedRewardListener { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount}")
                activateBonus()
                pendingOnComplete?.invoke()
                pendingOnComplete = null
            }
        )
    }
    
    private fun activateBonus() {
        gameEngine.activateBonus(currentRewardType)
    }
    
    fun getRandomRewardType(): AdRewardType {
        return AdRewardType.DOUBLE_COINS_5_MIN
    }
    
    fun hasActiveBonus(): Boolean {
        return gameEngine.hasActiveBonus()
    }
    
    fun getBonusTimeRemaining(): String {
        return gameEngine.getBonusTimeFormatted()
    }
}
