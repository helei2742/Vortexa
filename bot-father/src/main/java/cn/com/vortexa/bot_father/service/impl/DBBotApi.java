package cn.com.vortexa.bot_father.service.impl;

import cn.com.vortexa.bot_father.service.BotApi;
import cn.com.vortexa.job.service.BotJobService;
import cn.com.vortexa.rpc.*;
import cn.com.vortexa.rpc.bot.IBotAccountRPC;
import cn.com.vortexa.rpc.bot.IRewordInfoRPC;
import lombok.Getter;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Getter
@Component
public class DBBotApi implements BotApi {

    @Autowired
    private BotJobService botJobService;

    @Autowired
    private IBotAccountRPC botAccountRPC;

    @Autowired
    private IRewordInfoRPC rewordInfoRPC;

//    @DubboReference
    private IProjectInfoRPC projectInfoRPC;

    @DubboReference
    private IBotInfoRPC botInfoRPC;

    @DubboReference
    private IAccountBaseInfoRPC accountBaseInfoRPC;

    @DubboReference
    private ITwitterAccountRPC twitterAccountRPC;

    @DubboReference
    private ITelegramAccountRPC telegramAccountRPC;

    @DubboReference
    private IProxyInfoRPC proxyInfoRPC;

    @DubboReference
    private IBrowserEnvRPC browserEnvRPC;

    @DubboReference
    private IDiscordAccountRPC discordAccountRPC;
}
