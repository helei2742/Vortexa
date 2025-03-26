package cn.com.vortexa;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static cn.com.vortexa.browser_control.util.SeleniumProxyAuth.createProxyAuthExtension;

public class SeleniumExample {

    private static String driverPath =
            "/Users/helei/develop/ideaworkspace/depinbot/DepinBot/browser-controller/src/main/resources/chromedriver";
    private static String croPath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

    private static String extensionId = "andhndehpcjpmneneealacgnmealilal";

    private static String extensionPath =
            "/Users/helei/develop/ideaworkspace/depinbot/DepinBot/browser-controller/src/main/resources/haha-wallet/HaHa-Wallet-Chrome-Web-Store.crx";

    private static String host = "46.203.161.123";
    private static Integer port = 5620;
    private static String username = "";
    private static String password = "";

    private static String email = "@qq.com";

    private static String emailPassword = "";

    private static String wallet =
""            ;

    public static void main(String[] args) throws IOException, AWTException, InterruptedException {
        // 设置 WebDriver 路径（可省略，前提是 WebDriver 在 PATH 中）
//        System.setProperty("webdriver.chrome.driver", driverPath);
        // 指定 Google Chrome for Testing 的路径
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-gpu"); // 解决部分插件渲染问题
        options.addArguments("--remote-allow-origins=*"); // 适用于部分 Chrome 版本
        options.addArguments("--start-maximized");
        options.addArguments("--no-default-browser-check");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-infobars");
        options.addArguments("--proxy-auth=%s:%s".formatted(username, password));
//        options.setBinary("/Users/helei/develop/ideaworkspace/depinbot/DepinBot/browser-controller/src/main/resources/chrome-mac-arm64/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing");  // 这里填写 Chrome for Testing

        String extensionFile = createProxyAuthExtension(host, port, username, password);
        options.addExtensions(new File(extensionFile), new File(extensionPath));

        // 创建 Chrome 浏览器实例
        ChromeDriver driver = new ChromeDriver(options);
        // 使用 Robot 模拟输入用户名和密码
        Robot robot = new Robot();
        robot.delay(5000); // 等待弹框出现

        // 输入用户名
        for (char c : username.toCharArray()) {
            robot.keyPress(KeyEvent.getExtendedKeyCodeForChar(c));
            robot.keyRelease(KeyEvent.getExtendedKeyCodeForChar(c));
        }

        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);

        // 输入密码
        for (char c : password.toCharArray()) {
            robot.keyPress(KeyEvent.getExtendedKeyCodeForChar(c));
            robot.keyRelease(KeyEvent.getExtendedKeyCodeForChar(c));
        }
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);

        System.out.println("click");

        // 获取所有窗口句柄
        Set<String> handles = driver.getWindowHandles();
        for (String handle : handles) {
            driver.switchTo().window(handle);
            if (driver.getCurrentUrl().equals("data:,")) {
                driver.close(); // 关闭 data:, 页面
                break;
            }
        }

        // 切换到第二个标签页（索引 1）
        List<String> windowList = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(windowList.getFirst());


        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[3]/button[2]")));
        button.click();

        // 找到 email 输入框并输入邮箱
        driver.findElement(By.cssSelector("input[type='email']")).sendKeys(email);

        // 找到 password 输入框并输入密码
        driver.findElement(By.cssSelector("input[type='password']")).sendKeys(emailPassword);

        WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[3]/div[3]/button[1]")));
        loginButton.click();

        WebElement pinCodeInput = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[3]/div[1]/input")));
        pinCodeInput.sendKeys("123456789");
        WebElement pinCodeConfirmInput = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[3]/div[2]/input")));
        pinCodeConfirmInput.sendKeys("123456789");
        WebElement confirmPinCOde = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[4]/button")));
        confirmPinCOde.click();


        WebElement agreeBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/label/input")));
        agreeBtn.click();

        WebElement importWalletBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[5]/div[2]/button")));
        importWalletBtn.click();

        List<WebElement> inputs = driver.findElements(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[3]/div//input"));

        String[] split = wallet.split(" ");

        for (int i = 0; i < inputs.size(); i++) {
            inputs.get(i).sendKeys(split[i]);
        }


        WebElement importWalletConfirm
                = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[4]/button")));
        importWalletConfirm.click();


        WebElement importSuccessConfirm
                = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/button")));
        importSuccessConfirm.click();

        WebElement skipNowBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div[2]/div[2]/div[3]/button[2]")));
        skipNowBtn.click();

        TimeUnit.SECONDS.sleep(5);

        WebElement legacyPage = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"tab:r0:1\"]/div")));
        legacyPage.click();

        WebElement networkSelectBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[1]")));
        networkSelectBtn.click();

        WebElement teesNetSelect = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[4]/div[2]/div[2]/ul/li[2]")));
        teesNetSelect.click();

        WebElement monadTestNetBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[4]/div[2]/div[2]/div[2]/button[1]")));
        monadTestNetBtn.click();
        TimeUnit.SECONDS.sleep(5);

        WebElement swapPageBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[3]/div[2]/div[1]/div[1]/div[2]/div[2]/button[3]")));
        swapPageBtn.click();
        TimeUnit.SECONDS.sleep(5);

        WebElement swapCountInput = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(" //*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[1]/div[2]/div/div[3]/input")));

        WebDriverWait swapConfirmWait = new WebDriverWait(driver, Duration.ofSeconds(60));
        Random random = new Random();
        int monCount = random.nextInt(1, 4);
        int successTimes = 0;

        while (successTimes < monCount) {
            WebElement token2Selector = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[3]/div[1]/button")));
            token2Selector.click();

            List<WebElement> token2List = driver.findElements(By.xpath(" //*[@id=\"app-content\"]/div/div[2]/div[2]/div[3]/div[2]/div[3]/div//button"));
            token2List.removeFirst();

            token2List.get(random.nextInt(token2List.size())).click();

            double count = random.nextDouble(0.0001, 0.001);
            swapCountInput.sendKeys("");
            swapCountInput.sendKeys("%.4f".formatted(count));
            // 等待按钮可点击
            try {
                WebElement swapConfirmBtn = swapConfirmWait.until(ExpectedConditions.elementToBeClickable(By.xpath(" //*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[6]/button")));
                swapConfirmBtn.click();
                WebElement closeTransBtn = swapConfirmWait.until(ExpectedConditions.elementToBeClickable(By.xpath(" //*[@id=\"app-content\"]/div/div[2]/div[2]/div[3]/div[2]/div/button")));
                closeTransBtn.click();
                successTimes++;
            } catch (TimeoutException timeoutException) {
                System.out.println("超时");
            }
        }


        try {
//            // 打开网页
//            driver.get("chrome-extension://" + extensionId + "/popup.html");

            // 等待一会（可选）
            Thread.sleep(300000000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭浏览器
            driver.quit();
        }
    }
}
