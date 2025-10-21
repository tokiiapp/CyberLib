package com.cyber.ads.remote

class SplashHolder(key: String) : InterHolder(key) {
    internal var appOpenUnit: AdUnit? = null
    internal val appOpenIds: List<String>
        get() = getIds(appOpenUnit, APP_OPEN_TEST_ID)

    override fun toString(): String {
        return "SplashHolder(${appOpenUnit?.name}: enable = $enable, count=$count, stepCount=$stepCount, waitTime=$waitTime"
    }

}
