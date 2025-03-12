package cn.com.vortexa.common.util.aws;

import cn.com.vortexa.common.constants.HttpMethod;
import cn.com.vortexa.common.entity.ProxyInfo;
import cn.com.vortexa.common.util.http.RestApiClientFactory;
import com.alibaba.fastjson.JSONObject;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AWSAuthSRPFLOWClient {

    private final String awsUrl;

    private final String refreshUrl;

    private final String userPoolId;

    private final String clientId;

    public static void main(String[] args) throws Exception {
        AWSAuthSRPFLOWClient awsAuthSRPFLOWClient = new AWSAuthSRPFLOWClient(
                "https://cognito-idp.ap-northeast-1.amazonaws.com/",
                "https://stork-prod-apps.auth.ap-northeast-1.amazoncognito.com/oauth2/token",
                "ap-northeast-1_M22I44OpC",
                "5msns4n49hmg3dftp2tp1t2iuh"
        );

        Map<String, String> headers = new HashMap<>();
        headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        headers.put("origin", "https://app.stork.network");
        headers.put("referer", "https://app.stork.network/");

        AwsToken awsToken = awsAuthSRPFLOWClient.userSrpLogin(
                null,
                "914577981@qq.com",
                "123456789Abc.",
                headers
        );

        System.out.println("origin token :\n" + awsToken);
        AwsToken refreshed = awsAuthSRPFLOWClient.refreshToken(null, awsToken, headers);
        System.out.println("refreshed token :\n" + refreshed);
    }

    public AWSAuthSRPFLOWClient(String awsUrl, String refreshUrl, String userPoolId, String clientId) {
        this.awsUrl = awsUrl;
        this.refreshUrl = refreshUrl;
        this.userPoolId = userPoolId;
        this.clientId = clientId;
    }

    /**
     * 登录获取token
     *
     * @param proxyInfo proxyInfo
     * @param username  username
     * @param password  password
     * @param headers   headers
     * @return AwsToken
     */
    public AwsToken userSrpLogin(
            ProxyInfo proxyInfo,
            String username,
            String password,
            Map<String, String> headers
    ) {
        try {
            SRPHelper srpHelper = new SRPHelper(password);

            CompletableFuture<JSONObject> future = initAuth(proxyInfo, username, headers, srpHelper);

            JSONObject result = future.get();

            return passwordVerifier(proxyInfo, result, srpHelper, headers).get();
        } catch (Exception e) {
            throw new RuntimeException("login error", e);
        }
    }

    /**
     * 刷新token
     *
     * @param proxyInfo   proxyInfo
     * @param originToken originToken
     * @param headers     headers
     * @return AwsToken
     */
    public AwsToken refreshToken(ProxyInfo proxyInfo, AwsToken originToken, Map<String, String> headers) {
        JSONObject body = new JSONObject();

        body.put("grant_type", "refresh_token");
        body.put("client_id", clientId);
        body.put("refresh_token", originToken.getRefreshToken());
        headers.put("Content-Type", "application/x-www-form-urlencoded");

//        headers.put("X-Amz-Target", "AWSCognitoIdentityProviderService.InitiateAuth");
//        headers.put("x-amz-user-agent", "aws-amplify/6.12.1 auth/4 framework/2 Authenticator ui-react/6.9.0");

        try {
            return RestApiClientFactory.getClient(proxyInfo).request(
                    refreshUrl,
                    HttpMethod.POST,
                   headers,
                    null,
                    body
            ).thenApply(originToken::refresh).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("refresh token error", e);
        }
    }


    private CompletableFuture<JSONObject> initAuth(ProxyInfo proxyInfo, String username, Map<String, String> headers, SRPHelper srpHelper) {
        JSONObject body = new JSONObject();
        body.put("AuthFlow", "USER_SRP_AUTH");
        body.put("ClientId", clientId);

        JSONObject authParameters = new JSONObject();
        authParameters.put("USERNAME", username);

        // 生成 `a`（私钥）和 `A`（公钥）
        BigInteger A = srpHelper.getPublicA();
        BigInteger a = srpHelper.getPrivateA();

        authParameters.put("SRP_A", A.toString(16));
        body.put("AuthParameters", authParameters);

        headers.put("x-amz-target", "AWSCognitoIdentityProviderService.InitiateAuth");
        headers.put("x-amz-user-agent", "aws-amplify/6.12.1 auth/4 framework/2 Authenticator ui-react/6.9.0");

        return RestApiClientFactory.getClient(proxyInfo).request(
                awsUrl,
                HttpMethod.POST,
                buildHeaders(headers),
                null,
                body
        ).thenApply(JSONObject::parseObject);
    }


    private CompletableFuture<AwsToken> passwordVerifier(
            ProxyInfo proxyInfo,
            JSONObject initAuthResult,
            SRPHelper srpHelper,
            Map<String, String> headers
    ) throws Exception {
        JSONObject challengeParameters = initAuthResult.getJSONObject("ChallengeParameters");
        String saltHex = challengeParameters.getString("SALT");
        String srpBStr = challengeParameters.getString("SRP_B");
        String secretBlock = challengeParameters.getString("SECRET_BLOCK");
        String userIdForSrp = challengeParameters.getString("USER_ID_FOR_SRP");

        srpHelper.setUserId(userIdForSrp, userPoolId);

        String signature = srpHelper.getSignature(
                saltHex,
                srpBStr,
                secretBlock
        );


        JSONObject authParameters = new JSONObject();
        authParameters.put("USERNAME", userIdForSrp);
        authParameters.put("PASSWORD_CLAIM_SECRET_BLOCK", secretBlock);
        authParameters.put("PASSWORD_CLAIM_SIGNATURE", signature);
        authParameters.put("TIMESTAMP", srpHelper.getDateString());

        JSONObject body = new JSONObject();
        body.put("ChallengeName", "PASSWORD_VERIFIER");
        body.put("ClientId", clientId);
        body.put("ChallengeResponses", authParameters);

        headers.put("x-amz-target", "AWSCognitoIdentityProviderService.RespondToAuthChallenge");
        headers.put("x-amz-user-agent", "aws-amplify/6.12.1 framework/2");

        return RestApiClientFactory.getClient(proxyInfo).request(
                awsUrl,
                HttpMethod.POST,
                buildHeaders(headers),
                null,
                body
        ).thenApplyAsync(AwsToken::generateFromAWSResponse);
    }


    private Map<String, String> buildHeaders(Map<String, String> headers) {
        Map<String, String> awsHeaders = new HashMap<>(headers);
        awsHeaders.put("Content-Type", "application/x-amz-json-1.1");
        awsHeaders.put("sec-fetch-site", "cross-site");
        awsHeaders.put("sec-fetch-mode", "cors");
        awsHeaders.put("sec-fetch-dest", "empty");
        awsHeaders.put("accept", "*/*");

        return awsHeaders;
    }
}
