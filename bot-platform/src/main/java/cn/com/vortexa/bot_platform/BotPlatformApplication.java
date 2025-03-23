package cn.com.vortexa.bot_platform;

import cn.com.vortexa.db_layer.DBLayerAutoConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ImportAutoConfiguration(classes = DBLayerAutoConfig.class)
public class BotPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(BotPlatformApplication.class, args);
    }
}
