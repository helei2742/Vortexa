package cn.com.vortexa.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BotACJobResult implements Serializable {
    @Serial
    private static final long serialVersionUID = -894375743645678459L;

    private Integer botId;

    private String group;

    private String jobName;

    private Integer acId;

    private Boolean success;

    private String errorMsg;

    private Object data;

    public BotACJobResult(Integer botId, String group, String jobName, Integer acId) {
        this(botId, group, jobName, acId, true, null, null);
    }

    public static BotACJobResult ok(Integer botId, String group, String jobName, Integer acId) {
        return new BotACJobResult(botId, group, jobName, acId, true, null, null);
    }

    public BotACJobResult setResult(Result result) {
        this.success = result.getSuccess();
        this.errorMsg = result.getErrorMsg();
        this.data = result.getData();

        return this;
    }
}
