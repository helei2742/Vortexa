package cn.com.vortexa.common.entity;

import com.baomidou.mybatisplus.annotation.*;

import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
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
    public static final String BOT_INSTANCE_STATUS_KEY = "bot_instance_status";

    @Serial
    private static final long serialVersionUID = 4984719841947412242L;


    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("bot_id")
    private Integer botId;

    @TableField("bot_name")
    private String botName;

    @TableField("bot_key")
    private String botKey;

    @TableField("account_table_name")
    private String accountTableName;

    @TableField("job_params")
    private Map<String, AutoBotJobParam> jobParams = new HashMap<>();

    @TableField("params")
    private Map<String, Object> params;

    @TableField(value = "insert_datetime", fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer valid;

    @TableField(exist = false)
    public BotInfo botInfo;

    public synchronized void addParam(String key, Object value) {
        if (params == null) params = new HashMap<>();
        params.put(key, value);
    }
}
