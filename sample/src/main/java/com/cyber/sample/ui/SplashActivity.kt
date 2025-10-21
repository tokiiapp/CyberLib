package com.cyber.sample.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.admob.RemoteUtils
import com.cyber.ads.iap.IapConfig
import com.cyber.ads.iap.IapListener
import com.cyber.ads.iap.IapUtils
import com.cyber.ads.utils.replaceActivity
import com.cyber.sample.R
import com.cyber.sample.RemoteConfig
import com.cyber.sample.databinding.ActivitySplashBinding


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val binding by lazy { ActivitySplashBinding.inflate(layoutInflater) }
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (!isTaskRoot
            && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
            && intent.action != null && intent.action == Intent.ACTION_MAIN
        ) {
            finish()
            return
        }
        IapUtils.init(
            applicationContext,
            IapConfig(
                inappProductIds = emptyList(),
                subsProductIds = listOf(
                    getString(com.cyber.ads.R.string.monthly_pro),
                    getString(com.cyber.ads.R.string.yearly_pro),
                    getString(com.cyber.ads.R.string.trial_yearly_pro)
                ),
                autoAcknowledge = true
            ),
            object : IapListener {
                override fun onRestoreSuccess(activeProductIds: List<String>) {
                    val enableAds = activeProductIds.isEmpty()
                    AdmobUtils.isPremium = !enableAds
                    initData()
                }

                override fun onError(message: String?) {
                    initData()
                }
            }
        )
    }

    private fun initData() {
        RemoteUtils.init(this, R.xml.remote_config_defaults) {
            AdmobUtils.setupCMP(this) {
                AdmobUtils.initAdmob(this, isDebug = true)
                AdmobUtils.loadNativeLanguage(
                    this,
                    RemoteConfig.NATIVE_LANGUAGE,
                    object : AdmobUtils.NativeCallback() {})
                showInterOrAoa()
            }
        }
    }

    private fun showInterOrAoa() {
        AdmobUtils.loadAndShowAdSplash(
            this,
            RemoteConfig.ADS_SPLASH,
            object : AdmobUtils.InterCallback() {
                override fun onInterClosed() {
                    nextActivity()
                }

                override fun onInterFailed(error: String) {
                    handler.postDelayed({ nextActivity() }, 100)
                }
            })
    }

    private fun nextActivity() {
        replaceActivity<LanguageActivity>()
    }

}