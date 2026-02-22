package com.cyber.ads.utils

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.admob.RemoteUtils.getValue
import com.cyber.ads.custom.LoadingSize
import com.cyber.ads.remote.AdUnit
import com.cyber.ads.remote.BannerHolder
import com.cyber.ads.remote.InterHolder
import com.cyber.ads.remote.NativeHolder
import com.cyber.ads.remote.NativeMultiHolder
import com.cyber.ads.remote.RewardHolder
import com.cyber.ads.remote.SplashHolder
import com.google.android.gms.ads.nativead.NativeAd
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object Helper {
    internal var jsonObject: JsonObject? = null
    internal var enableReleaseLog = false
    internal var versionCode: Int? = null

    @JvmStatic
    fun enableOnResume() = settings()?.get("on_resume")?.asString == "1"

    @JvmStatic
    fun onResumeId() = settings()?.get("on_resume_id")?.asString

    fun enableAds(): Boolean {
        val isEnable = settings()?.get("ads_enable")?.asString
        enableReleaseLog = settings()?.get("release_log")?.asString?.lowercase() == "true"
        return (isEnable == "1" || isEnable == "2" || isEnable == "3") && !AdmobUtils.isPremium
    }

    fun jsonObject(): JsonObject? {
        if (jsonObject == null) {
            val jsonString = getValue("ad_config${versionCode?.let { "_$it" } ?: ""}")
//            log("fetch json: $jsonString")
            jsonObject = JsonParser.parseString(jsonString).takeIf { it.isJsonObject }?.asJsonObject
        }
//        if (jsonObject == null && System.currentTimeMillis() - timeStamp > 1000 * 3) {
//            log("re-fetch json2")
//            timeStamp = System.currentTimeMillis()
//            val remoteConfig = FirebaseRemoteConfig.getInstance()
//            val configSettings = FirebaseRemoteConfigSettings.Builder()
//                .setMinimumFetchIntervalInSeconds(3600)
//                .build()
//            remoteConfig.setConfigSettingsAsync(configSettings)
//            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//                    FirebaseRemoteConfig.getInstance().reset()
//                    val jsonString = getValue("ad_config${versionCode?.let { "_$it" } ?: ""}")
//                    log("re-fetch json2: ${task.result} - $jsonString")
//                    jsonObject = null
//                    AdmobUtils.isEnableAds = enableAds()
//                }
//            }
//        }
//        if (jsonObject == null) log("jsonObject still NULL")
        return jsonObject
    }

    fun settings() = jsonObject()?.get("settings")?.takeIf { it.isJsonObject }?.asJsonObject

    fun parseInter(holder: InterHolder) {
        val obj = jsonObject()?.get(holder.key)?.takeIf { it.isJsonObject }?.asJsonObject ?: return

        holder.enable = obj["enable"]?.asString ?: "0"
        holder.stepCount = obj["step_count"]?.asString?.toIntOrNull() ?: 1
        holder.waitTime = obj["wait_time"]?.asString?.toIntOrNull() ?: 0
//        holder.nativeTemplate = obj["native_template"]?.asString

        obj["show_loading"]?.asString?.toBooleanStrictOrNull()?.let { holder.showLoading = it }
        obj["inter"]?.takeIf { it.isJsonObject }?.asJsonObject?.let {
            holder.interUnit = parseUnitIds(it)
        }
        obj["native_inter"]?.takeIf { it.isJsonObject }?.asJsonObject?.let {
            holder.nativeUnit = parseUnitIds(it)
        }
    }

    fun parseReward(holder: RewardHolder) {
        val obj = jsonObject()?.get(holder.key)?.takeIf { it.isJsonObject }?.asJsonObject ?: return

        holder.enable = obj["enable"]?.asString ?: "0"

        obj["show_loading"]?.asString?.toBooleanStrictOrNull()?.let { holder.showLoading = it }
        obj["reward"]?.takeIf { it.isJsonObject }?.asJsonObject?.let {
            holder.rewardUnit = parseUnitIds(it)
        }
    }

    fun parseSplash(holder: SplashHolder) {
        val obj = jsonObject()?.get(holder.key)?.takeIf { it.isJsonObject }?.asJsonObject ?: return

        holder.enable = obj["enable"]?.asString ?: "0"
        holder.stepCount = obj["step_count"]?.asString?.toIntOrNull() ?: 1
        holder.waitTime = obj["wait_time"]?.asString?.toIntOrNull() ?: 0
//        holder.nativeTemplate = obj["native_template"]?.asString

        obj["show_loading"]?.asString?.toBooleanStrictOrNull()?.let { holder.showLoading = it }
        obj["app_open"]?.takeIf { it.isJsonObject }?.asJsonObject?.let {
            holder.appOpenUnit = parseUnitIds(it)
        }
        obj["inter"]?.takeIf { it.isJsonObject }?.asJsonObject?.let {
            holder.interUnit = parseUnitIds(it)
        }
        obj["native_inter"]?.takeIf { it.isJsonObject }?.asJsonObject?.let {
            holder.nativeUnit = parseUnitIds(it)
        }
    }

    fun parseBanner(holder: BannerHolder) {
        val obj = jsonObject()?.get(holder.key)?.takeIf { it.isJsonObject }?.asJsonObject ?: return

        holder.enable = obj["enable"]?.asString ?: "0"
        holder.refreshRate = obj["refresh_rate"]?.asString?.toIntOrNull() ?: 0
        runCatching {
            holder.collapConfig = obj["collap_config"]?.asString?.map { it.toString().toInt() }
        }
        obj["native_template"]?.asString?.lowercase()?.let {
            holder.nativeTemplate = it
            holder.loadingSize = if (it.startsWith("small")) LoadingSize.SMALL
            else LoadingSize.TINY
        }
        obj["banner"]?.takeIf { it.isJsonObject }?.asJsonObject?.let {
            holder.bannerUnit = parseUnitIds(it)
        }
        obj["banner_collap"]?.takeIf { it.isJsonObject }?.asJsonObject?.let {
            holder.bannerCollapUnit = parseUnitIds(it)
        }
        obj["native"]?.takeIf { it.isJsonObject }?.asJsonObject?.let {
            holder.nativeUnit = parseUnitIds(it)
        }
    }

    fun parseNative(holder: NativeHolder) {
        val obj = jsonObject()?.get(holder.key)?.takeIf { it.isJsonObject }?.asJsonObject ?: return

        holder.enable = obj["enable"]?.asString ?: "0"
        holder.refreshRate = obj["refresh_rate"]?.asString?.toIntOrNull() ?: 0
        runCatching {
            holder.collapConfig = obj["collap_config"]?.asString?.map { it.toString().toInt() }
        }
        obj["native_template"]?.asString?.lowercase()?.let {
            holder.nativeTemplate = it
            when {
                holder.layoutId != null -> {}
                it.startsWith("small") -> holder.loadingSize = LoadingSize.SMALL
                it.startsWith("tiny") -> holder.loadingSize = LoadingSize.TINY
                else -> holder.loadingSize = LoadingSize.MEDIUM
            }
        }
//        log(holder.toString())
        obj["native"]?.takeIf { it.isJsonObject }?.asJsonObject?.let {
            holder.nativeUnit = parseUnitIds(it)
        }
    }

    fun parseNativeMulti(holder: NativeMultiHolder) {
        val obj = jsonObject()?.get(holder.key)?.takeIf { it.isJsonObject }?.asJsonObject ?: return
        holder.enable = obj["enable"]?.asString ?: "0"
        obj["native_template"]?.asString?.lowercase()?.let {
            holder.nativeTemplate = it
            when {
                it.startsWith("small") -> holder.loadingSize = LoadingSize.SMALL
                it.startsWith("tiny") -> holder.loadingSize = LoadingSize.TINY
                else -> holder.loadingSize = LoadingSize.MEDIUM
            }
        }
    }

    internal fun parseUnitIds(obj: JsonObject): AdUnit {
        val name = obj["name"]?.asString ?: obj["name1"]?.asString ?: ""
//        val name2 = obj["name2"]?.asString ?: ""
        val id1 = obj["id1"]?.asString ?: obj["id1"]?.toString() ?: ""
        val id2 = obj["id2"]?.asString ?: obj["id2"]?.toString() ?: ""
        val id3 = obj["id3"]?.asString ?: obj["id3"]?.toString() ?: ""
        return AdUnit(name, id1, id2, id3)
    }

    fun nativeExtras(ad: NativeAd?): Boolean {
        ad?.headline?.let {
            return !TextUtils.contains(it)
        }

        return true
    }

    fun checkFirstOpen(context: Context) {
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                try {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            if (TextUtils.isNotNull(referrerClient.installReferrer.installReferrer)) {
                                context.prefs().edit { putBoolean("is_are", false) }
                                FirebaseAnalytics.getInstance(context).logEvent("ads_ref_true", null)
                            } else {
                                FirebaseAnalytics.getInstance(context).logEvent("ads_ref_false", null)
                            }
                            FirebaseAnalytics.getInstance(context).logEvent(referrerClient.installReferrer.installReferrer.toString(), null)
                            referrerClient.endConnection()
                        }

                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                            logE("Ref not supported on this device/Play Store")
                            referrerClient.endConnection()
                        }

                        InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                            logE("Ref service unavailable")
                            referrerClient.endConnection()
                        }

                        InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR -> {
                            logE("Developer error calling Ref")
                            referrerClient.endConnection()
                        }

                        else -> {
                            logE("Unknown response: $responseCode")
                            referrerClient.endConnection()
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    referrerClient.endConnection()
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
            }
        })
    }

}