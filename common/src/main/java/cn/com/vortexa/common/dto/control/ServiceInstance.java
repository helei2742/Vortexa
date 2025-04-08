package cn.com.vortexa.common.dto.control;

import com.baomidou.mybatisplus.annotation.TableField;
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

    @TableField("group_id")
    protected String groupId;

    @TableField("service_id")
    protected String serviceId;

    @TableField("instance_id")
    protected String instanceId;

    @TableField("host")
    protected String host;

    @TableField("port")
    protected Integer port;

    @Override
    public String toString() {
        return "[%s][%s][%s]-[%s:%s]".formatted(groupId, serviceId, instanceId, host, port);
    }
}
