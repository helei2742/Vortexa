package cn.com.vortexa.captcha;


import cn.com.vortexa.common.entity.ProxyInfo;
import com.twocaptcha.TwoCaptcha;

public class TwoCaptchaSolverFactory {

    public static TwoCaptcha getTwoCaptchaSolver(String apiKey, ProxyInfo proxy) {
        TwoCaptcha solver = new TwoCaptcha(apiKey);
        solver.setHttpClient(new ProxyApiClient(proxy));
        return solver;
    }
}
