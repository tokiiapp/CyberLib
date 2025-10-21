package com.cyber.ads.admob

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.Window
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.cyber.ads.R
import com.cyber.ads.adjust.AdjustUtils
import com.cyber.ads.admob.NativeHelper.Companion.populateNativeAdView
import com.cyber.ads.admob.NativeHelper.Companion.populateNativeAdViewCollap
import com.cyber.ads.admob.NativeHelper.Companion.populateNativeAdViewFull
import com.cyber.ads.cmp.GoogleMobileAdsConsentManager
import com.cyber.ads.custom.LoadingSize
import com.cyber.ads.remote.BannerHolder
import com.cyber.ads.remote.InterHolder
import com.cyber.ads.remote.NativeHolder
import com.cyber.ads.remote.NativeMultiHolder
import com.cyber.ads.remote.RewardHolder
import com.cyber.ads.remote.SplashHolder
import com.cyber.ads.utils.Helper
import com.cyber.ads.utils.Helper.parseUnitIds
import com.cyber.ads.utils.dpToPx
import com.cyber.ads.utils.gone
import com.cyber.ads.utils.invisible
import com.cyber.ads.utils.prefs
import com.cyber.ads.utils.visible
import com.facebook.FacebookSdk
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

object AdmobUtils {
    private var dialogFullScreen: Dialog? = null

    @JvmStatic
    var lastTimeShowInterstitial: Long = 0

    @JvmStatic
    var THROTTLE_MILLIS: Long = 3000L

    var timeOut = 10000

    @JvmField
    var isAdShowing = false

    @JvmField
    var isEnableAds = false

    @JvmField
    var isPremium = false

    @JvmField
    var isTesting = true
    internal var isInitialized = false
    internal var isConsented = false
    private var testDevices: MutableList<String> = mutableListOf()

    @JvmField
    var mBannerCollapView: AdView? = null
    var mRewardedAd: RewardedAd? = null

    //    var mRewardedInterstitialAd: RewardedInterstitialAd? = null
    var mInterstitialAd: InterstitialAd? = null
    var shimmerFrameLayout: ShimmerFrameLayout? = null
    private var adRequest: AdRequest? = null
    private var refreshJob: Job? = null
    private var splashSuccess = false

    @JvmStatic
    fun setupCMP(activity: Activity, onCompleted: () -> Unit) {
        if (isConsented) return
        isConsented = true
        val googleMobileAdsConsentManager = GoogleMobileAdsConsentManager(activity)
        googleMobileAdsConsentManager.gatherConsent { error ->
            if (error != null || googleMobileAdsConsentManager.canRequestAds) {
                FacebookSdk.setIsDebugEnabled(isTesting)
                FacebookSdk.setAdvertiserIDCollectionEnabled(true)
                onCompleted()
            }
        }
    }

    @JvmStatic
    fun initAdmob(activity: Activity, isDebug: Boolean) {
        if (isInitialized) return
        isInitialized = true
        isTesting = isDebug
        isEnableAds = Helper.enableAds()
        MobileAds.initialize(activity) { }
        initListIdTest()
        val requestConfiguration = RequestConfiguration.Builder().setTestDeviceIds(testDevices).build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        initAdRequest(timeOut)
        OnResumeUtils.init(activity)
    }

    @JvmStatic
    private fun initAdRequest(timeOut: Int) {
        adRequest = AdRequest.Builder().setHttpTimeoutMillis(timeOut).build()
    }

    private fun initListIdTest() {
//        testDevices.add("D4A597237D12FDEC52BE6B2F15508BB")
    }

    @JvmStatic
    fun isNetworkConnected(context: Context): Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
        } catch (_: Exception) {
            return false
        }
    }

    @JvmStatic
    fun loadAndShowAdSplash(activity: AppCompatActivity, holder: SplashHolder, callback: InterCallback) {
        Helper.parseSplash(holder)
        splashSuccess = false
        if (!isEnableAds || !isNetworkConnected(activity) || isPremium) {
            logE("Not EnableAds or No Internet")
            callback.onInterFailed("")
        }

        fun showAOA() {
            AOAUtils(activity, holder, 20000, object : AOAUtils.AoaCallback {
                override fun onAdsClose() {
                    splashSuccess = true
                    callback.onInterClosed()
                }

                override fun onAdsFailed(message: String) {
                    callback.onInterFailed(message)
                }

                override fun onAdsLoaded() {
                    callback.onInterLoaded()
                }

            }).loadAndShowAoa()
        }

        if (holder.enable != "0" && activity.adOrg()) {
            showAOA()
            return
        }

        val newCallback = object: InterCallback() {
            override fun onInterClosed() {
                splashSuccess = true
                callback.onInterClosed()
            }

            override fun onInterFailed(error: String) {
                callback.onInterFailed(error)
            }

        }


        when (holder.enable) {
            "1" -> showAOA()

            "2" -> performLoadAndShowInterstitial(activity, holder, newCallback)

            "3" -> performLoadAndShowInterWithNative(activity, holder, newCallback)

            "4" -> performLoadAndShowNativeInter(activity, holder, newCallback, false)

            else -> callback.onInterFailed("Not show AOA")

        }
    }

    @JvmStatic
    fun loadAndShowBanner(
        activity: AppCompatActivity,
        holder: BannerHolder,
        viewGroup: ViewGroup,
        callback: BannerCallback,
        callBack: NativeCallback
    ) {
        Helper.parseBanner(holder)

        if (holder.enable != "0" && activity.adOrg()) {
            performLoadAndShowBanner(activity, holder, viewGroup, callback)
            return
        }

        when (holder.enable) {
            "1" -> performLoadAndShowBanner(activity, holder, viewGroup, callback)  //* Banner

            "2" -> performLoadAndShowBannerCollap(activity, holder, viewGroup, callback) //* Banner Collap

            "3" -> performLoadAndShowNative(activity, viewGroup, holder, callBack) //* Native Small

            "4" -> performLoadAndShowNativeCollap(activity, holder, viewGroup, callBack) //* Native Collap

            else -> {
                viewGroup.gone()
                callback.onBannerFailed("")
            }
        }
    }

    @JvmStatic
    fun loadInterstitial(context: Context, holder: InterHolder, callback: LoadInterCallback) {
        Helper.parseInter(holder)
        if (holder.enable == "0") {
            callback.onInterFailed("")
        } else {
            performLoadInterstitial(context, holder, callback)
        }
    }

    @JvmStatic
    fun showInterstitial(activity: AppCompatActivity, holder: InterHolder, callback: InterCallback) {
        Helper.parseInter(holder)
        destroyBannerCollapView()
        isAdShowing = false

        if (holder.enable == "0") {
            OnResumeUtils.setEnableOnResume(true)
            callback.onInterFailed("")
        } else {
            holder.count++
            val stepCount = if (activity.adOrg()) 3 else holder.stepCount
            if (stepCount == 0 || holder.count % stepCount != 0) {
                logE("Inter count: ${holder.count}")
                callback.onInterFailed("")
            } else {
                performShowInterstitial(activity, holder, callback)
            }
        }
    }

    @JvmStatic
    fun loadAndShowReward(activity: AppCompatActivity, holder: RewardHolder, callback: RewardCallback) {
        Helper.parseReward(holder)
        mRewardedAd = null
        isAdShowing = false
        if (holder.enable == "0" || !isEnableAds || holder.rewardIds.isEmpty() || activity.adOrg()) {
            callback.onRewardEarned()
            callback.onRewardClosed()
        } else {
            performLoadAndShowReward(activity, holder, callback)
        }
    }

//    @JvmStatic
//    fun loadReward(context: Context, holder: AdmobHolder, callback: LoadRewardCallback) {
//        val remoteValue = RemoteUtils.getValue("reward_${holder.uid}", holder.versionCode)
//        if (!isEnableAds || !isNetworkConnected(context) || remoteValue == "0") {
//            callback.onRewardFailed("Not load reward")
//            return
//        }
//        if (holder.rewardInter.value != null) {
//            Log.d("+===Admob", "mInterstitialRewardAd not null")
//            return
//        }
//        if (adRequest == null) {
//            initAdRequest(timeOut)
//        }
//        holder.isRewardLoading = true
//        val adId = if (isTesting) {
//            context.logId("reward_${holder.uid}")
//            context.getString(R.string.test_admob_reward_id)
//        } else {
//            RemoteUtils.getAdId("reward_${holder.uid}")
//        }
//        RewardedAd.load(context, adId, adRequest!!, object : RewardedAdLoadCallback() {
//            override fun onAdLoaded(interstitialRewardAd: RewardedAd) {
//                holder.reward.value = interstitialRewardAd
//                holder.isRewardLoading = false
//                callback.onRewardLoaded()
//                Log.i("adLog", "onAdLoaded")
//            }
//
//            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
//                holder.isRewardLoading = false
//                holder.reward.value = null
//                callback.onRewardFailed(loadAdError.message)
//            }
//        })
//    }

