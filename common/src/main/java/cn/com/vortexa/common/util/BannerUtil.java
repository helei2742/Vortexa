package cn.com.vortexa.common.util;

/**
 * @author h30069248
 * @since 2025/3/25 16:32
 */
public class BannerUtil {

    public static void printBanner(String filePath) {
        System.setProperty("pagehelper.banner", "false");
        System.setProperty("spring.main.banner-mode", "OFF");
        System.setProperty("mybatis-plus.global-config.banner", "off");
    }
}
