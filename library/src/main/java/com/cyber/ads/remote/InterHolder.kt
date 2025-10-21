package com.cyber.ads.remote

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.interstitial.InterstitialAd

open class InterHolder(key: String) : NativeHolder(key) {
    internal var count = 0
    internal var stepCount: Int = 1
    internal var isInterLoading = false
    internal var showLoading: Boolean = true
    internal var interUnit: AdUnit? = null
    internal var waitTime: Int = 0
    internal val inter: MutableLiveData<InterstitialAd> = MutableLiveData()
    internal val interIds: List<String>
        get() = getIds(interUnit, INTER_TEST_ID)

    override fun toString(): String {
        return "InterHolder(enable = $enable, count=$count, stepCount=$stepCount, waitTime=$waitTime)"
    }

}
