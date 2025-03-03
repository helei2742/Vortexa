package cn.com.helei.bot_father.service;


import cn.com.helei.job.service.BotJobService;
import cn.com.helei.rpc.*;
import cn.com.helei.rpc.bot.IBotAccountRPC;
import cn.com.helei.rpc.bot.IRewordInfoRPC;

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
