package cn.com.vortexa.browser_control.execute;

import cn.com.vortexa.browser_control.SeleniumInstance;
import org.openqa.selenium.WebDriver;

public interface ExecuteLogic {

    void execute(WebDriver webDriver, SeleniumInstance seleniumInstance);

}
