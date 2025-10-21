package com.cyber.ads.remote

open class NativeMultiHolder(key: String) : NativeHolder(key) {
    internal val holders = mutableListOf<NativeHolder>()

    override fun toString(): String {
        return "enable = $enable | NativeMultiHolder(holders=$holders)"
    }

}
