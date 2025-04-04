package cn.com.vortexa.script_node;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author helei
 * @since 2025-04-04
 */
@SpringBootApplication(exclude = ServletWebServerFactoryAutoConfiguration.class)
@ImportAutoConfiguration({ScriptNodeAutoConfig.class})
public class ScriptNodeApplication {

    public static ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        applicationContext = SpringApplication.run(ScriptNodeApplication.class, args);
    }
}
