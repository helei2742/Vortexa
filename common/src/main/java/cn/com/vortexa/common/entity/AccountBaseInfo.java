package cn.com.vortexa.common.entity;

import cn.com.vortexa.common.util.tableprinter.CommandTableField;
import cn.com.vortexa.common.util.typehandler.LocalDateTimeTypeHandler;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;


import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;


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
@TableName("t_account_base_info")
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AccountBaseInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1000026515674412242L;

    @TableId(value = "id", type = IdType.AUTO)
    @CommandTableField
    private Integer id;

    @TableField("type")
    @CommandTableField
    private String type;

    @TableField("name")
    @ExcelProperty("name")
    @CommandTableField
    private String name;

    @TableField("email")
    @ExcelProperty("email")
    @CommandTableField
    private String email;

    @TableField("password")
    @ExcelProperty("password")
    @CommandTableField
    private String password;

    @TableField("params")
    private Map<String, Object> params = new HashMap<>();

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "is_valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer isValid;


    public AccountBaseInfo(Object originLine) {
        String emailAndPassword = (String) originLine;

        String[] split = emailAndPassword.split(", ");
        email = split[0];

        password = split[1];

        if (split.length == 3) {
            name = split[2];
        }
        if (split.length == 4) {
            type = split[3];
        }
    }
}
