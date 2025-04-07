package cn.com.vortexa.common.entity;

import cn.com.vortexa.common.util.tableprinter.CommandTableField;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * <p>
 *
 * </p>
 *
 * @author com.helei
 * @since 2025-02-06
 */
@Getter
@Setter
@Builder
@TableName("t_telegram_account")
@AllArgsConstructor
@NoArgsConstructor
public class TelegramAccount implements Serializable {

    @Serial
    private static final long serialVersionUID = 1649841654984796545L;

    @TableId(value = "id", type = IdType.INPUT)
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

    @TableField("phone_prefix")
    @ExcelProperty("phone_prefix")
    @CommandTableField
    private String phonePrefix;

    @TableField("phone")
    @ExcelProperty("phone")
    @CommandTableField
    private String phone;

    @TableField("token")
    @ExcelProperty("token")
    @CommandTableField
    private String token;

    @TableField("params")
    private Map<String, Object> params;

    @TableField("insert_datetime")
    private LocalDateTime insertDatetime;

    @TableField("update_datetime")
    private LocalDateTime updateDatetime;

    @TableField("valid")
    private Integer valid;
}
