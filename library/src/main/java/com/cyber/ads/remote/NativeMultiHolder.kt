package com.cyber.ads.remote

open class NativeMultiHolder(key: String) : NativeHolder(key) {
    internal val holders = mutableListOf<NativeHolder>()

    /**
     * Kiểm tra xem có holder nào đang loading hay không
     */
    fun isAnyLoading(): Boolean {
        return holders.any { it.isNativeLoading }
    }

    /**
     * Kiểm tra xem tất cả holders đã ready hay chưa
     */
    fun isAllReady(): Boolean {
        return holders.isNotEmpty() && holders.all { !it.isNativeLoading && it.isNativeReady() }
    }

    override fun toString(): String {
        return "enable = $enable | NativeMultiHolder(holders=$holders)"
    }

}
