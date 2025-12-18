package com.cyber.ads.onboading

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.cyber.ads.R
import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.admob.OnResumeUtils
import com.cyber.ads.admob.RemoteUtils
import com.cyber.ads.databinding.ActivityLanguageBinding
import com.cyber.ads.remote.NativeHolder
import com.cyber.ads.remote.NativeMultiHolder
import com.cyber.ads.utils.Helper
import com.cyber.ads.utils.gone
import com.cyber.ads.utils.prefs
import com.cyber.ads.utils.toast
import com.cyber.ads.utils.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

abstract class BaseLanguageActivity : AppCompatActivity() {
    private val binding by lazy { ActivityLanguageBinding.inflate(layoutInflater) }
    private var adapter: LanguageAdapter? = null
    open val fromSplash by lazy { intent.getBooleanExtra("fromSplash", false) }
    open val uninstall by lazy { intent.getBooleanExtra("uninstall", false) }
    private val btnNext: View by lazy {
        if (languageSetup.substringAfter("next=").firstOrNull()
                ?.toString() == "1"
        ) binding.ivNext else binding.tvNext
    }
    private val languageSetup by lazy {
        Helper.settings()?.get("language_setup")?.asString?.replace(Regex("\\s+"), "")?.lowercase()
            ?: "next=1,loading=true"
    }
    private var lang: String = ""
    private val showLoading by lazy { languageSetup.contains("loading=true") }

    abstract var nativeLanguage: NativeMultiHolder
    abstract var nativeSmall: NativeHolder
    abstract var nativeFull: NativeHolder
    abstract var nativeIntro: NativeMultiHolder
    abstract fun nextActivity()

    open fun loadIntros() {
        AdmobUtils.loadNativeFull(this, nativeFull, object : AdmobUtils.NativeCallback() {})
        AdmobUtils.loadNativeIntro(this, nativeIntro, object : AdmobUtils.NativeCallback() {})
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        init()
        setupRvLanguage()
    }

    private fun init() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        if (fromSplash || uninstall) {
            setCurrentLang(null)
            btnNext.isVisible = RemoteUtils.languageDelayMillis() < 0
            binding.toolbar.navigationIcon = null
            loadIntros()
            if (fromSplash) {
                showNativeLanguage(0)
                AdmobUtils.showNative(
                    this,
                    nativeSmall,
                    binding.flNativeSmall,
                    object : AdmobUtils.NativeCallbackSimple() {})
            }
        } else {
            lang = currentLanguage(this) ?: ""
        }
        btnNext.setOnClickListener {
            if (lang.isBlank()) {
                toast("Please select a language before continue!")
            } else {
                setCurrentLang(lang)
                nextActivity()
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (!fromSplash) {
                finish()
            }
        }
    }

    private fun showNativeLanguage(position: Int) {
        if (position == 0) showLoading(true)
        AdmobUtils.showNativeLanguage(
            this,
            nativeLanguage,
            binding.flNative,
            position,
            object : AdmobUtils.NativeCallbackSimple() {
                override fun onNativeLoaded() {
                    if (position == 1) {
                        checkAndShowNext()
                    }
                    showLoading(false)
                }

                override fun onNativeFailed(error: String) {
                    if (position == 1) {
                        checkAndShowNext()
                    }
                    showLoading(false)
                }
            })
    }


    private fun checkAndShowNext() {
        if (btnNext.isVisible) return
        lifecycleScope.launch(Dispatchers.Main) {
            runCatching {
                withTimeout(5000) {
                    while (true) {
                        val isNativeFullDone = !AdmobUtils.isNativeFullLoading(nativeFull)
                        val isNativeIntroDone = !AdmobUtils.isNativeIntroLoading(nativeIntro)
                        if (isNativeFullDone && isNativeIntroDone) {
                            if (!isFinishing && !isDestroyed) {
                                showNext()
                            }
                            break
                        }
                        delay(200)
                    }
                }
            }.onFailure {
                if (!isFinishing && !isDestroyed) {
                    showNext()
                }
            }
        }
    }

    private fun setupRvLanguage() {
        adapter = LanguageAdapter { position ->
            if (position < 0) return@LanguageAdapter
            if (lang.isBlank()) {
                binding.progressBar.visible()
                showNativeLanguage(1)
            } else {
                checkAndShowNext()
            }
            lang = languages[position].langCode
            adapter?.updatePosition(position)

        }.apply {
            submitList(languages)
            binding.rvLanguage.adapter = this
            if (!fromSplash) {
                val index =
                    languages.indexOfFirst { it.langCode == currentLanguage(this@BaseLanguageActivity) }
                updatePosition(index)
            }
        }
    }

    fun showNext() {
        if (RemoteUtils.languageDelayMillis() <= 0 || !fromSplash) {
            binding.progressBar.gone()
            btnNext.visible()
        } else {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(RemoteUtils.languageDelayMillis())
                binding.progressBar.gone()
                btnNext.visible()
            }
        }
    }

    fun setCurrentLang(langCode: String?) {
        prefs().edit { putString("languageCode", langCode) }
    }

    private fun showLoading(show: Boolean) {
        if (!showLoading) return
        binding.loading.isVisible = show
        binding.rvLanguage.isVisible = !show
    }

    override fun onStart() {
        super.onStart()
        if (!fromSplash) {
            AdmobUtils.loadAndShowNative(
                this,
                nativeLanguage,
                binding.flNative,
                object : AdmobUtils.NativeCallback() {})
            AdmobUtils.loadAndShowNative(
                this,
                nativeSmall,
                binding.flNativeSmall,
                object : AdmobUtils.NativeCallback() {
                })
        }
    }

    override fun onResume() {
        super.onResume()
        OnResumeUtils.enableOnResume(javaClass)
    }

    companion object {
        @JvmStatic
        fun currentLanguage(context: Context) = context.prefs().getString("languageCode", null)
        var languages = listOf(
            LanguageModel(R.drawable.hindi, "हिंदी", "hi"),
            LanguageModel(R.drawable.spanish, "Español", "es"),
            LanguageModel(R.drawable.french, "Français", "fr"),
            LanguageModel(R.drawable.english, "English", "en"),
            LanguageModel(R.drawable.arabic, "عربي", "ar"),
            LanguageModel(R.drawable.bengali, "বাংলা", "bn"),
            LanguageModel(R.drawable.russian, "Русский", "ru"),
            LanguageModel(R.drawable.portuguese, "Português", "pt"),
            LanguageModel(R.drawable.indonesian, "Bahasa Indonesia", "in"),
            LanguageModel(R.drawable.german, "Deutsch", "de"),
            LanguageModel(R.drawable.italian, "Italiano", "it"),
            LanguageModel(R.drawable.korean, "한국어", "ko")
        )
    }
}