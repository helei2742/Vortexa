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
import cn.com.vortexa.script_node.scriptagent.BotScriptAgent;
import cn.com.vortexa.script_node.service.BotApi;
import cn.com.vortexa.script_node.util.ScriptBotLauncher;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

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

    @Autowired
    private MybatisConfiguration mybatisConfiguration;

    @Autowired
    private GlobalConfig globalConfig;

    @Lazy
    @Autowired
    private BotApi botApi;

    @Autowired
    private BotScriptAgent botScriptAgent;

    @Bean("vortexaDataSource")
    public DataSource vortexaDataSource() {
        String scriptNodeName = scriptNodeConfiguration.getScriptNodeName();
        if (StrUtil.isBlank(scriptNodeName)) {
            throw new IllegalArgumentException("scriptNodeName is empty");
        }

        try {
            String path = tryCreateDBFile(scriptNodeName);
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl("jdbc:sqlite:/" + path);
            return dataSource;
        } catch (IOException e) {
            throw new RuntimeException("create Bot[%s]DB file error".formatted(scriptNodeName), e);
        }
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

    @Bean
    public ScriptBotLauncher scriptBotLauncher() {
        return ScriptBotLauncher.buildScriptBotLauncher(scriptNodeConfiguration, botApi, botScriptAgent);
    }

    /**
     * 尝试创建DB File
     *
     * @param scriptNodeName scriptNodeName
     * @return db file absolutePath
     * @throws IOException IOException
     */
    private static String tryCreateDBFile(String scriptNodeName) throws IOException {
        // 创建BotKey对应的数据库文件
        Path absolutePath = Paths.get(
                FileUtil.getDBResourceDir(),
                scriptNodeName,
                "script_node_" + scriptNodeName + ".db"
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
