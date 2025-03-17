package cn.com.vortexa.nameserver.service;


import cn.com.vortexa.nameserver.constant.RegistryState;
import cn.com.vortexa.nameserver.dto.ServiceInstance;

import java.util.concurrent.CompletableFuture;

/**
 * @author helei
 * @since 2025-03-17
 */
public interface IRegistryService {
    CompletableFuture<RegistryState> registryService(ServiceInstance serviceInstance);

    CompletableFuture<Boolean> saveRegistryInfo();
}
