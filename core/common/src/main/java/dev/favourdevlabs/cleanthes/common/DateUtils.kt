package dev.favourdevlabs.cleanthes.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val FORMAT_FULL  = "MMM d, yyyy h:mm a"
private const val FORMAT_SHORT = "MMM d, yyyy"

fun formatFull(epochMillis: Long): String =
    SimpleDateFormat(FORMAT_FULL, Locale.getDefault()).format(Date(epochMillis))

fun formatShort(epochMillis: Long): String =
    SimpleDateFormat(FORMAT_SHORT, Locale.getDefault()).format(Date(epochMillis))

fun formatRelative(epochMillis: Long): String {
    val delta   = System.currentTimeMillis() - epochMillis
    val seconds = delta / 1000
    val minutes = seconds / 60
    val hours   = minutes / 60
    val days    = hours   / 24

    return when {
        seconds < 60  -> "just now"
        minutes < 60  -> "$minutes ${if (minutes == 1L) "minute ago" else "minutes ago"}"
        hours   < 24  -> "$hours ${if (hours   == 1L) "hour ago"   else "hours ago"}"
        days    < 7   -> "$days ${if (days     == 1L) "day ago"    else "days ago"}"
        else          -> formatShort(epochMillis)
    }
}
