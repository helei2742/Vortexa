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
}
