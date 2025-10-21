package com.cyber.sample.utils

import android.content.Context
import com.cyber.demo.opening.LanguageModel
import com.cyber.sample.R
import java.util.Locale

class Common {

    companion object {
        var currentLang: String? = null

        fun getListLocation(context: Context): ArrayList<LanguageModel> {
            return ArrayList<LanguageModel>().apply {
                add(LanguageModel(R.drawable.english, "English", "en"))
            }
        }

        fun setLocale(context: Context) {
            val language = currentLang ?: "en"
            val myLocale = Locale(language)
            Locale.setDefault(myLocale)
            val resource = context.resources
            val displayMetrics = resource.displayMetrics
            val configuration = resource.configuration
            configuration.setLocale(myLocale)
            resource.updateConfiguration(configuration, displayMetrics)
        }

    }
}