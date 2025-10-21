package com.cyber.ads.iap

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

internal class IapHelper private constructor(private val appContext: Context) :
    PurchasesUpdatedListener {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var billing: BillingClient? = null

    private var config: IapConfig = IapConfig()
    private var listener: IapListener? = null

    val state = MutableLiveData<IapState>(IapState.Idle)
    var stateCurrent: IapState = IapState.Idle

    private val productDetailsCache = mutableMapOf<String, ProductDetails>()
    private val initialized = AtomicBoolean(false)

    fun init(cfg: IapConfig, lst: IapListener?) {
        config = cfg
        listener = lst
        if (initialized.compareAndSet(false, true)) {
            billing = BillingClient.newBuilder(appContext)
                .setListener(this)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
                )
                .build()
            connectBilling()
        } else {
            scope.launch { refreshProducts() }
        }
    }

    private fun connectBilling() {
        billing?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { refreshProducts() }
                } else {
                    postLoadError("Billing setup fail: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() { /* auto reconnect on demand */
            }
        })
    }

    suspend fun refreshProducts(): Result<Unit> = withContext(Dispatchers.IO) {
        val client =
            billing ?: return@withContext Result.failure(IllegalStateException("Billing null"))
        try {
            productDetailsCache.clear()

            // INAPP
            if (config.inappProductIds.isNotEmpty()) {
                val inappParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(config.inappProductIds.map {
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(it)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    }).build()
                val inappRes = client.queryProductDetails(inappParams)
                if (inappRes.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    inappRes.productDetailsList?.forEach { pd ->
                        productDetailsCache[pd.productId] = pd
                    }
                }
            }

            // SUBS
            if (config.subsProductIds.isNotEmpty()) {
                val subsParams = QueryProductDetailsParams.newBuilder()
                    .setProductList(config.subsProductIds.map {
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(it)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    }).build()
                val subsRes = client.queryProductDetails(subsParams)
                if (subsRes.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    subsRes.productDetailsList?.forEach { pd ->
                        productDetailsCache[pd.productId] = pd
                    }
                }
            }
            if (productDetailsCache.isEmpty()) {
                postLoadError("No product found")
                return@withContext Result.failure(IllegalStateException("No product found"))
            }
            listener?.let { restore() }
            state.postValue(IapState.ProductsReady(productDetailsCache.keys.toList()))
            Result.success(Unit)
        } catch (e: Exception) {
            postLoadError(e.message)
            Result.failure(e)
        }
    }

    fun getFormattedPrice(productId: String): String? {
        return productDetailsCache[productId]?.let { pd ->
            when (pd.productType) {
                BillingClient.ProductType.INAPP ->
                    pd.oneTimePurchaseOfferDetails?.formattedPrice

                BillingClient.ProductType.SUBS ->
                    pd.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.pricingPhases?.pricingPhaseList
                        ?.firstOrNull()?.formattedPrice

                else -> null
            }
        }
    }

    fun getProductInfo(productId: String): SkuInfo? {
        val pd = productDetailsCache[productId] ?: return null
        return pd.toSkuInfo()
    }

    fun launchPurchase(activity: Activity, productId: String, offerToken: String?) {
        val client = billing ?: return postPurchaseError("Billing not connected")
        val pd = productDetailsCache[productId]
            ?: return postPurchaseError("Product not found: $productId")

        val pdParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(pd)
            .apply {
                if (pd.productType == BillingClient.ProductType.SUBS) {
                    val token = offerToken ?: pd.subscriptionOfferDetails?.firstOrNull()?.offerToken
                    if (token != null) setOfferToken(token)
                }
            }
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(pdParams))
            .build()

        val res = client.launchBillingFlow(activity, flowParams)
        if (res.responseCode != BillingClient.BillingResponseCode.OK) {
            postPurchaseError("Launch purchase failed: ${res.debugMessage}")
        } else {
            state.postValue(IapState.Purchasing(productId))
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { handlePurchases(purchases) }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(appContext, "error", Toast.LENGTH_SHORT).show()
        } else {
            postPurchaseError(result.debugMessage)
        }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        val client = billing ?: return
        purchases.forEach { p ->
            if (p.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!p.isAcknowledged && config.autoAcknowledge) {
                    val ack = client.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(p.purchaseToken)
                            .build()
                    )
                    if (ack.responseCode != BillingClient.BillingResponseCode.OK) {
                        postPurchaseError("Acknowledge failed: ${ack.debugMessage}")
                        return
                    }
                }
                p.products.forEach { pid ->
                    state.postValue(IapState.Purchased(pid))
                }
            }
        }
    }

    suspend fun restore(): Result<List<String>> = withContext(Dispatchers.IO) {
        val client =
            billing ?: return@withContext Result.failure(IllegalStateException("Billing null"))
        try {
            val inapp = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            val subs = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

            val active = mutableListOf<String>()
            (inapp.purchasesList + subs.purchasesList).forEach { p ->
                if (p.purchaseState == Purchase.PurchaseState.PURCHASED) active += p.products
            }

            listener?.onRestoreSuccess(active)
            listener = null
            state.postValue(IapState.Restored(active))
            Result.success(active)
        } catch (e: Exception) {
            postRestoreError(e.message)
            Result.failure(e)
        }
    }

    private fun postLoadError(msg: String?) {
        state.postValue(IapState.LoadError(msg))
        listener?.onError(msg)
        listener = null
    }

    private fun postPurchaseError(msg: String?, productId: String? = null) {
        state.postValue(IapState.PurchaseError(msg, productId))
    }

    private fun postRestoreError(msg: String?) {
        state.postValue(IapState.RestoreError(msg))
    }

    fun getDisplayPrice(productId: String): MutableList<Map<String, Any?>>? {
        val infoTrial = mutableListOf<Map<String, Any?>>()
        val pd = productDetailsCache[productId] ?: return null
        return when (pd.productType) {
            BillingClient.ProductType.INAPP -> {
                val inAppInfo = pd.oneTimePurchaseOfferDetails?.formattedPrice
                infoTrial.add(mapOf("price" to inAppInfo))
                return infoTrial
            }

            BillingClient.ProductType.SUBS -> {
                val offer = pd.subscriptionOfferDetails?.firstOrNull() ?: return null
                val phases = offer.pricingPhases.pricingPhaseList
                if (phases.isEmpty()) return null

                val firstPaid = phases.firstOrNull { it.priceAmountMicros > 0 }
                val mainPhase = firstPaid ?: phases.last()
                val trialPhase = phases.firstOrNull()?.takeIf { it.priceAmountMicros == 0L }
                trialPhase?.let {
                    val period = it.billingPeriod
                    val (number, periodUnit) = formatIsoPeriodVi(period)
                    infoTrial.add(mapOf("dayFree" to number))
                    infoTrial.add(mapOf("unitFree" to periodUnit))
                } ?: ""

                val mainPrice = mainPhase.formattedPrice
                val (number, periodUnit) = formatIsoPeriodVi(mainPhase.billingPeriod)
                infoTrial.add(mapOf("price" to mainPrice))
                infoTrial.add(mapOf("day" to number))
                infoTrial.add(mapOf("unit" to periodUnit))
                return infoTrial
            }

            else -> null
        }
    }

    private fun formatIsoPeriodVi(iso: String): Pair<Int?, Int?> {
        if (iso.isEmpty()) return Pair(null, null)
        val m = Regex("""P(\d+)([DWMY])""").matchEntire(iso) ?: return Pair(null, null)
        val (numStr, unit) = m.destructured
        val n = numStr.toIntOrNull() ?: return Pair(null, null)
        val time = when (unit) {
            "D" -> 0
            "W" -> 1
            "M" -> 2
            "Y" -> 3
            else -> null
        }
        return Pair(n, time)
    }

    companion object {
        @Volatile
        private var INSTANCE: IapHelper? = null
        fun get(context: Context): IapHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: IapHelper(context.applicationContext).also { INSTANCE = it }
            }

        fun instanceOrNull(): IapHelper? = INSTANCE
    }
}
