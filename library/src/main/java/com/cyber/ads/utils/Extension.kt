package com.cyber.ads.utils

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import com.cyber.ads.admob.AdmobUtils

fun Context.toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, msg, length).show()
    }
}

@Keep
fun log(msg: String) {
    if (AdmobUtils.isTesting || Helper.enableReleaseLog) Log.d("===L", msg)
}

@Keep
fun logE(msg: String) {
    if (AdmobUtils.isTesting || Helper.enableReleaseLog) Log.e("===L", msg)
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun Int.dpToPx(context: Context): Int {
    val density = context.resources.displayMetrics.density
    return (this * density).toInt()
}

fun Context.prefs(): SharedPreferences {
    return getSharedPreferences("APP_PREFS", MODE_PRIVATE)
}

inline fun <reified T : Activity> Context.addActivity(block: Intent.() -> Unit = {}) {
    startActivity(Intent(this, T::class.java).apply(block))
}

inline fun <reified T : Activity> Context.replaceActivity(block: Intent.() -> Unit = {}) {
    val i = Intent(this, T::class.java).apply(block)
    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(i)
}

fun AlertDialog.setupDialog(activity: Activity) {
    window!!.setBackgroundDrawableResource(android.R.color.transparent)
    window!!.setFlags(
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    )
    window!!.decorView.systemUiVisibility = activity.window.decorView.systemUiVisibility

    setOnShowListener {
        window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        val wm =
            activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.updateViewLayout(window!!.decorView, window!!.attributes)
    }
//    window?.setDimAmount(0.8f)
    window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
}