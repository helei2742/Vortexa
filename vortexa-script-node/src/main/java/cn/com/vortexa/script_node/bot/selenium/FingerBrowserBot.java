package cn.com.vortexa.script_node.bot.selenium;

import cn.com.vortexa.browser_control.constants.BrowserDriverType;
import cn.com.vortexa.browser_control.driver.BitBrowserDriver;
import cn.com.vortexa.browser_control.driver.FingerprintBrowserDriver;
import cn.com.vortexa.browser_control.dto.SeleniumParams;
import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.script_node.anno.BotMethod;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.script_node.dto.selenium.ACBotTypedSeleniumExecuteInfo;
import cn.com.vortexa.script_node.service.BotApi;
import cn.hutool.core.lang.Pair;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author h30069248
 * @since 2025/4/9 14:35
 */
@Slf4j
public abstract class FingerBrowserBot extends AutoLaunchBot<FingerBrowserBot> {

    public static final int BROWSER_BATCH_SIZE = 2; //  浏览器批大小
    public static final String DEFAULT_WEB_SITE_URL = "default_web_site_url";   // 默认打开地址Key
    public static final String FINGER_BROWSER_API_URL_KEY = "finger_browser_api_url"; // 指纹浏览器api地址Key
    public static final String CHROME_DRIVER_URL_KEY = "chrome_driver_url"; // chromedriver连接地址Key
    public static final String FINGER_BROWSER_SEQ_KEY = "finger_browser_seq";   //  指纹浏览器序号Key
    private static final Set<Integer> currentWindowSeq = new HashSet<>();

    private static final ConcurrentHashMap<AccountContext, AccountFingerBrowserSelenium> accountFBSeleniumMap = new ConcurrentHashMap<>();
    private static final List<ACBotTypedSeleniumExecuteInfo> acExecuteInfos = new ArrayList<>();
    private static volatile FingerprintBrowserDriver browserDriver;
    private static String chromeDriverUrl;
    private static String fingerBrowserApiUrl;


    @Override
    protected void botInitialized(AutoBotConfig botConfig, BotApi botApi) {
        if (chromeDriverUrl == null) {
            chromeDriverUrl = (String) botConfig.getCustomConfig().get(CHROME_DRIVER_URL_KEY);
        }
        if (fingerBrowserApiUrl == null) {
            fingerBrowserApiUrl = (String) botConfig.getCustomConfig().get(FINGER_BROWSER_API_URL_KEY);
        }
        initBrowserDriver();

        synchronized (acExecuteInfos) {
            ACBotTypedSeleniumExecuteInfo executeInfo = buildExecuteGroupChain();
            for (ACBotTypedSeleniumExecuteInfo acExecuteInfo : acExecuteInfos) {
                String botKey = botConfig.getBotKey();
                if (acExecuteInfo.getBotKey().equals(botKey)) {
                    logger.warn(botKey + " execute info almost added...");
                    return;
                }
            }
            acExecuteInfos.add(executeInfo);
        }
    }

    @Override
    protected FingerBrowserBot getInstance() {
        return this;
    }

    @BotMethod(jobType = BotJobType.ONCE_TASK, concurrentCount = BROWSER_BATCH_SIZE)
    public void accountBrowserTask(AccountContext accountContext) {
        int seq = Integer.parseInt((String) accountContext.getParams().get(FINGER_BROWSER_SEQ_KEY));

        try {
            currentWindowSeq.add(seq);
            AccountFingerBrowserSelenium browserSelenium = accountFBSeleniumMap.compute(accountContext, (k, v) -> {
                if (v == null) {
                    try {
                        v = new AccountFingerBrowserSelenium(
                                accountContext,
                                buildSeleniumParams(seq)
                        );

                        for (ACBotTypedSeleniumExecuteInfo acExecuteInfo : acExecuteInfos) {
                            v.addExecuteInfo(acExecuteInfo);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("create account selenium error", e);
                    }
                }
                return v;
            });

            if (!browserSelenium.isRunning()) {
                browserDriver.flexAbleWindowBounds(new ArrayList<>(currentWindowSeq));
                browserSelenium.startACFBSelenium();
            } else {
                logger.warn(accountContext.getSimpleInfo() + " selenium is running...cancel new start");
            }
        } catch (Exception e) {
            logger.error(accountContext.getSimpleInfo() + " - start selenium task error", e);
        } finally {
            currentWindowSeq.remove(seq);
        }
    }

    /**
     * 构建selenium参数
     *
     * @return SeleniumParams
     */
    private SeleniumParams buildSeleniumParams(Integer seq) {
        String debuggerAddress = browserDriver.startWebDriverBySeq(seq);

        return SeleniumParams
                .builder()
                .driverPath(chromeDriverUrl)
                .experimentalOptions(List.of(new Pair<>("debuggerAddress", debuggerAddress)))
                .targetWebSite("https://www.google.com")
                .build();
    }

    /**
     * 构建指纹浏览器driver
     */
    private void initBrowserDriver() {
        if (browserDriver == null) {
            synchronized (FingerBrowserBot.class) {
                if (browserDriver == null) {
                    browserDriver = switch (browserDriverType()) {
                        case BIT_BROWSER -> new BitBrowserDriver(fingerBrowserApiUrl);
                    };
                }
            }
        }
    }

    /**
     * 指纹浏览器类型
     *
     * @return BrowserDriverType
     */
    protected abstract BrowserDriverType browserDriverType();

    /**
     * 构建执行链
     *
     * @return List<ExecuteGroup>
     */
    protected abstract ACBotTypedSeleniumExecuteInfo buildExecuteGroupChain();
}
