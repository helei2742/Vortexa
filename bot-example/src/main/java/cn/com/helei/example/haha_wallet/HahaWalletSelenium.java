package cn.com.helei.example.haha_wallet;

import cn.com.helei.browser_control.ExecuteGroup;
import cn.com.helei.browser_control.ExecuteItem;
import cn.com.helei.browser_control.SeleniumInstance;
import cn.com.helei.browser_control.SeleniumProxy;
import cn.com.helei.common.entity.AccountContext;
import cn.com.helei.common.entity.BrowserEnv;
import cn.com.helei.common.entity.ProxyInfo;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static cn.com.helei.example.haha_wallet.HaHaWalletBot.*;

public class HahaWalletSelenium {

    private final AccountContext accountContext;

    private final SeleniumInstance seleniumInstance;

    public static void main(String[] args) throws IOException, InterruptedException {
        AccountContext testAC = new AccountContext();

        testAC.setProxy(ProxyInfo.builder().host("192.46.201.191").port(6705).username("").password("").build());
        testAC.setParam(WALLET_KEY, "23");
        testAC.setParam(USERNAME_KEY, "@qq.com");
        testAC.setParam(PASSWORD_KEY, "23");

        BrowserEnv browserEnv = new BrowserEnv();
        browserEnv.setUserAgent("Mozilla/55.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.116 Safari/537.36");

        testAC.setBrowserEnv(browserEnv);
        HahaWalletSelenium hahaWalletSelenium = new HahaWalletSelenium(new HaHaWalletBot(), testAC);

        hahaWalletSelenium.start();
    }

    public HahaWalletSelenium(HaHaWalletBot bot, AccountContext accountContext) throws IOException {
        String email = accountContext.getParam(HaHaWalletBot.USERNAME_KEY);
        String password = accountContext.getParam(HaHaWalletBot.PASSWORD_KEY);
        String wallet = accountContext.getParam(WALLET_KEY);

        if (StrUtil.isBlank(wallet) || StrUtil.isBlank(email) || StrUtil.isBlank(password)) {
            bot.logger.warn("%s no email or password or wallet".formatted(accountContext.getSimpleInfo()));
            throw new IllegalArgumentException("email or password or wallet is empty");
        }

        this.accountContext = accountContext;
        ProxyInfo proxy = accountContext.getProxy();
        JSONObject params = getParams(accountContext);

        this.seleniumInstance = new SeleniumInstance(
                new SeleniumProxy(
                        proxy.getHost(),
                        proxy.getPort(),
                        proxy.getUsername(),
                        proxy.getPassword()
                ),
                params
        );
    }

    @NotNull
    private static JSONObject getParams(AccountContext accountContext) {
        JSONObject params = new JSONObject();

        JSONArray extensions = new JSONArray();
        extensions.add("/Users/helei/develop/ideaworkspace/BotFramework/bot-example/src/main/resources/haha-wallet/HaHa-Wallet-Chrome-Web-Store.crx");
        params.put(SeleniumInstance.EXTENSIONS_PATH_LIST, extensions);

        JSONArray options = new JSONArray();
        options.add(accountContext.getBrowserEnv().getUserAgent());

        params.put(SeleniumInstance.OPTION_LIST, options);
        params.put(SeleniumInstance.DRIVER_PATH, "/Users/helei/develop/ideaworkspace/BotFramework/bot-example/src/main/resources/chromedriver");
        return params;
    }

    public void start() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        seleniumInstance
                .addExecuteFun(ExecuteGroup.builder()
                        .name("初始化")
                        .enterCondition((webDriver, params) -> {
                            return true;
                        })
                        .executeItems(List.of(
                                ExecuteItem.builder().name("代理验证").executeLogic(this::proxyVerify).build()
                        ))
                        .build()
                )
                .addExecuteFun(ExecuteGroup.builder()
                        .name("登录HaHa")
                        .enterCondition((webDriver, params) -> {
                            return true;
                        })
                        .executeItems(List.of(
                                ExecuteItem.builder().name("切换到目标页面").executeLogic(this::changeToTargetPage).build(),
                                ExecuteItem.builder().name("登录账号").executeLogic(this::loginAccount).build(),
                                ExecuteItem.builder().name("导入钱包").executeLogic(this::importWallet).build()
                        ))
                        .build()
                )
                .addExecuteFun(ExecuteGroup.builder()
                                .name("每日任务")
                                .enterCondition((webDriver, params) -> {
                                    return true;
                                })
                                .executeItems(List.of(
                                ExecuteItem.builder().name("进入monad Swap页面").executeLogic(this::enterSwapPage).build(),
                                ExecuteItem.builder().name("交换Monad").executeLogic(this::monadSwap).build(),
                                        ExecuteItem.builder().name("trans sepolia Eth").executeLogic(this::sepoliaSwapPage).build()
                                ))
                                .build()
                );
        seleniumInstance.setFinishHandler(cost -> {
            latch.countDown();
        });

        seleniumInstance.start(accountContext.getSimpleInfo());

