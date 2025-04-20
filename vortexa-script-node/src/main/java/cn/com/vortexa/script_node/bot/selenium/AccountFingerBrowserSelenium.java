package cn.com.vortexa.script_node.bot.selenium;

import cn.com.vortexa.browser_control.OptSeleniumInstance;
import cn.com.vortexa.browser_control.dto.SeleniumParams;
import cn.com.vortexa.browser_control.execute.ExecuteGroup;
import cn.com.vortexa.common.entity.AccountContext;
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

    @Override
    protected void addDefaultChromeOptions(ChromeOptions chromeOptions) {

    }

    /**
     * 添加过的bot执行链的key set
     */
    private final Set<String> addedBotExecutionSet = new HashSet<>();
    /**
     * 执行链信息
     */
    private final List<ACBotTypedSeleniumExecuteInfo> executeInfoList = new ArrayList<>();

    @Getter
    private volatile boolean running = false;

    public AccountFingerBrowserSelenium(AccountContext accountContext, SeleniumParams params) throws IOException {
        super(accountContext.getSimpleInfo(), params);
    }

    @Override
    protected WebDriver createWebDriver(ChromeOptions chromeOptions) {
        if (getWebDriver() != null) return getWebDriver();

        try {
            URL remoteWebDriverUrl = new URL(getParams().getDriverPath());
            chromeOptions.setCapability("timeouts", new HashMap<String, Integer>() {{
                put("script", 30000);  // 设置脚本执行超时
                put("pageLoad", 30000);  // 设置页面加载超时
                put("implicit", 30000);  // 设置隐式等待超时
            }});
            return new RemoteWebDriver(remoteWebDriverUrl, chromeOptions);
        } catch (MalformedURLException e) {
            log.error("[{}] create remote web driver error", getInstanceId(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 开始账户指纹浏览器selenium控制
     */
    public void startACFBSelenium() {
        lock.lock();
        try {
            running = true;
            log.info("[{}] start all added bot selenium execute...", getInstanceId());

            for (ACBotTypedSeleniumExecuteInfo executeInfo : executeInfoList) {
                try {
                    log.info("[{}]-[{}] start selenium execute...", getInstanceId(), executeInfo.getBotKey());
                    List<ExecuteGroup> seleniumExecuteChain = super.getSeleniumExecuteChain();
                    seleniumExecuteChain.clear();
                    seleniumExecuteChain.addAll(executeInfo.getSeleniumExecuteChain());

                    syncStart();
                    log.info("[{}]-[{}] selenium execute finish", getInstanceId(), executeInfo.getBotKey());
                } catch (Exception e) {
                    log.error("[{}]-[{}] selenium execute error", getInstanceId(), executeInfo.getBotKey(), e);
                    randomWait(3);
                }
            }

            log.info("[{}] all selenium execute finish", getInstanceId());
        } finally {
            log.info("[{}] closing webDriver", getInstanceId());
            close();
            log.info("[{}] close webDriver finish", getInstanceId());

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
        log.info("[{}]-[{}] adding execute info...", getInstanceId(), executeInfo.getBotKey());
        lock.lock();
        try {
            executeInfoList.add(executeInfo);
            addedBotExecutionSet.add(executeInfo.getBotKey());
            log.info("[{}]-[{}] add execute info finish", getInstanceId(), executeInfo.getBotKey());
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void doClose() {
        super.doClose();
        executeInfoList.clear();
        addedBotExecutionSet.clear();
    }
}
