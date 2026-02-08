package com.cyber.sample.ui

import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.onboading.BaseLanguageActivity
import com.cyber.ads.remote.NativeHolder
import com.cyber.ads.remote.NativeMultiHolder
import com.cyber.ads.utils.replaceActivity
import com.cyber.sample.RemoteConfig

class LanguageActivity(
    override var nativeLanguage: NativeMultiHolder = RemoteConfig.NATIVE_LANGUAGE,
    override var nativeSmall: NativeHolder = RemoteConfig.NATIVE_SMALL_LANGUAGE,
) : BaseLanguageActivity() {
    override var nativeFull: NativeHolder =RemoteConfig.NATIVE_INTRO_FULL
    override var nativeIntro: NativeMultiHolder =RemoteConfig.NATIVE_INTRO

    override fun nextActivity() {
        AdmobUtils.loadAndShowInterstitial(this, RemoteConfig.INTER_LANGUAGE) {
            replaceActivity<IntroActivity>()
        }
    }

}