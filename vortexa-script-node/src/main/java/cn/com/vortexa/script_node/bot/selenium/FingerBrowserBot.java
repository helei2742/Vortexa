package cn.com.vortexa.script_node.bot.selenium;

import cn.com.vortexa.browser_control.constants.BrowserDriverType;
import cn.com.vortexa.browser_control.driver.BitBrowserDriver;
import cn.com.vortexa.browser_control.driver.FingerprintBrowserDriver;
import cn.com.vortexa.browser_control.dto.SeleniumParams;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.common.util.DiscardingBlockingQueue;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.script_node.dto.selenium.ACBotTypedSeleniumExecuteInfo;
import cn.com.vortexa.script_node.service.BotApi;
import cn.hutool.core.lang.Pair;
import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * @author com.helei
 * @since 2025/4/9 14:35
 */
@Slf4j
public abstract class FingerBrowserBot extends AutoLaunchBot<FingerBrowserBot> {
    public static final int BROWSER_WINDOW_LIMIT = 4;
    public static final String DEFAULT_WEB_SITE_URL = "http://www.google.com";   // 默认打开地址Key
    public static final String DEFAULT_WEB_SITE_URL_KEY = "default_web_site_url";   // 默认打开地址Key
    public static final String FINGER_BROWSER_API_URL_KEY = "finger_browser_api_url"; // 指纹浏览器api地址Key
    public static final String CHROME_DRIVER_URL_KEY = "chrome_driver_url"; // chromedriver连接地址Key
    public static final String FINGER_BROWSER_SEQ_KEY = "finger_browser_seq";   //  指纹浏览器序号Key

    private static final DiscardingBlockingQueue<Integer> currentWindowSeq = new DiscardingBlockingQueue<>(BROWSER_WINDOW_LIMIT);   //  当前窗口长度
    private static final ConcurrentHashMap<AccountContext, AccountFingerBrowserSelenium> accountFBSeleniumMap = new ConcurrentHashMap<>();  //  账户与指纹的缓存
    private static final Map<String, ACBotTypedSeleniumExecuteInfo> acExecuteInfos = new ConcurrentHashMap<>(); //  jobName -》 executeInfo
    private static final Map<Integer, ReentrantLock> fingerBrowserLockMap = new ConcurrentHashMap<>();    //  浏览器锁

    private static volatile FingerprintBrowserDriver browserDriver;
    private static String chromeDriverUrl;
    private static String fingerBrowserApiUrl;

    private String openUrl = DEFAULT_WEB_SITE_URL;

    @Override
    protected void botStopped() {
        super.botStopped();
        Iterator<Map.Entry<AccountContext, AccountFingerBrowserSelenium>> iterator = accountFBSeleniumMap.entrySet()
                .iterator();
        while (iterator.hasNext()) {
            Map.Entry<AccountContext, AccountFingerBrowserSelenium> entry = iterator.next();
            entry.getValue().close();
            iterator.remove();
        }
        acExecuteInfos.clear();
    }

    @Override
    protected void botInitialized(AutoBotConfig botConfig, BotApi botApi) {
        if (chromeDriverUrl == null) {
            chromeDriverUrl = (String) botConfig.getCustomConfig().get(CHROME_DRIVER_URL_KEY);
        }
        if (fingerBrowserApiUrl == null) {
            fingerBrowserApiUrl = (String) botConfig.getCustomConfig().get(FINGER_BROWSER_API_URL_KEY);
        }

        openUrl = getDefaultWebSiteUrl(botConfig);
        initBrowserDriver();
    }

