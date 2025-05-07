package cn.com.vortexa.control_server.service.impl;

import cn.com.vortexa.control_server.dto.ConnectEntry;
import cn.com.vortexa.control_server.service.IConnectionService;
import cn.com.vortexa.common.util.ServerInstanceUtil;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.BooleanUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author helei
 * @since 2025/3/18 15:03
 */
@Slf4j
public class MemoryConnectionService implements IConnectionService {

    public static int OFF_LINE_SECONDS = 120;

    private final ConcurrentMap<String, ConnectEntry> connectionMap = new ConcurrentHashMap<>();

    @Override
    public void addServiceChannel(String key, Channel channel) {
        connectionMap.compute(key, (k, v) -> {
            if (v == null) {
                channel.attr(NettyConstants.CLIENT_NAME).set(key);
                v = ConnectEntry.builder()
                        .channel(channel)
                        .build();
            }
            v.setChannel(channel);
            v.setUsable(true);
            v.setLastActiveTimestamp(System.currentTimeMillis());
            return v;
        });
    }

    @Override
    public void addServiceChannel(String group, String serviceId, String instanceId, Channel channel) {
        String key = ServerInstanceUtil.generateServiceInstanceKey(
                group,
                serviceId,
                instanceId
        );
        addServiceChannel(key, channel);
    }

    @Override
    public void closeServiceChannel(Channel channel, String key) {
        ConnectEntry remove = connectionMap.remove(key);
        if (remove != null) {
            remove.close();
        }
        if (channel != null && channel.isActive()) {
            channel.close();
        }
        log.debug("close service instance [{}] channel", key);
    }

    @Override
    public ConnectEntry getServiceInstanceChannel(String key) {
        return connectionMap.compute(key, (k,v)->{
            if (v == null) {
                return null;
            }
            if (System.currentTimeMillis() - v.getLastActiveTimestamp() > OFF_LINE_SECONDS * 1000L) {
                log.warn("{} long time on refresh, offline.", key);
                return null;
            }
            return v;
        });
    }

    @Override
    public void freshServiceInstanceConnection(String key, Channel channel) {
        addServiceChannel(key, channel);
    }

    @Override
    public List<String> queryOnlineInstanceKey() {
        return connectionMap.entrySet().stream()
                .filter(entry -> BooleanUtils.isTrue(entry.getValue().isUsable()))
                .map(Map.Entry::getKey)
                .toList();
    }
}
