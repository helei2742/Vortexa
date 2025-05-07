package cn.com.vortexa.script_node.config;

import cn.com.vortexa.common.constants.HttpMethod;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.YamlConfigLoadUtil;
import cn.com.vortexa.common.util.http.RestApiClientFactory;
import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class RemoteConfigLoader implements EnvironmentPostProcessor {


    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            String appConfigPath = FileUtil.USER_DIR + File.separator + "config" + File.separator + "application.yaml";
            Map<String, Object> appConfigMap = null;
            Yaml localYaml = new Yaml();
            if (Files.notExists(Paths.get(appConfigPath))) {
                try (InputStream inputStream = RemoteConfigLoader.class.getClassLoader().getResourceAsStream("application.yaml")) {
                    appConfigMap = localYaml.load(inputStream);
                }
            } else {
                try (InputStream inputStream = Files.newInputStream(Paths.get(appConfigPath))) {
                    appConfigMap = localYaml.load(inputStream);
                }
            }
            ScriptNodeConfiguration.RAW_CONFIG = appConfigMap;
            ScriptNodeConfiguration preLoadSNConfig = getLocalScriptNodeConfiguration(appConfigMap);

            if (preLoadSNConfig != null) {
                String configUrl = preLoadSNConfig.buildRemoteConfigRestApi();
                String scriptNodeName = preLoadSNConfig.getScriptNodeName();
                if (configUrl == null || scriptNodeName == null) {
                    System.err.println("no remote.config-url or script node name，skip load remote config");
                    return;
                }
                // 拉取 YAML 内容
                String yamlContent = fetchRemoteConfig(configUrl, scriptNodeName);
                Yaml yaml = new Yaml();

                // 使用 SnakeYAML 2.2 解析成 Map
                Map<String, Object> yamlMap = yaml.load(yamlContent);

                // 转成 PropertySource，放在最前面
                MapPropertySource propertySource = new MapPropertySource(
                        "remote-config", YamlConfigLoadUtil.flattenMap(yamlMap)
                );
                environment.getPropertySources().addFirst(propertySource);
            } else {
                System.err.println("no script node configuration");
            }
        } catch (Exception e) {
            System.err.println("remote config load error，use local config: " + e.getMessage());
        }
    }

    private ScriptNodeConfiguration getLocalScriptNodeConfiguration(Map<String, Object> appConfigMap) {
        ScriptNodeConfiguration vortexa = YamlConfigLoadUtil.load(new Yaml(), appConfigMap, List.of("vortexa", "script-node"), ScriptNodeConfiguration.class);
        return vortexa == null
                ? YamlConfigLoadUtil.load(new Yaml(), appConfigMap, List.of("vortexa", "scriptNode"), ScriptNodeConfiguration.class)
                : vortexa;
    }

    /**
     * 拉取远程配置
     *
     * @param configUrl      configUrl
     * @param scriptNodeName scriptNodeName
     * @return String
     * @throws IOException IOException
     */
    private String fetchRemoteConfig(String configUrl, String scriptNodeName) throws IOException {
        try {
            String response = RestApiClientFactory.getClient().request(
                    configUrl + "/" + scriptNodeName,
                    HttpMethod.POST,
                    new HashMap<>(),
                    null,
                    new JSONObject()
            ).get();
            Result result = JSONObject.parseObject(response, Result.class);
            if (result.getSuccess()) {
                return String.valueOf(result.getData());
            } else {
                throw new IOException(result.getErrorMsg());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("fetch remote config error", e);
        }
    }
}
