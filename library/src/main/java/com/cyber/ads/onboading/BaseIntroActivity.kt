package com.cyber.ads.onboading

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.databinding.ActivityIntroBinding
import com.cyber.ads.remote.NativeHolder
import com.cyber.ads.remote.NativeMultiHolder
import com.cyber.ads.utils.Helper

abstract class BaseIntroActivity : AppCompatActivity() {
    private val binding by lazy { ActivityIntroBinding.inflate(layoutInflater) }
    private var fragments = mutableListOf<Fragment>()
    abstract var nativeIntroFull: NativeHolder
    abstract var nativeIntro: NativeMultiHolder
    abstract fun nextActivity()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        createListFragment()
        binding.viewPager.adapter = IntroAdapter(this)
        binding.viewPager.isUserInputEnabled =
            Helper.settings()?.get("intro_swipe")?.asString?.lowercase() != "false"
    }

    private fun createListFragment() {
        fragments.add(BaseIntroFragment.newInstance(1))
        if (nativeIntroFull.isNativeReady() &&
            nativeIntroFull.enable().contains("1") && AdmobUtils.isEnableAds
        ) {
            fragments.add(NativeFullScreenFragment())
        }
        fragments.add(BaseIntroFragment.newInstance(2))
        if (nativeIntroFull.isNativeReady() &&
            nativeIntroFull.enable().contains("2") && AdmobUtils.isEnableAds
        ) {
            fragments.add(NativeFullScreenFragment())
        }
        fragments.add(BaseIntroFragment.newInstance(3))

        onBackPressedDispatcher.addCallback(this) {
            val current = binding.viewPager.currentItem
            if (current > 0) {
                binding.viewPager.currentItem = current - 1
            }
        }
    }

    fun onNext() {
        val current = binding.viewPager.currentItem
        if (current == fragments.size - 1) {
            nextActivity()
        } else {
            if (nativeIntroFull.enable()
                    .contains("${current + 1}") && AdmobUtils.isEnableAds && !nativeIntroFull.isNativeReady()
            ) {
                AdmobUtils.loadAndShowNativeIntro(this, nativeIntroFull) {
                    binding.viewPager.setCurrentItem(current + 1, true)
                }
            } else {
                binding.viewPager.setCurrentItem(current + 1, true)
            }
        }
    }

    inner class IntroAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }

        override fun getItemCount(): Int {
            return fragments.size
        }
    }
}
