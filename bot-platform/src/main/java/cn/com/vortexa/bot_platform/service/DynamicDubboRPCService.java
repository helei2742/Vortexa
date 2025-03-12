package cn.com.vortexa.bot_platform.service;

import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static cn.com.vortexa.common.constants.DubboConstants.DEFAULT_VERSION;

/**
 * 动态DubboRPC调用service
 *
 * @param <T> 具体的RPC API
 */
public abstract class DynamicDubboRPCService<T> {

    private final Map<String, ReferenceConfig<T>> referenceCache = new ConcurrentHashMap<>();

    private final Map<String, RegistryConfig> registryConfigCache = new ConcurrentHashMap<>();

    private final Class<T> tClass;

    public DynamicDubboRPCService(Class<T> tClass) {
        this.tClass = tClass;
    }


    /**
     * 根据address、group、version获取RPC实例
     *
     * @param group   group
     * @param version version
     * @return T
     */
    public T getRPCInstance(String group, String version) {
        return getRPCInstance(null, group, version);
    }

    /**
     * 根据address、group、version获取RPC实例
     *
     * @param address address
     * @param group   group
     * @param version version
     * @return T
     */
    public T getRPCInstance(String address, String group, String version) {
        ReferenceConfig<T> reference = getReferenceConfig(address, group, version);
        if (null != reference) {
            return reference.get();
        }
        return null;
    }

    private ReferenceConfig<T> getReferenceConfig(String address, String group, String version) {

        return referenceCache.compute(group, (k, v) -> {
            if (v == null) {
                v = new ReferenceConfig<>();
                v.setRegistry(getRegistryConfig(address, group, version));
                v.setInterface(tClass);
                v.setGroup(group);
                v.setVersion(DEFAULT_VERSION);
            }
            return v;
        });
    }

    private RegistryConfig getRegistryConfig(String address, String group, String version) {
        String key = address + "-" + group + "-" + version;
        return registryConfigCache.compute(key, (k, v) -> {
            if (v == null) {
                v = new RegistryConfig();
                v.setAddress(address);
                registryConfigCache.put(key, v);
            }
            return v;
        });
    }
}
