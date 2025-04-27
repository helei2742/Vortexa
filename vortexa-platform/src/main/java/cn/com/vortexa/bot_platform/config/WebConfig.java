package cn.com.vortexa.bot_platform.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;

import cn.com.vortexa.common.util.typehandler.JsonTypeHandler;
import cn.com.vortexa.common.util.typehandler.LocalDateTimeTypeHandler;
import cn.com.vortexa.common.util.typehandler.MapTextTypeHandler;
import cn.com.vortexa.db_layer.plugn.table_shard.TableShardInterceptor;
import cn.com.vortexa.db_layer.plugn.table_shard.strategy.BotIdBasedTableShardStrategy;
import cn.com.vortexa.db_layer.plugn.table_shard.strategy.ITableShardStrategy;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.sql.DataSource;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JsonTypeHandler jsonTypeHandler;

    @Autowired
    private MapTextTypeHandler mapTextTypeHandler;

    @Autowired
    private LocalDateTimeTypeHandler localDateTimeTypeHandler;
    @Autowired
    private MybatisConfiguration mybatisConfiguration;

    @Autowired
    private GlobalConfig globalConfig;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5181", "http://localhost:5180",
                        "http://localhost:9531")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter(){
        return new ServerEndpointExporter();
    }


    @Bean
    public SqlSessionFactory sqlSessionFactory(@Qualifier("vortexaDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:/mapper/*.xml"));
        factoryBean.setTypeHandlers(
                jsonTypeHandler, mapTextTypeHandler, localDateTimeTypeHandler
        );
        factoryBean.setTypeAliasesPackage("cn.com.vortexa.entity");
        factoryBean.setConfiguration(mybatisConfiguration);
        factoryBean.setGlobalConfig(globalConfig);
        // 分表插件， 不同bot账户用不同的表
        factoryBean.setPlugins(tableShardInterceptor());
        return factoryBean.getObject();
    }


    @Bean
    public ITableShardStrategy tableShardStrategy() {
        return new BotIdBasedTableShardStrategy();
    }

    @Bean
    public Interceptor tableShardInterceptor() {
        return new TableShardInterceptor(tableShardStrategy());
    }
}
