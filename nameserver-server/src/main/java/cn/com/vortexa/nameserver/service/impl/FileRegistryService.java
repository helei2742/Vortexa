package cn.com.vortexa.nameserver.service.impl;


import cn.com.vortexa.nameserver.constant.RegistryState;
import cn.com.vortexa.nameserver.dto.ServiceInstance;
import cn.com.vortexa.nameserver.service.IRegistryService;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * @author helei
 * @since 2025-03-17
 */
@Slf4j
public class FileRegistryService implements IRegistryService {

    private final static String STORE_FILE_RESOURCE_PATH = System.getProperty("user.dir") +
            File.separator + "nameserver" + File.separator + "nameserver-registry.dat";

    private final ConcurrentHashMap<String, ServiceInstance> registryServiceMap = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<RegistryState> registryService(ServiceInstance serviceInstance) {
        String group = serviceInstance.getGroup();
        String serviceId = serviceInstance.getServiceId();
        String clientId = serviceInstance.getClientId();

        if (StrUtil.isNotBlank(group) && StrUtil.isNotBlank(serviceId) && StrUtil.isNotBlank(clientId)) {
            return CompletableFuture.completedFuture(RegistryState.PARAM_ERROR);

        }

        return CompletableFuture.supplyAsync(() -> {
            String key = generateServiceInstanceKey(group, serviceId, clientId);

            // 存内存
            registryServiceMap.put(key, serviceInstance);

            // 存磁盘
            Boolean b = null;
            try {
                b = saveRegistryInfo().get();
                return BooleanUtil.isTrue(b) ? RegistryState.OK : RegistryState.STORE_ERROR;
            } catch (InterruptedException | ExecutionException e) {
                log.error("store registry error", e);
                return RegistryState.STORE_ERROR;
            }
        }).exceptionally(throwable -> {
            log.error("registry config error", throwable);
            return RegistryState.UNKNOWN_ERROR;
        });
    }

    @Override
    public CompletableFuture<Boolean> saveRegistryInfo() {
        return CompletableFuture.supplyAsync(()->{
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(STORE_FILE_RESOURCE_PATH))) {
                bw.write(JSONObject.toJSONString(registryServiceMap));
                bw.flush();
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String generateServiceInstanceKey(String group, String serviceId, String clientId) {
        return group + "<>" + serviceId + "<>" + clientId;
    }
}
