package com.cyber.ads.solar

import android.content.Context
import android.util.Log
import com.cyber.ads.R
import com.cyber.ads.adjust.AdjustUtils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.LoadAdError
import com.reyun.solar.engine.AdType
import com.reyun.solar.engine.SolarEngineConfig
import com.reyun.solar.engine.SolarEngineManager
import com.reyun.solar.engine.infos.SEAdImpEventModel
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object SolarUtils {
    private const val TAG = "SolarUtils"
    private const val DEFAULT_MEDIATION_PLATFORM = "admob_sdk"
    private const val DEFAULT_AD_NETWORK_PLATFORM = "AdMob"
    private const val DEFAULT_CURRENCY = "USD"

    private val isInitialized = AtomicBoolean(false)

    private var isDebugSolar: Boolean = true
    private var currentChannel: String = "googleplay"
    private var includeZeroRevenueInDebug: Boolean = false

    @JvmField
    var userId: String? = null
    var appId: String = ""

    @JvmStatic
    @JvmOverloads
    fun init(
        context: Context,
        appKey: String,
        debug: Boolean = false,
        channel: String? = "googleplay",
        gdprArea: Boolean = false,
        adPersonalizationEnabled: Boolean = true,
        adUserDataEnabled: Boolean = true,
        configBlock: (SolarEngineConfig.Builder.() -> Unit)? = null
    ) {
        val appContext = context.applicationContext
        isDebugSolar = debug
        currentChannel = when {
            isDebugSolar -> "debug"
            else -> channel ?: "googleplay"
        }
        val builder = SolarEngineConfig.Builder().apply {
            if (debug) {
                logEnabled()
                isDebugModel = true
            } else {
                isDebugModel = false
            }
            setFbAppID(context.getString(R.string.facebook_id))
            isGDPRArea(gdprArea)
            adPersonalizationEnabled(adPersonalizationEnabled)
            adUserDataEnabled(adUserDataEnabled)
            configBlock?.invoke(this)
        }
        val config = builder.build()
        val manager = SolarEngineManager.getInstance()

        runCatching {
            manager.preInit(appContext, appKey)
            manager.setChannel(currentChannel)
            manager.initialize(appContext, appKey, config) { code ->
                if (code == 0) {
                    isInitialized.set(true)
                    appId = context.getString(R.string.admob_app_id) ?: context.packageName

                    userId = SolarEngineManager.getInstance().distinctId
                    Log.d(TAG, "SolarEngine initialized (debug=$debug, channel=$currentChannel)")
                } else {
                    Log.e(TAG, "SolarEngine init failed, code=$code")
                }
            }
        }.onFailure { t ->
            Log.e(TAG, "Failed to initialize SolarEngine", t)
            throw t
        }
    }

    @JvmStatic
    @JvmOverloads
    fun setDebugPolicy(
        enableDebugSend: Boolean = true,
        debugChannel: String = "debug",
        includeZeroRevenue: Boolean = true
    ) {
        isDebugSolar = enableDebugSend
        currentChannel = debugChannel
        includeZeroRevenueInDebug = includeZeroRevenue
        if (isInitialized.get()) {
            SolarEngineManager.getInstance().setChannel(currentChannel)
        }
    }

    @JvmStatic
    fun updateConsent(
        gdprArea: Boolean? = null,
        adPersonalizationEnabled: Boolean? = null,
        adUserDataEnabled: Boolean? = null
    ) {
        if (!ensureInitialized()) return
        val mgr = SolarEngineManager.getInstance()
        gdprArea?.let { mgr.setGDPRArea(it) }
        // adPersonalizationEnabled / adUserDataEnabled: SDK có API public nào thì bật tại đây.
    }

    @JvmStatic
    fun setPublicProps(props: JSONObject) {
        if (!ensureInitialized()) return
        // SolarEngineManager.getInstance().setPublicEventProperty(props)
    }

    @JvmStatic
    fun clearPublicProps() {
        if (!ensureInitialized()) return
        // SolarEngineManager.getInstance().clearPublicEventProperty()
    }

    @JvmStatic
    fun setUserId(userId: String) {
        if (!ensureInitialized()) return
        // SolarEngineManager.getInstance().distinctId = userId
    }

    @JvmStatic
    @JvmOverloads
    fun trackAdImpression(
        ad: AdValue,
        adUnit: String?,
        format: String,
        mediationPlatform: String = DEFAULT_MEDIATION_PLATFORM,
        adNetworkPlatform: String = DEFAULT_AD_NETWORK_PLATFORM,
        customProperties: JSONObject? = null
    ) {
        if (!ensureInitialized()) return

        runCatching {
            val micros = ad.valueMicros
            val isZeroRevenue = micros <= 0
            if (isZeroRevenue && !(isDebugSolar && includeZeroRevenueInDebug)) {
                Log.d(TAG, "Skip Solar ad impression: valueMicros=$micros (debug=$isDebugSolar)")
                return
            }

            val revenue = (if (micros < 0) 0 else micros) / 1_000_000.0
            val ecpm = revenue * 1000.0
            val currency = (ad.currencyCode ?: DEFAULT_CURRENCY).uppercase(Locale.US)
            val adType = mapFormatToAdType(format)

            val props = JSONObject().apply {
                put("ad_unit_id", adUnit.orEmpty())
                put("precision", ad.precisionType)
                put("source_sdk", mediationPlatform)
                put("debug", isDebugSolar)                 // ← cờ debug
                put("debug_channel", currentChannel)       // ← kênh debug/prod
                if (userId != null) put("user_id", userId)
                if (isZeroRevenue) put("no_revenue", true) // ← đánh dấu zero revenue
            }
            customProperties?.let { mergeJson(props, it) }

            val event = SEAdImpEventModel(
                adNetworkPlatform,
                mediationPlatform,
                adType.intValue(),
                appId,
                adUnit.orEmpty(),
                ecpm,
                currency,
                /* p7 = */ true,
                props
            )

            SolarEngineManager.getInstance().trackAdImpression(event)
            Log.d(
                TAG,
                "Solar ad imp tracked: unit=${adUnit.orEmpty()}, fmt=${format.lowercase()}, ecpm=${
                    "%.6f".format(
                        ecpm
                    )
                } $currency, debug=$isDebugSolar"
            )
        }.onFailure { e ->
            Log.e(TAG, "Failed to track Solar ad impression", e)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun forwardAdjustRevenueToSolar(
        adUnit: String?,
        format: String,
        mediationPlatform: String = DEFAULT_MEDIATION_PLATFORM,
        adNetworkPlatform: String = DEFAULT_AD_NETWORK_PLATFORM,
        customPropertiesProvider: (() -> JSONObject?)? = null
    ) {
        AdjustUtils.callback = createAdValueForwarder(
            adUnit = adUnit,
            format = format,
            mediationPlatform = mediationPlatform,
            adNetworkPlatform = adNetworkPlatform,
            customPropertiesProvider = customPropertiesProvider
        )
    }

    @JvmStatic
    @JvmOverloads
    fun createAdValueForwarder(
        adUnit: String?,
        format: String,
        mediationPlatform: String = DEFAULT_MEDIATION_PLATFORM,
        adNetworkPlatform: String = DEFAULT_AD_NETWORK_PLATFORM,
        customPropertiesProvider: (() -> JSONObject?)? = null
    ): (AdValue) -> Unit = { adValue ->
        val extras = runCatching { customPropertiesProvider?.invoke() }.getOrNull()
        trackAdImpression(
            ad = adValue,
            adUnit = adUnit,
            format = format,
            mediationPlatform = mediationPlatform,
            adNetworkPlatform = adNetworkPlatform,
            customProperties = extras
        )
    }

    @JvmStatic
    fun trackEvent(eventName: String, properties: JSONObject? = null) {
        if (!ensureInitialized()) return
        if (eventName.isBlank()) {
            Log.w(TAG, "trackEvent called with blank eventName")
            return
        }

        runCatching {
            val payload = (properties ?: JSONObject()).apply {
                put("debug", isDebugSolar)
                put("debug_channel", currentChannel)
                if (userId != null && !has("user_id")) put("user_id", userId)
            }
            SolarEngineManager.getInstance().track(eventName, payload)
            Log.d(TAG, "Solar custom event: $eventName (debug=$isDebugSolar)")
        }.onFailure { e ->
            Log.e(TAG, "Failed to track Solar event: $eventName", e)
        }
    }

    private fun ensureInitialized(): Boolean {
        val initialized = isInitialized.get()
        if (!initialized) {
            Log.w(TAG, "SolarEngine not initialized. Call SolarUtils.init() first.")
        }
        return initialized
    }

    private fun JSONObject.putIfNotNull(key: String, value: Any?) {
        if (value != null) put(key, value)
    }

    @JvmStatic
    @JvmOverloads
    fun trackAdLoadFailure(
        adUnit: String?,
        format: String,                  // banner | interstitial | rewarded | app_open | native | mrec...
        errorCode: Int?,
        errorDomain: String? = null,
        errorMessage: String? = null,
        mediationPlatform: String = DEFAULT_MEDIATION_PLATFORM,
        adNetworkPlatform: String = DEFAULT_AD_NETWORK_PLATFORM,
        isNoFill: Boolean? = null,       // gợi ý: với AdMob code=3 => NO_FILL
        latencyMs: Long? = null,
        waterfallIndex: Int? = null,
        extras: JSONObject? = null
    ) {
        if (!ensureInitialized()) return
        runCatching {
            val props = JSONObject().apply {
                put("event_type", "load_fail")
                put("format", format.lowercase(Locale.US))
                put("ad_unit_id", adUnit.orEmpty())
                put("app_id", appId)
                put("mediation", mediationPlatform)
                put("network", adNetworkPlatform)
                put("debug", isDebugSolar)
                put("debug_channel", currentChannel)
                userId?.let { put("user_id", it) }
                putIfNotNull("error_code", errorCode)
                putIfNotNull("error_domain", errorDomain)
                putIfNotNull("error_message", errorMessage)
                putIfNotNull("no_fill", isNoFill)
                putIfNotNull("latency_ms", latencyMs)
                putIfNotNull("waterfall_index", waterfallIndex)
                if (extras != null) mergeJson(this, extras)
            }
            SolarEngineManager.getInstance().track("ad_load_failed", props)
            Log.d(
                TAG,
                "Solar track ad_load_failed: unit=${adUnit.orEmpty()}, fmt=$format, code=$errorCode"
            )
        }.onFailure { Log.e(TAG, "trackAdLoadFailure error", it) }
    }

    @JvmStatic
    @JvmOverloads
    fun trackAdShowFailure(
        adUnit: String?,
        format: String,
        errorCode: Int?,
        errorDomain: String? = null,
        errorMessage: String? = null,
        mediationPlatform: String = DEFAULT_MEDIATION_PLATFORM,
        adNetworkPlatform: String = DEFAULT_AD_NETWORK_PLATFORM,
        extras: JSONObject? = null
    ) {
        if (!ensureInitialized()) return
        runCatching {
            val props = JSONObject().apply {
                put("event_type", "show_fail")
                put("format", format.lowercase(Locale.US))
                put("ad_unit_id", adUnit.orEmpty())
                put("app_id", appId)
                put("mediation", mediationPlatform)
                put("network", adNetworkPlatform)
                put("debug", isDebugSolar)
                put("debug_channel", currentChannel)
                userId?.let { put("user_id", it) }
                putIfNotNull("error_code", errorCode)
                putIfNotNull("error_domain", errorDomain)
                putIfNotNull("error_message", errorMessage)
                if (extras != null) mergeJson(this, extras)
            }
            SolarEngineManager.getInstance().track("ad_show_failed", props)
            Log.d(
                TAG,
                "Solar track ad_show_failed: unit=${adUnit.orEmpty()}, fmt=$format, code=$errorCode"
            )
        }.onFailure { Log.e(TAG, "trackAdShowFailure error", it) }
    }

    @JvmStatic
    fun trackAdLoadFailure(
        adUnit: String?,
        format: String,
        loadAdError: LoadAdError,
        mediationPlatform: String = DEFAULT_MEDIATION_PLATFORM,
        adNetworkPlatform: String = DEFAULT_AD_NETWORK_PLATFORM,
        latencyMs: Long? = null,
        waterfallIndex: Int? = null,
        extras: JSONObject? = null
    ) {
        val isNoFill = loadAdError.code == 3 // AdRequest.ERROR_CODE_NO_FILL (mapping mới vẫn =3)
        trackAdLoadFailure(
            adUnit = adUnit,
            format = format,
            errorCode = loadAdError.code,
            errorDomain = loadAdError.domain,
            errorMessage = loadAdError.message,
            mediationPlatform = mediationPlatform,
            adNetworkPlatform = adNetworkPlatform,
            isNoFill = isNoFill,
            latencyMs = latencyMs,
            waterfallIndex = waterfallIndex,
            extras = extras
        )
    }

    @JvmStatic
    fun trackAdShowFailure(
        adUnit: String?,
        format: String,
        adError: AdError,
        mediationPlatform: String = DEFAULT_MEDIATION_PLATFORM,
        adNetworkPlatform: String = DEFAULT_AD_NETWORK_PLATFORM,
        extras: JSONObject? = null
    ) {
        trackAdShowFailure(
            adUnit = adUnit,
            format = format,
            errorCode = adError.code,
            errorDomain = adError.domain,
            errorMessage = adError.message,
            mediationPlatform = mediationPlatform,
            adNetworkPlatform = adNetworkPlatform,
            extras = extras
        )
    }

    private fun mergeJson(target: JSONObject, source: JSONObject) {
        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            target.put(key, source.get(key))
        }
    }

    private fun mapFormatToAdType(format: String): AdType = when (format.lowercase(Locale.US)) {
        "banner", "adaptive_banner", "mrec" -> AdType.Banner
        "interstitial" -> AdType.Interstitial
        "rewarded", "rewarded_interstitial", "reward" -> AdType.RewardVideo
        "native", "native_advanced" -> AdType.Native
        "app_open", "splash" -> AdType.Splash
        else -> AdType.OTHER
    }
}
