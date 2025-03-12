package cn.com.vortexa.db_layer;

import cn.com.vortexa.db_layer.config.MybatisConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@MapperScan(basePackages = "cn.com.vortexa.db_layer.mapper")
@Import(MybatisConfig.class)
public class DBLayerAutoConfig {

}
