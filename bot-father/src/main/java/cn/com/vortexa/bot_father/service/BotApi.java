package cn.com.vortexa.bot_father.service;


import cn.com.vortexa.job.service.BotJobService;
import cn.com.vortexa.rpc.*;
import cn.com.vortexa.rpc.bot.IBotAccountRPC;
import cn.com.vortexa.rpc.bot.IRewordInfoRPC;

public interface BotApi {

    IProjectInfoRPC getProjectInfoRPC();

    IBotInfoRPC getBotInfoRPC();

    IAccountBaseInfoRPC getAccountBaseInfoRPC();

    IBrowserEnvRPC getBrowserEnvRPC();

    IDiscordAccountRPC getDiscordAccountRPC();

    IProxyInfoRPC getProxyInfoRPC();

    IRewordInfoRPC getRewordInfoRPC();

    ITwitterAccountRPC getTwitterAccountRPC();

    ITelegramAccountRPC getTelegramAccountRPC();


    IBotAccountRPC getBotAccountRPC();

    BotJobService getBotJobService();
}
