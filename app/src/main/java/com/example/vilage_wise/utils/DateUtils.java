package com.example.vilage_wise.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    public static String formatDate(long timestamp) {
        if (timestamp <= 0) {
            return "N/A";
        }
        return DISPLAY_FORMAT.format(new Date(timestamp));
    }

    public static String formatDateTime(long timestamp) {
        if (timestamp <= 0) {
            return "N/A";
        }
        return DATE_TIME_FORMAT.format(new Date(timestamp));
    }
}
