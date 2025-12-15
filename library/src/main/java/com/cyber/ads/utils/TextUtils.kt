package com.cyber.ads.utils

object TextUtils {
    init {
        System.loadLibrary("textutils")
    }

    external fun contains(text: String): Boolean
    external fun isNotNull(text: String): Boolean
}
