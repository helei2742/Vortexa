package cn.com.vortexa.script_node.bot.selenium;

import cn.com.vortexa.browser_control.OptSeleniumInstance;
import cn.com.vortexa.browser_control.dto.SeleniumParams;
import cn.com.vortexa.browser_control.execute.ExecuteGroup;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.common.util.log.AppendLogger;
import cn.com.vortexa.script_node.dto.selenium.ACBotTypedSeleniumExecuteInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class AccountFingerBrowserSelenium extends OptSeleniumInstance {

    private final ReentrantLock lock = new ReentrantLock();

    @Getter
    private final AccountContext accountContext;

    @Override
    protected void addDefaultChromeOptions(ChromeOptions chromeOptions) {

    }

    /**
     * 执行链信息
     */
    private final List<ACBotTypedSeleniumExecuteInfo> executeInfoList = new ArrayList<>();

    @Getter
    private volatile boolean running = false;

    public AccountFingerBrowserSelenium(
            AccountContext accountContext,
            SeleniumParams params,
            AppendLogger logger
    ) throws IOException {
        super(accountContext.getSimpleInfo(), params, logger);
        this.accountContext = accountContext;
    }

//    @Override
//    protected WebDriver createWebDriver(ChromeOptions chromeOptions) {
//        if (getWebDriver() != null) return getWebDriver();
//
//        try {
//            URL remoteWebDriverUrl = new URL(getParams().getDriverPath());
//            chromeOptions.setCapability("timeouts", new HashMap<String, Integer>() {{
//                put("script", 30000);  // 设置脚本执行超时
//                put("pageLoad", 30000);  // 设置页面加载超时
//                put("implicit", 30000);  // 设置隐式等待超时
//            }});
//            return new RemoteWebDriver(remoteWebDriverUrl, chromeOptions);
//        } catch (MalformedURLException e) {
//            log.error("[{}] create remote web driver error", getInstanceId(), e);
//            throw new RuntimeException(e);
//        }
//    }

    /**
     * 开始账户指纹浏览器selenium控制
     */
    public void startACFBSelenium() {
        lock.lock();
        try {
            running = true;
            getLogger().info(getInstanceId() + " start all added bot selenium execute...");

            for (ACBotTypedSeleniumExecuteInfo executeInfo : executeInfoList) {
                try {
                    getLogger().info("[%s]-[%s]-[%s] start selenium execute...".formatted(
                            getInstanceId(), executeInfo.getBotKey(), executeInfo.getJobName()
                    ));
                    List<ExecuteGroup> seleniumExecuteChain = super.getSeleniumExecuteChain();
                    seleniumExecuteChain.clear();
                    seleniumExecuteChain.addAll(executeInfo.getSeleniumExecuteChain());

                    syncStart();
                    getLogger().info("[%s]-[%s]-[%s] selenium execute finish".formatted(
                            getInstanceId(), executeInfo.getBotKey(), executeInfo.getJobName()
                    ));
                } catch (Exception e) {
                    getLogger().error("[%s]-[%s]-[%s] selenium execute error".formatted(
                            getInstanceId(), executeInfo.getBotKey(), executeInfo.getJobName()
                    ), e);
                    randomWait(3);
                }
            }

            getLogger().info(getInstanceId() + " all selenium execute finish");
        } finally {
            getLogger().debug(getInstanceId() + "  closing webDriver");
            close();
            getLogger().debug(getInstanceId() + " close webDriver finish");
            executeInfoList.clear();
            running = false;
            lock.unlock();
        }
    }

    /**
     * 添加执行链
     *
     * @param executeInfo executeInfo
     */
    public void addExecuteInfo(ACBotTypedSeleniumExecuteInfo executeInfo) {
        getLogger().info("[%s]-[%s] adding execute info...".formatted(getInstanceId(), executeInfo.getBotKey()));
        lock.lock();
        try {
            executeInfoList.add(executeInfo);
            log.info("[%s]-[%s] add execute info finish".formatted(getInstanceId(), executeInfo.getBotKey()));
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void doClose() {
        super.doClose();
        executeInfoList.clear();
    }
}
