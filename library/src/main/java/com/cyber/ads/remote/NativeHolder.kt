package com.cyber.ads.remote

import androidx.lifecycle.MutableLiveData
import com.cyber.ads.custom.LoadingSize
import com.google.android.gms.ads.nativead.NativeAd

open class NativeHolder(key: String) : BaseHolder(key) {
    internal var nativeUnit: AdUnit? = null
    internal var nativeAd: MutableLiveData<NativeAd> = MutableLiveData()
    internal var isNativeLoading = false
    internal var currentAdId = ""
    internal open var loadingSize = LoadingSize.MEDIUM
    internal open var nativeTemplate = "big1"
    internal var anchor = "bottom"
    internal var layoutId: Int? = null
    internal var refreshRate = 0
    internal var collapConfig: List<Int>? = null
    internal var loadTimestamp = 0L
    internal val nativeIds: List<String>
        get() = getIds(nativeUnit, NATIVE_TEST_ID)

    open fun isNativeReady() = nativeAd.value != null

    open fun customLayout(layoutId: Int, size: LoadingSize = LoadingSize.MEDIUM): NativeHolder {
        loadingSize = size
        this.layoutId = layoutId
        return this
    }

    open fun anchorTop(): NativeHolder {
        anchor = "top"
        return this
    }

    override fun toString(): String {
        return "NativeHolder(nativeUnit=$nativeUnit, layoutId=$layoutId, template=$nativeTemplate, loading=$loadingSize)"
    }
}
