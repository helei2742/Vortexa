package cn.com.vortexa.bot_father.config;


import cn.com.vortexa.bot_father.service.impl.BotAccountContextServiceImpl;
import cn.com.vortexa.bot_father.service.impl.RewordInfoServiceImpl;
import cn.com.vortexa.rpc.bot.IBotAccountRPC;
import cn.com.vortexa.rpc.bot.IRewordInfoRPC;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DubboConfig {

    @Autowired
    private AutoBotConfig botConfig;

    @Autowired
    private BotAccountContextServiceImpl botAccountContextService;

    @Autowired
    private RewordInfoServiceImpl rewordInfoService;

    @Bean
    public ServiceConfig<IBotAccountRPC> botAccountRPCServiceConfig() {
        ServiceConfig<IBotAccountRPC> serviceConfig = new ServiceConfig<>();

        serviceConfig.setInterface(IBotAccountRPC.class);
        serviceConfig.setRef(botAccountContextService);
        serviceConfig.setGroup(AutoBotConfig.BOT_NAME);
        serviceConfig.setVersion(botConfig.getBotKey());
        serviceConfig.export();

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setProtocol("nacos");
        registryConfig.setAddress("nacos://localhost:8848");
        registryConfig.setGroup(AutoBotConfig.BOT_NAME);
        serviceConfig.setRegistry(registryConfig);

        return serviceConfig;
    }

    @Bean
    public ServiceConfig<IRewordInfoRPC> botRewordInfoRPCServiceConfig() {
        ServiceConfig<IRewordInfoRPC> serviceConfig = new ServiceConfig<>();

        serviceConfig.setInterface(IRewordInfoRPC.class);
        serviceConfig.setRef(rewordInfoService);
        serviceConfig.setGroup(AutoBotConfig.BOT_NAME);
        serviceConfig.setVersion(botConfig.getBotKey());
        serviceConfig.export();
        return serviceConfig;
    }
}
