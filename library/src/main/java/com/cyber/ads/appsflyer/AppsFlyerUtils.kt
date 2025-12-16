package com.cyber.ads.appsflyer

import android.app.Application
import android.content.Context
import android.util.Log
import com.appsflyer.AppsFlyerLib
import com.appsflyer.adrevenue.AppsFlyerAdRevenue
import com.appsflyer.adrevenue.adnetworks.generic.MediationNetwork
import com.appsflyer.adrevenue.adnetworks.generic.Scheme
import com.appsflyer.attribution.AppsFlyerRequestListener
import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.utils.Helper
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdapterResponseInfo
import java.util.Currency

object AppsFlyerUtils {

    var callback: ((AdValue) -> Unit)? = null

    @JvmStatic
    fun initAppsFlyer(application: Application, devKey: String, debug: Boolean) {
        // 1. Cấu hình Debug
        AppsFlyerLib.getInstance().setDebugLog(debug)

        // 2. Init Core
        AppsFlyerLib.getInstance().init(devKey, null, application)

        // 3. Start Core
        AppsFlyerLib.getInstance().start(application, devKey, object : AppsFlyerRequestListener {
            override fun onSuccess() {
                log("AppsFlyer started successfully")
            }
            override fun onError(errorCode: Int, errorDesc: String) {
                log("AppsFlyer start failed: $errorCode - $errorDesc")
            }
        })

        // 4. Init AdRevenue
        val afAdRevenue = AppsFlyerAdRevenue.Builder(application).build()
        AppsFlyerAdRevenue.initialize(afAdRevenue)
    }

    @JvmStatic
    fun postRevenueAppsFlyer(
        adValue: AdValue,
        adUnitId: String?,
        adapterInfo: AdapterResponseInfo?,
        adType: String
    ) {
        callback?.invoke(adValue)

        val revenueAmount = adValue.valueMicros / 1000000.0
        val currencyCode = adValue.currencyCode

        // SỬA ĐỔI 1: adapterClassName là property đúng của AdMob SDK
        val networkName = adapterInfo?.adSourceName ?: "unknown_network"
        val adapterClass = adapterInfo?.adapterClassName ?: ""

        log("Revenue: $revenueAmount $currencyCode | Source: $networkName | Unit: $adUnitId")

        if (AdmobUtils.isTesting) return


        val customParams: MutableMap<String, String> = HashMap()
        customParams[Scheme.AD_UNIT] = adUnitId ?: ""
        customParams[Scheme.AD_TYPE] = adType
        customParams["network_class_name"] = adapterClass

        // SỬA ĐỔI 2: Dùng MediationNetwork.googleadmob (lowercase)
        AppsFlyerAdRevenue.logAdRevenue(
            networkName,
            MediationNetwork.googleadmob,
            Currency.getInstance(currencyCode),
            revenueAmount,
            customParams
        )
    }


    private fun log(msg: String) {
        if (AdmobUtils.isTesting || Helper.enableReleaseLog) {
            Log.d("AppsFlyerRevenue", msg)
        }
    }
}