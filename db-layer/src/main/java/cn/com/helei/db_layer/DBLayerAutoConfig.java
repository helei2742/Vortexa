package cn.com.helei.db_layer;

import cn.com.helei.db_layer.config.MybatisConfig;
import cn.com.helei.db_layer.plugn.table_shard.strategy.BotIdBasedTableShardStrategy;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@MapperScan(basePackages = "cn.com.helei.db_layer.mapper")
@Import(MybatisConfig.class)
public class DBLayerAutoConfig {

}
