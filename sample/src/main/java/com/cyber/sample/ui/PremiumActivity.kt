package com.cyber.sample.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.admob.RemoteUtils
import com.cyber.ads.iap.IapState
import com.cyber.ads.iap.IapUtils
import com.cyber.sample.R
import com.cyber.sample.databinding.ActivityPremiumBinding

class PremiumActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPremiumBinding
    private val fromSplash by lazy { intent.getBooleanExtra("fromSplash", false) }
    private val pidMonthly by lazy { getString(com.cyber.ads.R.string.monthly_pro) }
    private val pidTrialYearly by lazy { getString(com.cyber.ads.R.string.trial_yearly_pro) }
    private val pidYearly by lazy { getString(com.cyber.ads.R.string.yearly_pro) }
    private lateinit var dialog: AlertDialog
    private var premiumHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        dialog = RemoteUtils.dialogNoInternet(this) {
            if (it) IapUtils.refreshProducts(lifecycleScope) {}
        }
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnClose.visibility = if (fromSplash) View.VISIBLE else View.GONE
        binding.btnClose.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        try {
            IapUtils.refreshProducts(lifecycleScope) {
                IapUtils.observeState()?.observe(this) { st ->
                    when (st) {
                        is IapState.ProductsReady -> {
                            bindPrices()
                            dialog.dismiss()
                        }

                        is IapState.Purchasing -> {
                        }

                        is IapState.Purchased -> {
                            handlePremiumOnce()
                        }

                        is IapState.Restored -> {
                            if (!premiumHandled) {
                                if (st.activeProductIds.isNotEmpty()) handlePremiumOnce()
                                else Toast.makeText(
                                    this,
                                    getString(R.string.no_active_subscription),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        is IapState.LoadError -> {
                            dialog.show()
                        }

                        is IapState.RestoreError -> {
                            val message =
                                st.message?.takeIf { it.isNotEmpty() } ?: getString(R.string.error)
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }

                        is IapState.PurchaseError -> {
                            val message =
                                st.message?.takeIf { it.isNotEmpty() } ?: getString(R.string.error)
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        }

                        else -> {
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        binding.btnMonth.setOnClickListener { IapUtils.buy(this, pidMonthly) }
        binding.btnContinue.setOnClickListener { IapUtils.buy(this, pidTrialYearly) }
        binding.btnYear.setOnClickListener { IapUtils.buy(this, pidYearly) }
        binding.btnTermOfUse.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    PolicyActivity::class.java
                )
            )
        }
        binding.btnPolicy.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    PolicyActivity::class.java
                ).putExtra("isPolicy", true)
            )
        }
        binding.btnRestore.setOnClickListener {
            IapUtils.restore(lifecycleScope) {}
        }
    }

    private fun bindPrices() {
        binding.tvPriceMonth.text = IapUtils.getFormattedPrice(pidMonthly) ?: "_"
        binding.tvPriceYear.text = IapUtils.getFormattedPrice(pidYearly) ?: "_"
        setTrial()
    }


    private fun handlePremiumOnce() {
        if (premiumHandled) return
        premiumHandled = true
        Toast.makeText(this, getString(R.string.you_have_purchased), Toast.LENGTH_SHORT).show()
        AdmobUtils.isPremium = true
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun List<Map<String, Any?>>.merged(): Map<String, Any?> = buildMap {
        for (m in this@merged) putAll(m)
    }


    private fun unitTextEn(count: Int?, code: Int?): String {
        if (count == null || code == null) return ""
        return when (code) {
            0 -> if (count == 1) "day" else "days"
            1 -> if (count == 1) "week" else "weeks"
            2 -> if (count == 1) "month" else "months"
            3 -> if (count == 1) "year" else "years"
            else -> ""
        }
    }

    fun setTrial() {
        val infoList = IapUtils.getDisplayPrice(pidTrialYearly)
        if (infoList == null || infoList.isEmpty()) {
            binding.btnContinue.text = "—"
        } else {
            val info = infoList.merged()
            val price = info["price"] as? String
            val day = info["day"] as? Int
            val dayFree = info["dayFree"] as? Int
            val unitFree = info["unitFree"] as? Int
            val unit = info["unit"] as? Int

            val text = when {
                dayFree != null && unitFree != null && price != null && day != null && unit != null -> {
                    val trialTxt =
                        getString(R.string.free_trial, dayFree, unitTextEn(dayFree, unitFree))
                    val mainTxt = "$price/${unitTextEn(day, unit)}"
                    "$trialTxt, then $mainTxt"
                }

                price != null && day != null && unit != null -> {
                    "$price/${unitTextEn(day, unit)}"
                }

                price != null -> price
                else -> "—"
            }
            binding.btnContinue.text = text
        }
    }
}
