package com.cyber.ads.remote

import com.cyber.ads.admob.AdmobUtils.isTesting
import com.cyber.ads.utils.Helper
import com.cyber.ads.utils.log

const val APP_OPEN_TEST_ID = "ca-app-pub-3940256099942544/9257395921"
const val INTER_TEST_ID = "ca-app-pub-3940256099942544/1033173712"
const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
const val BANNER_COLLAP_TEST_ID = "ca-app-pub-3940256099942544/2014213617"
const val NATIVE_TEST_ID = "ca-app-pub-3940256099942544/2247696110"
//const val NATIVE_VIDEO_TEST_ID = "ca-app-pub-3940256099942544/1044960115"
//const val NATIVE_FULL_TEST_ID = "ca-app-pub-3940256099942544/7342230711"
const val REWARD_TEST_ID = "ca-app-pub-3940256099942544/5224354917"
//const val REWARD_INTER_TEST_ID = "ca-app-pub-3940256099942544/5354046379"

open class BaseHolder(val key: String) {
    internal var enable: String = "0"

    fun getIds(unit: AdUnit?, testId: String): List<String> {
        val realIds = listOfNotNull(unit?.id1, unit?.id2, unit?.id3).filter { it.isNotBlank() }
        log("$key - ${unit?.name} - $realIds")
        return if (isTesting) listOf(testId) else realIds
    }

    fun enable(): String {
        when (this) {
            is SplashHolder -> Helper.parseSplash(this)
            is InterHolder -> Helper.parseInter(this)
            is NativeMultiHolder -> Helper.parseNativeMulti(this)
            is BannerHolder -> Helper.parseBanner(this)
            is NativeHolder -> Helper.parseNative(this)
            is RewardHolder -> Helper.parseReward(this)
        }
        return enable
    }
}

data class AdUnit(val name: String, val id1: String, val id2: String, val id3: String)