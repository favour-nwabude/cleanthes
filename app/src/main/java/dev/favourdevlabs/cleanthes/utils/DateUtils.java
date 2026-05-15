package dev.favourdevlabs.cleanthes.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    private static final String FORMAT_FULL = "MMM d, yyyy h:mm a";
    private static final String FORMAT_SHORT = "MMM d, yyyy";

    private DateUtils() {
    }

    public static String formatFull(long epochMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_FULL, Locale.getDefault());
        return sdf.format(new Date(epochMillis));
    }

    public static String formatShort(long epochMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_SHORT, Locale.getDefault());
        return sdf.format(new Date(epochMillis));
    }

    public static String formatRelative(long epochMillis) {
        long now = System.currentTimeMillis();
        long delta = now - epochMillis;

        long seconds = delta / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60)
            return "just now";
        if (minutes < 60)
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        if (hours < 24)
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        if (days < 7)
            return days + (days == 1 ? " day ago" : " days ago");

        return formatShort(epochMillis);
    }
}
