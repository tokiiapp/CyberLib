package com.cyber.ads.onboading.uninstall

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cyber.ads.R
import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.admob.OnResumeUtils
import com.cyber.ads.databinding.ActivityUninstallSurveyBinding
import com.cyber.ads.remote.NativeHolder
import com.cyber.ads.utils.toFirebaseEventName
import com.google.firebase.analytics.FirebaseAnalytics


abstract class BaseUninstallSurveyActivity : AppCompatActivity() {
    private val binding by lazy { ActivityUninstallSurveyBinding.inflate(layoutInflater) }
    abstract var nativeUninstallSurvey: NativeHolder
    abstract val activityBack: Class<*>
    open fun onBackPressedUninstallSurvey() {
        startActivity(Intent(this, activityBack).putExtra("uninstall", true))
        finish()
    }

    open fun openUninstall() {
        FirebaseAnalytics.getInstance(this).logEvent("uninstall_${reason()}", null)
        OnResumeUtils.disableNextResume()
        val intent = Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
        intent.setData(Uri.fromParts("package", packageName, null))
        startActivity(intent)
        finish()
    }

    private fun reason() = when (binding.rbGroup.checkedRadioButtonId) {
        R.id.rbNotUnderstand -> "did_not_understand"
        R.id.rbAds -> "too_many_ads"
        R.id.rbFeature -> "feature_not_work"
        R.id.rbNoNeed -> "no_need_anymore"
        R.id.rbOther -> "other_${binding.edtReason.text.toFirebaseEventName()}"
        else -> "no_reason"
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
        initView()
    }

    private fun initView() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.btnCancel.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.btnUnstall.setOnClickListener {
            openUninstall()
        }
        onBackPressedDispatcher.addCallback(this) {
            onBackPressedUninstallSurvey()
        }
    }

    override fun onStart() {
        super.onStart()
        AdmobUtils.loadAndShowNative(
            this,
            nativeUninstallSurvey,
            binding.flNative,
            object : AdmobUtils.NativeCallback() {})
    }
}