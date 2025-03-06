package cn.com.helei.example.haha_wallet;

import cn.com.helei.browser_control.*;
import cn.com.helei.common.entity.AccountContext;
import cn.com.helei.common.entity.BrowserEnv;
import cn.com.helei.common.entity.ProxyInfo;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static cn.com.helei.example.haha_wallet.HaHaWalletBot.*;

public class HahaWalletSelenium extends OptSeleniumInstance {

    //    private static final String HAHA_WALLET_EXTENSION_CRX_PATH = "D:\\workspace\\DepinBot\\auto-bot-v_1.1\\bot-example\\src\\main\\resources\\haha-wallet\\HaHa-Wallet-Chrome-Web-Store.crx";
    private static final String HAHA_WALLET_EXTENSION_CRX_PATH =
            "/Users/helei/develop/ideaworkspace/BotFramework/bot-example/src/main/resources/haha-wallet/HaHa-Wallet-Chrome-Web-Store.crx";
    //    private static final String CHROME_DRIVER_PATH = "D:\\workspace\\DepinBot\\auto-bot-v_1.1\\bot-example\\src\\main\\resources\\chromedriver";
    private static final String CHROME_DRIVER_PATH = "/Users/helei/develop/ideaworkspace/BotFramework/bot-example/src/main/resources/chromedriver";

    private static final Logger log = LoggerFactory.getLogger(HahaWalletSelenium.class);

    private final AccountContext accountContext;

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

