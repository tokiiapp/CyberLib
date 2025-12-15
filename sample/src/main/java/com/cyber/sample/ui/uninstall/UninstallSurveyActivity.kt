package com.cyber.sample.ui.uninstall

import com.cyber.ads.onboading.uninstall.BaseUninstallSurveyActivity
import com.cyber.ads.remote.NativeHolder
import com.cyber.sample.RemoteConfig
import com.cyber.sample.ui.LanguageActivity

class UninstallSurveyActivity(override var nativeUninstallSurvey: NativeHolder = RemoteConfig.NATIVE_UNINSTALL_SURVEY) : BaseUninstallSurveyActivity() {

    override val activityBack: Class<*>
        get() = LanguageActivity::class.java

}