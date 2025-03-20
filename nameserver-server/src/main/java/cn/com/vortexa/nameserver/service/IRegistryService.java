package cn.com.vortexa.nameserver.service;

import cn.com.vortexa.nameserver.constant.RegistryState;
import cn.com.vortexa.nameserver.dto.RegisteredService;
import cn.com.vortexa.nameserver.dto.ServiceInstance;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author helei
 * @since 2025-03-17
 */
public interface IRegistryService {

    /**
     * 注册服务
     *
     * @param serviceInstance serviceInstance
     * @return 注册状态
     */
    RegistryState registryService(ServiceInstance serviceInstance, Map<String, Object> props);

    /**
     * 保存注册信息
     *
     * @return Boolean
     */
    Boolean saveRegistryInfo() throws IOException;

    /**
     * 查找服务实例
     *
     * @param groupId groupId
     * @param serviceId serviceId
     * @param clientId clientId
     * @return List<ServiceInstance>
     */
    List<RegisteredService> queryServiceInstance(
            String groupId,
            String serviceId,
            String clientId
    );

    /**
     * 是否存在已注册的实例
     *
     * @param key key
     * @return  boolean
     */
    boolean existServiceInstance(String key);
}
