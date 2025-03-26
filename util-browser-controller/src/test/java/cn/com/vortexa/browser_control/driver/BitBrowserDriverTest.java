package cn.com.vortexa.browser_control.driver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BitBrowserDriverTest {
    static BitBrowserDriver bitBrowserDriver;
    @BeforeAll
    public static void setUp() {
        bitBrowserDriver = new BitBrowserDriver("http://192.168.1.2:59555");

    }

    @Test
    void buildPageQueryWindowBodyTest() {
        JSONObject params = new JSONObject();
        params.put("page", 0);
        params.put("pageSize", 10);
        JSON result = bitBrowserDriver.pageQueryWindow(params);

        System.out.println(result);
    }
}
