package cn.com.vortexa.script_node.service.impl;

import cn.com.vortexa.rpc.api.platform.IBotLaunchConfigRPC;
import cn.com.vortexa.rpc.api.platform.IRewordInfoRPC;
import cn.com.vortexa.script_node.service.BotApi;
import cn.com.vortexa.script_node.service.IBotAccountContextService;
import cn.com.vortexa.script_node.service.IRewordInfoService;
import cn.com.vortexa.db_layer.plugn.table_shard.strategy.ITableShardStrategy;
import cn.com.vortexa.rpc.api.platform.IAccountBaseInfoRPC;
import cn.com.vortexa.rpc.api.platform.IBotInfoRPC;
import cn.com.vortexa.job.service.BotJobService;
import cn.com.vortexa.rpc.api.platform.IBotInstanceRPC;
import cn.com.vortexa.rpc.api.platform.IBrowserEnvRPC;
import cn.com.vortexa.rpc.api.platform.IDiscordAccountRPC;
import cn.com.vortexa.rpc.api.platform.IProxyInfoRPC;
import cn.com.vortexa.rpc.api.platform.ITelegramAccountRPC;
import cn.com.vortexa.rpc.api.platform.ITwitterAccountRPC;
import cn.com.vortexa.control.anno.RPCReference;
import cn.com.vortexa.script_node.service.IWeb3WalletService;
import cn.com.vortexa.web3.service.IWeb3WalletOPTRPC;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Getter
@Component
public class DBBotApi implements BotApi {

    @Autowired
    private BotJobService botJobService;

    @Autowired
    private IBotAccountContextService botAccountService;

    @Autowired
    private IRewordInfoService rewordInfoService;


    @RPCReference
    private IBotInfoRPC botInfoRPC;

    @RPCReference
    private IBotLaunchConfigRPC botLaunchConfigRPC;

    @RPCReference
    private IBotInstanceRPC botInstanceRPC;

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

    @RPCReference
    private IWeb3WalletOPTRPC web3WalletRPC;

    @RPCReference
    private IRewordInfoRPC rewordInfoRPC;

    @Autowired
    private ITableShardStrategy tableShardStrategy;

    @Autowired
    private IWeb3WalletService web3WalletService;
}
