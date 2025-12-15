package com.cyber.ads.remote

import com.cyber.ads.custom.LoadingSize

class BannerHolder(key: String) : NativeHolder(key) {
    internal var bannerUnit: AdUnit? = null
    internal var bannerCollapUnit: AdUnit? = null
    override var nativeTemplate = "tiny1"
    override var loadingSize = LoadingSize.TINY
    internal val bannerIds: List<String>
        get() = getIds(bannerUnit, BANNER_TEST_ID)
    
    internal val bannerCollapIds: List<String>
        get() = getIds(bannerCollapUnit, BANNER_COLLAP_TEST_ID)

    fun customLayout(layoutId: Int): BannerHolder {
        loadingSize = LoadingSize.TINY
        this.layoutId = layoutId
        return this
    }

    override fun anchorTop(): BannerHolder {
        super.anchorTop()
        return this
    }

}
