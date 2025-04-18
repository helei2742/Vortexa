package cn.com.vortexa.script_node.service;


import cn.com.vortexa.db_layer.plugn.table_shard.strategy.ITableShardStrategy;
import cn.com.vortexa.job.service.BotJobService;
import cn.com.vortexa.rpc.api.bot.IScriptAgentRPC;
import cn.com.vortexa.rpc.api.platform.IAccountBaseInfoRPC;
import cn.com.vortexa.rpc.api.platform.IBotInfoRPC;
import cn.com.vortexa.rpc.api.platform.IBotInstanceRPC;
import cn.com.vortexa.rpc.api.platform.IBrowserEnvRPC;
import cn.com.vortexa.rpc.api.platform.IDiscordAccountRPC;
import cn.com.vortexa.rpc.api.platform.IProxyInfoRPC;
import cn.com.vortexa.rpc.api.platform.ITelegramAccountRPC;
import cn.com.vortexa.rpc.api.platform.ITwitterAccountRPC;

public interface BotApi {

    IBotInfoRPC getBotInfoRPC();

    IBotInstanceRPC getBotInstanceRPC();

    IAccountBaseInfoRPC getAccountBaseInfoRPC();

    IBrowserEnvRPC getBrowserEnvRPC();

    IDiscordAccountRPC getDiscordAccountRPC();

    IProxyInfoRPC getProxyInfoRPC();

    ITwitterAccountRPC getTwitterAccountRPC();

    ITelegramAccountRPC getTelegramAccountRPC();

    ITableShardStrategy getTableShardStrategy();

    IRewordInfoService getRewordInfoService();

    IBotAccountContextService getBotAccountService();

    BotJobService getBotJobService();

    IScriptAgentRPC getScriptAgentRPC();
}
