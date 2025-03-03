package cn.com.helei.common.entity;

import cn.com.helei.common.util.tableprinter.CommandTableField;
import cn.com.helei.common.util.typehandler.LocalDateTimeTypeHandler;
import com.alibaba.excel.annotation.ExcelProperty;
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
 * @since 2025-02-05
 */
@Getter
@Setter
@Builder
@TableName("t_twitter_account")
@AllArgsConstructor
@NoArgsConstructor
public class TwitterAccount implements Serializable {

    @Serial
    private static final long serialVersionUID = 8945289498414514514L;


    @TableId(value = "id", type = IdType.AUTO)
    @CommandTableField
    private Integer id;

    @TableField("username")
    @ExcelProperty("username")
    @CommandTableField
    private String username;

    @TableField("password")
    @ExcelProperty("password")
    @CommandTableField
    private String password;

    @TableField("email")
    @ExcelProperty("email")
    @CommandTableField
    private String email;

    @TableField("email_password")
    @ExcelProperty("email_password")
    @CommandTableField
    private String emailPassword;

    @TableField("token")
    @ExcelProperty("token")
    @CommandTableField
    private String token;

    @TableField("f2a_key")
    @ExcelProperty("f2a_key")
    @CommandTableField
    private String f2aKey;

    @TableField("params")
    private Map<String, Object> params;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "is_valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer isValid;
}
