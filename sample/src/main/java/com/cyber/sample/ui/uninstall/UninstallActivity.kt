package com.cyber.sample.ui.uninstall

import android.content.Intent
import com.cyber.ads.onboading.uninstall.BaseUninstallActivity
import com.cyber.ads.remote.NativeHolder
import com.cyber.sample.RemoteConfig
import com.cyber.sample.ui.LanguageActivity

class UninstallActivity(override var nativeUninstall: NativeHolder = RemoteConfig.NATIVE_UNINSTAL) : BaseUninstallActivity() {

    override val activityBack: Class<*>
        get() = LanguageActivity::class.java

    override fun nextActivity() {
        startActivity(Intent(this, UninstallSurveyActivity::class.java))
        finish()
    }
}