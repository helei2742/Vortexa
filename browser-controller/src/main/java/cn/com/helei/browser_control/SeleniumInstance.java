package cn.com.helei.browser_control;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.HasAuthentication;
import org.openqa.selenium.UsernameAndPassword;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.v133.fetch.Fetch;
import org.openqa.selenium.devtools.v133.fetch.model.HeaderEntry;
import org.openqa.selenium.devtools.v133.fetch.model.RequestPattern;
import org.openqa.selenium.devtools.v133.network.Network;
import org.openqa.selenium.devtools.v133.network.model.Headers;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static cn.com.helei.browser_control.SeleniumProxyAuth.createProxyAuthExtension;

@Slf4j
@Getter
public class SeleniumInstance {

    public static final String EXTENSIONS_PATH_LIST = "extension_path_list";

    public static final String DRIVER_PATH = "driver_path";

    public static final String OPTION_LIST = "option_list";

    private final JSONObject params;

    private final ChromeOptions chromeOptions;

    private final SeleniumProxy proxy;

    private final List<ExecuteGroup> seleniumExecuteChain = new ArrayList<>();

    private final Random random = new Random();

    private ChromeDriver webDriver;

    @Setter
    private Consumer<Long> finishHandler;

    public SeleniumInstance(
            SeleniumProxy proxy,
            JSONObject params
    ) throws IOException {
        this.params = params;
        this.proxy = proxy;
        this.chromeOptions = initChromeOption(proxy, proxy.getUsername(), proxy.getPassword(), params);
    }


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
     * 开始执行selenium
     *
     * @param name name
     */
    public void start(String name) {
        long start = System.currentTimeMillis();
        try {
            // Step 1 设置driver
            String driverPath = params.getString(DRIVER_PATH);
            if (driverPath != null && !driverPath.isEmpty()) {
                System.setProperty("webdriver.chrome.driver", driverPath);
            }

            // Step 2 启动浏览器
            log.info("starting chrome browser [{}}", name);
            this.webDriver = new ChromeDriver(chromeOptions);
            log.info("chrome browser started, start execute chain");

            setProxyAuth();

            webDriver.get("http://%s:%s@google.com".formatted(proxy.getUsername(), proxy.getPassword()));
            // Step 3 遍历execute chain 执行
            for (ExecuteGroup executeGroup : seleniumExecuteChain) {
                String groupName = executeGroup.getName();

                log.info("[{}]-[{}] group start execute", name, groupName);

                // Step 3.1 判断该组操作是否能够进入
                Boolean isEnter = executeGroup.getEnterCondition().apply(webDriver, this);
                if (isEnter != null && isEnter) {
                    // Step 3.1.1 能够进入，开始执行group的逻辑
                    log.info("[{}]-[{}] group can execute", name, groupName);

                    executeGroup.getExecuteItems().forEach(item -> {
                        // Step 3.1.1.1 带重试
                        Integer retryTimes = item.getRetryTimes() == null ? 1 : item.getRetryTimes();
                        for (int j = 0; j < retryTimes; j++) {
                            try {
                                log.info("[{}]-[{}]-[{}] start invoke logic [{}/{}]",
                                        name, groupName, item.getName(), j, retryTimes);
                                ExecuteLogic executeLogic = item.getExecuteLogic();

                                if (executeLogic != null) {
                                    executeLogic.execute(webDriver, this);
                                }
                                // Step 3。1.1.2 运行成功，return下一个item执行
                                return;
                            } catch (Exception e) {
                                // Step 3.1.1.3 运行失败，调用重试Rest方法后，继续执行
                                log.error("[{}]-[{}]-[{}] invoke logic error, retry {}", name, groupName, item.getName(), j, e);

                                ExecuteLogic resetLogic = item.getResetLogic();
                                if (resetLogic != null) {
                                    resetLogic.execute(webDriver, this);
                                }
                            }
                        }
                        // Step 3.1.1.4 超过次数抛出异常
                        throw new RuntimeException("[%S]-[%s]-[%s] invoke logic error, out of limit %s"
                                .formatted(name, groupName, item.getName(), retryTimes));
                    });
                } else {
                    // Step 3.1.2 不能进入执行
                    log.warn("[{}]-[{}] group can not execute", name, groupName);
                }

                // Step 3.2 group 操作执行完，sleep一段时间
                int timeout = random.nextInt(500, 5000);
                log.info("[{}]-[{}] execute finish, sleep [{}]ms", name, groupName, timeout);
                TimeUnit.MILLISECONDS.sleep(timeout);
            }
        } catch (Exception e) {
            log.error("selenium instance invoke error", e);
        } finally {
            if (finishHandler != null) {
                finishHandler.accept(System.currentTimeMillis() - start);
            }
//            webDriver.close();
        }
    }

    private void setProxyAuth() {
        // This "HasAuthentication" interface is the key!
        HasAuthentication authentication = (HasAuthentication) webDriver;

// You can either register something for all sites
        authentication.register(() -> new UsernameAndPassword("admin", "admin"));

// Or use something different for specific sites
//        authentication.register(
////                uri -> uri.getHost().contains(proxy.getHost()),
//                ()-> new UsernameAndPassword(proxy.getUsername(), proxy.getPassword())
//        );

//        DevTools devTools = webDriver.getDevTools();
//        devTools.createSession();
//        String auth = proxy.getUsername() + ":" + proxy.getPassword();
//        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
//
//        // 拦截请求并添加 `Proxy-Authorization` 头
//        devTools.send(Network.enable(null, null, null));
//        devTools.addListener(Network.requestWillBeSent(), request -> {
//            devTools.send(Network.setExtraHTTPHeaders(
//                    new org.openqa.selenium.devtools.v133.network.model.Headers(
//                            java.util.Map.of("Proxy-Authorization", "Basic " + encodedAuth)
//                    )
//            ));
//        });
//
//        // 设置全局 HTTP 头部，添加 Proxy-Authorization
//        Map<String, Object> headersMap = new HashMap<>();
//        headersMap.put("Proxy-Authorization", "Basic " + encodedAuth);
//        Headers headers = new Headers(headersMap);
//
//        // 通过 CDP 设置额外的 HTTP 头
//        devTools.send(Network.setExtraHTTPHeaders(headers));
    }


    /**
     * 初始化参数
     *
     * @param proxy    proxy
     * @param username username
     * @param password password
     * @param params   params
     * @return ChromeOptions
     * @throws IOException IOException
     */
    private ChromeOptions initChromeOption(SeleniumProxy proxy, String username, String password, JSONObject params) throws IOException {
        final ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--proxy-server=" + proxy.getHost() + ":" + proxy.getPort());
        chromeOptions.addArguments("--disable-gpu");
        chromeOptions.addArguments("--remote-allow-origins=*");
        chromeOptions.addArguments("--start-maximized");
        chromeOptions.addArguments("--no-default-browser-check");
        chromeOptions.addArguments("--disable-popup-blocking");
        chromeOptions.addArguments("--disable-infobars");
        chromeOptions.addArguments("---no-sandbox");
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");  // 禁用自动化检测
        chromeOptions.setExperimentalOption("useAutomationExtension", false);
        chromeOptions.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        JSONArray options = params.getJSONArray(OPTION_LIST);
        if (options != null && !options.isEmpty()) {
            for (int i = 0; i < options.size(); i++) {
                chromeOptions.addArguments(options.getString(i));
            }
        }

//        chromeOptions.addExtensions(new File(createProxyAuthExtension(proxy.getHost(), proxy.getPort(), username, password)));

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
