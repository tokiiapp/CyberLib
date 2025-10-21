package com.cyber.sample.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.admob.RemoteUtils
import com.cyber.ads.utils.replaceActivity
import com.cyber.ads.utils.toast
import com.cyber.ads.utils.visible
import com.cyber.demo.opening.LanguageModel
import com.cyber.sample.RemoteConfig
import com.cyber.sample.databinding.ActivityLanguageBinding
import com.cyber.sample.utils.Common
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class LanguageActivity : AppCompatActivity() {
    private val binding by lazy { ActivityLanguageBinding.inflate(layoutInflater) }
    private lateinit var languages: ArrayList<LanguageModel>
    private var adapter: LanguageAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.toolbar.navigationIcon = null
        val delayMillis = RemoteUtils.languageDelayMillis()
        binding.tvNext.isVisible = delayMillis < 0
        AdmobUtils.loadNativeFull(this, RemoteConfig.NATIVE_INTRO_FULL, object : AdmobUtils.NativeCallback() {})
        AdmobUtils.loadNativeIntro(this, RemoteConfig.NATIVE_INTRO, object : AdmobUtils.NativeCallback() {})
        AdmobUtils.showNativeLanguage(this, RemoteConfig.NATIVE_LANGUAGE, binding.flNative, 0, object : AdmobUtils.NativeCallbackSimple() {})

        languages = Common.getListLocation(this)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        adapter = LanguageAdapter {
            if (delayMillis <= 0) {
                binding.tvNext.visible()
            } else {
                lifecycleScope.launch(Dispatchers.Main) {
                    delay(delayMillis)
                    binding.tvNext.visible()
                }
            }
            if (Common.currentLang == null) {
                AdmobUtils.showNativeLanguage(this, RemoteConfig.NATIVE_LANGUAGE, binding.flNative, 1, object : AdmobUtils.NativeCallbackSimple() {})
            }
            Common.currentLang = languages[it].langCode
            adapter?.updatePosition(it)
        }.apply {
            submitList(languages)
            binding.rvLanguage.adapter = this
        }

        binding.tvNext.setOnClickListener {
            if (Common.currentLang == null) {
                toast("Please select a language before continue!")
            } else {
                nextActivity()
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        exitProcess(0)
    }

    private fun nextActivity() {
        AdmobUtils.loadAndShowInterstitial(this, RemoteConfig.INTER_LANGUAGE) {
            replaceActivity<IntroActivity>()
        }
    }

}