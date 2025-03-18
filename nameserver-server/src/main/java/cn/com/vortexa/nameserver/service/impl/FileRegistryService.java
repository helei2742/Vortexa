package cn.com.vortexa.nameserver.service.impl;

import cn.com.vortexa.nameserver.constant.RegistryState;
import cn.com.vortexa.nameserver.dto.RegisteredService;
import cn.com.vortexa.nameserver.dto.ServiceInstance;
import cn.com.vortexa.nameserver.service.IRegistryService;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
        import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author helei
 * @since 2025-03-17
 */
@Slf4j
public class FileRegistryService implements IRegistryService {

    private final static String STORE_FILE_RESOURCE_PATH = System.getProperty("user.dir") +
            File.separator + "nameserver" + File.separator + "nameserver-registry.dat";

    private final ConcurrentHashMap<String, RegisteredService> registryServiceMap = new ConcurrentHashMap<>();

    @Override
    public RegistryState registryService(ServiceInstance serviceInstance, Map<String, Object> props) {
        String group = serviceInstance.getGroup();
        String serviceId = serviceInstance.getServiceId();
        String clientId = serviceInstance.getClientId();

        if (StrUtil.isBlank(group) && StrUtil.isBlank(serviceId) && StrUtil.isBlank(clientId)) {
            return RegistryState.PARAM_ERROR;
        }

        try {
            String key = generateServiceInstanceKey(group, serviceId, clientId);

            // 存内存
            registryServiceMap.put(key, new RegisteredService(serviceInstance, props));

            // 存磁盘
            Boolean b = null;
            try {
                b = saveRegistryInfo();
                return BooleanUtil.isTrue(b) ? RegistryState.OK : RegistryState.STORE_ERROR;
            } catch (Exception e) {
                log.error("store registry error", e);
                return RegistryState.STORE_ERROR;
            }
        } catch (Exception e) {
            log.error("registry config error", e);
            return RegistryState.UNKNOWN_ERROR;
        }
    }

    @Override
    public Boolean saveRegistryInfo() throws IOException {
        Path path = Paths.get(STORE_FILE_RESOURCE_PATH);
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path.toFile()))) {
            bw.write(JSONObject.toJSONString(registryServiceMap));
            bw.flush();
            return true;
        }
    }

    @Override
    public List<RegisteredService> queryServiceInstance(
            String groupId,
            String serviceId,
            String clientId
    ) {
        String keyPattern = generateServiceInstanceKey(
                StrUtil.isBlank(groupId) ? "*" : groupId,
                StrUtil.isBlank(serviceId) ? "*" : serviceId,
                StrUtil.isBlank(clientId) ? "*" : clientId
        );
        Pattern compile = Pattern.compile(keyPattern);

        return registryServiceMap.keySet().stream().filter(key -> {
            Matcher matcher = compile.matcher(key);
            return matcher.find();
        }).map(registryServiceMap::get).toList();
    }

    private String generateServiceInstanceKey(String group, String serviceId, String clientId) {
        return group + "#%&%#" + serviceId + "#%&%#" + clientId;
    }
}
