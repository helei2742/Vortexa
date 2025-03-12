package cn.com.vortexa.nameserver.config;


import cn.com.vortexa.nameserver.dto.ServiceInstance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NameserverServerConfig {

    private static final String RESOURCE_PATH = "nameserver-config.yaml";

    private static final List<String> PREFIX_PATH = List.of("cn", "com", "helei", "nameserver");

    public static final NameserverServerConfig DEFAULT;

    static {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = NameserverServerConfig.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            Map<String, Object> yamlData = yaml.load(inputStream);
            for (String key : PREFIX_PATH) {
                yamlData = (Map<String, Object>) yamlData.get(key);
                Map<String, Object> target = new HashMap<>();
                for (Map.Entry<String, Object> entry : yamlData.entrySet()) {
                    target.put(toCamelCase(entry.getKey()), entry.getValue());
                }
                yamlData = target;
            }

            DEFAULT = yaml.loadAs(yaml.dump(yamlData), NameserverServerConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("nameserver config[%s] load error", RESOURCE_PATH), e);
        }
    }

    /**
     * netty nio 线程数
     */
    private Integer nioThreadCount = 1;

    /**
     * 服务地址
     */
    private ServiceInstance serviceInstance;

    /**
     * 下线时间
     */
    private Integer serviceOfflineTtl;

    private static String toCamelCase(String name) {
        String[] parts = name.split("-");
        StringBuilder camelCase = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            camelCase.append(parts[i].substring(0, 1).toUpperCase()).append(parts[i].substring(1));
        }
        return camelCase.toString();
    }
}
