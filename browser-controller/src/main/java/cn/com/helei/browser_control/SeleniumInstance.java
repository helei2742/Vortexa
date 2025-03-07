package cn.com.helei.browser_control;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.io.IOException;
import java.util.*;
        import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


@Slf4j
@Getter
public abstract class SeleniumInstance {

    public static final String EXTENSIONS_PATH_LIST = "extension_path_list";

    public static final String DRIVER_PATH = "driver_path";

    public static final String OPTION_LIST = "option_list";

    public static final String TARGET_WEB_SITE = "target_web_site";

    private final JSONObject params;

    private final ChromeOptions chromeOptions;

    private final SeleniumProxy proxy;

    private final List<ExecuteGroup> seleniumExecuteChain = new ArrayList<>();

    private final Random random = new Random();

    private final String instanceId;

    private ChromeDriver webDriver;

    @Setter
    private Consumer<Long> finishHandler;

    public SeleniumInstance(
            String instanceId,
            SeleniumProxy proxy,
            JSONObject params
    ) throws IOException {
        this.instanceId = instanceId;
        if (instanceId == null || instanceId.isEmpty()) throw new IllegalArgumentException("instanceId is empty");
        this.params = params;
        this.proxy = proxy;
        this.chromeOptions = initChromeOption(proxy, params);
    }

    public abstract void init();

    public abstract void webDriverLaunched();

    /**
     * 添加执行方法
     *
     * @param executeGroup executeGroup
     * @return SeleniumInstance
     */
    public SeleniumInstance addExecuteFun(ExecuteGroup executeGroup) {
        this.seleniumExecuteChain.add(executeGroup);
        return this;
    }

    /**
     * 同步启动selenium
     */
    public void syncStart() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        if (finishHandler == null) {
            setFinishHandler(cost -> {
                latch.countDown();
            });
        } else {
            Consumer<Long> fh = getFinishHandler();
            setFinishHandler(cost -> {
                fh.accept(cost);
                latch.countDown();
            });
        }

