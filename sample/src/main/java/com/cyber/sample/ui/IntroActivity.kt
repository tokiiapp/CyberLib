package com.cyber.sample.ui

import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.onboading.BaseIntroActivity
import com.cyber.ads.remote.InterHolder
import com.cyber.ads.remote.NativeHolder
import com.cyber.ads.remote.NativeMultiHolder
import com.cyber.ads.utils.replaceActivity
import com.cyber.sample.RemoteConfig

class IntroActivity(
    override var nativeIntroFull: NativeHolder = RemoteConfig.NATIVE_INTRO_FULL,
    override var nativeIntro: NativeMultiHolder = RemoteConfig.NATIVE_INTRO
) : BaseIntroActivity() {

    override fun nextActivity() {
        AdmobUtils.loadAndShowInterstitial(this, RemoteConfig.INTER_INTRO) {
            replaceActivity<MainActivity>()
        }
    }
}
