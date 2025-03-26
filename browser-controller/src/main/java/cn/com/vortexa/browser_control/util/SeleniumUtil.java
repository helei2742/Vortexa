package cn.com.vortexa.browser_control.util;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.*;

@Slf4j
public class SeleniumUtil {

    private static final String SELENIUNM_FILE = System.getProperty("user.dir") + File.separator + "selenium_data";

    private static final String COOKIE_DIR = "cookie";

    private static final String USER_DATA_DIR = "user_data_dir";

    /**
     * 保存 Cookies
     *
     * @param driver driver
     * @param key    key
     * @throws IOException IOException
     */
    public static void saveCookies(WebDriver driver, String key) throws IOException {
        try (FileWriter fileWriter = new FileWriter(getCookieFile(key));
             BufferedWriter writer = new BufferedWriter(fileWriter)) {
            for (Cookie cookie : driver.manage().getCookies()) {
                writer.write(cookie.getName() + ";" + cookie.getValue() + ";" + cookie.getDomain() +
                        ";" + cookie.getPath() + ";" + cookie.getExpiry() + ";" + cookie.isSecure());
                writer.newLine();
            }
            log.info("key [{}] ✅ Cookies 已保存！", key);
        }
    }

    /**
     * 读取 Cookies 并重新加载
     *
     * @param driver driver
     * @param key    key
     * @throws IOException IOException
     */
    public static void loadCookies(WebDriver driver, String key) throws IOException {
        try (FileReader fileReader = new FileReader(getCookieFile(key));
             BufferedReader reader = new BufferedReader(fileReader)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                Cookie cookie = new Cookie(parts[0], parts[1], parts[2], parts[3], null, Boolean.parseBoolean(parts[5]));
                driver.manage().addCookie(cookie);
            }
            log.info("key [{}] ✅ Cookies 已加载！", key);
        }
    }


    public static void saveLocalStorage(WebDriver driver, String key) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
    }


    public static String getUserDataDir(String key) {
        return getUserDataRootDir() + File.separator + key;
    }


    public static String getUserDataRootDir() {
        return SELENIUNM_FILE + File.separator + USER_DATA_DIR;
    }


    public static String getCookieFile(String key) {
        return getCookieRootDir() + File.separator + key;
    }

    public static String getCookieRootDir() {
        return SELENIUNM_FILE + File.separator + COOKIE_DIR;
    }

}
