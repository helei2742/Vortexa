package cn.com.vortexa.script_node;

import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.typehandler.JsonTypeHandler;
import cn.com.vortexa.common.util.typehandler.LocalDateTimeTypeHandler;
import cn.com.vortexa.common.util.typehandler.MapTextTypeHandler;
import cn.com.vortexa.db_layer.DBLayerAutoConfig;
import cn.com.vortexa.db_layer.plugn.table_shard.TableShardInterceptor;
import cn.com.vortexa.db_layer.plugn.table_shard.strategy.BotIdBasedTableShardStrategy;
import cn.com.vortexa.db_layer.plugn.table_shard.strategy.ITableShardStrategy;
import cn.com.vortexa.job.JobAutoConfig;
import cn.com.vortexa.script_node.config.ScriptNodeConfiguration;
import cn.hutool.core.util.StrUtil;
import com.zaxxer.hikari.HikariDataSource;
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

@Configuration
@ImportAutoConfiguration({DBLayerAutoConfig.class, JobAutoConfig.class})
@ComponentScan({"cn.com.vortexa.script_node.service.impl", "cn.com.vortexa.script_node.config"})
@MapperScan(basePackages = "cn.com.vortexa.script_node.mapper")
public class ScriptNodeAutoConfig {

    @Autowired
    private JsonTypeHandler jsonTypeHandler;

    @Autowired
    private MapTextTypeHandler mapTextTypeHandler;

    @Autowired
    private LocalDateTimeTypeHandler localDateTimeTypeHandler;

    @Autowired
    private ScriptNodeConfiguration scriptNodeConfiguration;

    @Bean("vortexaDataSource")
    public DataSource vortexaDataSource() {
        String botGroup = scriptNodeConfiguration.getBotGroup();
        if (StrUtil.isBlank(botGroup)) {
            throw new IllegalArgumentException("botGroup is empty");
        }

        try {
            String path = tryCreateDBFile(botGroup);
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl("jdbc:sqlite:/" + path);
            return dataSource;
        } catch (IOException e) {
            throw new RuntimeException("create Bot[%s]DB file error".formatted(botGroup), e);
        }
    }


    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();

        factoryBean.setDataSource(vortexaDataSource());
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:/mapper/*.xml"));

        factoryBean.addTypeHandlers(jsonTypeHandler, mapTextTypeHandler, localDateTimeTypeHandler);
        factoryBean.addPlugins(tableShardInterceptor());

        return factoryBean.getObject();
    }

    // 配置事务管理器
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(vortexaDataSource());
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
     * @param scriptNodeGroup scriptNodeGroup
     * @return db file absolutePath
     * @throws IOException IOException
     */
    private static String tryCreateDBFile(String scriptNodeGroup) throws IOException {
        // 创建BotKey对应的数据库文件
        Path absolutePath = Paths.get(
                FileUtil.getBotAppConfigPath(),
                scriptNodeGroup,
                "script_node_" + scriptNodeGroup + ".db"
        );

        if (Files.notExists(absolutePath)) {
            if (Files.notExists(absolutePath.getParent())) {
                Files.createDirectories(absolutePath.getParent());
            }
            Files.createFile(absolutePath);
        }

        return absolutePath.toString();
    }
}
