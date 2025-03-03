package cn.com.helei.bot_father;

import cn.com.helei.bot_father.config.AutoBotConfig;
import cn.com.helei.bot_father.config.DubboConfig;
import cn.com.helei.common.util.FileUtil;
import cn.com.helei.common.util.typehandler.JsonTypeHandler;
import cn.com.helei.common.util.typehandler.LocalDateTimeTypeHandler;
import cn.com.helei.common.util.typehandler.MapTextTypeHandler;
import cn.com.helei.db_layer.DBLayerAutoConfig;
import cn.com.helei.db_layer.plugn.table_shard.TableShardInterceptor;
import cn.com.helei.db_layer.plugn.table_shard.strategy.BotIdBasedTableShardStrategy;
import cn.com.helei.db_layer.plugn.table_shard.strategy.ITableShardStrategy;
import cn.com.helei.job.JobAutoConfig;
import cn.hutool.core.util.StrUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@EnableDubbo
@Configuration
@ImportAutoConfiguration({DBLayerAutoConfig.class, DubboConfig.class, JobAutoConfig.class})
@ComponentScan({"cn.com.helei.bot_father.service.impl", "cn.com.helei.bot_father.config"})
@MapperScan(basePackages = "cn.com.helei.bot_father.mapper")
public class BotFatherAutoConfig {

    @Autowired
    private JsonTypeHandler jsonTypeHandler;

    @Autowired
    private MapTextTypeHandler mapTextTypeHandler;

    @Autowired
    private LocalDateTimeTypeHandler localDateTimeTypeHandler;

    @Autowired
    private AutoBotConfig botConfig;


    @Bean("jobDataSource")
    public DataSource jobDataSource() {
        String botKey = botConfig.getBotKey();
        if (StrUtil.isBlank(botKey)) {
            throw new IllegalArgumentException("botKey is empty");
        }

        try {
            String path = tryCreateDBFile(botKey);
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl("jdbc:sqlite:/" + path);
            return dataSource;
        } catch (IOException e) {
            throw new RuntimeException("create Bot[%s]DB file error".formatted(botKey), e);
        }
    }


    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();

        factoryBean.setDataSource(jobDataSource());
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:/mapper/*.xml"));

        factoryBean.addTypeHandlers(jsonTypeHandler, mapTextTypeHandler, localDateTimeTypeHandler);
        factoryBean.addPlugins(tableShardInterceptor());

        return factoryBean.getObject();
    }

    // 配置事务管理器
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(jobDataSource());
    }

    @Bean
    public ITableShardStrategy tableShardStrategy() {
        return new BotIdBasedTableShardStrategy();
    }

    @Bean
    public Interceptor tableShardInterceptor() {
        return new TableShardInterceptor(tableShardStrategy());
    }


    /**
     * 尝试创建DB File
     *
     * @param botKey botKey
     * @return db file absolutePath
     * @throws IOException IOException
     */
    private static String tryCreateDBFile(String botKey) throws IOException {
        // 创建BotKey对应的数据库文件
        Path absolutePath = Paths.get(FileUtil.getBotAppConfigPath(), botKey, "bot_" + botKey + ".db");

        if (Files.notExists(absolutePath)) {
            if (Files.notExists(absolutePath.getParent())) {
                Files.createDirectories(absolutePath.getParent());
            }
            Files.createFile(absolutePath);
        }

        return absolutePath.toString();
    }
}