        latch.await();
    }

    private void sepoliaSwapPage(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));

        WebElement legacyPage = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div[1]/div[2]/div[2]/div[3]/ul/li[2]")));
        legacyPage.click();

        WebElement networkSelectBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[1]")));
        networkSelectBtn.click();

        WebElement teesNetSelect = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[4]/div[2]/div[2]/ul/li[2]")));
        teesNetSelect.click();

        WebElement sepoliaTestNetBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[4]/div[2]/div[2]/div[2]/button[4]")));
        sepoliaTestNetBtn.click();

        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        int count = seleniumInstance.getRandom().nextInt(5, 10);

        for (int i = 0; i < count; i++) {
            sepoliaSwap(webDriver, seleniumInstance);
        }

    }

    private void sepoliaSwap(WebDriver webDriver, SeleniumInstance seleniumInstance) {

        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(30));

        WebElement sendPageBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[3]/div[2]/div[1]/div[1]/div[2]/div[2]/button[1]")));
        sendPageBtn.click();
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<WebElement> selectionBtnList = webDriver.findElements(By.xpath("//*[@id=\"app-content\"]/div[1]/div[2]/div[2]/div[2]/div[1]/div[1]/div[1]//button"));
        int select = seleniumInstance.getRandom().nextInt(selectionBtnList.size());
        selectionBtnList.get(select).click();

        WebElement myAccountBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[4]/button")));
        myAccountBtn.click();

        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        WebElement countP = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[1]/div[2]/div[2]/div[3]/p")));
        Double total = Double.parseDouble(countP.getText());
        double count = seleniumInstance.getRandom().nextDouble(0.1, 0.7) * total;

        WebElement countInput = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[1]/div[2]/div[5]/div/div[2]/input")));
        countInput.sendKeys("%.4f".formatted(count));

        WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[2]/div[2]/button")));
        nextBtn.click();

        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        WebElement confirmBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[2]/div[2]/button")));
        confirmBtn.click();
    }


    private void monadSwap(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));

        WebElement swapCountInput = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(" //*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[1]/div[2]/div/div[3]/input")));

        WebDriverWait swapConfirmWait = new WebDriverWait(webDriver, Duration.ofSeconds(60));
        Random random = new Random();
        int monCount = random.nextInt(1, 4);
        int successTimes = 0;

        while (successTimes < monCount) {
            WebElement token2Selector = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[3]/div[1]/button")));
            token2Selector.click();

            List<WebElement> token2List = webDriver.findElements(By.xpath(" //*[@id=\"app-content\"]/div/div[2]/div[2]/div[3]/div[2]/div[3]/div//button"));
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

        WebElement returnBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[1]/button")));
        returnBtn.click();
    }

    private void enterSwapPage(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));

        WebElement legacyPage = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"tab:r0:1\"]/div")));
        legacyPage.click();

        WebElement networkSelectBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[1]")));
        networkSelectBtn.click();

        WebElement teesNetSelect = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[4]/div[2]/div[2]/ul/li[2]")));
        teesNetSelect.click();

        WebElement monadTestNetBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[4]/div[2]/div[2]/div[2]/button[1]")));
        monadTestNetBtn.click();
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        WebElement swapPageBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[3]/div[2]/div[1]/div[1]/div[2]/div[2]/button[3]")));
        swapPageBtn.click();
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void importWallet(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));

        WebElement importWalletBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[5]/div[2]/button")));
        importWalletBtn.click();

        List<WebElement> inputs = webDriver.findElements(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[3]/div//input"));

        String[] split = accountContext.getParam(WALLET_KEY).split(" ");

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

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void loginAccount(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));

        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[3]/button[2]")));
        button.click();

        // 找到 email 输入框并输入邮箱
        webDriver.findElement(By.cssSelector("input[type='email']")).sendKeys(accountContext.getParam(USERNAME_KEY));

        // 找到 password 输入框并输入密码
        webDriver.findElement(By.cssSelector("input[type='password']")).sendKeys(accountContext.getParam(PASSWORD_KEY));

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
    }

    private void changeToTargetPage(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        // 获取所有窗口句柄
        Set<String> handles = webDriver.getWindowHandles();
        for (String handle : handles) {
            webDriver.switchTo().window(handle);
            if (webDriver.getCurrentUrl().equals("data:,")) {
                webDriver.close(); // 关闭 data:, 页面
                break;
            }
        }

        // 切换到第二个标签页（索引 1）
        List<String> windowList = new ArrayList<>(webDriver.getWindowHandles());
        webDriver.switchTo().window(windowList.getFirst());
    }

    private void proxyVerify(WebDriver webDriver, SeleniumInstance instance) {
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // This "HasAuthentication" interface is the key!
        HasAuthentication authentication = (HasAuthentication) webDriver;

// You can either register something for all sites
//        authentication.register(() -> new UsernameAndPassword("admin", "admin"));

// Or use something different for specific sites
        authentication.register(
                uri -> uri.getHost().contains(instance.getProxy().getHost()),
                ()-> new UsernameAndPassword(instance.getProxy().getUsername(), instance.getProxy().getPassword())
        );


//        // 使用 Robot 模拟输入用户名和密码
//        Robot robot = null;
//        try {
//            robot = new Robot();
//        } catch (AWTException e) {
//            throw new RuntimeException(e);
//        }
//        robot.delay(5000); // 等待弹框出现
//
//        // 输入用户名
//        for (char c : instance.getProxy().getUsername().toCharArray()) {
//            robot.keyPress(KeyEvent.getExtendedKeyCodeForChar(c));
//            robot.keyRelease(KeyEvent.getExtendedKeyCodeForChar(c));
//        }
//
//        robot.keyPress(KeyEvent.VK_TAB);
//        robot.keyRelease(KeyEvent.VK_TAB);
//
//        // 输入密码
//        for (char c : instance.getProxy().getPassword().toCharArray()) {
//            robot.keyPress(KeyEvent.getExtendedKeyCodeForChar(c));
//            robot.keyRelease(KeyEvent.getExtendedKeyCodeForChar(c));
//        }
//        robot.keyPress(KeyEvent.VK_ENTER);
//        robot.keyRelease(KeyEvent.VK_ENTER);
    }
}
