package cn.com.vortexa.nameserver.processor;


import cn.com.vortexa.nameserver.constant.LoadBalancePolicy;
import cn.com.vortexa.nameserver.dto.RegisteredService;

import java.util.List;

/**
 * @author helei
 * @since 2025-03-12
 */
public class ServiceDiscoverProcessor {
    public List<RegisteredService> discoverService(String group, String clientId, String serviceId, LoadBalancePolicy policy) {
        return null;
    }
}
