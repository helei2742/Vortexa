package cn.com.vortexa.captcha;

import cn.com.vortexa.common.entity.ProxyInfo;
import com.twocaptcha.TwoCaptcha;
import com.twocaptcha.captcha.Captcha;
import com.twocaptcha.captcha.ReCaptcha;
import com.twocaptcha.captcha.Turnstile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CaptchaResolver {

    private static final int GET_RESULT_TIMES = 20;

    private static final int GET_RESULT_INTERVAL_SECONDS = 3;

    public static CompletableFuture<String> reCaptchaV3EnterpriseResolve(
            ProxyInfo proxy,
            String websiteUrl,
            String websiteKey,
            String action,
            String twoCaptchaApiKey
    ) {
        return CompletableFuture.supplyAsync(()->{
            TwoCaptcha solver = TwoCaptchaSolverFactory.getTwoCaptchaSolver(twoCaptchaApiKey, proxy);

            ReCaptcha captcha = new ReCaptcha();
            captcha.setSiteKey(websiteKey);
            captcha.setUrl(websiteUrl);
            captcha.setVersion("v3");
            captcha.setAction(action);
            captcha.setScore(0.9);
            captcha.setEnterprise(true);

            try {
                solver.solve(captcha);

                for (int i = 0; i < GET_RESULT_TIMES; i++) {
                    String result = captcha.getCode();
                    if (result != null) {
                        log.debug(proxy + " recaptcha v3 enterprise resolved successfully.");
                        return result;
                    }
                    log.debug(proxy + " waiting for recaptcha v3 enterprise result.[{}/{}]", i + 1, GET_RESULT_INTERVAL_SECONDS);
                    TimeUnit.SECONDS.sleep(GET_RESULT_INTERVAL_SECONDS);
                }

                log.error(proxy + " get recaptcha v3 enterprise result timeout.");
                throw new RuntimeException("get recaptcha v3 enterprise result timeout.");
            } catch (Exception e) {
                throw new RuntimeException(websiteUrl + " recaptcha v3 enterprise resolve error", e);
            }
        });
    }


    public static CompletableFuture<String> cloudFlareResolve(
            ProxyInfo proxy,
            String websiteUrl,
            String websiteKey,
            String twoCaptchaApiKey
    ) {
        return CompletableFuture.supplyAsync(()->{
            TwoCaptcha solver = TwoCaptchaSolverFactory.getTwoCaptchaSolver(twoCaptchaApiKey, proxy);

            Turnstile captcha = new Turnstile();
            captcha.setSiteKey(websiteKey);
            captcha.setUrl(websiteUrl);

            try {
                String taskId = solver.send(captcha);

                for (int i = 0; i < GET_RESULT_TIMES; i++) {
                    String result = solver.getResult(taskId);
                    if (result != null) {
                        log.debug(proxy + " cloudflare resolved successfully.");
                        return result;
                    }
                    log.debug(proxy + " waiting for cloudflare result.[{}/{}]", i + 1, GET_RESULT_INTERVAL_SECONDS);
                    TimeUnit.SECONDS.sleep(GET_RESULT_INTERVAL_SECONDS);
                }

                log.error(proxy + " get cloudflare result timeout.");
                throw new RuntimeException("get cloudflare result timeout.");
            } catch (Exception e) {
                throw new RuntimeException(websiteUrl + " CloudFlare resolve error", e);
            }
        });
    }

}
