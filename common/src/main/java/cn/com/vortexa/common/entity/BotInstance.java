package cn.com.vortexa.common.entity;

import com.baomidou.mybatisplus.annotation.*;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author com.helei
 * @since 2025-02-18
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_bot_instance")
public class BotInstance implements Serializable {

    @Serial
    private static final long serialVersionUID = 4984719841947412242L;


    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("bot_id")
    private Integer botId;

    @TableField("bot_key")
    private String botKey;

    @TableField("account_table_name")
    private String accountTableName;

    @TableField("params")
    private Map<String, Object> params;

    @TableField(value = "insert_datetime", fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "is_valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer isValid;

    public BotInfo botInfo;
}
