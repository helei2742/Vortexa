package cn.com.vortexa.control_server.service.impl;

import cn.com.vortexa.common.entity.ScriptNode;
import cn.com.vortexa.control.constant.RegistryState;
import cn.com.vortexa.common.dto.control.RegisteredScriptNode;
import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.control_server.service.IRegistryService;
import cn.com.vortexa.control.util.ControlServerUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;

import com.alibaba.fastjson.JSONObject;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author helei
 * @since 2025-03-17
 */
@Slf4j
public class FileRegistryService implements IRegistryService {

    private static final String FILE_NAME = "nameserver-registry.json";
    private final ConcurrentHashMap<String, RegisteredScriptNode> registryServiceMap = new ConcurrentHashMap<>();
    private final AtomicBoolean updated = new AtomicBoolean(false);
    @Setter
    private int saveIntervalSecond = 60;
    private boolean running = true;

    public FileRegistryService(ExecutorService executorService) {
        executorService.execute(() -> {
            while (running) {
                try {
                    saveRegistryInfo();

                    TimeUnit.SECONDS.sleep(saveIntervalSecond);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    log.warn("registry file auto store task interrupted", e);
                    running = false;
                }
            }
        });
    }

    @Override
    public RegistryState registryService(ServiceInstance serviceInstance) {
        String group = serviceInstance.getGroupId();
        String serviceId = serviceInstance.getServiceId();
        String clientId = serviceInstance.getInstanceId();

        if (StrUtil.isBlank(group) && StrUtil.isBlank(serviceId) && StrUtil.isBlank(clientId)) {
            return RegistryState.PARAM_ERROR;
        }

        try {
            String key = ControlServerUtil.generateServiceInstanceKey(group, serviceId, clientId);
            // 存内存
            registryServiceMap.put(key, new RegisteredScriptNode(ScriptNode.generateFromServiceInstance(serviceInstance)));
            updated.set(true);

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
        if (updated.compareAndSet(true, false)) {
            log.info("start save registry info - [{}]", registryServiceMap.keySet());

            Path path = Paths.get(ControlServerUtil.getStoreFileResourcePath(FILE_NAME));
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(path.toFile()))) {
                bw.write(JSONObject.toJSONString(registryServiceMap));
                bw.flush();
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            log.debug("no update cancel save registry info to file");
            return true;
        }
    }

    @Override
    public List<RegisteredScriptNode> queryServiceInstance(ServiceInstance query) {
        return queryServiceInstance(query.getGroupId(), query.getServiceId(), query.getInstanceId());
    }

    @Override
    public List<RegisteredScriptNode> queryServiceInstance(String target) {
        return registryServiceMap.keySet().stream().filter(key -> key.contains(target)).map(registryServiceMap::get).toList();
    }

    @Override
    public List<RegisteredScriptNode> queryServiceInstance(
            String groupId,
            String serviceId,
            String clientId
    ) {
        String keyPattern = ControlServerUtil.generateServiceInstanceKey(
                groupId,
                serviceId,
                clientId
        );

        return queryServiceInstance(keyPattern);
    }

    @Override
    public boolean existServiceInstance(String key) {
        return registryServiceMap.containsKey(key);
    }
}
