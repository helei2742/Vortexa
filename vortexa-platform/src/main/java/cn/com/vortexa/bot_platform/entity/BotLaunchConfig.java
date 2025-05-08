package cn.com.vortexa.bot_platform.entity;

import cn.com.vortexa.common.util.typehandler.MapTextTypeHandler;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import cn.com.vortexa.common.util.typehandler.LocalDateTimeTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author helei
 * @since 2025/5/8 15:01
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_bot_launch_config")
public class BotLaunchConfig {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("script_node_name")
    private String scriptNodeName;

    @TableField("bot_name")
    private String botName;

    @TableField("bot_key")
    private String botKey;

    @TableField(value = "custom_config", typeHandler = MapTextTypeHandler.class)
    private Map<String, Object> customConfig;

    @TableField(value = "insert_datetime", fill = FieldFill.INSERT, typeHandler = LocalDateTimeTypeHandler.class)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", fill = FieldFill.INSERT_UPDATE, typeHandler = LocalDateTimeTypeHandler.class)
    private LocalDateTime updateDatetime;

    @TableField(value = "valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer valid;
}
