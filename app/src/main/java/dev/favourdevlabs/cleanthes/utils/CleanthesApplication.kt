package dev.favourdevlabs.cleanthes.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager

class CleanthesApplication : Application() {

    private var startedActivityCount = 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(AppLifecycleTracker())
    }

    private inner class AppLifecycleTracker : ActivityLifecycleCallbacks {

        override fun onActivityStarted(activity: Activity) {
            startedActivityCount++
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivityCount--
            if (startedActivityCount == 0) onAppBackgrounded()
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity)                              = Unit
        override fun onActivityPaused(activity: Activity)                               = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle)  = Unit
        override fun onActivityDestroyed(activity: Activity)                            = Unit
    }

    private fun onAppBackgrounded() {
        SessionManager.clearSession()
        ClipboardHelper.clearClipboard(applicationContext)
    }
}
