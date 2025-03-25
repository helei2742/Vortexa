package cn.com.vortexa.control.service;

import cn.com.vortexa.control.dto.ConnectEntry;
import io.netty.channel.Channel;

import java.util.List;

/**
 * @author helei
 * @since 2025/3/18 15:02
 */
public interface IConnectionService {

    /**
     * 添加服务连接
     *
     * @param key     key
     * @param channel channel
     */
    void addServiceChannel(String key, Channel channel);

    /**
     * 添加服务连接
     *
     * @param group      group
     * @param serviceId  serviceId
     * @param instanceId instanceId
     * @param channel    channel
     */
    void addServiceChannel(String group, String serviceId, String instanceId, Channel channel);

    /**
     * 关闭服务连接
     *
     * @param channel channel
     * @param key     key
     */
    void closeServiceChannel(Channel channel, String key);

    /**
     * 获取连接
     *
     * @param key key
     * @return ConnectEntry
     */
    ConnectEntry getServiceInstanceChannel(String key);

    /**
     * 刷新服务实例连接
     *
     * @param key     key
     * @param channel channel
     */
    void freshServiceInstanceConnection(String key, Channel channel);

    List<String> queryOnlineInstanceKey();
}
