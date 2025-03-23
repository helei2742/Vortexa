package cn.com.vortexa.control.dto;


import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.control.constant.NameserverState;
import lombok.Data;

/**
 * @author helei
 * @since 2025-03-20
 */
@Data
public class RemoteControlServerStatus {

    private NameserverState nameserverState = NameserverState.SHUT_DOWN;

    private long lastUpdateTimestamp = System.currentTimeMillis();

    private ServiceInstance nameserverInstance;
}
