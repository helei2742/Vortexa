package cn.com.vortexa.nameserver.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ServiceInstance {

    private String group;

    private String serviceId;

    private String clientId;

    private String host;

    private int port;

    @Override
    public String toString() {
        return "[%s][%s][%s]-[%s:%s]".formatted(group, serviceId, clientId, host, port);
    }
}
