package cn.com.vortexa.common.dto.job;


import cn.com.vortexa.common.constants.JobStatus;
import lombok.Data;

import java.util.Date;

/**
 * @author helei
 * @since 2025-05-04
 */
@Data
public class JobTrigger {
    private String name;
    private String group;
    private String jobName;
    private String jobGroup;

    private String fullJobName;
    private String cronExpression;
    private int misfireInstruction;

    private Date startTime;
    private Date endTime;
    private Date previousFireTime;
    private Date nextFireTime;

    private int priority;
    private long repeatInterval;
    private int repeatCount;
    private int timesTriggered;

    private Object jobDataMap;

    // 状态
    private JobStatus jobStatus;
}
