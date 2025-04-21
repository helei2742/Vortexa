package cn.com.vortexa.common.util;


import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * @author helei
 * @since 2025-04-21
 */
public class DateUtil {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(java.time.ZoneOffset.UTC);

    public static String getCurrentUtcTimeStr() {
        return formatter.format(Instant.now());
    }
}
