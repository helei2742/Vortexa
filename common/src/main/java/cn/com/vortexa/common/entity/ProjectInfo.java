package cn.com.vortexa.common.entity;

import cn.com.vortexa.common.util.tableprinter.CommandTableField;
import cn.com.vortexa.common.util.typehandler.LocalDateTimeTypeHandler;
import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

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
@TableName("t_project_info")
public class ProjectInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1654642415647348564L;

    @TableId(value = "id", type = IdType.AUTO)
    @CommandTableField
    private Integer id;

    @TableField("name")
    @CommandTableField
    private String name;

    @TableField("describe")
    @CommandTableField
    private String describe;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "is_valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer isValid;
}
