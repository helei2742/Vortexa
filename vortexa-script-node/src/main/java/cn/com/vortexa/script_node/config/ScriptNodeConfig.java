package cn.com.vortexa.script_node.config;


import cn.com.vortexa.common.dto.control.ServiceInstance;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author helei
 * @since 2025-04-04
 */
@Data
@Component
@ConfigurationProperties(prefix = "vortexa.script-node")
public class ScriptNodeConfig {

    private String nodeId;

    private ServiceInstance serviceInstance;


}
