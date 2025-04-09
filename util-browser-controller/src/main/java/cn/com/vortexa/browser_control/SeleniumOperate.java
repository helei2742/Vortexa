package cn.com.vortexa.browser_control;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

public interface SeleniumOperate {

    void xPathClick(String xPath, int waitSecond);

    void xPathClick(String xPath);

    void xPathClick(WebDriverWait wait, String xPath);

    WebElement xPathFindElement(String xPath, int waitSecond);

    WebElement xPathFindElement(String xPath);

    WebElement xPathFindElement(WebDriverWait wait, String xPath) ;

    boolean xPathExist(String xPath) ;

    List<WebElement> xPathFindElements(String xPath);

    void randomWait(int i);

    void randomWait();
}
