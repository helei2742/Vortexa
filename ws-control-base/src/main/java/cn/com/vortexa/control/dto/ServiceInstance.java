package cn.com.vortexa.control.dto;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ServiceInstance implements Serializable {

    @Serial
    private static final long serialVersionUID = 189273891273821798L;

    private String group;

    private String serviceId;

    private String instanceId;

    private String host;

    private int port;

    @Override
    public String toString() {
        return "[%s][%s][%s]-[%s:%s]".formatted(group, serviceId, instanceId, host, port);
    }
}
