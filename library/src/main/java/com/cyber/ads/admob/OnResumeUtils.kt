package com.cyber.ads.admob

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Window
import android.widget.LinearLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.cyber.ads.R
import com.cyber.ads.adjust.AdjustUtils
import com.cyber.ads.admob.AdmobUtils.dismissAdDialog
import com.cyber.ads.remote.APP_OPEN_TEST_ID
import com.cyber.ads.solar.SolarUtils
import com.cyber.ads.utils.Helper
import com.cyber.ads.utils.Helper.enableOnResume
import com.cyber.ads.utils.Helper.onResumeId
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import java.lang.ref.WeakReference
import java.util.Date

object OnResumeUtils : ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private var appResumeAd: AppOpenAd? = null
    private var loadCallback: AppOpenAdLoadCallback? = null
    private var fullScreenContentCallback: FullScreenContentCallback? = null
    private var appResumeAdId: String? = null
    private var currentActivity: WeakReference<Activity>? = null
    private var myApplication: Application? = null
    private var appResumeLoadTime: Long = 0
    var isInitialized: Boolean = false
        private set
    var isOnResumeEnable: Boolean = true
    private val disabledAppOpenList = mutableListOf<Class<*>>()
    private var dialogFullScreen: Dialog? = null
    var isLoading: Boolean = false
    var isDismiss: Boolean = false
    private var disableNextResume: Boolean = false

    internal fun init(activity: Activity) {
        if (!enableOnResume() || !AdmobUtils.isEnableAds) {
            logE("Not EnableAds or No Internet")
            return
        }
        isInitialized = true
        this.myApplication = activity.application
        initAdRequest()
        disableOnResume(activity.javaClass)

        if (AdmobUtils.isTesting) {
            this.appResumeAdId = APP_OPEN_TEST_ID
        } else {
            this.appResumeAdId = onResumeId()
        }
        this.myApplication!!.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
//        if (!isAdAvailable() && appResumeAdId != null) {
//            fetchAd()
//        }
    }

    @JvmStatic
    fun setEnableOnResume(enable: Boolean) {
        isOnResumeEnable = enable && isInitialized
        log("isOnResumeEnable = $enable")
    }

    private var adRequest: AdRequest? = null

    private fun initAdRequest() {
        adRequest = AdRequest.Builder()
            .setHttpTimeoutMillis(5000)
            .build()
    }

    var isShowingAd: Boolean = false

    /**
     * Disable app open app on specific activity
     */
    @JvmStatic
    fun disableOnResume(activityClass: Class<*>?) {
        log("disableOnResume: " + activityClass?.getSimpleName())
        activityClass?.let { disabledAppOpenList.add(it) }
    }

    @JvmStatic
    fun disableNextResume() {
        disableNextResume = true
    }

    @JvmStatic
    fun enableOnResume(activityClass: Class<*>?) {
        log("enableOnResume: " + activityClass?.getSimpleName())
        Handler(Looper.getMainLooper()).postDelayed({ disabledAppOpenList.remove(activityClass) }, 40)
    }

    @JvmStatic
    fun setFullScreenContentCallback(callback: FullScreenContentCallback?) {
        this.fullScreenContentCallback = callback
    }

    private fun fetchAd() {
        if (AdmobUtils.isPremium) {
            return
        }
        if (!enableOnResume() || !AdmobUtils.isEnableAds || !AdmobUtils.isNetworkConnected(myApplication!!)) {
            logE("OnResume disabled or No Internet")
            return
        }
        if (isAdAvailable() || appResumeAdId == null) {
            logE("Ad unavailable or id = null")
            return
        }

        if (isLoading) {
            logE("Ad is loading")
            return
        }
        log("fetchAd $appResumeAdId")
        isLoading = true
        loadCallback = object : AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                log("onAdLoaded")
                appResumeAd = ad
                appResumeLoadTime = (Date()).time
                ad.setOnPaidEventListener {
                    SolarUtils.trackAdImpression(
                        ad = it,
                        adUnit = appResumeAdId,
                        format = "app_open"
                    )
                    AdjustUtils.postRevenueAdjust(myApplication!!, it, appResumeAdId)

                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                isLoading = false
                logE("onAdFailedToLoad: " + loadAdError.message)
            }
        }
        AppOpenAd.load(myApplication!!, appResumeAdId!!, adRequest!!, loadCallback!!)
    }

    private fun wasLoadTimeLessThanNHoursAgo(loadTime: Long, numHours: Long): Boolean {
        val dateDifference = (Date()).time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return (dateDifference < (numMilliSecondsPerHour * numHours))
    }

    private fun isAdAvailable(): Boolean {
        val loadTime = appResumeLoadTime
        val wasLoadTimeLessThanNHoursAgo = wasLoadTimeLessThanNHoursAgo(loadTime, 4)
//        log("wasLoadTimeLessThanNHoursAgo $wasLoadTimeLessThanNHoursAgo")
        return appResumeAd != null && wasLoadTimeLessThanNHoursAgo
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
        log("onActivityStarted: " + activity.javaClass.getSimpleName())
        currentActivity = WeakReference(activity)
    }

    override fun onActivityResumed(activity: Activity) {
//        log("onActivityResumed: ${activity.javaClass.simpleName}")
        currentActivity = WeakReference(activity)
        if (activity.javaClass.getName() != AdActivity::class.java.getName()) {
            fetchAd()
        }
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
        if (dialogFullScreen != null && dialogFullScreen!!.isShowing) {
            dialogFullScreen!!.dismiss()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        log("LifecycleOwner onStart ${currentActivity?.javaClass?.simpleName}")
        Handler(Looper.getMainLooper()).postDelayed({ checkAndShowOnResume(true) }, 30)
    }

    /**
     * ⚠️ DO NOT use this in Project unless you know what you are doing
     */
    @JvmStatic
    fun checkAndShowOnResumeNoLifecycle() {
        checkAndShowOnResume(false)
    }

    private fun checkAndShowOnResume(checkLifecycle: Boolean) {
        if (disableNextResume) {
            logE("Disable next OnResume")
            disableNextResume = false
            return
        }

        val curActivity = currentActivity?.get() ?: return
        if (curActivity.javaClass == AdActivity::class.java || AdmobUtils.isAdShowing || !AdmobUtils.isEnableAds || AdmobUtils.isPremium) {
            return
        }

        if (AdmobUtils.isNativeInterShowing(curActivity)) {
            logE("Native inter is showing => disable on_resume")
            return
        }

        //        if (ApplovinUtils.INSTANCE.isClickAds()) {
//            ApplovinUtils.INSTANCE.setClickAds(false);
//            return;
//        }
        for (javaClass in disabledAppOpenList) {
            if (javaClass.getName() == curActivity.javaClass.getName()) {
                logE(javaClass.getSimpleName() + " is disabled onResume")
                return
            }
        }

        if (!isOnResumeEnable) {
            log("enableOnResume: false")
            return
        } else {
            log("enableOnResume: true")
            dismissAdDialog()
        }
        showAppOpenAd(checkLifecycle)
    }

    private fun showAppOpenAd(checkLifecycle: Boolean) {
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && checkLifecycle) {
            logE("LifecycleOwner NOT STARTED")
            if (fullScreenContentCallback != null) {
                try {
                    dialogFullScreen!!.dismiss()
                    dialogFullScreen = null
                } catch (ignored: Exception) {
                }
                fullScreenContentCallback!!.onAdDismissedFullScreenContent()
            }
            return
        }
        if (!isShowingAd && isAdAvailable()) {
            isDismiss = true
            val callback: FullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Handler(Looper.getMainLooper()).postDelayed({
                            isDismiss = false
                        }, 200)
                        isLoading = false
                        try {
                            dialogFullScreen!!.dismiss()
                            dialogFullScreen = null
                        } catch (ignored: Exception) {
                        }
                        // Set the reference to null so isAdAvailable() returns false.
                        appResumeAd = null
                        if (fullScreenContentCallback != null) {
                            fullScreenContentCallback!!.onAdDismissedFullScreenContent()
                        }
                        isShowingAd = false
                        fetchAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        isLoading = false
                        isDismiss = false
                        logE("onAdFailedToShowFullScreenContent")
                        try {
                            dialogFullScreen!!.dismiss()
                            dialogFullScreen = null
                        } catch (ignored: Exception) {
                        }

                        if (fullScreenContentCallback != null) {
                            fullScreenContentCallback!!.onAdFailedToShowFullScreenContent(adError)
                        }
                        fetchAd()
                    }

                    override fun onAdShowedFullScreenContent() {
                        log("onAdShowedFullScreenContent")
                        isShowingAd = true
                        appResumeAd = null
                    }
                }
            showAdsResume(callback)
        } else {
            logE("OnResume is showing or not available")
            fetchAd()
        }
    }

    private fun showAdsResume(callback: FullScreenContentCallback?) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (appResumeAd != null) {
                appResumeAd!!.fullScreenContentCallback = callback
                if (currentActivity?.get() != null) {
                    showDialog(currentActivity!!.get()!!)
                    log("Showing OnResume...")
                    appResumeAd!!.show(currentActivity!!.get()!!)
                }
            }
        }, 100)
    }

    private fun log(msg: String) {
        if (AdmobUtils.isTesting || Helper.enableReleaseLog) Log.d("OnResumeUtils", msg)
    }

    private fun logE(msg: String) {
        if (AdmobUtils.isTesting || Helper.enableReleaseLog) Log.e("OnResumeUtils", msg)
    }

    private fun showDialog(context: Context) {
        dialogFullScreen = Dialog(context)
        dialogFullScreen!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogFullScreen!!.setContentView(R.layout.dialog_loading_on_resume)
        dialogFullScreen!!.setCancelable(false)
        dialogFullScreen!!.window!!.setBackgroundDrawable(Color.WHITE.toDrawable())
        dialogFullScreen!!.window!!.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        runCatching {
            if (!currentActivity!!.get()!!.isFinishing && dialogFullScreen != null && !dialogFullScreen!!.isShowing) {
                dialogFullScreen!!.show()
            }
        }
    }
}

