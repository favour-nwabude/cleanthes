package dev.favourdevlabs.cleanthes.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class ClipboardHelper {

    private static final long CLEAR_DELAY_MS = 30_000L;

    private static final String CLIP_LABEL_PASSWORD = "Cleanthes Password";
    private static final String CLIP_USERNAME = "Cleanthes Username";
    private static final String CLIP_LABEL_GENERIC = "Cleanthes";

    private static Runnable pendingClearRunnable = null;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ClipboardHelper() {
    }

    public static void copyPassword(Context context, String password) {
        copyToClipboard(context, CLIP_LABEL_PASSWORD, password);
    }

    public static void copyUsername(Context context, String username) {
        copyToClipboard(context, CLIP_USERNAME, username);
    }

    public static void copyText(Context context, String text) {
        copyToClipboard(context, CLIP_LABEL_GENERIC, text);
    }

    public static void clearClipboard(Context context) {
        cancelPendingClear();
        overwriteClipboard(context);
    }

    public static boolean hasPendingClear() {
        return pendingClearRunnable != null;
    }

    private static void copyToClipboard(Context context, String label, String text) {
        cancelPendingClear();

        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard == null)
            return;

        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);

        pendingClearRunnable = () -> {
            overwriteClipboard(context);
            pendingClearRunnable = null;
        };

        mainHandler.postDelayed(pendingClearRunnable, CLEAR_DELAY_MS);
    }

    private static void overwriteClipboard(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard == null)
            return;

        ClipData emptyClip = ClipData.newPlainText(CLIP_LABEL_GENERIC, "");
        clipboard.setPrimaryClip(emptyClip);
    }

    private static void cancelPendingClear() {
        if (pendingClearRunnable != null) {
            mainHandler.removeCallbacks(pendingClearRunnable);
            pendingClearRunnable = null;
        }
    }
}
