package cn.com.vortexa.script_node.config;

import cn.com.vortexa.common.constants.HttpMethod;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.util.YamlConfigLoadUtil;
import cn.com.vortexa.common.util.http.RestApiClientFactory;
import cn.com.vortexa.script_node.constants.ScriptNodeConstants;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class RemoteConfigLoader implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            String configUrl = System.getProperty(ScriptNodeConstants.REMOTE_CONFIG_URL_KEY);
            String nodeId = System.getProperty(ScriptNodeConstants.NODE_ID_KEH);
            if (configUrl == null || nodeId == null) {
                System.err.println("no remote.config-url pr node-id，skip load remote config");
                return;
            }

            // 拉取 YAML 内容
            String yamlContent = fetchRemoteConfig(configUrl, nodeId);
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

    private String fetchRemoteConfig(String configUrl, String nodeId) throws IOException {
        try {
            String response = RestApiClientFactory.getClient().request(
                    configUrl + "/" + nodeId,
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