    /**
     * 账号的不同job之间同步运行指纹浏览器
     *
     * @param jobName   jobName
     * @param accountContext    账户上下文
     * @param seleniumExecuteInfoSupplier   生成执行信息
     */
    public void syncAccountFBInvoker(
            String jobName,
            AccountContext accountContext,
            Supplier<ACBotTypedSeleniumExecuteInfo> seleniumExecuteInfoSupplier
    ) {
        // Step 1 获取（不存在则注册job的执行信息）
        ACBotTypedSeleniumExecuteInfo executeInfo = tryGetJobSeleniumExecuteInfo(jobName, seleniumExecuteInfoSupplier);
        if (executeInfo == null) {
            return;
        }

        // Step 2 尝试获取浏览器的锁，来执行
        int seq = Integer.parseInt((String) accountContext.getParams().get(FINGER_BROWSER_SEQ_KEY));
        ReentrantLock fbLock = getFBLock(seq);
        String simpleInfo = accountContext.getSimpleInfo();
        logger.info("[%s] get browser lock[%s] success, try use it...".formatted(
                accountContext.getSimpleInfo(), seq
        ));

        boolean isRun;
        try {
            isRun = fbLock.tryLock(executeInfo.getWaitTime(), executeInfo.getWaitTimeUnit());
        } catch (InterruptedException e) {
            logger.warn(simpleInfo + " waiting for execute job " + jobName + " interrupted");
            return;
        }
        if (!isRun) {
            logger.warn(simpleInfo + " waiting for execute job timeout...");
            return;
        }

        // Step 3 获取到锁，尝试获取浏览器控制实例，添加执行任务
        try {
            currentWindowSeq.put(seq);
            AccountFingerBrowserSelenium browserSelenium = getAccountFingerBrowserSelenium(accountContext, seq);

            if (!browserSelenium.isRunning()) {
                browserDriver.flexAbleWindowBounds(List.of(currentWindowSeq.toArray(new Integer[0])));
                browserSelenium.startACFBSelenium();
                accountFBSeleniumMap.remove(accountContext);
            } else {
                logger.warn(simpleInfo + " selenium is running...cancel new start");
            }
        } catch (Exception e) {
            logger.error(simpleInfo + " - start selenium task error", e);
        } finally {
            if (fbLock.isHeldByCurrentThread()) {
                fbLock.unlock();
            }
        }
    }

    private ACBotTypedSeleniumExecuteInfo tryGetJobSeleniumExecuteInfo(String jobName,
                                                                       Supplier<ACBotTypedSeleniumExecuteInfo> seleniumExecuteInfoSupplier) {

        ACBotTypedSeleniumExecuteInfo executeInfo = acExecuteInfos.compute(getAutoBotConfig().getBotKey() + "-" + jobName, (k, v) -> {
            if(v == null) {
                v = seleniumExecuteInfoSupplier.get();
            }
            return v;
        });
        if (executeInfo == null) {
            logger.warn("no execute info for: " + jobName);
            return null;
        }
        if (executeInfo.getWaitTime() == null || executeInfo.getWaitTimeUnit() == null) {
            logger.warn("execute info params error, " + executeInfo);
            return null;
        }
        if (executeInfo.getBotKey() == null) {
            executeInfo.setBotKey(getAutoBotConfig().getBotKey());
        }
        if (executeInfo.getJobName() == null) {
            executeInfo.setJobName(jobName);
        }
        return executeInfo;
    }

    /**
     * 获取指纹浏览器窗口的锁
     *
     * @param seq   seq
     * @return  ReentrantLock
     */
    private static @NotNull ReentrantLock getFBLock(int seq) {
        return fingerBrowserLockMap.compute(seq, (k, v) -> {
            if (v == null) {
                v = new ReentrantLock();
            }
            return v;
        });
    }

    /**
     * 获取浏览器控制实例
     *
     * @param accountContext    accountContext
     * @param seq   seq
     * @return  AccountFingerBrowserSelenium
     */
    private @NotNull AccountFingerBrowserSelenium getAccountFingerBrowserSelenium(AccountContext accountContext, int seq) {
        return accountFBSeleniumMap.compute(accountContext, (k, v) -> {
            if (v == null) {
                try {
                    v = new AccountFingerBrowserSelenium(
                            accountContext,
                            buildSeleniumParams(seq),
                            logger
                    );

                    for (ACBotTypedSeleniumExecuteInfo acExecuteInfo : acExecuteInfos.values()) {
                        v.addExecuteInfo(acExecuteInfo);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("create account selenium error", e);
                }
            }
            return v;
        });
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
                .targetWebSite(openUrl)
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
     * 获取默认打开的url
     *
     * @param botConfig botConfig
     * @return String
     */
    private String getDefaultWebSiteUrl(AutoBotConfig botConfig) {
        return (String) botConfig.getCustomConfig().getOrDefault(DEFAULT_WEB_SITE_URL_KEY, DEFAULT_WEB_SITE_URL);
    }
}
