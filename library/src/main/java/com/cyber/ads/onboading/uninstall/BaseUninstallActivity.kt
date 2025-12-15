package com.cyber.ads.onboading.uninstall

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.databinding.ActivityUninstallBinding
import com.cyber.ads.remote.NativeHolder

abstract class BaseUninstallActivity : AppCompatActivity() {
    private val binding by lazy { ActivityUninstallBinding.inflate(layoutInflater) }
    abstract var nativeUninstall: NativeHolder
    abstract val activityBack: Class<*>
    open fun onBackPressedConfirmUninstall() {
        startActivity(Intent(this, activityBack).putExtra("uninstall", true))
        finish()
    }

    abstract fun nextActivity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.btnTry.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.btnUninstall.setOnClickListener {
            nextActivity()
        }
        onBackPressedDispatcher.addCallback(this) {
            onBackPressedConfirmUninstall()
        }
    }

    override fun onStart() {
        super.onStart()
        AdmobUtils.loadAndShowNative(
            this,
            nativeUninstall,
            binding.flNative,
            object : AdmobUtils.NativeCallback() {})
    }
}