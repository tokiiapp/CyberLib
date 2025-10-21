package com.cyber.ads.iap

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LiveData
import com.cyber.ads.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object IapUtils {
    /** Gọi 1 lần ở Application/Splash. Truyền list productId (inapp/subs). */
    @JvmStatic
    fun init(
        context: Context,
        config: IapConfig,
        listener: IapListener? = null
    ) = IapHelper.get(context).init(config, listener)

    /** Query lại ProductDetails & cache. */
    @JvmStatic
    fun refreshProducts(scope: CoroutineScope, onComplete: (Result<Unit>) -> Unit): Job {
        return scope.launch(Dispatchers.IO) {
            val result = try {
                IapHelper.instanceOrNull()?.refreshProducts()
            } catch (e: Exception) {
                Result.failure(e)
            }
            withContext(Dispatchers.Main) {
                result?.let { onComplete(it) }
            }
        }
    }

    /** Lấy giá đã format theo locale Play từ productId. */
    @JvmStatic
    fun getFormattedPrice(productId: String): String? =
        IapHelper.instanceOrNull()?.getFormattedPrice(productId)

    /** Lấy info đầy đủ của product. */
    @JvmStatic
    fun getProductInfo(productId: String): SkuInfo? =
        IapHelper.instanceOrNull()?.getProductInfo(productId)

    /** Mua theo productId. Với SUBS có nhiều offer, truyền offerToken nếu muốn. */
    @JvmStatic
    fun purchase(
        activity: Activity,
        productId: String,
        offerToken: String? = null
    ) = IapHelper.instanceOrNull()?.launchPurchase(activity, productId, offerToken)

    /** Khôi phục quyền lợi: trả về list productId đang active. */
    @JvmStatic
    fun restore(scope: CoroutineScope, onComplete: (Result<List<String>>) -> Unit): Job {
        return scope.launch(Dispatchers.IO) {
            val result = try {
                IapHelper.instanceOrNull()?.restore()
            } catch (e: Exception) {
                Result.failure(e)
            }
            result?.let { onComplete(it) }
        }
    }

    /** Quan sát trạng thái để cập nhật UI. */
    @JvmStatic
    fun observeState(): LiveData<IapState>? =
        IapHelper.instanceOrNull()?.state

    /** Mua. */
    @JvmStatic
    fun buy(activity: Activity, productId: String) {
        val offerToken = getProductInfo(productId)?.offerToken
        purchase(activity, productId, offerToken)
    }

    @JvmStatic
    fun getDisplayPrice(productId: String): MutableList<Map<String, Any?>>? =
        IapHelper.instanceOrNull()?.getDisplayPrice(productId)

    @JvmStatic
    fun openManageSubscription(
        context: Context,
        packageName: String,
    ) {
        try {
            val uri =
                Uri.parse("https://play.google.com/store/account/subscriptions?&package=$packageName")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.can_t_open_the_browser), Toast.LENGTH_SHORT
            ).show()
        }
    }

}
