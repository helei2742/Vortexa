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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class RemoteConfigLoader implements EnvironmentPostProcessor {


    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            final String appConfigPath = FileUtil.USER_DIR + File.separator + "config" + File.separator + "application.yaml";
            ScriptNodeConfiguration preLoadSNConfig = YamlConfigLoadUtil.load(new File(appConfigPath),
                    List.of("vortexa", "scriptNode"), ScriptNodeConfiguration.class);

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
        } catch (Exception e) {
            System.err.println("remote config load error，use local config: " + e.getMessage());
        }
    }

    private String fetchRemoteConfig(String configUrl, String scriptNodeName) throws IOException {
        try {
            String response = RestApiClientFactory.getClient().request(
                    configUrl + "/script-node/remote-config" + scriptNodeName,
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
