package com.cyber.ads.onboading

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cyber.ads.R
import com.cyber.ads.admob.AdmobUtils
import com.cyber.ads.databinding.FragmentIntroBinding
import com.cyber.ads.utils.Helper
import com.cyber.ads.utils.logE
import com.cyber.ads.utils.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class BaseIntroFragment : Fragment() {
    private var number = 1
    private val binding by lazy { FragmentIntroBinding.inflate(layoutInflater) }
    private val introSetup by lazy {
        Helper.settings()?.get("intro${number}_setup")?.asString?.replace(Regex("\\s+"), "")
            ?.lowercase()?.takeIf { it.isNotBlank() } ?: ""
    }

    private val nextLayout by lazy {
        if (introSetup.substringAfter("ds=").firstOrNull()?.toString() == "1") {
            binding.center.root.apply { visible() }
        } else {
            binding.right.root.apply { visible() }
        }
    }

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

        (activity as? BaseIntroActivity)?.nativeIntro?.let {
            AdmobUtils.showNativeIntro(
                requireActivity(),
                it,
                binding.flNative,
                number,
                object : AdmobUtils.NativeCallbackSimple() {})
        } ?: logE("BaseIntroFragment: NativeIntro is null")

        val ivIndicator = nextLayout.findViewById<ImageView>(R.id.ivIndicator)

        when (number) {
            1 -> {
                ivIndicator.setImageResource(R.drawable.ic_dot1)
                binding.tvTitle.text = getString(R.string.intro1)
                binding.ivImage.setImageResource(getDrawableId("im_intro1"))
                binding.tvContent.text = getString(R.string.intro1_content)
            }

            2 -> {
                ivIndicator.setImageResource(R.drawable.ic_dot2)
                binding.tvTitle.text = getString(R.string.intro2)
                binding.ivImage.setImageResource(getDrawableId("im_intro2"))
                binding.tvContent.text = getString(R.string.intro2_content)
            }

            3 -> {
                ivIndicator.setImageResource(R.drawable.ic_dot3)
                binding.tvTitle.text = getString(R.string.intro3)
                binding.ivImage.setImageResource(getDrawableId("im_intro3"))
                binding.tvContent.text = getString(R.string.intro3_content)
            }
        }
    }

    private fun checkAndShowNext() {
        val nativeIntro = (activity as? BaseIntroActivity)?.nativeIntro
        if (nativeIntro == null) setupButtonNext()
        else {
            lifecycleScope.launch(Dispatchers.Main) {
                runCatching {
                    withTimeout(5000) {
                        while (true) {
                            val isNativeIntroDone =
                                !AdmobUtils.isNativeIntroLoading(nativeIntro)
                            if (isNativeIntroDone) {
                                if ((activity as? BaseIntroActivity)?.isFinishing == false
                                    && (activity as? BaseIntroActivity)?.isDestroyed == false
                                ) {
                                    setupButtonNext()
                                }
                                break
                            }
                            delay(200)
                        }
                    }
                }.onFailure {
                    if ((activity as? BaseIntroActivity)?.isFinishing == false
                        && (activity as? BaseIntroActivity)?.isDestroyed == false
                    ) {
                        setupButtonNext()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndShowNext()
    }

    private fun getDrawableId(name: String): Int =
        resources.getIdentifier(name, "drawable", requireContext().packageName)

    private fun setupButtonNext() {
        val btnNext = nextLayout.findViewById<TextView>(R.id.btnNext)
        btnNext.setOnClickListener { (activity as? BaseIntroActivity)?.onNext() }
        //* Delay to show next
        val delayMillis = introSetup.substringAfter("delay=").toLongOrNull() ?: 0
        if (delayMillis > 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(delayMillis)
                btnNext.visible()
            }
        } else {
            btnNext.visible()
        }
        //* Next style: 1-Button, 2-Text (Default)
        if (introSetup.substringAfter("next=").firstOrNull()?.toString() == "1") {
            btnNext.setBackgroundResource(R.drawable.bg_intro_button)
            (btnNext).setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.color_native_install_button_text
                )
            )
        } else {
            btnNext.setBackgroundResource(0)
            (btnNext).setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.native_button_color
                )
            )
        }

    }

    companion object {
        private const val ARG_NUMBER = "ARG_NUMBER"
        fun newInstance(number: Int): BaseIntroFragment {
            val fragment = BaseIntroFragment()
            val args = Bundle()
            args.putInt(ARG_NUMBER, number)
            fragment.arguments = args
            return fragment
        }
    }
}
