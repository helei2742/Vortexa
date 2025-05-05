package cn.com.vortexa.common.entity;

import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import cn.com.vortexa.common.util.typehandler.LocalDateTimeTypeHandler;
import cn.com.vortexa.common.util.typehandler.MapTextTypeHandler;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *
 * </p>
 *
 * @author com.helei
 * @since 2025-02-07
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_bot_info")
public class BotInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1000026515674412242L;

    public static final String CONFIG_PARAMS_KEY = "config_params_key";

    public static final String ACCOUNT_PARAMS_KEY = "account_params_key";

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("image")
    private String image;

    @TableField("limit_project_ids")
    private String limitProjectIds;

    @TableField("job_params")
    private Map<String, AutoBotJobParam> jobParams = new HashMap<>();

    @TableField(value = "params", typeHandler = MapTextTypeHandler.class)
    private Map<String, Object> params = new HashMap<>();

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer valid;


    public void setJobParams(Map<String, Object> params) {
        this.jobParams = new HashMap<>();
        if (params == null || params.isEmpty()) {
            return;
        }
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof AutoBotJobParam param) {
                this.jobParams.put(key, param);
            } else if (value instanceof JSONObject jb) {
                this.jobParams.put(key, JSONObject.parseObject(jb.toJSONString(), AutoBotJobParam.class));
            } else {
                throw new IllegalArgumentException("error map entity value type");
            }
        }
    }
}
