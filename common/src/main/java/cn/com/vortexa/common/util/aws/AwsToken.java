package cn.com.vortexa.common.util.aws;

import com.alibaba.fastjson.JSONObject;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class AwsToken {

    private String idToken;

    private String expireIn;

    private String tokenType;

    private String refreshToken;

    private String accessToken;

    public String getAuthorization() {
        return tokenType + " " + accessToken;
    }

    public static AwsToken generateFromAWSResponse(String response) {
        JSONObject result = JSONObject.parseObject(response);
        JSONObject authenticationResult = result.getJSONObject("AuthenticationResult");

        return AwsToken.builder()
                .accessToken(authenticationResult.getString("AccessToken"))
                .idToken(authenticationResult.getString("IdToken"))
                .expireIn(authenticationResult.getString("ExpiresIn"))
                .refreshToken(authenticationResult.getString("RefreshToken"))
                .tokenType(authenticationResult.getString("TokenType"))
                .build();
    }

    public AwsToken refresh(String response) {
        JSONObject authenticationResult = JSONObject.parseObject(response);

        return AwsToken.builder()
                .accessToken(authenticationResult.getString("access_token"))
                .idToken(authenticationResult.getString("id_token"))
                .expireIn(authenticationResult.getString("expires_in"))
                .refreshToken(this.getRefreshToken())
                .tokenType(authenticationResult.getString("token_type"))
                .build();
    }
}
