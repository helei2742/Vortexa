package cn.com.vortexa.control_server.service;

import cn.com.vortexa.control.constant.RegistryState;
import cn.com.vortexa.common.dto.control.RegisteredScriptNode;
import cn.com.vortexa.common.dto.control.ServiceInstance;

import java.io.IOException;
import java.util.List;

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
    RegistryState registryService(ServiceInstance serviceInstance);

    /**
     * 保存注册信息
     *
     * @return Boolean
     */
    Boolean saveRegistryInfo() throws IOException, InterruptedException;

    /**
     * 查询服务实例
     *
     * @param query query
     * @return List<RegisteredService>
     */
    List<RegisteredScriptNode> queryServiceInstance(ServiceInstance query);

    /**
     * 查找服务实例
     *
     * @param key key
     * @return List<ServiceInstance>
     */
    List<RegisteredScriptNode> queryServiceInstance(
            String key
    );

    /**
     * 查找服务实例
     *
     * @param groupId groupId
     * @param serviceId serviceId
     * @param clientId clientId
     * @return List<ServiceInstance>
     */
    List<RegisteredScriptNode> queryServiceInstance(
            String groupId,
            String serviceId,
            String clientId
    );

    /**
     * 是否存在已注册的实例
     *
     * @param key key
     * @return boolean
     */
    boolean existServiceInstance(String key);
}
