package com.cyber.ads.onboading

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.admob.OnResumeUtils
import com.cyber.ads.databinding.FragmentNativeFullScreenBinding

class NativeFullScreenFragment : Fragment() {
    private val binding by lazy { FragmentNativeFullScreenBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isAdded && activity == null) return
        val introActivity = requireActivity() as BaseIntroActivity
        binding.btnNext.setOnClickListener { introActivity.onNext() }
        AdmobUtils.showNativeFull(introActivity, introActivity.nativeIntroFull, binding.flNative, object : AdmobUtils.NativeCallbackSimple() {})
    }

    override fun onResume() {
        super.onResume()
        OnResumeUtils.disableOnResume(activity?.javaClass)
    }

    override fun onPause() {
        super.onPause()
        OnResumeUtils.enableOnResume(activity?.javaClass)
    }
}