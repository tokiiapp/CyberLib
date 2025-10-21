package com.cyber.ads.remote

import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.utils.Helper

class RewardHolder(key: String) : BaseHolder(key) {
    var rewardUnit: AdUnit? = null
    var showLoading: Boolean = true

    val rewardIds: List<String>
        get() = getIds(rewardUnit, REWARD_TEST_ID)

    fun isRewardEnable(): Boolean {
        Helper.parseReward(this)
        return enable == "1" && AdmobUtils.isEnableAds && !AdmobUtils.isPremium
    }

}
