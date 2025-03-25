package cn.com.vortexa.example.optimai;

import cn.com.vortexa.bot_father.anno.BotMethod;
import cn.com.vortexa.bot_father.anno.BotWSMethodConfig;
import cn.com.vortexa.bot_father.bot.AutoLaunchBot;
import cn.com.vortexa.bot_father.config.AutoBotConfig;
import cn.com.vortexa.bot_father.service.BotApi;
import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.AccountContext;

import java.util.List;

/**
 * @author h30069248
 * @since 2025/3/24 17:14
 */
public class OptimAIBot extends AutoLaunchBot<OptimAIBot> {

    private static final String WS_CONNECT_URL = "wss://ws.optimai.network";

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
        client.set
        return client;
    }
}
