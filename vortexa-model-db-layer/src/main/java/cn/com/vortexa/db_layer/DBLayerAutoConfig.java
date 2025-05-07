package cn.com.vortexa.db_layer;

import cn.com.vortexa.common.util.typehandler.JsonTypeHandler;
import cn.com.vortexa.common.util.typehandler.LocalDateTimeTypeHandler;
import cn.com.vortexa.common.util.typehandler.MapTextTypeHandler;
import cn.com.vortexa.db_layer.config.MyMetaObjectHandler;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@Import(MyMetaObjectHandler.class)
public class DBLayerAutoConfig {
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    @Qualifier("vortexaDataSource")
    @ConditionalOnMissingBean(name = "vortexaDataSource")
    public DataSource vortexaDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(5);

        return new HikariDataSource(hikariConfig);
    }


    @Bean
    @ConditionalOnMissingBean(SqlSessionFactory.class)
    public SqlSessionFactory sqlSessionFactory(@Qualifier("vortexaDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:/mapper/*.xml"));
        factoryBean.setTypeHandlers(
                jsonTypeHandler(), mapTextTypeHandler()
        );
        factoryBean.setTypeAliasesPackage("cn.com.vortexa.entity");
        factoryBean.setConfiguration(mybatisConfiguration());
        factoryBean.setGlobalConfig(globalConfig());
        return factoryBean.getObject();
    }

    @Bean
    @ConditionalOnMissingBean
    public MybatisConfiguration mybatisConfiguration() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(StdOutImpl.class);
        return configuration;
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalConfig globalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setLogicDeleteField("valid");
        dbConfig.setLogicDeleteValue("0");
        dbConfig.setLogicNotDeleteValue("1");
        globalConfig.setDbConfig(dbConfig);
        globalConfig.setBanner(false);
        globalConfig.setMetaObjectHandler(metaObjectHandler());
        return globalConfig;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MyMetaObjectHandler();
    }

    // 配置事务管理器
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(vortexaDataSource());
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonTypeHandler jsonTypeHandler() {
        return new JsonTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public MapTextTypeHandler mapTextTypeHandler() {
        return new MapTextTypeHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalDateTimeTypeHandler localDateTimeTypeHandler() {
        return new LocalDateTimeTypeHandler();
    }
}
