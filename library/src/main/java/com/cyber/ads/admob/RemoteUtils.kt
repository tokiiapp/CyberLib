package com.cyber.ads.admob

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.XmlRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import com.cyber.ads.R
import com.cyber.ads.utils.Helper
import com.cyber.ads.utils.Helper.checkUtm
import com.cyber.ads.utils.prefs
import com.cyber.ads.utils.setupDialog
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object RemoteUtils {

    /**
     * @param versionCode BuildConfig.VERSION_CODE - sử dụng để phân biệt các phiên bản remote config
     *
     * - VD: versionCode = 101 => giá trị tương ứng trong file xml là `<key>ad_config_101</key>`
     *
     * - mặc định là null => `<key>ad_config</key>`
     */
    @JvmStatic
    fun init(context: Context, @XmlRes xmlFile: Int, versionCode: Int? = null, onCompleted: () -> Unit) {
        if (context.prefs().getBoolean("ads_first_open", true)) {
            checkUtm(context)
            context.prefs().edit { putBoolean("ads_first_open", false) }
        }
        AdmobUtils.isConsented = false
        AdmobUtils.isInitialized = false
        Helper.versionCode = versionCode
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(xmlFile)

        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                remoteConfig.activate().addOnCompleteListener {
                    Helper.jsonObject = null
                    AdmobUtils.isEnableAds = Helper.enableAds()
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.e("===RemoteUtils", "onError: ${error.message}")
            }
        })

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            task.exception?.let { Log.e("===RemoteUtils", "onComplete: $it") }
            Helper.jsonObject = null
            AdmobUtils.isEnableAds = Helper.enableAds()
            onCompleted()
        }
    }

    @JvmStatic
    fun getValue(key: String): String {
        val value = FirebaseRemoteConfig.getInstance().getString(key)
//        log("getValue: $key = $value")
        return value
    }

    @JvmStatic
    fun languageDelayMillis(): Long {
        return Helper.settings()?.get("language_delay_millis")?.asString?.toLongOrNull() ?: 0
    }

    @JvmStatic
    fun dialogNoInternet(activity: Activity, callback: (isConnected: Boolean) -> Unit): AlertDialog {
        val dialog = AlertDialog.Builder(activity).create()
        dialog.setupDialog(activity)
        val dialogLayout = LayoutInflater.from(activity).inflate(R.layout.dialog_no_internet, null, false)
        dialog.setView(dialogLayout)
        dialogLayout.findViewById<View>(R.id.btnRetry).setOnClickListener {
            val isConnected = AdmobUtils.isNetworkConnected(activity)
            if (isConnected) dialog.dismiss()
            callback(isConnected)
        }
        dialog.setCancelable(false)
        return dialog
    }

}