//    @JvmStatic
//    fun showReward(activity: Activity, holder: AdmobHolder, callback: RewardCallback) {
//        val remoteValue = RemoteUtils.getValue("reward_${holder.uid}", holder.versionCode)
//        if (!isEnableAds || !isNetworkConnected(activity) || remoteValue == "0") {
//            if (OnResumeUtils.getInstance().isInitialized) {
//                OnResumeUtils.getInstance().isOnResumeEnable = true
//            }
//            callback.onRewardFailed("Not show reward")
//            return
//        }
//
//        if (OnResumeUtils.getInstance().isInitialized) {
//            if (!OnResumeUtils.getInstance().isOnResumeEnable) {
//                return
//            } else {
//                isAdShowing = false
//                if (OnResumeUtils.getInstance().isInitialized) {
//                    OnResumeUtils.getInstance().isOnResumeEnable = false
//                }
//            }
//        }
//
//        if (adRequest == null) {
//            initAdRequest(timeOut)
//        }
//        CoroutineScope(Dispatchers.Main).launch {
//            if (holder.isRewardLoading) {
//                dialogLoading(activity)
//                delay(800)
//
//                holder.reward.observe(activity as LifecycleOwner) { reward: RewardedAd? ->
//                    reward?.let {
//                        holder.reward.removeObservers((activity as LifecycleOwner))
//                        it.setOnPaidEventListener { value ->
//                            AdjustUtils.postRevenueAdjust(value, it.adUnitId)
//                        }
//                        holder.reward.value?.fullScreenContentCallback = object : FullScreenContentCallback() {
//                            override fun onAdDismissedFullScreenContent() {
////                                        mInterstitialRewardAd.inter = null
//                                holder.reward.removeObservers((activity as LifecycleOwner))
//                                holder.reward.value = null
//                                if (OnResumeUtils.getInstance().isInitialized) {
//                                    OnResumeUtils.getInstance().isOnResumeEnable = true
//                                }
//                                isAdShowing = false
//                                dismissAdDialog()
//                                callback.onRewardClosed()
//                                Log.d("+===Admob", "The ad was dismissed.")
//                            }
//
//                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                                holder.reward.removeObservers((activity as LifecycleOwner))
//                                holder.reward.value = null
//                                if (OnResumeUtils.getInstance().isInitialized) {
//                                    OnResumeUtils.getInstance().isOnResumeEnable = true
//                                }
//                                isAdShowing = false
//                                dismissAdDialog()
//                                callback.onRewardFailed(adError.message)
//                                Log.d("+===Admob", "The ad failed to show.")
//                            }
//
//                            override fun onAdShowedFullScreenContent() {
//                                isAdShowing = true
//                                callback.onRewardShowed()
//                                Log.d("+===Admob", "The ad was shown.")
//                            }
//                        }
//                        it.show(activity) { callback.onRewardEarned() }
//                    }
//                }
//            } else {
//                if (holder.reward.value != null) {
//                    dialogLoading(activity)
//                    delay(800)
//
//                    holder.reward.value?.setOnPaidEventListener {
//                        AdjustUtils.postRevenueAdjust(it, holder.reward.value?.adUnitId)
//                    }
//                    holder.reward.value?.fullScreenContentCallback = object : FullScreenContentCallback() {
//                        override fun onAdDismissedFullScreenContent() {
//                            holder.reward.removeObservers((activity as LifecycleOwner))
//                            holder.reward.value = null
//                            if (OnResumeUtils.getInstance().isInitialized) {
//                                OnResumeUtils.getInstance().isOnResumeEnable = true
//                            }
//                            isAdShowing = false
//                            dismissAdDialog()
//                            callback.onRewardClosed()
//                            log("Admob: Reward was dismissed.")
//                        }
//
//                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                            holder.reward.removeObservers((activity as LifecycleOwner))
//                            holder.reward.value = null
//                            if (OnResumeUtils.getInstance().isInitialized) {
//                                OnResumeUtils.getInstance().isOnResumeEnable = true
//                            }
//                            isAdShowing = false
//                            dismissAdDialog()
//                            callback.onRewardFailed(adError.message)
//                            logE("Admob: Reward failed to show.")
//                        }
//
//                        override fun onAdShowedFullScreenContent() {
//                            isAdShowing = true
//                            callback.onRewardShowed()
//                            log("Reward was shown.")
//                        }
//                    }
//                    holder.reward.value?.show(activity) { callback.onRewardEarned() }
//
//                } else {
//                    isAdShowing = false
//                    callback.onRewardFailed("None Show")
//                    dismissAdDialog()
//                    if (OnResumeUtils.getInstance().isInitialized) {
//                        OnResumeUtils.getInstance().isOnResumeEnable = true
//                    }
//                    logE("Reward did not load")
//                }
//            }
//        }
//    }

    @JvmStatic
    fun loadNative(context: Context, holder: NativeHolder, callback: NativeCallback) {
        Helper.parseNative(holder)
        if (holder.enable == "0" || context.adOrg()) {
            callback.onNativeFailed("")
        } else {
            performLoadNative(context, holder, callback)
        }
    }

    @JvmStatic
    fun showNative(activity: Activity, holder: NativeHolder, viewGroup: ViewGroup, callback: NativeCallbackSimple) {
        Helper.parseNative(holder)
        if (holder.enable == "0" || activity.adOrg()) {
            viewGroup.gone()
        } else {
            performShowNative(activity, viewGroup, holder, callback)
        }
    }

