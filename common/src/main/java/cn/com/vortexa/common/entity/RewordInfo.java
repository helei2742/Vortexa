package cn.com.vortexa.common.entity;

import cn.com.vortexa.common.util.propertylisten.PropertyChangeListenClass;
import cn.com.vortexa.common.util.tableprinter.CommandTableField;
import cn.com.vortexa.common.util.typehandler.LocalDateTimeTypeHandler;
import com.baomidou.mybatisplus.annotation.*;


import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

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
@TableName("t_reword_info")
@PropertyChangeListenClass
public class RewordInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 6919845416514161654L;


    @TableId(value = "project_account_id", type = IdType.INPUT)
    @CommandTableField
    private Integer projectAccountId;

    @TableField("total_points")
    @CommandTableField
    private Double totalPoints;

    @TableField("session")
    @CommandTableField
    private String session;

    @TableField("session_points")
    @CommandTableField
    private Double sessionPoints;

    @TableField("daily_points")
    @CommandTableField
    private Double dailyPoints;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "valid", fill = FieldFill.INSERT)
    @TableLogic
    private Boolean valid;


    public RewordInfo newInstance() {
        RewordInfo rewordInfo = new RewordInfo();
        rewordInfo.totalPoints = this.totalPoints;
        rewordInfo.session = this.session;
        rewordInfo.sessionPoints = this.sessionPoints;
        rewordInfo.dailyPoints = this.dailyPoints;
        rewordInfo.insertDatetime = this.insertDatetime;
        rewordInfo.updateDatetime = this.updateDatetime;
        rewordInfo.valid = this.valid;

        return rewordInfo;
    }
}
