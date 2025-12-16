package com.cyber.ads.admob

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.cyber.ads.R
import com.cyber.ads.adjust.AdjustUtils
import com.cyber.ads.appsflyer.AppsFlyerUtils
import com.cyber.ads.remote.SplashHolder
import com.cyber.ads.solar.SolarUtils
import com.cyber.ads.utils.Helper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AOAUtils(private val activity: Activity, val holder: SplashHolder,val index:Int, val timeOut: Long, val callback: AoaCallback) {
    private var appOpenAd: AppOpenAd? = null
    var isShowingAd = true
    var isLoading = true
    var dialogFullScreen: Dialog? = null
    var isStart = true

    private val adRequest: AdRequest
        get() = AdRequest.Builder().build()

    private val isAdAvailable: Boolean
        get() = appOpenAd != null

    fun loadAndShowAoa() {
//        if (AdmobUtils.isPremium) {
//            callback.onAdsClose()
//            return
//        }
//        if (!AdmobUtils.isEnableAds || !AdmobUtils.isNetworkConnected(activity)) {
//            logE("Not EnableAds or No Internet")
//            callback.onAdsFailed("")
//            return
//        }
        //Check timeout show inter
        val job = CoroutineScope(Dispatchers.Main).launch {
            delay(timeOut)
            if (isLoading && isStart) {
                isStart = false
                isLoading = false
                onAOADestroyed()
                callback.onAdsFailed("")
                logE("TimeOut")
            }
        }
        if (isAdAvailable) {
            job.cancel()
            isShowingAd = false
            isLoading = true
            showAOA()
        } else {
            isShowingAd = false
            loadAoa(job,index)
        }
    }

    private fun loadAoa(job: Job, index: Int = 0) {
        val adIds = holder.appOpenIds
        if (index >= adIds.size) { //* Failed
            isLoading = false
            if (isStart) {
                isStart = false
                callback.onAdsFailed("")
            }
            job.cancel()
            return
        }

        val adId = adIds[index]
        log("Loading AOA... $adId")
        val loadStart = nowMs()
        AppOpenAd.load(activity, adId, adRequest, object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdFailedToLoad(p0: LoadAdError) {
                super.onAdFailedToLoad(p0)
                logE("onAdFailedToLoad ${p0.message} - ${p0.cause}")
                val latency = nowMs() - loadStart
                SolarUtils.trackAdLoadFailure(adUnit = adId, format = "app_open", loadAdError = p0, latencyMs = latency, waterfallIndex = index)
                //                loadAoa(job, index + 1)
                isLoading = false
                isShowingAd = false
                job.cancel()
                callback.onAdsFailed("")
            }

            override fun onAdLoaded(ad: AppOpenAd) {
                super.onAdLoaded(ad)
                log("onAdLoaded")
                appOpenAd = ad
                if (!AdmobUtils.splashSuccess) {
                    callback.onAdsLoaded()
                }
                job.cancel()
                if (!OnResumeUtils.isShowingAd && !isShowingAd && !AdmobUtils.splashSuccess) {
                    showAOA()
                }
            }
        })
    }

    private fun nowMs() = SystemClock.elapsedRealtime()
    private fun showAOA() {
        if (!isShowingAd && isAdAvailable && isLoading) {
            isLoading = false
            OnResumeUtils.setEnableOnResume(false)
            val fullScreenContentCallback: FullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    runCatching { dialogFullScreen?.dismiss() }
                    appOpenAd = null
                    isShowingAd = true
                    if (isStart) {
                        isStart = false
                        callback.onAdsClose()
                    }
                    OnResumeUtils.setEnableOnResume(true)
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    runCatching { dialogFullScreen?.dismiss() }
                    appOpenAd?.let { ad ->
                        SolarUtils.trackAdShowFailure(
                            adUnit = ad.adUnitId,
                            format = "app_open",
                            adError = p0
                        )
                    }
                    isShowingAd = true
                    if (isStart && !AdmobUtils.splashSuccess) {
                        isStart = false
                        callback.onAdsFailed(p0.message)
                        logE("onAdFailedToShowFullScreenContent ${p0.message} - ${p0.cause}")
                    }
                    OnResumeUtils.setEnableOnResume(true)
                }

                override fun onAdShowedFullScreenContent() {
                    log("onAdShowedFullScreenContent")
                    isShowingAd = true
                }
            }
            appOpenAd?.run {
                this.fullScreenContentCallback = fullScreenContentCallback
                var img: LottieAnimationView? = null
                // Không tạo dialog nếu là splash ads để tránh nháy màn hình
                if (holder.key != "ads_splash") {
                    dialogFullScreen = Dialog(activity)
                    dialogFullScreen?.requestWindowFeature(Window.FEATURE_NO_TITLE)
                    dialogFullScreen?.setContentView(R.layout.dialog_loading_interstitial)
                    dialogFullScreen?.setCancelable(false)
                    dialogFullScreen?.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
                    dialogFullScreen?.window?.setLayout(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    img = dialogFullScreen?.findViewById<LottieAnimationView>(R.id.imageView3)
                    img?.setAnimation(R.raw.loading)
                    runCatching {
                        if (!activity.isFinishing && dialogFullScreen != null && dialogFullScreen?.isShowing == false) {
                            dialogFullScreen?.show()
                        }
                    }
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    if (AdmobUtils.splashSuccess) {
                        return@postDelayed
                    }
                    if (!OnResumeUtils.isShowingAd && !isShowingAd) {
                        log("Showing AOA...")
                        runCatching {
                            val txt = dialogFullScreen?.findViewById<TextView>(R.id.txtLoading)
                            img?.visibility = View.INVISIBLE
                            txt?.visibility = View.INVISIBLE
                        }
                        setOnPaidEventListener {
                            SolarUtils.trackAdImpression(
                                ad = it,
                                adUnit = adUnitId,
                                format = "app_open"
                            )
                            val adapterInfo = appOpenAd?.responseInfo?.loadedAdapterResponseInfo
                            AppsFlyerUtils.postRevenueAppsFlyer(it, holder.currentAdId, adapterInfo, "AppOpen")
                            AdjustUtils.postRevenueAdjust(activity, it, adUnitId)

                        }
                        show(activity)
                    } else {
                        logE("OnResume or AOA is showing")
                        if (!AdmobUtils.splashSuccess) {
                            callback.onAdsFailed("")
                        }
                    }
                }, 800)
            }
        } else {
            logE("AOA not available")
            if (!AdmobUtils.splashSuccess) {
                callback.onAdsFailed("")
            }
        }
    }

    private fun onAOADestroyed() {
        isShowingAd = true
        isLoading = false
        runCatching {
            if (!activity.isFinishing && dialogFullScreen != null && dialogFullScreen?.isShowing == true) {
                dialogFullScreen?.dismiss()
            }
            appOpenAd?.fullScreenContentCallback?.onAdDismissedFullScreenContent()
        }
    }

    private fun log(msg: String) {
        if (AdmobUtils.isTesting || Helper.enableReleaseLog) Log.d("AOAUtils", msg)
    }

    private fun logE(msg: String) {
        if (AdmobUtils.isTesting || Helper.enableReleaseLog) Log.e("AOAUtils", msg)
    }

    interface AoaCallback {
        fun onAdsClose()
        fun onAdsLoaded()
        fun onAdsFailed(message: String)
    }

}