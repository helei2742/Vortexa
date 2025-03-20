package cn.com.vortexa.nameserver.dto;


import cn.com.vortexa.nameserver.constant.NameserverState;
import lombok.Data;

/**
 * @author helei
 * @since 2025-03-20
 */
@Data
public class RemoteNameserverStatus {

    private NameserverState nameserverState = NameserverState.SHUT_DOWN;

    private long lastUpdateTimestamp = System.currentTimeMillis();

    private ServiceInstance nameserverInstance;
}
