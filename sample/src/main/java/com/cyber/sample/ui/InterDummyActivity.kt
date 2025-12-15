package com.cyber.sample.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.utils.addActivity
import com.cyber.sample.R
import com.cyber.sample.RemoteConfig
import com.cyber.sample.databinding.ActivityInterDummyBinding

class InterDummyActivity : AppCompatActivity() {
    private val binding by lazy { ActivityInterDummyBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btnOk.setOnClickListener { finish() }
    }

    override fun onBackPressed() {
//        AdmobUtils.loadAndShowInterstitial(this, RemoteConfig.INTER_HOME2, R.layout.ad_template_fullscreen) {
//            finish()
//        }
    }
}