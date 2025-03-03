package cn.com.helei.captcha;


import cn.com.helei.common.entity.ProxyInfo;
import com.twocaptcha.TwoCaptcha;

public class TwoCaptchaSolverFactory {

    public static TwoCaptcha getTwoCaptchaSolver(String apiKey, ProxyInfo proxy) {
        TwoCaptcha solver = new TwoCaptcha(apiKey);
        solver.setHttpClient(new ProxyApiClient(proxy));
        return solver;
    }
}
