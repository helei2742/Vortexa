package cn.com.vortexa.browser_control;

import cn.com.vortexa.browser_control.dto.SeleniumProxy;
import com.alibaba.fastjson.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OptSeleniumInstance extends SeleniumInstance {

    public static final int CLICK_WAIT_MIN_TIME = 500;

    public static final int CLICK_WAIT_MAX_TIME = 2500;

    public static final int NORMAL_DRIVER_WAITE_TIME = 30;

    private WebDriverWait normalDriverWaiter;

    public OptSeleniumInstance(String instanceId, SeleniumProxy proxy, JSONObject params) throws IOException {
        super(instanceId, proxy, params);
    }

    @Override
    public void init() {

    }

    @Override
    public void webDriverLaunched() {
        normalDriverWaiter = new WebDriverWait(getWebDriver(), Duration.ofSeconds(NORMAL_DRIVER_WAITE_TIME));
    }

    public void xPathClick(String xPath, int waitSecond) {
        xPathClick(new WebDriverWait(getWebDriver(), Duration.ofSeconds(waitSecond)), xPath);
    }

    public void xPathClick(String xPath) {
        xPathClick(normalDriverWaiter, xPath);
    }

    public void xPathClick(WebDriverWait wait, String xPath) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xPath)));
        randomWait();
        element.click();
    }


    public WebElement xPathFindElement(String xPath, int waitSecond) {
        return xPathFindElement(new WebDriverWait(getWebDriver(), Duration.ofSeconds(waitSecond)), xPath);
    }

    public WebElement xPathFindElement(String xPath) {
        return xPathFindElement(normalDriverWaiter, xPath);
    }

    public WebElement xPathFindElement(WebDriverWait wait, String xPath) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xPath)));
        randomWait();
        return element;
    }

    public boolean xPathExist(String xPath) {
        try {
            WebElement element = getWebDriver().findElement(By.xpath(xPath));
            return element != null;
        }catch (Exception e) {
            return false;
        }
    }



    public List<WebElement> xPathFindElements(String xPath) {
        return getWebDriver().findElements(By.xpath(xPath));
    }

    public void randomWait() {
        try {
            TimeUnit.MILLISECONDS.sleep(getRandom().nextInt(CLICK_WAIT_MIN_TIME, CLICK_WAIT_MAX_TIME));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