//    @JvmStatic
//    fun loadAndShowNativeCustom(activity: Activity, holder: NativeHolder, viewGroup: ViewGroup, callback: NativeCallback) {
//        Parser.parseNative(holder)
//        when (holder.enable) {
//            "0" -> viewGroup.gone()
//            "1" -> performLoadAndShowNative(activity, viewGroup, holder, callback)
//        }
//    }

    @JvmStatic
    fun loadAndShowNative(activity: AppCompatActivity, holder: NativeHolder, viewGroup: ViewGroup, callback: NativeCallback) {
        Helper.parseNative(holder)
        if (activity.adOrg()) {
            viewGroup.gone()
            callback.onNativeFailed("")
            return
        }
        when (holder.enable) {
            "0" -> viewGroup.gone()
            "1" -> performLoadAndShowNative(activity, viewGroup, holder, callback)
        }
    }

    @JvmStatic
    fun loadAndShowNativeCollap(activity: AppCompatActivity, holder: NativeHolder, viewGroup: ViewGroup, callback: NativeCallback) {
        Helper.parseNative(holder)
        if (activity.adOrg()) {
            viewGroup.gone()
            return
        }
        if (holder.enable == "0") {
            callback.onNativeFailed("Not show Native Collap")
            viewGroup.gone()
        } else {
            performLoadAndShowNativeCollap(activity, holder, viewGroup, callback)
        }
    }

    @JvmStatic
    fun loadNativeFull(context: Context, holder: NativeHolder, callback: NativeCallback) {
        Helper.parseNative(holder)
        if (holder.enable == "0" || context.adOrg()) {
            callback.onNativeFailed("Not show native")
        } else {
            performLoadNativeFull(context, holder, callback)
        }
    }

    @JvmStatic
    fun showNativeFull(activity: Activity, holder: NativeHolder, viewGroup: ViewGroup, callback: NativeCallbackSimple) {
        Helper.parseNative(holder)
        if (holder.enable == "0" || activity.adOrg()) {
            callback.onNativeFailed("Not show native")
            viewGroup.gone()
        } else {
            performShowNativeFull(activity, viewGroup, holder, callback)
        }
    }

    @JvmStatic
    fun loadAndShowNativeFull(activity: Activity, viewGroup: ViewGroup, holder: NativeHolder, callback: NativeCallbackSimple) {
        Helper.parseNative(holder)
        if (holder.enable == "0" || activity.adOrg()) {
            viewGroup.gone()
            callback.onNativeFailed("Not show Native Full")
        } else {
            performLoadAndShowNativeFull(activity, viewGroup, holder, callback)
        }
    }

    @JvmStatic
    fun loadNativeLanguage(context: Context, holder: NativeMultiHolder, callback: NativeCallback) {
        Helper.parseNativeMulti(holder)
        val obj = Helper.jsonObject()?.get(holder.key)?.asJsonObject ?: return
        log("loadNativeLanguage: ${holder.enable} ${holder.key}")
        if (holder.enable == "0" || context.adOrg()) {
            log("loadNativeLanguage: disabled")
        } else {
            val nativeLanguage1 = NativeHolder("${holder.key}1")
            nativeLanguage1.nativeTemplate = holder.nativeTemplate
            nativeLanguage1.loadingSize = holder.loadingSize
            holder.holders.add(nativeLanguage1)
            obj["native_language1"]?.asJsonObject?.let { nativeLanguage1.nativeUnit = parseUnitIds(it) }

            val nativeLanguage2 = NativeHolder("${holder.key}2")
            holder.holders.add(nativeLanguage2)
            obj["native_language2"]?.asJsonObject?.let { nativeLanguage2.nativeUnit = parseUnitIds(it) }

            performLoadNative(context, holder.holders[0], callback)
            performLoadNative(context, holder.holders[1], callback)
        }
    }

    @JvmStatic
    fun showNativeLanguage(activity: Activity, holder: NativeMultiHolder, viewGroup: ViewGroup, index: Int, callback: NativeCallbackSimple) {
        log("holder language: $holder")
        if (holder.enable == "0" || activity.adOrg()) {
            logE("Native language disabled")
            viewGroup.gone()
        } else {
            val nativeHolder = holder.holders.getOrNull(index)
            if (nativeHolder == null) {
                logE("NativeHolder null index=$index")
                callback.onNativeFailed("")
                return
            }
            if (index == 1) nativeHolder.customLayout(R.layout.native_template_medium_language)
            performShowNative(activity, viewGroup, nativeHolder, callback)
        }
    }

    @JvmStatic
    fun loadNativeIntro(context: Context, holder: NativeMultiHolder, callback: NativeCallback) {
        Helper.parseNativeMulti(holder)
        val obj = Helper.jsonObject()?.get(holder.key)?.asJsonObject ?: return
        if (holder.enable == "0" || context.adOrg()) {
            logE("NativeIntro disabled")
        } else {
            val nativeIntro1 = NativeHolder("${holder.key}1")
            holder.holders.add(nativeIntro1)
            obj["native_intro1"]?.asJsonObject?.let { nativeIntro1.nativeUnit = parseUnitIds(it) }

            val nativeIntro2 = NativeHolder("${holder.key}2")
            holder.holders.add(nativeIntro2)
            obj["native_intro2"]?.asJsonObject?.let { nativeIntro2.nativeUnit = parseUnitIds(it) }

            val nativeIntro3 = NativeHolder("${holder.key}3")
            holder.holders.add(nativeIntro3)
            obj["native_intro3"]?.asJsonObject?.let { nativeIntro3.nativeUnit = parseUnitIds(it) }

            holder.holders.forEach {
                it.nativeTemplate = holder.nativeTemplate
                it.loadingSize = holder.loadingSize
            }

            if (holder.enable.contains("1")) performLoadNative(context, nativeIntro1, callback)
            if (holder.enable.contains("2")) performLoadNative(context, nativeIntro2, callback)
            if (holder.enable.contains("3")) performLoadNative(context, nativeIntro3, callback)
        }
    }

    @JvmStatic
    fun showNativeIntro(
        activity: Activity,
        holder: NativeMultiHolder,
        viewGroup: ViewGroup,
        index: Int,
        callback: NativeCallbackSimple
    ) {
        if (holder.enable == "0" || activity.adOrg()) {
            logE("showNativeIntros: NativeIntro disabled")
            return
        }
        val nativeHolder = holder.holders.getOrNull(index - 1) ?: run {
            logE("NativeIntro is null index=$index")
            callback.onNativeFailed("")
            return
        }

        if (holder.enable.contains(index.toString())) {
            performShowNative(activity, viewGroup, nativeHolder, callback)
        }
    }

    @JvmStatic
    fun loadAndShowInterstitial(activity: AppCompatActivity, holder: InterHolder, onFinished: () -> Unit) {
        Helper.parseInter(holder)
        if (isNativeInterShowing(activity)) {
            logE("Native Inter is showing")
            return
        }
        if (!isEnableAds || !isNetworkConnected(activity)) {
            onFinished()
            return
        }
        holder.count++
        val stepCount = if (activity.adOrg()) 3 else holder.stepCount
        if (stepCount == 0 || holder.count % stepCount != 0) {
            log("stepCount = $stepCount, count = ${holder.count}")
            onFinished()
            return
        }

        val splashFallback = Helper.settings()?.get("splash_fallback")?.asString
        if(splashSuccess && holder.key == splashFallback?.lowercase()) {
            logE("${holder.key} disabled as splash fallback")
            onFinished()
            return
        }

        runCatching {
            val tag = "native_full_view"
            val decorView = activity.window.decorView as ViewGroup
            decorView.findViewWithTag<View>(tag)?.let { decorView.removeView(it) }
        }
        val callback = object : InterCallback() {
            override fun onInterClosed() {
                onFinished()
            }

            override fun onInterFailed(error: String) {
                onFinished()
            }
        }

        destroyBannerCollapView()
        if (activity.adOrg()) {
            performLoadAndShowInterstitial(activity, holder, callback)
            return
        }
        when (holder.enable) {
            "1" -> performLoadAndShowInterstitial(activity, holder, callback)

            "2" -> performLoadAndShowNativeInter(activity, holder, callback)

            "3" -> performLoadAndShowInterWithNative(activity, holder, callback)

            else -> {
                logE("loadAndShowInter: inter is disabled")
                onFinished()
            }
        }

    }

    @JvmStatic
    fun dismissAdDialog() {
        runCatching {
            dialogFullScreen?.takeIf { it.isShowing }?.dismiss()
        }
    }

    @JvmStatic
    fun isNativeInterShowing(activity: Activity): Boolean {
        runCatching {
            val decorView = activity.window.decorView as ViewGroup
            val tag = "native_full_view"
            val nativeView = decorView.findViewWithTag<View>(tag)
            return if (nativeView != null && nativeView.isVisible) {
                logE("Native Inter is showing")
                true
            } else {
                false
            }
        }
        return false
    }


    //* ========================Private Internal Functions======================== */

    private fun destroyBannerCollapView() {
        runCatching {
            mBannerCollapView?.destroy()
            (mBannerCollapView?.parent as? ViewGroup)?.removeView(mBannerCollapView)
        }.onFailure {
            logE("destroyBannerCollapView: ${it.message}")
        }
    }

    private fun performLoadAndShowNativeInter(
        activity: AppCompatActivity,
        holder: NativeHolder,
        callback: InterCallback,
        showShimmer: Boolean = true
    ) {
        if (isPremium) {
            callback.onInterClosed()
            return
        }
        val container = activity.layoutInflater.inflate(R.layout.layout_native_inter_container, null, false)
        val viewGroup = container.findViewById<FrameLayout>(R.id.viewGroup)
        val btnClose = container.findViewById<View>(R.id.ad_close)
        val tvTimer = container.findViewById<TextView>(R.id.ad_timer)

        val tag = "native_full_view"
        val decorView: ViewGroup
        try {
            decorView = activity.window.decorView as ViewGroup
            decorView.findViewWithTag<View>(tag)?.let { decorView.removeView(it) }
            container.tag = tag
            if (showShimmer) decorView.addView(container)
        } catch (e: Exception) {
            logE("Native Inter: ${e.message}")
            callback.onInterFailed(e.message.toString())
            return
        }

        container.visible()
        OnResumeUtils.setEnableOnResume(false)
        tvTimer.gone()
        btnClose.invisible()
        btnClose.setOnClickListener {
            OnResumeUtils.setEnableOnResume(true)
            container.gone()
            runCatching { decorView.removeView(container) }
            callback.onInterClosed()
        }

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            container.gone()
            runCatching { decorView.removeView(container) }
            logE("Native Inter Timeout")
            callback.onInterFailed("")
        }, 15000) //* Timeout 15s for loading NativeFull

        performLoadAndShowNativeFull(activity, viewGroup, holder, object : NativeCallbackSimple() {
            override fun onNativeLoaded() {
                if (!showShimmer) {
                    runCatching {
                        decorView.addView(container)
                    }.onFailure {
                        callback.onInterFailed("Native Inter failed to addView")
                    }
                }
                btnClose.visible()
                handler.removeCallbacksAndMessages(null)
            }

            override fun onNativeFailed(error: String) {
                handler.removeCallbacksAndMessages(null)
                container.gone()
                runCatching { decorView.removeView(container) }
                OnResumeUtils.setEnableOnResume(true)
                callback.onInterFailed(error)
            }
        })
    }

    private fun performLoadAndShowInterWithNative(activity: AppCompatActivity, holder: InterHolder, callback: InterCallback) {
        if (isPremium) {
            callback.onInterClosed()
            return
        }
        val container = activity.layoutInflater.inflate(R.layout.layout_native_inter_container, null, false)
        val viewGroup = container.findViewById<FrameLayout>(R.id.viewGroup)
        val btnClose = container.findViewById<View>(R.id.ad_close)
        val tvTimer = container.findViewById<TextView>(R.id.ad_timer)

        val tag = "native_full_view"
        val decorView: ViewGroup
        try {
            decorView = activity.window.decorView as ViewGroup
            decorView.findViewWithTag<View>(tag)?.let { decorView.removeView(it) }
            container.tag = tag
            decorView.addView(container)
        } catch (e: Exception) {
            logE("Native Inter: ${e.message}")
            callback.onInterFailed(e.message.toString())
            return
        }
        container.visible()
//                OnResumeUtils.getInstance().isOnResumeEnable = false
        tvTimer.gone()
        btnClose.invisible()
        btnClose.setOnClickListener {
            OnResumeUtils.setEnableOnResume(true)
            container.gone()
            runCatching { decorView.removeView(container) }
            callback.onInterClosed()
        }

        performLoadNativeFull(activity, holder, object : NativeCallback() {
            override fun onNativeReady(ad: NativeAd?) {
            }

            override fun onNativeFailed(error: String) {
            }

            override fun onNativeClicked() {
            }

        })
        performLoadAndShowInterstitial(activity, holder, object : InterCallback() {
            override fun onInterClosed() {
                if (holder.isNativeReady()) {
                    if (holder.waitTime > 0) {
                        activity.lifecycleScope.launch(Dispatchers.Main) {
                            tvTimer.visible()
                            val timeOut = holder.waitTime
                            for (i in timeOut downTo 0) {
                                tvTimer.text = i.toString()
                                delay(1000)
                            }
                            tvTimer.gone()
                            tvTimer.text = timeOut.toString()
                            delay(1000)
                            btnClose.visible()
                        }
                    } else {
                        btnClose.visible()
                    }

                    destroyBannerCollapView()
                    performShowNativeFull(activity, viewGroup, holder, object : NativeCallbackSimple() {
                        override fun onNativeLoaded() {
                        }

                        override fun onNativeFailed(error: String) {
                            container.gone()
                            runCatching { decorView.removeView(container) }
                            OnResumeUtils.setEnableOnResume(true)
                            holder.nativeAd.removeObservers(activity)
                            holder.nativeAd.value = null
                            callback.onInterFailed(error)
                        }

                    })
                } else {
                    container.gone()
                    runCatching { decorView.removeView(container) }
                    OnResumeUtils.setEnableOnResume(true)
                    holder.nativeAd.removeObservers(activity)
                    holder.nativeAd.value = null
                    logE("Native Inter not ready")
                    callback.onInterFailed("")
                }
            }

            override fun onInterFailed(error: String) {
                onInterClosed()
            }

        })
    }

    private fun performLoadInterstitial(context: Context, holder: InterHolder, callback: LoadInterCallback) {
        isAdShowing = false
        if (!isEnableAds || !isNetworkConnected(context) || isPremium) {
            logE("Not EnableAds or No Internet")
            callback.onInterFailed("")
            return
        }
        if (holder.isInterLoading) {
            logE("Inter still loading")
            return
        }
        if (holder.inter.value != null) {
            logE("Inter not null")
//            holder.inter.value = null
            return
        }
        holder.isInterLoading = true
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        tryLoadInter(context, holder, callback)
    }

    private fun tryLoadInter(context: Context, holder: InterHolder, callback: LoadInterCallback, index: Int = 0) {
        val adIds = holder.interIds
        if (index >= adIds.size) { //* Failed
            isAdShowing = false
            mInterstitialAd = null
            holder.isInterLoading = false
            holder.inter.value = null
            callback.onInterFailed("")
            return
        }

        val adId = adIds[index]
        log("Loading Inter: ${holder.key}/$adId")
        InterstitialAd.load(context, adId, adRequest!!, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                log("InterLoaded")
                holder.inter.value = interstitialAd
                holder.isInterLoading = false
                interstitialAd.setOnPaidEventListener {
                    AdjustUtils.postRevenueAdjust(context, it, interstitialAd.adUnitId)
                }
                callback.onInterLoaded(interstitialAd, false)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logE("InterLoadFailed: ${loadAdError.message}")
                tryLoadInter(context, holder, callback, index + 1)
            }
        })
    }

    private fun performShowInterstitial(activity: AppCompatActivity, holder: InterHolder, callback: InterCallback) {
        if (isPremium) {
            callback.onInterClosed()
            return
        }
        if (!isEnableAds || !isNetworkConnected(activity)) {
            logE("Not EnableAds or No Internet")
            callback.onInterFailed("")
            return
        }
        callback.onInterLoaded()
        val handler = Handler(Looper.getMainLooper())

        //* Set timeout showing inter
        val runnable = Runnable {
            if (holder.isInterLoading) {
                OnResumeUtils.setEnableOnResume(true)
                holder.inter.removeObservers((activity as LifecycleOwner))
                holder.inter.value = null
                isAdShowing = false
                dismissAdDialog()
                logE("ShowInter timeout")
                callback.onInterFailed("timeout")
            }
        }
        handler.postDelayed(runnable, 10000)

        //Inter is Loading...
        if (holder.isInterLoading) {
            if (holder.showLoading) {
                dialogLoading(activity)
            }
            holder.inter.observe((activity as LifecycleOwner)) { interstitialAd: InterstitialAd? ->
                if (interstitialAd != null) {
                    holder.inter.removeObservers((activity as LifecycleOwner))
                    Handler(Looper.getMainLooper()).postDelayed({
                        log("showing inter...")
                        interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                isAdShowing = false
                                OnResumeUtils.setEnableOnResume(true)
                                holder.inter.removeObservers((activity as LifecycleOwner))
                                holder.inter.value = null
                                callback.onInterClosed()
                                dismissAdDialog()
                                log("InterDismissedFullScreenContent")
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                logE("InterFailedToLoad" + adError.message)
                                isAdShowing = false
                                OnResumeUtils.setEnableOnResume(true)
                                dismissAdDialog()
                                holder.inter.removeObservers((activity as LifecycleOwner))
                                holder.inter.value = null
                                handler.removeCallbacksAndMessages(null)
                                callback.onInterFailed(adError.message)
                            }

                            override fun onAdShowedFullScreenContent() {
                                handler.removeCallbacksAndMessages(null)
                                isAdShowing = true
                                callback.onInterShowed()

                            }
                        }
                        implementShowInterstitial(activity, interstitialAd, callback)
                    }, 400)
                } else {
                    holder.isInterLoading = true
                }
            }
            return
        }
        //Load inter done
        if (holder.inter.value == null) {
            logE("inter null (maybe errorCodeAds null)")
            isAdShowing = false
            OnResumeUtils.setEnableOnResume(true)
            callback.onInterFailed("")
            handler.removeCallbacksAndMessages(null)
        } else {
            if (holder.showLoading) {
                dialogLoading(activity)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                log("showing inter...")
                holder.inter.value?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        isAdShowing = false
                        OnResumeUtils.setEnableOnResume(true)
                        holder.inter.removeObservers((activity as LifecycleOwner))
                        holder.inter.value = null
                        dismissAdDialog()
                        callback.onInterClosed()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        logE("InterFailedToLoad" + adError.message)
                        isAdShowing = false
                        OnResumeUtils.setEnableOnResume(true)
                        handler.removeCallbacksAndMessages(null)
                        holder.inter.value = null
                        holder.inter.removeObservers((activity as LifecycleOwner))
                        dismissAdDialog()
                        callback.onInterFailed(adError.message)
                    }

                    override fun onAdShowedFullScreenContent() {
                        handler.removeCallbacksAndMessages(null)
                        isAdShowing = true
                        callback.onInterShowed()
                    }
                }
                implementShowInterstitial(activity, holder.inter.value, callback)
            }, 400)
        }
    }

    private fun implementShowInterstitial(activity: AppCompatActivity, interstitialAd: InterstitialAd?, callback: InterCallback?) {
        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && interstitialAd != null) {
            isAdShowing = true
            Handler(Looper.getMainLooper()).postDelayed({
                callback?.onStartAction()
                interstitialAd.show(activity)
                dismissAdDialog()
            }, 400)
        } else {
            logE("App is in background")
            isAdShowing = false
            OnResumeUtils.setEnableOnResume(true)
            dismissAdDialog()
            callback?.onInterFailed("")
        }
    }

    private fun performLoadAndShowInterstitial(activity: Activity, holder: InterHolder, callback: InterCallback) {
        mInterstitialAd = null
        if (isPremium) {
            callback.onInterClosed()
            return
        }
        if (!isEnableAds || !isNetworkConnected(activity) || holder.isInterLoading) {
            logE("Not EnableAds or No Internet or Inter still loading")
            callback.onInterFailed("")
            return
        }
        isAdShowing = false
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        if (OnResumeUtils.isInitialized) {
            if (!OnResumeUtils.isOnResumeEnable) {
                logE("OnResume is disabled??")
                callback.onInterFailed("")
                return
            } else {
                isAdShowing = false
                OnResumeUtils.setEnableOnResume(false)
            }
        }

        if (holder.showLoading) {
            dialogLoading(activity)
        }

        holder.isInterLoading = true
        tryLoadAndShowInter(activity, holder, callback)
    }

    private fun tryLoadAndShowInter(activity: Activity, holder: InterHolder, callback: InterCallback, index: Int = 0) {
        val adIds = holder.interIds
        if (index >= adIds.size) { //* Failed
            holder.isInterLoading = false
            mInterstitialAd = null
            OnResumeUtils.setEnableOnResume(true)
            isAdShowing = false
            dismissAdDialog()
            callback.onInterFailed("")
            return
        }

        val adId = adIds[index]
        log("Loading Inter ${holder.key}/$adId")
        InterstitialAd.load(activity, adId, adRequest!!, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                super.onAdLoaded(interstitialAd)
                log("Inter Loaded")
                holder.isInterLoading = false
                callback.onInterLoaded()
                Handler(Looper.getMainLooper()).postDelayed({
                    mInterstitialAd = interstitialAd
                    mInterstitialAd!!.onPaidEventListener = OnPaidEventListener { adValue: AdValue? ->
                        adValue?.let {
                            AdjustUtils.postRevenueAdjust(activity, it, interstitialAd.adUnitId)
                        }
                    }
                    mInterstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            logE("InterFailedToShowFullScreen" + adError.message)
                            callback.onInterFailed(adError.message)
                            isAdShowing = false
                            OnResumeUtils.setEnableOnResume(true)
                            isAdShowing = false
                            if (mInterstitialAd != null) {
                                mInterstitialAd = null
                            }
                            dismissAdDialog()
                        }

                        override fun onAdDismissedFullScreenContent() {
                            lastTimeShowInterstitial = Date().time
                            callback.onInterClosed()
                            if (mInterstitialAd != null) {
                                mInterstitialAd = null
                            }
                            isAdShowing = false
                            OnResumeUtils.setEnableOnResume(true)
                        }

                        override fun onAdShowedFullScreenContent() {
                            super.onAdShowedFullScreenContent()
                            logE("onInterShowedFullScreenContent")
                            callback.onInterShowed()
                            Handler(Looper.getMainLooper()).postDelayed({
                                dismissAdDialog()
                            }, 800)
                        }
                    }
                    if ((activity as AppCompatActivity).lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && mInterstitialAd != null) {
                        callback.onStartAction()
                        mInterstitialAd!!.show(activity)
                        dismissAdDialog()
                        isAdShowing = true
                    } else {
                        logE("Interstitial can't show in background")
                        mInterstitialAd = null
                        dismissAdDialog()
                        isAdShowing = false
                        OnResumeUtils.setEnableOnResume(true)
                        callback.onInterFailed("")
                    }
                }, 800)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                logE("InterFailed: ${loadAdError.message}")
                tryLoadAndShowInter(activity, holder, callback, index + 1)
            }
        })
    }

    private fun dialogLoading(activity: Activity) {
        if (dialogFullScreen?.isShowing == true) {
            return
        }
        dialogFullScreen = Dialog(activity)
        dialogFullScreen?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogFullScreen?.setContentView(R.layout.dialog_loading_interstitial)
        dialogFullScreen?.setCancelable(false)
        dialogFullScreen?.window?.setBackgroundDrawable(Color.WHITE.toDrawable())
        dialogFullScreen?.window?.setLayout(MATCH_PARENT, MATCH_PARENT)
        val img = dialogFullScreen?.findViewById<LottieAnimationView>(R.id.imageView3)
        img?.setAnimation(R.raw.loading)
        if (!activity.isFinishing) {
            dialogFullScreen?.show()
        }
    }

    private fun aggregateErr(ad: NativeAd?) {
        if (!isEnableAds || isTesting) return
        val enable = Helper.settings()?.get("ads_enable")?.asString
        if (enable == "2" || enable == "3") {
            try {
                val x = ad?.headline.toString().replace(" ", "").split(":")[0]
                val y = Helper.aggr()
                isEnableAds = !y.contains(x)
            } catch (e: Exception) {
                isEnableAds = false
            }
        }
    }

    private fun performLoadAndShowBanner(activity: AppCompatActivity, holder: BannerHolder, viewGroup: ViewGroup, callback: BannerCallback) {
        if (System.currentTimeMillis() - holder.loadTimestamp < THROTTLE_MILLIS) return
        if (!isEnableAds || !isNetworkConnected(activity) || isPremium) {
            viewGroup.gone()
            logE("Not EnableAds or No Internet")
            callback.onBannerFailed("")
            return
        }
        val tagView = activity.layoutInflater.inflate(R.layout.layout_banner_loading, null, false)
        runCatching {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(bannerDivider(activity, holder.anchor))
        }
        viewGroup.visible()
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()

        holder.loadTimestamp = System.currentTimeMillis()
        tryLoadAndShowBanner(activity, holder, viewGroup, callback)
    }

    private fun tryLoadAndShowBanner(activity: AppCompatActivity, holder: BannerHolder, viewGroup: ViewGroup, callback: BannerCallback, index: Int = 0) {
        val adIds = holder.bannerIds
        if (index >= adIds.size) { //* Failed
            shimmerFrameLayout?.stopShimmer()
            viewGroup.removeAllViews()
            viewGroup.gone()
            callback.onBannerFailed("")
            return
        }

        val adId = adIds[index]
        val mAdView = AdView(activity).apply {
            adUnitId = adId
            val adSize = getBannerSize(activity)
            setAdSize(adSize)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    log("Banner Loaded")
                    onPaidEventListener = OnPaidEventListener { adValue ->
                        AdjustUtils.postRevenueAdjust(activity, adValue, adUnitId)
                    }
                    shimmerFrameLayout?.stopShimmer()
                    viewGroup.removeAllViews()
                    viewGroup.addView(this@apply)
                    viewGroup.addView(bannerDivider(activity, holder.anchor))
                    callback.onBannerLoaded(adSize)
                    postRefreshDelayed(activity, holder) {
                        performLoadAndShowBanner(activity, holder, viewGroup, callback)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    logE("BannerFailed: ${error.message}")
                    tryLoadAndShowBanner(activity, holder, viewGroup, callback, index + 1)
                }
            }
        }
        adRequest?.let {
            log("Loading Banner ${holder.key}/$adId")
            mAdView.loadAd(it)
        }
    }

    private fun performLoadAndShowBannerCollap(activity: AppCompatActivity, holder: BannerHolder, viewGroup: ViewGroup, callback: BannerCallback) {
        if (System.currentTimeMillis() - holder.loadTimestamp < THROTTLE_MILLIS) return
        if (!isEnableAds || !isNetworkConnected(activity) || isPremium) {
            viewGroup.gone()
            logE("Not EnableAds or No Internet")
            callback.onBannerFailed("")
            return
        }

        runCatching {
            mBannerCollapView?.let {
                it.destroy()
                viewGroup.removeView(it)
            }
            if (isNativeInterShowing(activity)) {
                viewGroup.gone()
                callback.onBannerFailed("Native Inter is showing")
                return
            }
        }

        val tagView = activity.layoutInflater.inflate(R.layout.layout_banner_loading, null, false)

        runCatching {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(bannerDivider(activity, holder.anchor))
        }
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()
        viewGroup.visible()

        holder.loadTimestamp = System.currentTimeMillis()
        tryLoadBannerCollap(activity, holder, viewGroup, callback)
    }

    private fun tryLoadBannerCollap(activity: AppCompatActivity, holder: BannerHolder, viewGroup: ViewGroup, callback: BannerCallback, index: Int = 0) {
        val adIds = holder.bannerIds
        if (index >= adIds.size) { //* Failed
            shimmerFrameLayout?.stopShimmer()
            viewGroup.removeAllViews()
            viewGroup.gone()
            callback.onBannerFailed("")
            return
        }

        val adId = adIds[index]
        val adSize = getBannerSize(activity)
        val adView = AdView(activity).apply {
            this.adUnitId = adId
            setAdSize(adSize)
        }

        mBannerCollapView = adView
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                log("BannerCollap Loaded")
                adView.onPaidEventListener = OnPaidEventListener { adValue ->
                    AdjustUtils.postRevenueAdjust(activity, adValue, adView.adUnitId)
                }
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeAllViews()
                if (!isNativeInterShowing(activity)) {
                    viewGroup.addView(adView)
                    viewGroup.addView(bannerDivider(activity, holder.anchor))
                    callback.onBannerLoaded(adSize)
                    postRefreshDelayed(activity, holder) {
                        performLoadAndShowBannerCollap(activity, holder, viewGroup, callback)
                    }
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                logE("BannerCollapFailedToLoad: ${adError.message}")
                tryLoadBannerCollap(activity, holder, viewGroup, callback, index + 1)
            }

            override fun onAdOpened() {}
            override fun onAdClicked() {
                callback.onBannerClicked()
            }

            override fun onAdClosed() {}

            override fun onAdImpression() {
                if (isNativeInterShowing(activity)) {
                    adView.destroy()
                }
            }
        }

        val extras = Bundle().apply { putString("collapsible", holder.anchor) }
        val request = AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            .build()
        adView.loadAd(request)
        log("Loading BannerCollap ${holder.key}/$adId")
    }

    private fun bannerDivider(activity: Context, anchor: String): View = View(activity).apply {
        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, 1.dpToPx(activity)).apply {
            if (anchor == "top") gravity = Gravity.BOTTOM
        }
        setBackgroundColor(ContextCompat.getColor(activity, R.color.banner_divider_color))
    }

    private fun getBannerSize(activity: Activity): AdSize {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    private fun postRefreshDelayed(activity: AppCompatActivity, holder: NativeHolder, doRefresh: () -> Unit) {
        val refreshRate = holder.refreshRate
        log("refresh rate: $refreshRate")
        refreshJob?.cancel()
        refreshJob = activity.lifecycleScope.launch(Dispatchers.Main) {
            if (refreshRate <= 0 || !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@launch
            delay(refreshRate * 1000L)
            if (isNativeInterShowing(activity) || !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return@launch
            if (OnResumeUtils.isShowingAd) {
                postRefreshDelayed(activity, holder, doRefresh)
                return@launch
            }
            log("do refreshing...")
            doRefresh()
            refreshJob?.cancel()
        }
    }

    private fun performLoadAndShowReward(activity: AppCompatActivity, holder: RewardHolder, callback: RewardCallback) {
        if (isPremium) {
            callback.onRewardEarned()
            callback.onRewardClosed()
            return
        }
        if (!isNetworkConnected(activity)) {
            callback.onRewardFailed("Network error")
            return
        }
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        if (holder.showLoading) {
            dialogLoading(activity)
        }
        OnResumeUtils.setEnableOnResume(false)
        tryLoadReward(activity, holder, callback)
    }

    private fun tryLoadReward(activity: AppCompatActivity, holder: RewardHolder, callback: RewardCallback, index: Int = 0) {
        val adIds = holder.rewardIds
        if (index >= adIds.size) { //* Failed
            mRewardedAd = null
            isAdShowing = false
            dismissAdDialog()
            callback.onRewardFailed("")
            OnResumeUtils.setEnableOnResume(true)
            return
        }

        val adId = adIds[index]
        log("Loading Reward: ${holder.key}/$adId")
        RewardedAd.load(activity, adId, adRequest!!, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logE("RewardFailed: ${loadAdError.message}")
                tryLoadReward(activity, holder, callback, index + 1)
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                log("Reward Loaded")
                mRewardedAd = rewardedAd
                mRewardedAd?.setOnPaidEventListener {
                    AdjustUtils.postRevenueAdjust(activity, it, rewardedAd.adUnitId)
                }
                mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdShowedFullScreenContent() {
                        isAdShowing = true
                        callback.onRewardShowed()
                        OnResumeUtils.setEnableOnResume(false)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                        if (adError.code != 1) {
                        isAdShowing = false
                        callback.onRewardFailed(adError.message)
                        mRewardedAd = null
                        dismissAdDialog()
//                        }
                        OnResumeUtils.setEnableOnResume(true)
                        logE("RewardFailedToShowFullScreenContent: ${adError.message} - ${adError.cause}")
                    }

                    override fun onAdDismissedFullScreenContent() {
                        mRewardedAd = null
                        isAdShowing = false
                        dismissAdDialog()
                        callback.onRewardClosed()
                        OnResumeUtils.setEnableOnResume(true)
                    }
                }
                if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    OnResumeUtils.setEnableOnResume(false)
                    mRewardedAd?.show(activity) {
                        mRewardedAd = null
                        callback.onRewardEarned()
                    }
                    isAdShowing = true
                } else {
                    logE("Reward can't show in background")
                    mRewardedAd = null
                    dismissAdDialog()
                    isAdShowing = false
                    OnResumeUtils.setEnableOnResume(true)
                }
            }
        })
    }

    private fun performLoadNative(context: Context, holder: NativeHolder, callback: NativeCallback) {
        if (!isEnableAds || !isNetworkConnected(context) || isPremium) {
            logE("Not EnableAds or No Internet")
            callback.onNativeFailed("")
            return
        }

        if (holder.isNativeLoading) {
            logE("Native still loading")
            return
        }
        if (holder.nativeAd.value != null) {
            logE("Native not null")
//            return
        }
        holder.isNativeLoading = true
//        val videoOptions = VideoOptions.Builder().setStartMuted(false).build()
        CoroutineScope(Dispatchers.IO).launch {
            tryLoadNative(context, holder, callback)
        }
    }

    private fun tryLoadNative(context: Context, holder: NativeHolder, callback: NativeCallback, index: Int = 0) {
        val adIds = holder.nativeIds
        if (index >= adIds.size) { //* Failed
            Handler(Looper.getMainLooper()).post {
                holder.isNativeLoading = false
                holder.nativeAd.value = null
                callback.onNativeFailed("")
            }
            return
        }

        val adId = adIds[index]
        val adLoader = AdLoader.Builder(context, adId).forNativeAd { nativeAd ->
            log("Native Loaded")
            holder.isNativeLoading = false
            holder.nativeAd.value = nativeAd
            nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                adValue?.let {
                    AdjustUtils.postRevenueAdjust(context, adValue, adId)
                }
            }
            aggregateErr(nativeAd)
            holder.currentAdId = adId
            callback.onNativeReady(nativeAd)
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                logE("NativeFailedToLoad: ${adError.message}")
                tryLoadNative(context, holder, callback, index + 1)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback.onNativeClicked()
            }
        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        adRequest?.let {
            log("Loading Native ${holder.key}/$adId")
            adLoader.loadAd(it)
        }
    }

    private fun performShowNative(activity: Activity, viewGroup: ViewGroup, holder: NativeHolder, callback: NativeCallbackSimple) {
        if (!isEnableAds || !isNetworkConnected(activity) || isPremium) {
            logE("Not EnableAds or No Internet")
            viewGroup.gone()
            return
        }
        shimmerFrameLayout?.stopShimmer()
        runCatching { viewGroup.removeAllViews() }
        val layoutId = nativeTemplateId(holder)

        if (!holder.isNativeLoading) {
            if (holder.nativeAd.value != null) {
                val adView = activity.layoutInflater.inflate(layoutId, null) as NativeAdView
                populateNativeAdView(holder.nativeAd.value!!, adView)
                holder.nativeAd.removeObservers((activity as LifecycleOwner))
//                holder.nativeAd.value = null
                runCatching {
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                    log("showing Native: ${holder.key}/${holder.currentAdId}")
                }

                callback.onNativeLoaded()
            } else {
                viewGroup.gone()
                holder.nativeAd.removeObservers((activity as LifecycleOwner))
                holder.nativeAd.value = null
                callback.onNativeFailed("None Show")
            }
        } else {
            val loadingLayout = nativeLoadingId(holder)

            val tagView = activity.layoutInflater.inflate(loadingLayout, null, false)
            runCatching {
                viewGroup.addView(tagView, 0)
            }
            if (shimmerFrameLayout == null) {
                shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
                shimmerFrameLayout?.startShimmer()
            }
            holder.nativeAd.observe((activity as LifecycleOwner)) { nativeAd ->
                if (nativeAd != null) {
                    nativeAd.setOnPaidEventListener {
                        AdjustUtils.postRevenueAdjust(activity, it, holder.currentAdId)
                    }
                    val adView = activity.layoutInflater.inflate(layoutId, null) as NativeAdView
                    populateNativeAdView(nativeAd, adView)
                    shimmerFrameLayout?.stopShimmer()
                    runCatching {
                        viewGroup.removeAllViews()
                        viewGroup.addView(adView)
                        log("showing Native: ${holder.key}/${holder.currentAdId}")
                    }

                    callback.onNativeLoaded()
                    holder.nativeAd.removeObservers((activity as LifecycleOwner))
//                    holder.nativeAd.value = null
                } else {
                    logE("NativeAd is null")
                    shimmerFrameLayout?.stopShimmer()
                    viewGroup.gone()
                    callback.onNativeFailed("")
                    holder.nativeAd.removeObservers((activity as LifecycleOwner))
                    holder.nativeAd.value = null
                }
            }
        }
    }

    private fun performLoadAndShowNative(context: AppCompatActivity, viewGroup: ViewGroup, holder: NativeHolder, callback: NativeCallback) {
        if (System.currentTimeMillis() - holder.loadTimestamp < THROTTLE_MILLIS) return
        if (!isEnableAds || !isNetworkConnected(context) || isPremium) {
            logE("Not EnableAds or No Internet")
            viewGroup.gone()
            return
        }

        val loadingLayout = nativeLoadingId(holder)
        val tagView = LayoutInflater.from(context).inflate(loadingLayout, null, false)
        runCatching {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
        }

        val shimmerFrameLayout = tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        holder.loadTimestamp = System.currentTimeMillis()
        tryLoadAndShowNative(context, holder, viewGroup, callback)
    }

    private fun nativeTemplateId(holder: NativeHolder): Int {
        if (holder.layoutId != null) {
            return holder.layoutId!!
        }
        val layoutId = when (holder.nativeTemplate.lowercase()) {
            "big2" -> R.layout.native_template_big2
            "big3" -> R.layout.native_template_big3
            "big4" -> R.layout.native_template_big4
            "big5" -> R.layout.native_template_big5
            "small1" -> R.layout.native_template_small1
            "small2" -> R.layout.native_template_small2
            "small3" -> R.layout.native_template_small3
            "small4" -> R.layout.native_template_small4
            "small5" -> R.layout.native_template_small5
            "tiny1" -> R.layout.native_template_tiny1
            "tiny2" -> R.layout.native_template_tiny2
            else -> R.layout.native_template_big1
        }
        return layoutId
    }

    private fun nativeLoadingId(holder: NativeHolder) = when (holder.loadingSize) {
        LoadingSize.TINY -> R.layout.layout_banner_loading
        LoadingSize.SMALL -> R.layout.layout_native_loading_small
        else -> R.layout.layout_native_loading_medium
    }

    private fun tryLoadAndShowNative(activity: AppCompatActivity, holder: NativeHolder, viewGroup: ViewGroup, callback: NativeCallback, index: Int = 0) {
        val adIds = holder.nativeIds
        if (index >= adIds.size) { //* Failed
            runCatching {
                viewGroup.removeAllViews()
                viewGroup.gone()
            }
            callback.onNativeFailed("")
            return
        }

        val adId = adIds[index]
        val adLoader = AdLoader.Builder(activity, adId).forNativeAd { nativeAd ->
            log("Native Loaded")
            callback.onNativeReady(nativeAd)

            val layoutId = nativeTemplateId(holder)
            val adView = LayoutInflater.from(activity).inflate(layoutId, null) as NativeAdView
            populateNativeAdView(nativeAd, adView)
            nativeAd.setOnPaidEventListener { adValue: AdValue ->
                AdjustUtils.postRevenueAdjust(activity, adValue, adId)
            }

            aggregateErr(nativeAd)
            runCatching {
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
            }
            postRefreshDelayed(activity, holder) {
                performLoadAndShowNative(activity, viewGroup, holder, callback)
            }
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                logE("NativeFailedToLoad ${adError.message} - ${adError.cause}")
                tryLoadAndShowNative(activity, holder, viewGroup, callback, index + 1)
            }

        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()

        adRequest?.let {
            log("Loading Native ${holder.key}/$adId")
            adLoader.loadAd(it)
        } ?: run { logE("adRequest is NULL") }
    }

    private fun performLoadAndShowNativeCollap(activity: AppCompatActivity, holder: NativeHolder, viewGroup: ViewGroup, callback: NativeCallback) {
        if (System.currentTimeMillis() - holder.loadTimestamp < THROTTLE_MILLIS) return
        val tag = "native_collap_view"
        runCatching {
            val decorView = activity.window.decorView as ViewGroup
            decorView.findViewWithTag<View>(tag)?.let { decorView.removeView(it) }
        }

        if (!isEnableAds || !isNetworkConnected(activity) || isPremium) {
            viewGroup.gone()
            logE("Not EnableAds or No Internet")
            callback.onNativeFailed("")
            return
        }
        if (isNativeInterShowing(activity)) {
            viewGroup.gone()
            logE("NativeInter is showing")
            callback.onNativeFailed("")
            return
        }
//        val videoOptions = VideoOptions.Builder().setStartMuted(false).setCustomControlsRequested(false).build()
        val tagView = activity.layoutInflater.inflate(R.layout.layout_banner_loading, null, false)
        runCatching {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
        }

        viewGroup.visible()
        val shimmerFrameLayout = tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        holder.loadTimestamp = System.currentTimeMillis()
        tryLoadAndShowNativeCollap(activity, holder, viewGroup, callback)
    }

    private fun tryLoadAndShowNativeCollap(activity: AppCompatActivity, holder: NativeHolder, viewGroup: ViewGroup, callback: NativeCallback, index: Int = 0) {
        val adIds = holder.nativeIds
        if (index >= adIds.size) { //* Failed
            runCatching {
                viewGroup.removeAllViews()
                viewGroup.gone()
            }
            callback.onNativeFailed("")
            return
        }

        val adId = adIds[index]
        val decorView = activity.window.decorView as ViewGroup
        val tag = "native_collap_view"
        val adLoader = AdLoader.Builder(activity, adId).forNativeAd { nativeAd ->
            log("NativeCollap Loaded")
            callback.onNativeReady(nativeAd)
            val adViewCollap = activity.layoutInflater.inflate(R.layout.native_template_collap, null) as NativeAdView
            adViewCollap.tag = tag
            nativeAd.setOnPaidEventListener { adValue: AdValue ->
                AdjustUtils.postRevenueAdjust(activity, adValue, adId)
            }
            aggregateErr(nativeAd)
            populateNativeAdViewCollap(nativeAd, adViewCollap, holder.anchor) {
                runCatching { //* On icon collapse clicked
                    val configList = holder.collapConfig ?: listOf()
                    val clickCount = activity.prefs().getInt("collap_click_count", 0) + 1
                    val currentStepIndex = activity.prefs().getInt("collap_step_index", 0)

                    log("NativeCollap Clicked: collapConfig=$configList || index=$currentStepIndex || clickCount=$clickCount")
                    if (currentStepIndex >= configList.size) {
                        logE("No config => Skip counting")
                        decorView.removeView(adViewCollap)
                        return@runCatching
                    }

                    activity.prefs().edit { putInt("collap_click_count", clickCount) }
                    if (clickCount >= configList[currentStepIndex]) {
                        activity.prefs().edit {
                            putInt("collap_step_index", currentStepIndex + 1)
                            putInt("collap_click_count", 0)
                        }
                        adViewCollap.findViewById<View>(R.id.ad_call_to_action).performClick()
                    } else {
                        decorView.removeView(adViewCollap)
                    }
                }
            }
            runCatching {
                viewGroup.removeAllViews()
                val layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    gravity = if (holder.anchor == "top") Gravity.TOP else Gravity.BOTTOM
                }
                if (!isNativeInterShowing(activity)) {
                    decorView.addView(adViewCollap, layoutParams)
                    val adViewSmall = activity.layoutInflater.inflate(R.layout.native_template_tiny1, null) as NativeAdView
                    populateNativeAdView(nativeAd, adViewSmall)
                    viewGroup.addView(adViewSmall)
                    postRefreshDelayed(activity, holder) {
                        performLoadAndShowNativeCollap(activity, holder, viewGroup, callback)
                    }
                }
            }.onFailure {
                logE(it.message.toString())
            }

        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                logE("NativeCollapFailedToLoad ${adError.message} - ${adError.cause}")
                tryLoadAndShowNativeCollap(activity, holder, viewGroup, callback, index + 1)
            }

            override fun onAdClicked() {
                super.onAdClicked()
                activity.prefs().edit { putInt("collap_click_count", 0) }
            }
        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        adRequest?.let {
            log("Loading Native ${holder.key}/$adId")
            adLoader.loadAd(it)
        }
    }

    private fun performLoadAndShowNativeFull(activity: Activity, viewGroup: ViewGroup, holder: NativeHolder, callback: NativeCallbackSimple) {
        if (!isEnableAds || !isNetworkConnected(activity) || isPremium) {
            viewGroup.gone()
            logE("Not EnableAds or No Internet")
            callback.onNativeFailed("")
            return
        }
        val tagView = activity.layoutInflater.inflate(R.layout.layout_native_loading_full, null, false)
        runCatching {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
        }

        OnResumeUtils.setEnableOnResume(false)
        viewGroup.visible()
        viewGroup.isClickable = true
        val shimmerFrameLayout = tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        tryLoadAndShowNativeFull(activity, holder, viewGroup, callback)
    }

    private fun tryLoadAndShowNativeFull(
        activity: Activity,
        holder: NativeHolder,
        viewGroup: ViewGroup,
        callback: NativeCallbackSimple,
        index: Int = 0
    ) {
        val adIds = holder.nativeIds
        if (index >= adIds.size) { //* Failed
            shimmerFrameLayout?.stopShimmer()
            viewGroup.gone()
            OnResumeUtils.setEnableOnResume(true)
            callback.onNativeFailed("")
            return
        }

        val adId = adIds[index]
        val adLoader = AdLoader.Builder(activity, adId).forNativeAd { nativeAd ->
            log("NativeFull Loaded")
            val adView = activity.layoutInflater.inflate(R.layout.native_template_full, null) as NativeAdView
            aggregateErr(nativeAd)
            nativeAd.setOnPaidEventListener {
                AdjustUtils.postRevenueAdjust(activity, it, adId)
            }
            callback.onNativeLoaded()
            populateNativeAdViewFull(nativeAd, adView)
            runCatching {
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
            }
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logE("NativeFullFailedToLoad ${loadAdError.message} - ${loadAdError.cause}")
                tryLoadAndShowNativeFull(activity, holder, viewGroup, callback, index + 1)
            }
        }).withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        adRequest?.let {
            log("Loading NativeFull ${holder.key}/$adId")
            adLoader.loadAd(it)
        } ?: run { logE("adRequest is NULL") }
    }

    private fun performLoadNativeFull(context: Context, holder: NativeHolder, callback: NativeCallback) {
        if (!isEnableAds || !isNetworkConnected(context) || isPremium) {
            logE("Not EnableAds or No Internet")
            callback.onNativeFailed("")
            return
        }
        if (holder.isNativeLoading) {
            logE("Native still loading")
            return
        }
        if (holder.nativeAd.value != null) {
            logE("Native not null")
//            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            tryLoadNativeFull(context, holder, callback)
        }
    }

    private fun tryLoadNativeFull(context: Context, holder: NativeHolder, callback: NativeCallback, index: Int = 0) {
        val adIds = holder.nativeIds
        if (index >= adIds.size) { //* Failed
            Handler(Looper.getMainLooper()).post {
                holder.isNativeLoading = false
                holder.nativeAd.value = null
                callback.onNativeFailed("")
            }
            return
        }

        val adId = adIds[index]
        holder.isNativeLoading = true
        val adLoader = AdLoader.Builder(context, adId)
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .forNativeAd { nativeAd ->
                log("NativeFull Loaded")
                holder.isNativeLoading = false
                holder.nativeAd.value = nativeAd
                holder.currentAdId = adId
                nativeAd.setOnPaidEventListener {
                    AdjustUtils.postRevenueAdjust(context, it, adId)
                }
                aggregateErr(nativeAd)
                callback.onNativeReady(nativeAd)
            }.withAdListener(object : AdListener() {
                override fun onAdClicked() {
                    super.onAdClicked()
                    callback.onNativeClicked()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    logE("NativeFullFailedToLoad ${adError.message} - ${adError.cause}")
                    tryLoadNativeFull(context, holder, callback, index + 1)
                }
            })
        adRequest?.let {
            log("Loading NativeFull ${holder.key}/$adId")
            adLoader.build().loadAd(it)
        }
    }

    private fun performShowNativeFull(context: Context, viewGroup: ViewGroup, holder: NativeHolder, callback: NativeCallbackSimple) {
        if (!isEnableAds || !isNetworkConnected(context) || isPremium) {
            logE("Not EnableAds or No Internet")
            viewGroup.gone()
            return
        }
        viewGroup.visible()
        val layout = R.layout.native_template_full
        viewGroup.removeAllViews()
        val inflater = LayoutInflater.from(context)
        if (!holder.isNativeLoading) {
            if (holder.nativeAd.value != null) {
                val adView = inflater.inflate(layout, null) as NativeAdView
                populateNativeAdViewFull(holder.nativeAd.value!!, adView)
                shimmerFrameLayout?.stopShimmer()
                holder.nativeAd.removeObservers((context as LifecycleOwner))
//                holder.nativeAd.value = null
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
                callback.onNativeLoaded()
            } else {
                shimmerFrameLayout?.stopShimmer()
                viewGroup.gone()
                holder.nativeAd.removeObservers((context as LifecycleOwner))
                holder.nativeAd.value = null
                logE("NativeFull is null")
                callback.onNativeFailed("")
//                OnResumeUtils.getInstance().isOnResumeEnable = true
            }
        } else {
            val tagView = inflater.inflate(R.layout.layout_native_loading_full, null, false)
            viewGroup.addView(tagView, 0)
            if (shimmerFrameLayout == null) shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
            shimmerFrameLayout?.startShimmer()

            holder.nativeAd.observe((context as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeAd.setOnPaidEventListener {
                        AdjustUtils.postRevenueAdjust(context, it, adUnit = holder.currentAdId)
                    }
                    val adView = inflater.inflate(layout, null) as NativeAdView
                    populateNativeAdViewFull(nativeAd, adView)

                    shimmerFrameLayout?.stopShimmer()
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)

                    callback.onNativeLoaded()
                    holder.nativeAd.removeObservers((context as LifecycleOwner))
//                    holder.nativeAd.value = null
                } else {
                    shimmerFrameLayout?.stopShimmer()
                    viewGroup.gone()
                    logE("NativeFull is null")
                    callback.onNativeFailed("")
//                    OnResumeUtils.getInstance().isOnResumeEnable = true
                    holder.nativeAd.removeObservers((context as LifecycleOwner))
                    holder.nativeAd.value = null
                }
            }
        }
    }

    private fun log(msg: String) {
        if (isTesting || Helper.enableReleaseLog) Log.d("AdmobUtils", msg)
    }

    private fun logE(msg: String) {
        if (isTesting || Helper.enableReleaseLog) Log.e("AdmobUtils", msg)
    }

    private fun Context.adOrg() = prefs().getBoolean("ads_ref_event", true) && !isTesting && Helper.settings()?.get("ads_enable")?.asString == "2"
