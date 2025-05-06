package cn.com.vortexa.common.entity;

import cn.com.vortexa.common.dto.BotMetaInfo;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.common.util.typehandler.MapTextTypeHandler;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

import lombok.*;

/**
 * <p>
 *
 * </p>
 *
 * @author com.helei
 * @since 2025-04-08
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_script_node")
@EqualsAndHashCode(callSuper = true)
public class ScriptNode extends ServiceInstance implements Serializable {
    @Serial
    private static final long serialVersionUID = -1023437865743897341L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("script_node_name")
    private String scriptNodeName;

    @TableField("description")
    private String description;

    @TableField("bot_config_map")
    private Map<String, AutoBotConfig> botConfigMap;    //  bot实例配置

    @TableField("bot_meta_info_map")
    private Map<String, BotMetaInfo> botMetaInfoMap;    // bot元信息

    @TableField("node_app_config")
    private String nodeAppConfig;   // script node 的application.yaml文件

    @TableField(value = "params", typeHandler = MapTextTypeHandler.class)
    private Map<String, Object> params;

    @TableField("version")
    private String version;

    @TableField(value = "insert_datetime", fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer valid;

    public boolean usable() {
        return StrUtil.isNotBlank(groupId) && StrUtil.isNotBlank(serviceId) && StrUtil.isNotBlank(instanceId)
                && StrUtil.isNotBlank(host) && port != null && StrUtil.isNotBlank(scriptNodeName);
    }

    public static ScriptNode generateFromServiceInstance(ServiceInstance serviceInstance) {
        ScriptNode scriptNode = new ScriptNode();
        scriptNode.setHost(serviceInstance.getHost());
        scriptNode.setPort(serviceInstance.getPort());
        scriptNode.setGroupId(serviceInstance.getGroupId());
        scriptNode.setServiceId(serviceInstance.getServiceId());
        scriptNode.setInstanceId(serviceInstance.getInstanceId());

        return scriptNode;
    }
}