        hahaWalletSelenium.syncStart();
    }


    public HahaWalletSelenium(HaHaWalletBot bot, AccountContext accountContext) throws IOException {
        super(
                accountContext.getParam(HaHaWalletBot.USERNAME_KEY),
                new SeleniumProxy(
                        accountContext.getProxy().getHost(),
                        accountContext.getProxy().getPort(),
                        accountContext.getProxy().getUsername(),
                        accountContext.getProxy().getPassword()
                ),
                getParams(accountContext)
        );
        String email = accountContext.getParam(HaHaWalletBot.USERNAME_KEY);
        String password = accountContext.getParam(HaHaWalletBot.PASSWORD_KEY);
        String wallet = accountContext.getParam(WALLET_KEY);

        if (StrUtil.isBlank(wallet) || StrUtil.isBlank(email) || StrUtil.isBlank(password)) {
            bot.logger.warn("%s no email or password or wallet".formatted(accountContext.getSimpleInfo()));
            throw new IllegalArgumentException("email or password or wallet is empty");
        }

        this.accountContext = accountContext;
    }


    @Override
    public void init() {


        super.addExecuteFun(ExecuteGroup.builder()
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
                            try {
                                String flag = xPathFindElement("//*[@id=\"app-content\"]/div/div[2]/div[2]/div/button[2]").getText();
                                // 已登录，需要解锁
                                return !"Forgot Pin Code?".equals(flag);
                            }catch (Exception e){
                               return true;
                            }
                        })
                        .executeItems(List.of(
                                ExecuteItem.builder().name("切换到目标页面").executeLogic(this::changeToTargetPage).build(),
                                ExecuteItem.builder().name("登录账号").executeLogic(this::loginAccount).build(),
                                ExecuteItem.builder().name("导入钱包").executeLogic(this::importWallet).build()
                        ))
                        .build()
                )
                .addExecuteFun(ExecuteGroup.builder()
                        .name("解锁钱包")
                        .enterCondition((webDriver, params) -> {
                            String flag = xPathFindElement("//*[@id=\"app-content\"]/div/div[2]/div[2]/div/button[2]").getText();
                            // 已登录，需要解锁
                            return "Forgot Pin Code?".equals(flag);
                        })
                        .executeItems(List.of(
                                ExecuteItem.builder().name("输入Pin Code").executeLogic(((webDriver, seleniumInstance) -> {
                                    xPathFindElement("//*[@id=\"app-content\"]/div/div[2]/div[2]/div/div/input").sendKeys("123456789");

                                    xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div/button[1]");
                                })).build()
                        ))
                        .build()
                )
                .addExecuteFun(ExecuteGroup.builder()
                        .name("每日任务")
                        .enterCondition((webDriver, params) -> {
                            return true;
                        })
                        .executeItems(List.of(
                                ExecuteItem.builder().name("进入monad Swap页面").executeLogic(this::enterMonadSwapPage).build(),
                                ExecuteItem.builder().name("交换Monad").executeLogic(this::monadSwap).build(),
                                ExecuteItem.builder().name("trans sepolia Eth").executeLogic(this::sepoliaSwapPage).build()
                        ))
                        .build()
                );

    }


    private void sepoliaSwapPage(WebDriver webDriver, SeleniumInstance seleniumInstance) {

        xPathClick("//*[@id=\"app-content\"]/div[1]/div[2]/div[2]/div[3]/ul/li[2]");

        //.点击选择网络界面
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[1]");

        // 点击选择测试网络
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[4]/div[2]/div[2]/ul/li[2]");

        // 选择sepolia
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[4]/div[2]/div[2]/div[2]/button[4]");

        randomWait();

        int count = seleniumInstance.getRandom().nextInt(5, 10);

        for (int i = 0; i < count; i++) {
            try {
                sepoliaSwap(webDriver, seleniumInstance);

            } catch (Exception e) {
                log.error("{} sepolia swap error", getInstanceId(), e);
            }
        }
    }

    private void sepoliaSwap(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        // 点击进入legacyPage
        xPathClick("//*[@id=\"app-content\"]/div[1]/div[2]/div[2]/div[3]/ul/li[2]");

        // 进入发送界面
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[3]/div[2]/div[1]/div[1]/div[2]/div[2]/button[1]");
        randomWait();

        // 选择代币
        List<WebElement> selectionBtnList = xPathFindElements("//*[@id=\"app-content\"]/div[1]/div[2]/div[2]/div[2]/div[1]/div[1]/div[1]//button");
        int select = seleniumInstance.getRandom().nextInt(selectionBtnList.size());
        selectionBtnList.get(select).click();

        // 选择自己的地址
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[4]/button");
        randomWait();

        WebElement countP = xPathFindElement("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[1]/div[2]/div[2]/div[3]/p");
        ;
        double total = Double.parseDouble(countP.getText());
        double count = seleniumInstance.getRandom().nextDouble(0.1, 0.7) * total;

        // 输入数量
        WebElement countInput = xPathFindElement("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[1]/div[2]/div[5]/div/div[2]/input");
        countInput.sendKeys("%.6f".formatted(count));

        // 点击下一步
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[2]/div[2]/button");

        // 点击确认
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[2]/div[2]/button");
    }


    private void monadSwap(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        WebElement swapCountInput = xPathFindElement("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[1]/div[2]/div/div[3]/input");

        Random random = new Random();
        int monCount = random.nextInt(2, 4);
        int successTimes = 0;

        while (successTimes < monCount) {
            // 点击进入交换代币选择界面
            xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[3]/div[1]/button");

            // 随机选择代币
            List<WebElement> token2List = xPathFindElements("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[3]/div[2]/div[3]/div//button");
            token2List.removeFirst();

            randomWait();
            token2List.get(random.nextInt(token2List.size())).click();


            double count = random.nextDouble(0.0001, 0.001);
            swapCountInput.sendKeys("");
            swapCountInput.sendKeys("%.4f".formatted(count));

            // 等待按钮可点击
            try {
                // 点击确认
                xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[6]/button");
                xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[3]/div[2]/div/button");
                successTimes++;
            } catch (TimeoutException timeoutException) {
                System.out.println("超时");
            }
        }

        // 点击返回按钮
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[1]/button");
    }

    private void enterMonadSwapPage(WebDriver webDriver, SeleniumInstance seleniumInstance) {

        // 点击进入legacyPage
        xPathClick("//*[@id=\"app-content\"]/div[1]/div[2]/div[2]/div[3]/ul/li[2]");

        // 点击网络选择按钮
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[1]");

        // 点击选择测试网
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[4]/div[2]/div[2]/ul/li[2]");

        // 点击选择Monad
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[4]/div[2]/div[2]/div[2]/button[1]");

        randomWait();

        // 点击swap页面
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[3]/div[2]/div[1]/div[1]/div[2]/div[2]/button[3]");

        randomWait();
    }

    private void importWallet(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        // 点击导入按钮
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[5]/div[2]/button");

        // 等待输入框出现， 输入钱包
        List<WebElement> inputs = xPathFindElements("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[3]/div//input");
        String[] split = accountContext.getParam(WALLET_KEY).split(" ");
        for (int i = 0; i < inputs.size(); i++) {
            inputs.get(i).sendKeys(split[i]);
        }

        // 点击导入按钮
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[4]/button");

        // 点击导入成功的确认按钮
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/button");

        // 点击跳过按钮
        xPathClick("//*[@id=\"app-content\"]/div[2]/div[2]/div[3]/button[2]");

        randomWait();
    }


    private void loginAccount(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[3]/button[2]");

        // 找到 email 输入框并输入邮箱
        webDriver.findElement(By.cssSelector("input[type='email']")).sendKeys(accountContext.getParam(USERNAME_KEY));
        // 找到 password 输入框并输入密码
        webDriver.findElement(By.cssSelector("input[type='password']")).sendKeys(accountContext.getParam(PASSWORD_KEY));
        // 点击登录按钮
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[3]/div[3]/button[1]");


        // 输入解锁密码
        xPathFindElement("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[3]/div[1]/input").sendKeys("123456789");
        // 输入解锁密码
        xPathFindElement("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[3]/div[2]/input").sendKeys("123456789");
        // 确认
        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/div[2]/div[4]/button");


        xPathClick("//*[@id=\"app-content\"]/div/div[2]/div[2]/label/input");
    }


    private void changeToTargetPage(WebDriver webDriver, SeleniumInstance seleniumInstance) {
        // 获取所有窗口句柄
        Set<String> handles = webDriver.getWindowHandles();
        for (String handle : handles) {
            webDriver.switchTo().window(handle);
            if (Objects.equals(webDriver.getCurrentUrl(), "data:,")) {
                webDriver.close(); // 关闭 data:, 页面
                break;
            }
        }

        // 切换到第二个标签页（索引 1）
        List<String> windowList = new ArrayList<>(webDriver.getWindowHandles());
        webDriver.switchTo().window(windowList.getFirst());
    }

    private void proxyVerify(WebDriver webDriver, SeleniumInstance instance) {
        // 使用 Robot 模拟输入用户名和密码
        Robot robot = null;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
        robot.delay(5000); // 等待弹框出现

        // 输入用户名
        for (char c : instance.getProxy().getUsername().toCharArray()) {
            robot.keyPress(KeyEvent.getExtendedKeyCodeForChar(c));
            robot.keyRelease(KeyEvent.getExtendedKeyCodeForChar(c));
        }

        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);

        // 输入密码
        for (char c : instance.getProxy().getPassword().toCharArray()) {
            robot.keyPress(KeyEvent.getExtendedKeyCodeForChar(c));
            robot.keyRelease(KeyEvent.getExtendedKeyCodeForChar(c));
        }
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
    }

    @NotNull
    private static JSONObject getParams(AccountContext accountContext) {
        JSONObject params = new JSONObject();

        JSONArray extensions = new JSONArray();
        extensions.add(HAHA_WALLET_EXTENSION_CRX_PATH);
        params.put(SeleniumInstance.EXTENSIONS_PATH_LIST, extensions);

        JSONArray options = new JSONArray();
        options.add("user-agent=" + accountContext.getBrowserEnv().getUserAgent());

        params.put(TARGET_WEB_SITE, "chrome-extension://andhndehpcjpmneneealacgnmealilal/popup.html");
        params.put(SeleniumInstance.OPTION_LIST, options);
        params.put(SeleniumInstance.DRIVER_PATH, CHROME_DRIVER_PATH);
        return params;
    }

}
