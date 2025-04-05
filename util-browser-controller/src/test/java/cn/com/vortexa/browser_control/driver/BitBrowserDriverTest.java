package cn.com.vortexa.browser_control.driver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

class BitBrowserDriverTest {
    static BitBrowserDriver bitBrowserDriver;
    @BeforeAll
    public static void setUp() {
        bitBrowserDriver = new BitBrowserDriver("http://192.168.1.2:54888");

    }

    @Test
    void startBySeq(){
        System.out.println(bitBrowserDriver.startWebDriverBySeq(1));
    }

    @Test
    void buildPageQueryWindowTest() {
        JSONObject params = new JSONObject();
        params.put("page", 0);
        params.put("pageSize", 10);
        params.put("seq", 1);
        JSON result = bitBrowserDriver.pageQueryWindow(params);

        System.out.println(result);
    }


    @Test
    void pageQueryWindow() {
    }

    @Test
    void openBrowserWindow() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", "bff65d8937f944349ed1070590605360");
        jsonObject.put("args", new ArrayList<>());
        jsonObject.put("queue", true);
        System.out.println(bitBrowserDriver.openBrowserWindow(jsonObject));
    }

    @Test
    void closeBrowserWindow() {
    }

    @Test
    void closeAllBrowserWindow() {
    }

    @Test
    void deleteBrowserWindow() {
    }


    @Test
    void deleteBrowserWindowBatch() {
    }

    @Test
    void browserWindowDetail() {
    }


    @Test
    void boundsBrowserWindow() {
    }

    @Test
    void autoFlexWindow() {
        bitBrowserDriver.autoFlexWindow();
    }


    @Test
    void updateWindowGroup() {
    }

    @Test
    void updateWindowGroupBody() {
    }

    @Test
    void updateWindowRemark() {
    }

    @Test
    void updateWindowRemarkBody() {
    }

    @Test
    void proxyCheckOut() {
    }

    @Test
    void proxyCheckOutBody() {
    }

    @Test
    void displayListQuery() {
    }

    @Test
    void displayListQueryBody() {
    }
}
