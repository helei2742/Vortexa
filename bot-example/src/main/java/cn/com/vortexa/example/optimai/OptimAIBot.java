package cn.com.vortexa.example.optimai;

import cn.com.vortexa.bot_father.BotLauncher;
import cn.com.vortexa.bot_father.anno.BotApplication;
import cn.com.vortexa.bot_father.anno.BotMethod;
import cn.com.vortexa.bot_father.anno.BotWSMethodConfig;
import cn.com.vortexa.bot_father.bot.AutoLaunchBot;
import cn.com.vortexa.bot_father.config.AutoBotConfig;
import cn.com.vortexa.bot_father.service.BotApi;
import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.common.exception.BotInitException;
import cn.com.vortexa.common.exception.BotStartException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author h30069248
 * @since 2025/3/24 17:14
 */
@BotApplication(name = "optim_ai", configParams = {OptimAIBot.TWO_CAPTCHA_API_KEY})
public class OptimAIBot extends AutoLaunchBot<OptimAIBot> {

    private static final String WS_CONNECT_URL = "wss://ws.optimai.network";
    public static final String TWO_CAPTCHA_API_KEY = "two_captcha_api_key";
    public static final String PASSWORD_KEY = "password";

    private final int WS_RECONNECT_INTERVAL_SECOND = 60 * 60 * 24;

    private OptimAIAPI optimAIAPI;

    @Override
    protected void botInitialized(AutoBotConfig botConfig, BotApi botApi) {
        this.optimAIAPI = new OptimAIAPI(this);
    }

    @Override
    protected OptimAIBot getInstance() {
        return this;
    }

    @BotMethod(
            jobType = BotJobType.REGISTER
    )
    public Result registry(AccountContext uniAC, List<AccountContext> sameIdACList, String inviteCode) {
        return optimAIAPI.registry(uniAC, inviteCode);
    }

    @BotMethod(
            jobType = BotJobType.LOGIN
    )
    public Result login(AccountContext accountContext) throws Exception {
        Result login = optimAIAPI.login(accountContext);
        return login;
    }

    @BotMethod(
            jobType = BotJobType.WEB_SOCKET_CONNECT,
            intervalInSecond = WS_RECONNECT_INTERVAL_SECOND,
            bowWsConfig = @BotWSMethodConfig(
                    isRefreshWSConnection = true,
                    reconnectLimit = 4,
                    heartBeatIntervalSecond = 15 * 60,
                    nioEventLoopGroupThreads = 2,
                    wsConnectCount = 20
            )
    )
    public OptimAIWSClient buildKeepAliveWSClient(AccountContext accountContext) {
        OptimAIWSClient client = new OptimAIWSClient(accountContext, WS_CONNECT_URL);
//        client.set
        return client;
    }

    public static void main(String[] args) throws BotStartException, BotInitException {
        List<String> list = new ArrayList<>(List.of(args));

        list.add("--vortexa.botKey=optimai_test");
        list.add("--vortexa.customConfig.two_captcha_api_key=");
        list.add("--vortexa.accountConfig.configFilePath=optimai_google.xlsx");
        list.add("--add-opens java.base/java.lang=ALL-UNNAMED");

        BotLauncher.launch(OptimAIBot.class, list.toArray(new String[0]));
    }
}
