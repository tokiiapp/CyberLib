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
import com.cyber.ads.utils.addActivity
import com.cyber.sample.R
import com.cyber.sample.RemoteConfig
import com.cyber.sample.databinding.ActivitySplashBinding
import com.cyber.sample.ui.uninstall.UninstallActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    var splash: String? = null
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
        splash = intent.data?.getQueryParameter("splash")
            ?: intent.getStringExtra("splash")
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
                if (splash != "uninstall") {
                    AdmobUtils.loadNativeLanguage(
                        this,
                        RemoteConfig.NATIVE_LANGUAGE,
                        object : AdmobUtils.NativeCallback() {})
                    AdmobUtils.loadNative(
                        this,
                        RemoteConfig.NATIVE_LANGUAGE_SMALL,
                        object : AdmobUtils.NativeCallback() {})
                }
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
                    handler.postDelayed({ nextActivity() }, 2000)
                }
            })


    }

    private fun nextActivity() {
        if (!isFinishing) {
            if (intent.action != Intent.ACTION_VIEW) {
                addActivity<LanguageActivity> { putExtra("fromSplash", true) }
                finish()
                return
            }
            if (splash == "uninstall") {
                addActivity<UninstallActivity>()
                finish()
            }

        }
    }

}