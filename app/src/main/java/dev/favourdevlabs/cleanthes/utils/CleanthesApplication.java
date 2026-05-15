package dev.favourdevlabs.cleanthes.utils;

import android.app.Application;
import android.app.Activity;
import android.os.Bundle;

import dev.favourdevlabs.cleanthes.ui.auth.SessionManager;
import dev.favourdevlabs.cleanthes.utils.ClipboardHelper;

public class CleanthesApplication extends Application {

    private int startedActivityCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new AppLifecycleTracker());
    }

    private class AppLifecycleTracker implements ActivityLifecycleCallbacks {

        @Override
        public void onActivityStarted(Activity activity) {
            startedActivityCount++;
        }

        @Override
        public void onActivityStopped(Activity activity) {
            startedActivityCount--;

            if (startedActivityCount == 0) {
                onAppBackgrounded();
            }
        }

        @Override
        public void onActivityCreated(Activity a, Bundle b) {
        }

        @Override
        public void onActivityResumed(Activity a) {
        }

        @Override
        public void onActivityPaused(Activity a) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity a, Bundle b) {
        }

        @Override
        public void onActivityDestroyed(Activity a) {
        }
    }

    private void onAppBackgrounded() {
        SessionManager.clearSession();
        ClipboardHelper.clearClipboard(getApplicationContext());
    }
}
