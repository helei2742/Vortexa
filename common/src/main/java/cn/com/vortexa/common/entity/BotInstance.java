package cn.com.vortexa.common.entity;

import cn.com.vortexa.common.util.typehandler.LocalDateTimeTypeHandler;
import cn.com.vortexa.common.util.typehandler.MapTextTypeHandler;
import com.alibaba.fastjson.JSONObject;
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

    @TableField("script_node_name")
    private String scriptNodeName;

    @TableField("bot_key")
    private String botKey;

    @TableField("account_table_name")
    private String accountTableName;

    @TableField(value = "job_params", typeHandler = MapTextTypeHandler.class)
    private Map<String, AutoBotJobParam> jobParams = new HashMap<>();

    @TableField(value = "params", typeHandler = MapTextTypeHandler.class)
    private Map<String, Object> params;

    @TableField(value = "insert_datetime", fill = FieldFill.INSERT, typeHandler = LocalDateTimeTypeHandler.class)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", fill = FieldFill.INSERT_UPDATE, typeHandler = LocalDateTimeTypeHandler.class)
    private LocalDateTime updateDatetime;

    @TableField(value = "valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer valid;

    public void setJobParams(Map<String, ?> jobParams) {
        Map<String, AutoBotJobParam> map = new HashMap<>();
        jobParams.forEach((k, v) -> {
            if (v instanceof JSONObject) {
                map.put(k, JSONObject.parseObject(JSONObject.toJSONString(v), AutoBotJobParam.class));
            } else if (v instanceof AutoBotJobParam p) {
                map.put(k, p);
            }
        });
        this.jobParams = map;
    }

    public synchronized void addParam(String key, Object value) {
        if (params == null) params = new HashMap<>();
        params.put(key, value);
    }
}
