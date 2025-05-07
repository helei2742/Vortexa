package cn.com.vortexa.common.dto.job;

import cn.com.vortexa.common.constants.BotJobType;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AutoBotJobParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 5651494651564179417L;


    public static final String START_AT = "start_at";

    private BotJobType jobType;

    private String jobName;

    private String description;

    private String cronExpression;

    private Integer intervalInSecond;

    private Integer concurrentCount;

    private AutoBotJobWSParam autoBotJobWSParam;

    private Boolean uniqueAccount;

    private Boolean syncExecute;

    private Boolean dynamicTrigger;

    private Integer dynamicTimeWindowMinute = 0;

    private Map<String, Object> params = new HashMap<>();

    public synchronized void putParam(String key, Object value) {
        if (params == null) { params = new HashMap<>(); }

        params.put(key, value);
    }

    public void merge(AutoBotJobParam jobParam) {
        if (jobParam == null) return;

        if (jobParam.getJobType() != null) { this.jobType = jobParam.getJobType(); }
        if (jobParam.getJobName() != null) { this.jobName = jobParam.getJobName(); }
        if (jobParam.getDescription() != null) { this.description = jobParam.getDescription(); }
        if (jobParam.getCronExpression() != null) { this.cronExpression = jobParam.getCronExpression(); }
        if (jobParam.getIntervalInSecond() != null) { this.intervalInSecond = jobParam.getIntervalInSecond(); }
        if (jobParam.getConcurrentCount() != null) { this.concurrentCount = jobParam.getConcurrentCount(); }
        if (jobParam.getAutoBotJobWSParam() != null) {
            if (this.autoBotJobWSParam == null) {
                this.autoBotJobWSParam = jobParam.getAutoBotJobWSParam();
            } else {
                this.autoBotJobWSParam.merge(jobParam.getAutoBotJobWSParam());
            }
        }
        if (jobParam.getUniqueAccount() != null) { this.uniqueAccount = jobParam.getUniqueAccount(); }
        if (jobParam.getSyncExecute() != null) { this.syncExecute = jobParam.getSyncExecute(); }
        if (jobParam.getDynamicTrigger() != null) { this.dynamicTrigger = jobParam.getDynamicTrigger(); }
        if (jobParam.getDynamicTimeWindowMinute() != null) { this.dynamicTimeWindowMinute = jobParam.getDynamicTimeWindowMinute(); }
        if (jobParam.getParams() != null) {
            if (this.params == null) {
                this.params = jobParam.getParams();
            } else {
                this.params.putAll(jobParam.getParams());
            }
        }
    }
}
