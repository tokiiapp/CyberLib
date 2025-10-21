package com.cyber.sample.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cyber.ads.admob.AdmobUtils
import com.cyber.sample.R
import com.cyber.sample.RemoteConfig
import com.cyber.sample.databinding.FragmentIntroBinding

class IntroFragment : Fragment() {
    private var number = 1
    private val binding by lazy { FragmentIntroBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        number = arguments?.getInt(ARG_NUMBER) ?: 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables", "CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!isAdded && activity == null) {
            return
        }

        AdmobUtils.showNativeIntro(requireActivity(), RemoteConfig.NATIVE_INTRO, binding.frNative,number, object : AdmobUtils.NativeCallbackSimple(){})
        when (number) {
            1 -> {
                binding.tvTitle.text = getString(R.string.intro1)
                binding.tvContent.text = getString(R.string.intro1_content)
//                binding.imageDot.setImageResource(R.drawable.ic_dot1)
//                if (RemoteConfig.native_intro_fullscreen != "0" && AdsManager.isNativeIntroFullScreenReady) {
//                    binding.lottieSlide.show()
//                } else {
//                    binding.lottieSlide.hide()
//                }
            }

            2 -> {
                binding.tvTitle.text = getString(R.string.intro2)
                binding.tvContent.text = getString(R.string.intro2_content)
//                binding.imageDot.setImageResource(R.drawable.ic_dot2)
//                binding.lottieSlide.hide()
            }

            3 -> {
                binding.tvTitle.text = getString(R.string.intro3)
                binding.tvContent.text = getString(R.string.intro3_content)
//                binding.imageDot.setImageResource(R.drawable.ic_dot3)
//                binding.lottieSlide.hide()
            }
        }

        binding.btnNext.setOnClickListener {
            (activity as? IntroActivity)?.onNext()
        }
    }

    companion object {
        private const val ARG_NUMBER = "ARG_NUMBER"
        fun newInstance(number: Int): IntroFragment {
            val fragment = IntroFragment()
            val args = Bundle()
            args.putInt(ARG_NUMBER, number)
            fragment.arguments = args
            return fragment
        }
    }
}
