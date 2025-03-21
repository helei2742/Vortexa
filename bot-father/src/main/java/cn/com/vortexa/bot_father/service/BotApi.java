package cn.com.vortexa.bot_father.service;


import cn.com.vortexa.job.service.BotJobService;
import cn.com.vortexa.rpc.IAccountBaseInfoRPC;
import cn.com.vortexa.rpc.IBotInfoRPC;
import cn.com.vortexa.rpc.IBrowserEnvRPC;
import cn.com.vortexa.rpc.IDiscordAccountRPC;
import cn.com.vortexa.rpc.IProxyInfoRPC;
import cn.com.vortexa.rpc.ITelegramAccountRPC;
import cn.com.vortexa.rpc.ITwitterAccountRPC;

public interface BotApi {

    IBotInfoRPC getBotInfoRPC();
    IAccountBaseInfoRPC getAccountBaseInfoRPC();
    IBrowserEnvRPC getBrowserEnvRPC();
    IDiscordAccountRPC getDiscordAccountRPC();
    IProxyInfoRPC getProxyInfoRPC();
    ITwitterAccountRPC getTwitterAccountRPC();
    ITelegramAccountRPC getTelegramAccountRPC();


    IRewordInfoService getRewordInfoService();
    IBotAccountContextService getBotAccountService();
    BotJobService getBotJobService();
}
