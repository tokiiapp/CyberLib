package com.cyber.sample.ui


import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.cyber.sample.R
import com.cyber.sample.databinding.ActivityPolicyBinding

class PolicyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPolicyBinding
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        val isPolicy = intent.getBooleanExtra(EXTRA_IS_POLICY, false)
        val explicitUrl = intent.getStringExtra(EXTRA_URL)
        val titleText = intent.getStringExtra(EXTRA_TITLE)

        binding.toolbar.title = titleText ?: if (isPolicy)
            getString(R.string.privacy_policy)
        else
            getString(R.string.term_of_use_new)
        val url = explicitUrl ?: if (isPolicy)
            getString(com.cyber.ads.R.string.privacy_url)
        else
            getString(com.cyber.ads.R.string.terms_url)

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.seekBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.seekBar.visibility = View.GONE
            }
        }
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                binding.seekBar.progress = newProgress
                if (newProgress >= 99) {
                    binding.seekBar.visibility = View.GONE
                } else {
                    binding.seekBar.visibility = View.VISIBLE
                }
            }
        }
        binding.webView.loadUrl(url)
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_IS_POLICY = "isPolicy"
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
    }
}
