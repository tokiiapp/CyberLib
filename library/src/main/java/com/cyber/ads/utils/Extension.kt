package com.cyber.ads.utils

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cyber.ads.admob.AdmobUtils
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

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

fun Context.dpToPx(number: Number): Int {
    val density = resources.displayMetrics.density
    return (number.toDouble() * density).toInt()
}

fun Fragment.dpToPx(number: Number): Int {
    val density = resources.displayMetrics.density
    return (number.toDouble() * density).toInt()
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
    val win = window ?: return
    win.setBackgroundDrawableResource(android.R.color.transparent)
    win.setFlags(
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    )
    win.decorView.systemUiVisibility = activity.window.decorView.systemUiVisibility

    setOnShowListener {
        if (activity.isFinishing || activity.isDestroyed) return@setOnShowListener
        val w = window ?: return@setOnShowListener
        w.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
    win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
}


internal fun CharSequence.toFirebaseEventName(): String {
    var event = this.toString().trim()
        .lowercase()
        .replace("[^a-z0-9_\\s]".toRegex(), "")
        .replace("\\s+".toRegex(), "_")

    if (event.isNotEmpty() && !event[0].isLetter()) {
        event = "event_$event"
    }

    return event.take(40)
}

fun setupInAppUpdate(activity: AppCompatActivity) {
    val updateManager = AppUpdateManagerFactory.create(activity)
    inAppUpdate(activity, updateManager)
}

private fun inAppUpdate(activity: AppCompatActivity, updateManager: AppUpdateManager) {
    val info: Task<AppUpdateInfo> = updateManager.appUpdateInfo
    info.addOnSuccessListener { update ->
        if (update.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
            flexibleUpdate(activity, update, updateManager)
        }
    }
    updateManager.registerListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            updateManager.completeUpdate()
        }
    }
}

private fun flexibleUpdate(
    activity: AppCompatActivity,
    info: AppUpdateInfo,
    updateManager: AppUpdateManager
) {
    try {
        updateManager.startUpdateFlowForResult(
            info,
            activity,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
            999
        )
    } catch (_: IntentSender.SendIntentException) {
    }
}
