package com.cyber.ads.iap


import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails

sealed class IapState {
    data object Idle : IapState()
    data class ProductsReady(val productIds: List<String>) : IapState()
    data class Purchasing(val productId: String) : IapState()
    data class Purchased(val productId: String) : IapState()
    data class Restored(val activeProductIds: List<String>) : IapState()
    data class LoadError(val message: String?) : IapState()
    data class PurchaseError(val message: String?, val productId: String? = null) : IapState()
    data class RestoreError(val message: String?) : IapState()
}

data class SkuInfo(
    val productId: String,
    val type: String,                 // INAPP / SUBS
    val title: String?,
    val description: String?,
    val formattedPrice: String?,
    val currencyCode: String?,
    val microsPrice: Long?,
    val offerToken: String? = null    // SUBS
)

internal fun ProductDetails.toSkuInfo(): SkuInfo {
    return when (productType) {
        BillingClient.ProductType.INAPP -> {
            val o = oneTimePurchaseOfferDetails
            SkuInfo(
                productId = productId,
                type = productType,
                title = title,
                description = description,
                formattedPrice = o?.formattedPrice,
                currencyCode = o?.priceCurrencyCode,
                microsPrice = o?.priceAmountMicros
            )
        }
        BillingClient.ProductType.SUBS -> {
            val offer = subscriptionOfferDetails?.firstOrNull()
            val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
            SkuInfo(
                productId = productId,
                type = productType,
                title = title,
                description = description,
                formattedPrice = phase?.formattedPrice,
                currencyCode = phase?.priceCurrencyCode,
                microsPrice = phase?.priceAmountMicros,
                offerToken = offer?.offerToken
            )
        }
        else -> SkuInfo(productId, productType, title, description, null, null, null)
    }
}
