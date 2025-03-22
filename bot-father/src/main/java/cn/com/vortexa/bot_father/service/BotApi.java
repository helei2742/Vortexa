package cn.com.vortexa.bot_father.service;


import cn.com.vortexa.job.service.BotJobService;
import cn.com.vortexa.rpc.api.platform.IAccountBaseInfoRPC;
import cn.com.vortexa.rpc.api.platform.IBotInfoRPC;
import cn.com.vortexa.rpc.api.platform.IBrowserEnvRPC;
import cn.com.vortexa.rpc.api.platform.IDiscordAccountRPC;
import cn.com.vortexa.rpc.api.platform.IProxyInfoRPC;
import cn.com.vortexa.rpc.api.platform.ITelegramAccountRPC;
import cn.com.vortexa.rpc.api.platform.ITwitterAccountRPC;

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
