package cn.com.vortexa.bot_platform;

import cn.com.vortexa.common.util.BannerUtil;
import cn.com.vortexa.db_layer.DBLayerAutoConfig;
import cn.com.vortexa.job.JobAutoConfig;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "cn.com.vortexa.bot_platform.mapper")
@ImportAutoConfiguration(classes = {DBLayerAutoConfig.class, JobAutoConfig.class})
public class BotPlatformApplication {
    public static void main(String[] args) {
        BannerUtil.printBanner("");
        SpringApplication.run(BotPlatformApplication.class, args);
    }
}
