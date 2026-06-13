package dev.favourdevlabs.cleanthes.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper

object ClipboardHelper {

    private const val CLEAR_DELAY_MS       = 30_000L
    private const val CLIP_LABEL_PASSWORD  = "Cleanthes Password"
    private const val CLIP_USERNAME        = "Cleanthes Username"
    private const val CLIP_LABEL_GENERIC   = "Cleanthes"

    private var pendingClearRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun copyPassword(context: Context, password: String) =
        copyToClipboard(context, CLIP_LABEL_PASSWORD, password)

    fun copyUsername(context: Context, username: String) =
        copyToClipboard(context, CLIP_USERNAME, username)

    fun copyText(context: Context, text: String) =
        copyToClipboard(context, CLIP_LABEL_GENERIC, text)

    fun clearClipboard(context: Context) {
        cancelPendingClear()
        overwriteClipboard(context)
    }

    fun hasPendingClear(): Boolean = pendingClearRunnable != null

    private fun copyToClipboard(context: Context, label: String, text: String) {
        cancelPendingClear()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))

        pendingClearRunnable = Runnable {
            overwriteClipboard(context)
            pendingClearRunnable = null
        }.also { mainHandler.postDelayed(it, CLEAR_DELAY_MS) }
    }

    private fun overwriteClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL_GENERIC, ""))
    }

    private fun cancelPendingClear() {
        pendingClearRunnable?.let {
            mainHandler.removeCallbacks(it)
            pendingClearRunnable = null
        }
    }
}
