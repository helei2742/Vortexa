package cn.com.vortexa.captcha;

import cn.com.vortexa.common.entity.ProxyInfo;
import com.twocaptcha.TwoCaptcha;
import com.twocaptcha.captcha.Turnstile;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CloudFlareResolver {

    private static final int GET_RESULT_TIMES = 20;

    private static final int GET_RESULT_INTERVAL_SECONDS = 3;

    public static CompletableFuture<String> cloudFlareResolve(
            ProxyInfo proxy,
            String websiteUrl,
            String websiteKey,
            String twoCaptchaApiKey
    ) {
        return CompletableFuture.supplyAsync(()->{
            TwoCaptcha solver = TwoCaptchaSolverFactory.getTwoCaptchaSolver(twoCaptchaApiKey, proxy);
            solver.setHttpClient(new ProxyApiClient(proxy));

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
