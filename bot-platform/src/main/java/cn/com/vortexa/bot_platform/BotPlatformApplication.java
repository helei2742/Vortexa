package cn.com.vortexa.bot_platform;

import cn.com.vortexa.db_layer.DBLayerAutoConfig;
import cn.com.vortexa.rpc.anno.RPCScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@RPCScan(basePackage = "cn.com.vortexa.rpc")
@SpringBootApplication
@ImportAutoConfiguration(classes = DBLayerAutoConfig.class)
public class BotPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(BotPlatformApplication.class, args);
    }
}
