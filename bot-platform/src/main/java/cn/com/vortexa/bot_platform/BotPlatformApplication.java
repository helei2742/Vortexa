package cn.com.vortexa.bot_platform;

import cn.com.vortexa.db_layer.DBLayerAutoConfig;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo(scanBasePackages = {"cn.com.vortexa.bot_platform.service.impl"})
@ImportAutoConfiguration(classes = DBLayerAutoConfig.class)
public class BotPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotPlatformApplication.class, args);
    }
}
