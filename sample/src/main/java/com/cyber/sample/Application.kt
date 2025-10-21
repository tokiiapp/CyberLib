package com.cyber.sample

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.view.View
import com.cyber.ads.adjust.AdjustUtils
import com.cyber.ads.application.AdsApplication

class Application : AdsApplication(), ActivityLifecycleCallbacks {
    override fun onCreateApplication() {
        AdjustUtils.initAdjust(this, "", false)
        registerActivityLifecycleCallbacks(this)

    }

//    override fun onTrimMemory(level: Int) {
//        super.onTrimMemory(level)
//        if (level == TRIM_MEMORY_UI_HIDDEN) {
//            log("onTrimMemory: $level")
//            OnResumeUtils.getInstance().timeToBackground = System.currentTimeMillis()
//        }
//    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activity.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
//        Common.setLocale(activity)
//        log(activity.javaClass.simpleName)
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

}