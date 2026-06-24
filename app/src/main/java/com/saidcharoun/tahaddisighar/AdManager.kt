package com.saidcharoun.tahaddisighar

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * مدير الإعلانات المكافئة (Rewarded Ads).
 *
 * التطبيق موجّه لكل الأعمار (13+) وليس مصمّماً للأطفال، لذا:
 * - لا نفعّل معاملة المحتوى الموجّه للأطفال (child-directed = UNSPECIFIED).
 * - نحدّ تقييم محتوى الإعلانات إلى G لإبقائها نظيفة ومناسبة للجميع.
 */
object AdManager {

    private const val TAG = "AdManager"

    // معرّف الإعلان المكافئ الحقيقي (AdMob — حساب pub-7149494294947585)
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-7149494294947585/5028855577"

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    /** يُستدعى مرة واحدة عند بدء التطبيق. */
    fun initialize(context: Context) {
        // التطبيق ليس موجّهاً للأطفال؛ نُبقي محتوى الإعلانات بتقييم G (نظيف للجميع).
        val config = RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(
                RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
            )
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
            .build()
        MobileAds.setRequestConfiguration(config)
        MobileAds.initialize(context) { Log.d(TAG, "AdMob initialized") }
        loadAd(context)
    }

    /** تحميل إعلان مسبقاً ليكون جاهزاً عند الطلب. */
    fun loadAd(context: Context) {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                    Log.d(TAG, "Rewarded ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                    Log.w(TAG, "Rewarded ad failed: ${error.message}")
                }
            }
        )
    }

    /**
     * عرض الإعلان. عند انتهائه بنجاح يُستدعى [onReward].
     * إذا لم يكن هناك إعلان جاهز، نمنح المكافأة مباشرة حتى لا نُحبط الطفل.
     */
    fun showRewardedAd(activity: Activity, onReward: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            loadAd(activity)
            onReward() // لا يوجد إعلان جاهز — امنح المكافأة على أي حال
            return
        }
        ad.show(activity) { onReward() }
        rewardedAd = null
        loadAd(activity) // حمّل التالي
    }
}
