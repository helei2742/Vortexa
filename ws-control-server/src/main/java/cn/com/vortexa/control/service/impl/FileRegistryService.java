package cn.com.vortexa.control.service.impl;

import cn.com.vortexa.control.constant.RegistryState;
import cn.com.vortexa.common.dto.control.RegisteredService;
import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.control.service.IRegistryService;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author helei
 * @since 2025-03-17
 */
@Slf4j
public class FileRegistryService implements IRegistryService {

    private static final String FILE_NAME = "nameserver-registry.dat";
    private final ConcurrentHashMap<String, RegisteredService> registryServiceMap = new ConcurrentHashMap<>();
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
    public RegistryState registryService(ServiceInstance serviceInstance, Map<Object, Object> props) {
        String group = serviceInstance.getGroup();
        String serviceId = serviceInstance.getServiceId();
        String clientId = serviceInstance.getInstanceId();

        if (StrUtil.isBlank(group) && StrUtil.isBlank(serviceId) && StrUtil.isBlank(clientId)) {
            return RegistryState.PARAM_ERROR;
        }

        try {
            String key = ControlServerUtil.generateServiceInstanceKey(group, serviceId, clientId);

            // 存内存
            registryServiceMap.put(key, new RegisteredService(serviceInstance, props));
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
    public List<RegisteredService> queryServiceInstance(String keyPattern) {
        Pattern compile = Pattern.compile(keyPattern);

        return registryServiceMap.keySet().stream().filter(key -> {
            Matcher matcher = compile.matcher(key);
            return matcher.find();
        }).map(registryServiceMap::get).toList();
    }

    @Override
    public List<RegisteredService> queryServiceInstance(
            String groupId,
            String serviceId,
            String clientId
    ) {
        String keyPattern = ControlServerUtil.generateServiceInstanceKey(
                StrUtil.isBlank(groupId) ? "*" : groupId,
                StrUtil.isBlank(serviceId) ? "*" : serviceId,
                StrUtil.isBlank(clientId) ? "*" : clientId
        );

        return queryServiceInstance(keyPattern);
    }

    @Override
    public boolean existServiceInstance(String key) {
        return registryServiceMap.containsKey(key);
    }
}
