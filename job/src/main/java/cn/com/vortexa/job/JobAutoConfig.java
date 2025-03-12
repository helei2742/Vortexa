package cn.com.vortexa.job;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import javax.sql.DataSource;

@Configuration
@ComponentScan(basePackages = "cn.com.vortexa.job.service.impl")
public class JobAutoConfig {

    @Value("${spring.datasource.quartz.url}")
    private String quartzUrl;

    @Value("${spring.datasource.quartz.username}")
    private String username;

    @Value("${spring.datasource.quartz.password}")
    private String password;

    @Value("${spring.datasource.quartz.driver-class-name}")
    private String driverClassName;

    @Bean
    @Qualifier("quartzDataSource")
    public DataSource quartzDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(quartzUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(5);

        return new HikariDataSource(hikariConfig);
    }


    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean factoryBean = new SchedulerFactoryBean();
        factoryBean.setDataSource(quartzDataSource());
        factoryBean.setJobFactory(springBeanJobFactory());
        return factoryBean;
    }

    @Bean
    public JobFactory springBeanJobFactory() {
        return new SpringBeanJobFactory();
    }
}
