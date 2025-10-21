package com.cyber.ads.adjust

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.utils.Helper
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.ads.AdValue


object AdjustUtils {
    var callback :((AdValue)->Unit)? = null
    @JvmStatic
    fun initAdjust(context: Context, key: String, debug: Boolean) {
        val config = if (debug) {
            AdjustConfig.ENVIRONMENT_SANDBOX
        } else {
            AdjustConfig.ENVIRONMENT_PRODUCTION
        }
        val adjustConfig = AdjustConfig(context, key, config)
        adjustConfig.setLogLevel(LogLevel.WARN)
        Adjust.initSdk(adjustConfig)
    }

//    @JvmStatic
//    fun postRevenueAdjustMax(ad: MaxAd) {
//        Log.d("==postRevenueAdjustMax==", "postRevenueAdjustMax: ${ad.revenue}")
//
//        val adjustAdRevenue = AdjustAdRevenue("applovin_max_sdk")
//        adjustAdRevenue.setRevenue(ad.revenue, "USD")
//        adjustAdRevenue.adRevenueNetwork = ad.networkName
//        adjustAdRevenue.adRevenueUnit = ad.adUnitId
//        adjustAdRevenue.adRevenuePlacement = ad.placement
//        Adjust.trackAdRevenue(adjustAdRevenue)
//    }

    fun postRevenueFacebook(context: Context, revenueValue: Double, currencyCode: String) {
        val logger = AppEventsLogger.newLogger(context)
        val params = Bundle()
        params.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, currencyCode)
        logger.logEvent(AppEventsConstants.EVENT_NAME_AD_IMPRESSION, revenueValue, params)
    }

    @JvmStatic
    fun postRevenueAdjust(context: Context, ad: AdValue, adUnit: String?) {
        callback?.invoke(ad)
        val revenueValue = ad.valueMicros / 1000000.0
        log("$revenueValue ${ad.currencyCode} - $adUnit - type ${ad.precisionType}")

        if(AdmobUtils.isTesting) return

        postRevenueFacebook(context, revenueValue, ad.currencyCode)

        val adjustAdRevenue = AdjustAdRevenue("admob_sdk")
        adjustAdRevenue.setRevenue(revenueValue, ad.currencyCode)
        adjustAdRevenue.adRevenueUnit = adUnit
//        adjustAdRevenue.adRevenueNetwork = loadedAdapterResponseInfo?.adSourceName
        Adjust.trackAdRevenue(adjustAdRevenue)
    }

//    @JvmStatic
//    fun postRevenueAdjustInter(context: Context, interAd: InterstitialAd, ad: AdValue, adUnit: String?) {
//        val revenueValue = ad.valueMicros / 1000000.0
//        log("postRevenueInter: $revenueValue")
//        postRevenueFacebook(context, revenueValue, ad.currencyCode)
//
//        val loadedAdapterResponseInfo = interAd.responseInfo.loadedAdapterResponseInfo
//        val adjustAdRevenue = AdjustAdRevenue("admob_sdk")
//        adjustAdRevenue.setRevenue(revenueValue, ad.currencyCode)
//        adjustAdRevenue.adRevenueUnit = adUnit
//        adjustAdRevenue.adRevenueNetwork = loadedAdapterResponseInfo?.adSourceName
//        Adjust.trackAdRevenue(adjustAdRevenue)
//    }

//    @JvmStatic
//    fun postRevenueAdjustNative(context: Context, nativeAd: NativeAd, ad: AdValue, adUnit: String?) {
//        val revenueValue = ad.valueMicros / 1000000.0
//        log("postRevenueNative: $revenueValue")
//        postRevenueFacebook(context, revenueValue, ad.currencyCode)
//
//        val loadedAdapterResponseInfo = nativeAd.responseInfo?.loadedAdapterResponseInfo
//        val adjustAdRevenue = AdjustAdRevenue("admob_sdk")
//        adjustAdRevenue.setRevenue(revenueValue, ad.currencyCode)
//        adjustAdRevenue.adRevenueUnit = adUnit
//        adjustAdRevenue.adRevenueNetwork = loadedAdapterResponseInfo?.adSourceName
//        Adjust.trackAdRevenue(adjustAdRevenue)
//    }

//    @JvmStatic
//    fun postRevenueAdjustRewarded(context: Context, interAd: RewardedAd, ad: AdValue, adUnit: String?) {
//        val revenueValue = ad.valueMicros / 1000000.0
//        log("postRevenueAdjustNative: $revenueValue")
//        postRevenueFacebook(context, revenueValue, ad.currencyCode)
//        val loadedAdapterResponseInfo: AdapterResponseInfo? = interAd.responseInfo.loadedAdapterResponseInfo
//        val adjustAdRevenue = AdjustAdRevenue("admob_sdk")
//        adjustAdRevenue.setRevenue(revenueValue, ad.currencyCode)
//        adjustAdRevenue.adRevenueUnit = adUnit
//        adjustAdRevenue.adRevenueNetwork = loadedAdapterResponseInfo?.adSourceName
//        Adjust.trackAdRevenue(adjustAdRevenue)
//    }

    private fun log(msg: String) {
        if (AdmobUtils.isTesting || Helper.enableReleaseLog) Log.d("postRevenue", msg)
    }

}