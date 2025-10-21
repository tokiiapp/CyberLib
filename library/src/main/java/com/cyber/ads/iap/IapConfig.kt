package com.cyber.ads.iap


data class IapConfig(
    @JvmField
    val inappProductIds: List<String> = emptyList(),
    @JvmField
    val subsProductIds: List<String> = emptyList(),
    @JvmField
    val autoAcknowledge: Boolean = true
)

interface IapListener {
    /** Khôi phục thành công: danh sách productId đang active. */
    fun onRestoreSuccess(activeProductIds: List<String>) {}
    /** Lỗi chung. */
    fun onError(message: String?) {}
}
