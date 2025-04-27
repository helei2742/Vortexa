package cn.com.vortexa.bot_platform.script_control.config;


import cn.com.vortexa.rpc.api.platform.*;
import cn.com.vortexa.control.dto.RPCServiceInfo;
import cn.com.vortexa.web3.service.IWeb3WalletOPTRPC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author helei
 * @since 2025-03-22
 */
@Configuration
public class ExposeRPCMethodConfig {

    @Bean
    @ConditionalOnBean(IAccountBaseInfoRPC.class)
    public RPCServiceInfo<IAccountBaseInfoRPC> accountBaseInfoRPCServiceInfo(
            IAccountBaseInfoRPC accountBaseInfoRPC
    ) {
        return RPCServiceInfo
                .<IAccountBaseInfoRPC>builder()
                .interfaces(IAccountBaseInfoRPC.class)
                .ref(accountBaseInfoRPC)
                .build();
    }

    @Bean
    @ConditionalOnBean(IBotInfoRPC.class)
    public RPCServiceInfo<IBotInfoRPC> botInfoRPCRPCServiceInfo(
            IBotInfoRPC botInfoRPC
    ) {
        return RPCServiceInfo
                .<IBotInfoRPC>builder()
                .interfaces(IBotInfoRPC.class)
                .ref(botInfoRPC)
                .build();
    }

    @Bean
    @ConditionalOnBean(IBotInstanceRPC.class)
    public RPCServiceInfo<IBotInstanceRPC> botInstanceRPCRPCServiceInfo(
            IBotInstanceRPC botInstanceRPC
    ) {
        return RPCServiceInfo
                .<IBotInstanceRPC>builder()
                .interfaces(IBotInstanceRPC.class)
                .ref(botInstanceRPC)
                .build();
    }

    @Bean
    @ConditionalOnBean(IBrowserEnvRPC.class)
    public RPCServiceInfo<IBrowserEnvRPC> browserEnvRPCRPCServiceInfo(
            IBrowserEnvRPC browserEnvRPC
    ) {
        return RPCServiceInfo
                .<IBrowserEnvRPC>builder()
                .interfaces(IBrowserEnvRPC.class)
                .ref(browserEnvRPC)
                .build();
    }

    @Bean
    @ConditionalOnBean(IDiscordAccountRPC.class)
    public RPCServiceInfo<IDiscordAccountRPC> discordAccountRPCRPCServiceInfo(
            IDiscordAccountRPC discordAccountRPC
    ) {
        return RPCServiceInfo
                .<IDiscordAccountRPC>builder()
                .interfaces(IDiscordAccountRPC.class)
                .ref(discordAccountRPC)
                .build();
    }

    @Bean
    @ConditionalOnBean(IProxyInfoRPC.class)
    public RPCServiceInfo<IProxyInfoRPC> proxyInfoRPCRPCServiceInfo(
            IProxyInfoRPC ref
    ) {
        return RPCServiceInfo
                .<IProxyInfoRPC>builder()
                .interfaces(IProxyInfoRPC.class)
                .ref(ref)
                .build();
    }

    @Bean
    @ConditionalOnBean(ITelegramAccountRPC.class)
    public RPCServiceInfo<ITelegramAccountRPC> telegramAccountRPCRPCServiceInfo(
            ITelegramAccountRPC ref
    ) {
        return RPCServiceInfo
                .<ITelegramAccountRPC>builder()
                .interfaces(ITelegramAccountRPC.class)
                .ref(ref)
                .build();
    }

    @Bean
    @ConditionalOnBean(ITwitterAccountRPC.class)
    public RPCServiceInfo<ITwitterAccountRPC> twitterAccountRPCRPCServiceInfo(
            ITwitterAccountRPC ref
    ) {
        return RPCServiceInfo
                .<ITwitterAccountRPC>builder()
                .interfaces(ITwitterAccountRPC.class)
                .ref(ref)
                .build();
    }

    @Bean
    @ConditionalOnBean(IWeb3WalletOPTRPC.class)
    public RPCServiceInfo<IWeb3WalletOPTRPC> web3WalletRPCRPCServiceInfo(IWeb3WalletOPTRPC ref) {
        return RPCServiceInfo
                .<IWeb3WalletOPTRPC>builder()
                .interfaces(IWeb3WalletOPTRPC.class)
                .ref(ref)
                .build();
    }

    @Bean
    @ConditionalOnBean(IRewordInfoRPC.class)
    public RPCServiceInfo<IRewordInfoRPC> rewordInfoRPCRPCServiceInfo(IRewordInfoRPC ref) {
        return RPCServiceInfo
                .<IRewordInfoRPC>builder()
                .interfaces(IRewordInfoRPC.class)
                .ref(ref)
                .build();
    }
}
