package cn.com.vortexa.example.klok;


import cn.com.vortexa.common.constants.HttpMethod;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.web3.EthWalletUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static cn.com.vortexa.example.klok.KlokBot.*;

public class KlokApi {

    private static final String VERIFY_API = "https://api1-pp.klokapp.ai/v1/verify";


    private final KlokBot klokBot;

    public KlokApi(KlokBot klokBot) {
        this.klokBot = klokBot;
    }


    public Result login(AccountContext accountContext) {
        try {
            Pair<String, String> signature = generateSignature(accountContext);
            klokBot.logger.debug(accountContext.getSimpleInfo() + " signature success");

            CompletableFuture<String> tokenFuture = verify(accountContext, signature.getKey(), signature.getValue(), null);
            String token = tokenFuture.get();
            klokBot.logger.info(accountContext.getSimpleInfo() + " login success, token: " + token);
            accountContext.setParam(SESSION_TOKEN, token);

            return Result.ok(token);
        } catch (Exception e) {
            String errorMsg = accountContext.getSimpleInfo() + " login success, token: " +
                    (e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
            klokBot.logger.error(
                    errorMsg
            );
            return Result.fail(errorMsg);
        }
    }


    public CompletableFuture<String> verify(
            AccountContext accountContext,
            String message,
            String signature,
            String inviteCode
    ) {
        JSONObject body = new JSONObject();
        body.put("signedMessage", signature);
        body.put("message", message);
        body.put("referral_code", inviteCode);

        Map<String, String> headers = accountContext.getBrowserEnv().generateHeaders();
        headers.put("origin", "https://klokapp.ai");
        headers.put("referer", "https://klokapp.ai/");
        headers.put("content-type", "application/json");
        headers.put("accept", "*/*");

        return klokBot.syncRequest(
                accountContext.getProxy(),
                VERIFY_API,
                HttpMethod.POST,
                headers,
                null,
                body,
                () -> accountContext.getSimpleInfo() + " send verify request"
        ).thenApply(response -> {
            JSONObject result = JSONObject.parseObject(response);

            if ("Verification successful".equals(result.getString("message"))) {
                return result.getString("session_token");
            } else {
                throw new RuntimeException("Verification failed, " + response);
            }
        });
    }

    private String buildMessage(String address) {
        String template = "klokapp.ai wants you to sign in with your Ethereum account:\n%s\n\n\nURI: https://klokapp.ai/\nVersion: 1\nChain ID: 1\nNonce: %s\nIssued At: %s";

        // 获取当前时间的 Instant 对象（UTC 时间）
        Instant now = Instant.now();

        // 使用自定义格式化器，确保毫秒部分保留 3 位
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(java.time.ZoneOffset.UTC);

        // 格式化 Instant 对象为字符串
        String issuedAt = formatter.format(now);

        return template.formatted(address, EthWalletUtil.getRandomNonce(), issuedAt);
    }

    private Pair<String, String> generateSignature(AccountContext accountContext) {
        String primaryKey = accountContext.getParam(PRIMARY_KEY);
        String address = accountContext.getParam(ETH_ADDRESS);
//        if (StrUtil.isBlank(address)) {
            address = EthWalletUtil.getETHAddress(primaryKey);
            accountContext.setParam(ETH_ADDRESS, address);
//        }
        String message = buildMessage("0x2dB603E747E2db72747E5b972006f19B2D0d73a1");

        return new Pair<>(
                message,
                EthWalletUtil.signatureMessage2String(primaryKey, message)
        );
    }
}

