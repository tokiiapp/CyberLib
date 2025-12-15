package com.cyber.sample.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.admob.RemoteUtils
import com.cyber.ads.utils.addActivity
import com.cyber.ads.utils.log
import com.cyber.ads.utils.prefs
import com.cyber.ads.utils.setupInAppUpdate
import com.cyber.ads.utils.toast
import com.cyber.sample.RemoteConfig
import com.cyber.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onStart() {
        super.onStart()
        AdmobUtils.loadAndShowBanner(
            this, RemoteConfig.BANNER_HOME, binding.flBanner,
            object : AdmobUtils.BannerCallback() {},
            object : AdmobUtils.NativeCallback() {})
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupInAppUpdate(this)

        binding.btnLoadShowBanner.setOnClickListener {
            AdmobUtils.loadAndShowBanner(
                this,
                RemoteConfig.BANNER_HOME_1,
                binding.flBanner,
                object : AdmobUtils.BannerCallback() {},
                object : AdmobUtils.NativeCallback() {})
        }

        binding.btnLoadShowBannerCollap.setOnClickListener {
            AdmobUtils.loadAndShowBanner(
                this,
                RemoteConfig.BANNER_HOME,
                binding.flBanner,
                object : AdmobUtils.BannerCallback() {},
                object : AdmobUtils.NativeCallback() {})
        }

        binding.btnLoadShowNativeBanner.setOnClickListener {
            AdmobUtils.loadAndShowBanner(
                this,
                RemoteConfig.BANNER_HOME_3,
                binding.flBanner,
                object : AdmobUtils.BannerCallback() {},
                object : AdmobUtils.NativeCallback() {})
        }

        binding.btnLoadShowNativeCollapBanner.setOnClickListener {
            AdmobUtils.loadAndShowBanner(
                this,
                RemoteConfig.BANNER_HOME_4,
                binding.flBanner,
                object : AdmobUtils.BannerCallback() {},
                object : AdmobUtils.NativeCallback() {})
        }

        binding.btnLoadShowBannerTop.setOnClickListener {
            AdmobUtils.loadAndShowBanner(
                this,
                RemoteConfig.BANNER_HOME_TOP1.anchorTop(),
                binding.flBannerTop,
                object : AdmobUtils.BannerCallback() {},
                object : AdmobUtils.NativeCallback() {})
        }

        binding.btnLoadShowBannerCollapTop.setOnClickListener {
            AdmobUtils.loadAndShowBanner(
                this,
                RemoteConfig.BANNER_HOME_TOP2.anchorTop(),
                binding.flBannerTop,
                object : AdmobUtils.BannerCallback() {},
                object : AdmobUtils.NativeCallback() {})
        }

        binding.btnLoadShowNative.setOnClickListener {
            AdmobUtils.loadAndShowNative(this, RemoteConfig.NATIVE_HAHA.anchorTop(), binding.flNative, object : AdmobUtils.NativeCallback() {})
        }

        binding.btnLoadShowNativeCollapTop.setOnClickListener {
            AdmobUtils.loadAndShowBanner(
                this,
                RemoteConfig.BANNER_HOME_TOP3.anchorTop(),
                binding.flBannerTop,
                object : AdmobUtils.BannerCallback() {},
                object : AdmobUtils.NativeCallback() {})
        }

        binding.btnLoadShowInter.setOnClickListener {
            AdmobUtils.loadAndShowInterstitial(this, RemoteConfig.INTER_HOME) {
                addActivity<InterDummyActivity>()
            }
        }
        binding.btnLoadAndShowReward.setOnClickListener {
            AdmobUtils.loadAndShowReward(this, RemoteConfig.REWARD_HOME, object : AdmobUtils.RewardCallback() {
                override fun onRewardClosed() {
                    toast("Reward Closed")
                }

                override fun onRewardEarned() {
                    toast("Reward Earned")
                }
            })
        }
        binding.btnLoadShowInterWithNative.setOnClickListener {
            AdmobUtils.loadAndShowInterstitial(this, RemoteConfig.INTER_HOME_2) {
                addActivity<InterDummyActivity>()
            }
        }

        RemoteUtils.dialogNoInternet(this) { toast("Network Connected") }.show()
    }

}