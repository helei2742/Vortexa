package cn.com.vortexa.common.entity;

import cn.com.vortexa.common.util.propertylisten.PropertyChangeListenClass;
import cn.com.vortexa.common.util.tableprinter.CommandTableField;
import cn.com.vortexa.common.util.typehandler.LocalDateTimeTypeHandler;
import com.baomidou.mybatisplus.annotation.*;

        import kotlin.Deprecated;
import kotlin.jvm.JvmOverloads;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("t_reword_info")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@PropertyChangeListenClass
public class RewordInfo implements Serializable {
    public static final String BOT_REWORD_INFO_TABLE_PREFIX = "t_reword_info";

    @Serial
    private static final long serialVersionUID = 6919845416514161654L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField(value = "bot_id")
    private Integer botId;

    @TableField(value = "bot_key")
    private String botKey;

    @TableField(value = "bot_account_id")
    @CommandTableField
    private Integer botAccountId;

    @TableField("total_points")
    @CommandTableField
    private Double totalPoints;

    @TableField("daily_points")
    @CommandTableField
    private Double dailyPoints;

    @TableField(value = "insert_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT)
    private LocalDateTime insertDatetime;

    @TableField(value = "update_datetime", typeHandler = LocalDateTimeTypeHandler.class, fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDatetime;

    @TableField(value = "valid", fill = FieldFill.INSERT)
    @TableLogic
    private Integer valid;


    public RewordInfo newInstance() {
        RewordInfo rewordInfo = new RewordInfo();
        rewordInfo.botId = this.botId;
        rewordInfo.botKey = this.botKey;
        rewordInfo.botAccountId = this.botAccountId;
        rewordInfo.totalPoints = this.totalPoints;
        rewordInfo.dailyPoints = this.dailyPoints;
        rewordInfo.insertDatetime = this.insertDatetime;
        rewordInfo.updateDatetime = this.updateDatetime;
        rewordInfo.valid = this.valid;

        return rewordInfo;
    }
}
