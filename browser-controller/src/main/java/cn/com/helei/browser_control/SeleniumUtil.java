package cn.com.helei.browser_control;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.*;

public class SeleniumUtil {
    private static final String COOKIE_FILE = "cookies.data";

    public static void main(String[] args) {

        WebDriver driver = new ChromeDriver();
        driver.get("https://www.example.com");

        // 登录后保存 Cookies
        saveCookies(driver);

        driver.quit();

        // 重新启动浏览器并加载 Cookies
        WebDriver newDriver = new ChromeDriver();
        newDriver.get("https://www.example.com");
        loadCookies(newDriver);
        newDriver.navigate().refresh(); // 刷新页面以应用 Cookies
    }
    // 保存 Cookies
    public static void saveCookies(WebDriver driver) {
        try (FileWriter fileWriter = new FileWriter(COOKIE_FILE);
             BufferedWriter writer = new BufferedWriter(fileWriter)) {
            for (Cookie cookie : driver.manage().getCookies()) {
                writer.write(cookie.getName() + ";" + cookie.getValue() + ";" + cookie.getDomain() +
                        ";" + cookie.getPath() + ";" + cookie.getExpiry() + ";" + cookie.isSecure());
                writer.newLine();
            }
            System.out.println("✅ Cookies 已保存！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 读取 Cookies 并重新加载
    public static void loadCookies(WebDriver driver) {
        try (FileReader fileReader = new FileReader(COOKIE_FILE);
             BufferedReader reader = new BufferedReader(fileReader)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                Cookie cookie = new Cookie(parts[0], parts[1], parts[2], parts[3], null, Boolean.parseBoolean(parts[5]));
                driver.manage().addCookie(cookie);
            }
            System.out.println("✅ Cookies 已加载！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