        asyncStart();
        latch.await();
    }


    /**
     * 异步启动selenium
     */
    public void asyncStart() {
        init();

        long start = System.currentTimeMillis();
        try {
            // Step 1 设置driver
            String driverPath = params.getString(DRIVER_PATH);
            if (driverPath != null && !driverPath.isEmpty()) {
                System.setProperty("webdriver.chrome.driver", driverPath);
            }

            // Step 2 启动浏览器
            launchBrowser();
            webDriverLaunched();

            // Step 3 遍历execute chain 执行
            executeChainInvoke();
        } catch (Exception e) {
            log.error("selenium instance invoke error", e);
        } finally {
            if (finishHandler != null) {
                finishHandler.accept(System.currentTimeMillis() - start);
            }
            webDriver.close();
        }
    }

    /**
     * 调用执行链
     *
     * @throws InterruptedException InterruptedException
     */
    private void executeChainInvoke() throws InterruptedException {
        for (ExecuteGroup executeGroup : seleniumExecuteChain) {
            String groupName = executeGroup.getName();

            log.info("[{}]-[{}] group start execute", instanceId, groupName);

            // Step 3.1 判断该组操作是否能够进入
            Boolean isEnter = executeGroup.getEnterCondition().apply(webDriver, this);
            if (isEnter != null && isEnter) {
                // Step 3.1.1 能够进入，开始执行group的逻辑
                log.info("[{}]-[{}] group can execute", instanceId, groupName);
                executeGroup.getExecuteItems().forEach(item -> {
                    // Step 3.1.1.1 带重试
                    Integer retryTimes = item.getRetryTimes() == null ? 1 : item.getRetryTimes();
                    for (int j = 0; j < retryTimes; j++) {
                        try {
                            log.info("[{}]-[{}]-[{}] start invoke logic [{}/{}]",
                                    instanceId, groupName, item.getName(), j, retryTimes);
                            ExecuteLogic executeLogic = item.getExecuteLogic();

                            if (executeLogic != null) {
                                executeLogic.execute(webDriver, this);
                            }
                            // Step 3。1.1.2 运行成功，return下一个item执行
                            return;
                        } catch (Exception e) {
                            // Step 3.1.1.3 运行失败，调用重试Rest方法后，继续执行
                            log.error("[{}]-[{}]-[{}] invoke logic error, retry {}", instanceId, groupName, item.getName(), j, e);

                            ExecuteLogic resetLogic = item.getResetLogic();
                            if (resetLogic != null) {
                                resetLogic.execute(webDriver, this);
                            }
                        }
                    }
                    // Step 3.1.1.4 超过次数抛出异常
                    throw new RuntimeException("[%S]-[%s]-[%s] invoke logic error, out of limit %s"
                            .formatted(instanceId, groupName, item.getName(), retryTimes));
                });
            } else {
                // Step 3.1.2 不能进入执行
                log.warn("[{}]-[{}] group can not execute", instanceId, groupName);
            }

            // Step 3.2 group 操作执行完，sleep一段时间
            int timeout = random.nextInt(500, 5000);
            log.info("[{}]-[{}] execute finish, sleep [{}]ms", instanceId, groupName, timeout);
            TimeUnit.MILLISECONDS.sleep(timeout);
        }
    }

    /**
     * 启动浏览器
     *
     * @throws IOException IOException
     */
    private void launchBrowser() throws IOException {
        log.info("starting chrome browser [{}}", instanceId);
        this.webDriver = new ChromeDriver(chromeOptions);
        log.info("chrome browser started, start execute chain");
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        ((JavascriptExecutor) webDriver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

        // Step 2.1 进入目标页面
        String targetWebSite = (String) params.get(TARGET_WEB_SITE);
        if (targetWebSite != null && !targetWebSite.isEmpty()) {
            webDriver.get(targetWebSite);
        }

        // Step 2.2 设置代理
        setProxyAuth();

        // Step 2.3 加载用户数据


        // Step 2.4 关闭多余标签页
        String mainWindow = webDriver.getWindowHandles().iterator().next();
        for (String handle : webDriver.getWindowHandles()) {
            if (!handle.equals(mainWindow)) {
                webDriver.switchTo().window(handle);
                webDriver.close();
            }
        }
    }

    private void setProxyAuth() {

    }

    /**
     * 初始化参数
     *
     * @param proxy  proxy
     * @param params params
     * @return ChromeOptions
     * @throws IOException IOException
     */
    private ChromeOptions initChromeOption(SeleniumProxy proxy, JSONObject params) throws IOException {
        final ChromeOptions chromeOptions = new ChromeOptions();
        // 设置用户数据目录
        chromeOptions.addArguments("user-data-dir=" + SeleniumUtil.getUserDataDir(instanceId));
        // 设置代理
        chromeOptions.addArguments("--proxy-server=" + proxy.getHost() + ":" + proxy.getPort());
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--remote-allow-origins=*");
        chromeOptions.addArguments("--no-default-browser-check");
        chromeOptions.addArguments("--disable-popup-blocking");
        chromeOptions.addArguments("--disable-infobars");
        chromeOptions.addArguments("---no-sandbox");

        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");  // 禁用自动化检测
        chromeOptions.addArguments("--start-maximized");
        chromeOptions.setExperimentalOption("useAutomationExtension", false);
        chromeOptions.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        JSONArray options = params.getJSONArray(OPTION_LIST);
        if (options != null && !options.isEmpty()) {
            for (int i = 0; i < options.size(); i++) {
                chromeOptions.addArguments(options.getString(i));
            }
        }

        JSONArray extensionsPathList = params.getJSONArray(EXTENSIONS_PATH_LIST);
        if (extensionsPathList != null && !extensionsPathList.isEmpty()) {
            List<File> files = new ArrayList<>(extensionsPathList.size());
            for (int i = 0; i < extensionsPathList.size(); i++) {
                String path = extensionsPathList.getString(i);
                files.add(new File(path));
            }
            chromeOptions.addExtensions(files);
        }

        return chromeOptions;
    }
}
