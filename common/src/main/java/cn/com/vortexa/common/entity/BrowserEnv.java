package cn.com.vortexa.common.entity;

import cn.com.vortexa.common.util.tableprinter.CommandTableField;
import cn.com.vortexa.common.util.typehandler.LocalDateTimeTypeHandler;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/**
 * <p>
 *
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@Getter
@Setter
@ToString
@TableName("t_browser_env")
public class BrowserEnv implements Serializable {
    @Serial
    private static final long serialVersionUID = 949819741465456514L;

    public static final String USER_AGENT_KEY = "userAgent";

    @TableId(value = "id", type = IdType.AUTO)
    @CommandTableField
    private Integer id;

    @TableField("user_agent")
    @ExcelProperty("user_agent")
    @CommandTableField
    private String userAgent;

    @TableField("other_header")
    @ExcelProperty("other_header")
    @CommandTableField
    private Map<String, Object> otherHeader;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer valid;


    public Map<String, String> generateHeaders() {
        HashMap<String, String> map = new HashMap<>();
        map.put("User-Agent", userAgent);
        otherHeader.forEach((k, v) -> map.put(k, StrUtil.toString(v)));
        return map;
    }
}
