package cn.com.vortexa.bot_father.service.impl;

import cn.com.vortexa.bot_father.service.BotApi;
import cn.com.vortexa.rpc.IAccountBaseInfoRPC;
import cn.com.vortexa.rpc.IBotInfoRPC;
import cn.com.vortexa.job.service.BotJobService;
import cn.com.vortexa.rpc.IBrowserEnvRPC;
import cn.com.vortexa.rpc.IDiscordAccountRPC;
import cn.com.vortexa.rpc.IProxyInfoRPC;
import cn.com.vortexa.rpc.ITelegramAccountRPC;
import cn.com.vortexa.rpc.ITwitterAccountRPC;
import cn.com.vortexa.rpc.anno.RPCReference;
import cn.com.vortexa.rpc.bot.IBotAccountRPC;
import cn.com.vortexa.rpc.bot.IRewordInfoRPC;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Getter
@Component
public class DBBotApi implements BotApi {

    @Autowired
    private BotJobService botJobService;

    @Autowired
    private IBotAccountRPC botAccountService;

    @Autowired
    private IRewordInfoRPC rewordInfoService;


    @RPCReference
    private IBotInfoRPC botInfoRPC;

    @RPCReference
    private IAccountBaseInfoRPC accountBaseInfoRPC;

    @RPCReference
    private ITwitterAccountRPC twitterAccountRPC;

    @RPCReference
    private ITelegramAccountRPC telegramAccountRPC;

    @RPCReference
    private IProxyInfoRPC proxyInfoRPC;

    @RPCReference
    private IBrowserEnvRPC browserEnvRPC;

    @RPCReference
    private IDiscordAccountRPC discordAccountRPC;
}
