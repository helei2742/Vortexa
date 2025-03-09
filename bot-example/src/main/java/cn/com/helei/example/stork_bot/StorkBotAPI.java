package cn.com.helei.example.stork_bot;

import cn.com.helei.common.constants.HttpMethod;
import cn.com.helei.common.dto.Result;
import cn.com.helei.common.entity.AccountContext;
import cn.com.helei.mail.constants.MailProtocolType;
import cn.com.helei.mail.factory.MailReaderFactory;
import cn.com.helei.mail.reader.MailReader;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StorkBotAPI {

    private static final String AWS_CLIENT_ID = "5msns4n49hmg3dftp2tp1t2iuh";

    private static final String STORK_SITE_APi = "https://cognito-idp.ap-northeast-1.amazonaws.com/";

    private static final String STORK_SIGNED_PRICE_API = "https://app-api.jp.stork-oracle.network/v1/stork_signed_prices";

    private static final String VALIDATE_SIGNED_PRICE_API = "https://app-api.jp.stork-oracle.network/v1/stork_signed_prices/validations";

    private static final String MAIL_FROM = "noreply@stork.network";

    private static final Pattern V_CODE_PATTERN = Pattern.compile("\\b\\d{6}\\b");

    public static final String PASSWORD_KEY = "stork_password";

    public static final String IMAP_PASSWORD_KEY = "imap_password";

    public static final String TOKEN_KEY = "stork_token";


    private static final MailReader mailReader = MailReaderFactory.getMailReader(MailProtocolType.imap,
            "imap.gmail.com", "993", true);
    private static final Logger log = LoggerFactory.getLogger(StorkBotAPI.class);

    private final StorkBot bot;

    public StorkBotAPI(StorkBot bot) {
        this.bot = bot;

    }


    /**
     * 注册
     *
     * @param exampleAC     exampleAC
     * @param sameABIACList sameABIACList
     * @param inviteCode    inviteCode
     * @return Result
     */
    public Result signup(AccountContext exampleAC, List<AccountContext> sameABIACList, String inviteCode) {
        if (exampleAC.getAccountBaseInfoId() != 50) return Result.fail("");
        bot.logger.info("%s start signup".formatted(exampleAC.getSimpleInfo()));

        CompletableFuture<String> signupFuture = sendSignUpRequest(exampleAC, inviteCode)
                .thenApplyAsync(responseStr -> {
                    bot.logger.info("%s signup request sent, %s".formatted(exampleAC.getSimpleInfo(), responseStr));
                    try {
                        TimeUnit.SECONDS.sleep(0);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return queryCheckCode(exampleAC);
                })
                .thenApplyAsync(checkCode -> confirmSignup(exampleAC, checkCode));

        try {
            String confirmResponse = signupFuture.get();
            bot.logger.info("%s sign up finish, %s".formatted(exampleAC.getSimpleInfo(), confirmResponse));
            for (AccountContext accountContext : sameABIACList) {
                AccountContext.signUpSuccess(accountContext);
            }
            return Result.ok();
        } catch (InterruptedException | ExecutionException e) {
            String errorMsg = "%s signup error, %s".formatted(exampleAC.getSimpleInfo(),
                    e.getCause() == null ? e.getCause().getMessage() : e.getMessage());
            bot.logger.error(errorMsg, e);
            return Result.fail(errorMsg);
        }
    }

    /**
     * 登录
     *
     * @param accountContext accountContext
     * @return Result
     */
    public Result login(AccountContext accountContext) {
        if (accountContext.getAccountBaseInfoId() != 1) {
            return Result.fail("test");
        }


        return Result.ok();
    }


    /**
     * keepAlive
     *
     * @param accountContext accountContext
     */
    public void keepAlive(AccountContext accountContext) {
        // TODO check token refresh it
        if (accountContext.getAccountBaseInfoId() != 1) return;

        try {
            String msgHash = getSignedPrice(accountContext).get();

            String response = validateSignedPrice(accountContext, msgHash).get();

            bot.logger.info(accountContext + "%s keep alive success, " + response);
        } catch (InterruptedException | ExecutionException e) {
            bot.logger.error(accountContext + " keep alive error " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
        }
    }


    private CompletableFuture<String> validateSignedPrice(AccountContext accountContext, String msgHash) {
        bot.logger.info(accountContext.getSimpleInfo() + " start validate signed price ");
        String token = accountContext.getParam(TOKEN_KEY);

        Map<String, String> headers = accountContext.getBrowserEnv().generateHeaders();
        headers.put("Authorization", "Bearer " + token);

        JSONObject body = new JSONObject();
        body.put("msg_hash", msgHash);
        body.put("valid", true);

        return bot.syncRequest(
                accountContext.getProxy(),
                VALIDATE_SIGNED_PRICE_API,
                HttpMethod.POST,
                headers,
                null,
                body,
                () -> accountContext.getSimpleInfo() + " send validate signed price request"
        );
    }


    private CompletableFuture<String> getSignedPrice(AccountContext accountContext) {
        bot.logger.info(accountContext.getSimpleInfo() + " start get signed price ");

        String token = accountContext.getParam(TOKEN_KEY);

        Map<String, String> headers = accountContext.getBrowserEnv().generateHeaders();
        headers.put("Authorization", "Bearer " + token);

        return bot.syncRequest(
                accountContext.getProxy(),
                STORK_SIGNED_PRICE_API,
                HttpMethod.GET,
                headers,
                null,
                null,
                () -> accountContext.getSimpleInfo() + " send get signed price request"
        ).thenApplyAsync(responseStr -> {
            JSONObject signedPrices = JSONObject.parseObject(responseStr);
            bot.logger.info(accountContext.getSimpleInfo() + " signed price is " + signedPrices);

            JSONArray prices = signedPrices.getJSONArray("data");
            for (int i = 0; i < prices.size(); i++) {
                JSONObject price = prices.getJSONObject(i);
                JSONObject timestampedSignature = price.getJSONObject("timestamped_signature");
                if (timestampedSignature != null) {
                    return timestampedSignature.getString("msg_hash");
                }
            }
            throw new RuntimeException("signed price is empty");
        });
    }


    private String confirmSignup(AccountContext exampleAC, String checkCode) {
        bot.logger.info(exampleAC.getSimpleInfo() + " check code is " + checkCode + " start confirm sign up");

        Map<String, String> headers = exampleAC.getBrowserEnv().generateHeaders();
        headers.put("X-Amz-Target", "AWSCognitoIdentityProviderService.ConfirmSignUp");

        headers.put("accept", "*/*");
        headers.put("x-amz-user-agent", "aws-amplify/6.12.1 auth/3 framework/2 Authenticator ui-react/6.9.0");
        headers.put("content-type", "x-amz-json-1.1");
        headers.put("origin", "https://app.stork.network");
        headers.put("referer", "https://app.stork.network/");

        JSONObject body = new JSONObject();
        body.put("Username", exampleAC.getAccountBaseInfo().getEmail());
        body.put("ConfirmationCode", checkCode);
        body.put("ClientId", AWS_CLIENT_ID);

        CompletableFuture<String> future = bot.syncRequest(
                exampleAC.getProxy(),
                STORK_SITE_APi,
                HttpMethod.POST,
                headers,
                null,
                body,
                () -> exampleAC.getSimpleInfo() + " confirm sign up"
        );
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("confirm sign up error, %s".formatted(e.getCause() == null ? e.getCause().getMessage() : e.getMessage()));
        }
    }


    /**
     * Step 2 从邮箱获取验证码
     *
     * @param exampleAC exampleAC
     * @return String
     */
    private @NotNull String queryCheckCode(AccountContext exampleAC) {
        bot.logger.info(exampleAC.getSimpleInfo() + " start query check code");

        String email = exampleAC.getAccountBaseInfo().getEmail();
        String imapPassword = (String) exampleAC.getAccountBaseInfo().getParams().get(IMAP_PASSWORD_KEY);

        AtomicReference<String> checkCode = new AtomicReference<>();
        mailReader.stoppableReadMessage(email, imapPassword, 3, message -> {
            try {
                String newValue = resolveVerifierCodeFromMessage(message);
                checkCode.set(newValue);
                return StrUtil.isNotBlank(newValue);
            } catch (MessagingException e) {
                throw new RuntimeException("email check code query error", e);
            }
        });

        if (StrUtil.isBlank(checkCode.get())) {
            throw new RuntimeException("check code is empty");
        }

        return checkCode.get();
    }


    private CompletableFuture<String> sendSignUpRequest(AccountContext exampleAC, String inviteCode) {
        Map<String, String> headers = exampleAC.getBrowserEnv().generateHeaders();
        headers.put("x-Amz-Target", "AWSCognitoIdentityProviderService.SignUp");
        headers.put("accept", "*/*");
        headers.put("x-amz-user-agent", "aws-amplify/6.12.1 auth/3 framework/2 Authenticator ui-react/6.9.0");
        headers.put("content-type", "x-amz-json-1.1");
        headers.put("origin", "https://app.stork.network");
        headers.put("referer", "https://app.stork.network/");

        JSONObject body = generateSignupBody(exampleAC, inviteCode);

        // Step 1 注册请求
        return bot.syncRequest(
                exampleAC.getProxy(),
                STORK_SITE_APi,
                HttpMethod.POST,
                headers,
                null,
                body,
                () -> exampleAC.getSimpleInfo() + " send sign up request"
        ).exceptionallyAsync(throwable -> {
            bot.logger.warn(exampleAC.getSimpleInfo() + " send sign up request error " +
                    throwable.getMessage() + " try resend");
            try {
                return resendSignUpCode(exampleAC).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("resend sign up error, %s".formatted(throwable.getMessage()));
            }
        });
    }

    private CompletableFuture<String> resendSignUpCode(AccountContext exampleAC) {
        Map<String, String> headers = exampleAC.getBrowserEnv().generateHeaders();
        headers.put("x-Amz-Target", "AWSCognitoIdentityProviderService.ResendConfirmationCode");
        headers.put("accept", "*/*");
        headers.put("x-amz-user-agent", "aws-amplify/6.12.1 auth/3 framework/2 Authenticator ui-react/6.9.0");
        headers.put("content-type", "x-amz-json-1.1");
        headers.put("origin", "https://app.stork.network");
        headers.put("referer", "https://app.stork.network/");

        JSONObject body = new JSONObject();
        body.put("ClientId", AWS_CLIENT_ID);
        body.put("Username", exampleAC.getAccountBaseInfo().getEmail());

        // Step 1 注册请求
        return bot.syncRequest(
                exampleAC.getProxy(),
                STORK_SITE_APi,
                HttpMethod.POST,
                headers,
                null,
                body,
                () -> exampleAC.getSimpleInfo() + " resend sign up request"
        );
    }

    private String resolveVerifierCodeFromMessage(Message message) throws MessagingException {
        boolean b = Arrays.stream(message.getFrom())
                .anyMatch(address -> address.toString().contains(MAIL_FROM));
        if (!b) return null;

        String context = MailReader.getTextFromMessage(message);
        Matcher matcher = V_CODE_PATTERN.matcher(context);

        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private static @NotNull JSONObject generateSignupBody(AccountContext exampleAC, String inviteCode) {
        JSONObject body = new JSONObject();

        String email = exampleAC.getAccountBaseInfo().getEmail();
        body.put("Username", email);
        body.put("Password", exampleAC.getParam(PASSWORD_KEY));
        body.put("ClientId", AWS_CLIENT_ID);

        JSONArray userAtributes = new JSONArray();
        JSONObject ua1 = new JSONObject();
        ua1.put("Name", "email");
        ua1.put("Value", email);
        JSONObject ua2 = new JSONObject();
        ua2.put("Name", "custom:referral_code");
        ua2.put("Value", inviteCode);
        userAtributes.add(ua1);
        userAtributes.add(ua2);
        body.put("UserAttributes", userAtributes);
        return body;
    }

}
