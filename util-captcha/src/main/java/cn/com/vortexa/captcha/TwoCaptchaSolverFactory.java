package cn.com.vortexa.captcha;


import cn.com.vortexa.common.entity.ProxyInfo;
import com.twocaptcha.TwoCaptcha;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TwoCaptchaSolverFactory {

    public static final Map<String, TwoCaptcha> INSTANCEMAP = new ConcurrentHashMap<>();

    public static TwoCaptcha getTwoCaptchaSolver(String apiKey, ProxyInfo proxy) {
        return INSTANCEMAP.compute(generateKey(apiKey, proxy), (k,v)->{
           if (v == null) {
               v = new TwoCaptcha();
               v.setHttpClient(new ProxyApiClient(proxy));
               v.setApiKey(apiKey);
           }
           return v;
        });
    }

    private static String generateKey(String apiKey, ProxyInfo proxy) {
        return apiKey + "_" + (proxy == null ? "NO_PROXY" : proxy.generateAddressStr());
    }
}