//    private fun Context.adOrg() = prefs().getBoolean("ads_ref_event", true) && Helper.settings()?.get("ads_enable")?.asString == "2"

    abstract class InterCallback {
        open fun onStartAction() {}
        abstract fun onInterClosed()
        open fun onInterShowed() {}
        open fun onInterLoaded() {}
        abstract fun onInterFailed(error: String)
    }

    abstract class LoadInterCallback {
        open fun onInterLoaded(interstitialAd: InterstitialAd?, isLoading: Boolean) {}
        open fun onInterFailed(error: String) {}
    }

    abstract class BannerCallback {
        open fun onBannerClicked() {}
        open fun onBannerLoaded(adSize: AdSize) {}
        open fun onBannerFailed(error: String) {}
    }

    abstract class NativeCallbackSimple {
        open fun onNativeLoaded() {}
        open fun onNativeFailed(error: String) {}
    }

    abstract class NativeCallback {
        open fun onNativeReady(ad: NativeAd?) {}
        open fun onNativeFailed(error: String) {}
        open fun onNativeClicked() {}
    }

    abstract class RewardCallback {
        abstract fun onRewardClosed()
        open fun onRewardShowed() {}
        open fun onRewardFailed(error: String) {}
        abstract fun onRewardEarned()
    }

//    abstract class LoadRewardCallback {
//        open fun onRewardFailed(error: String) {}
//        open fun onRewardLoaded() {}
//    }

}
