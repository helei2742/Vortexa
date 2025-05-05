package cn.com.vortexa.job.util;

import cn.com.vortexa.common.dto.job.JobTrigger;
import org.quartz.*;

public class TriggerConvertUtils {

    public static JobTrigger fromQuartzTrigger(Trigger trigger) {
        JobTrigger jobTrigger = new JobTrigger();

        TriggerKey key = trigger.getKey();
        JobKey jobKey = trigger.getJobKey();

        jobTrigger.setName(key.getName());
        jobTrigger.setGroup(key.getGroup());
        jobTrigger.setJobName(jobKey.getName());
        jobTrigger.setJobGroup(jobKey.getGroup());
        jobTrigger.setFullJobName(jobKey.getGroup() + "." + jobKey.getName());

        jobTrigger.setStartTime(trigger.getStartTime());
        jobTrigger.setEndTime(trigger.getEndTime());
        jobTrigger.setPreviousFireTime(trigger.getPreviousFireTime());
        jobTrigger.setNextFireTime(trigger.getNextFireTime());

        jobTrigger.setMisfireInstruction(trigger.getMisfireInstruction());
        jobTrigger.setPriority(trigger.getPriority());

        // 判断类型并提取额外字段
        if (trigger instanceof CronTrigger) {
            CronTrigger cronTrigger = (CronTrigger) trigger;
            jobTrigger.setCronExpression(cronTrigger.getCronExpression());
        } else if (trigger instanceof SimpleTrigger) {
            SimpleTrigger simpleTrigger = (SimpleTrigger) trigger;
            jobTrigger.setRepeatInterval(simpleTrigger.getRepeatInterval());
            jobTrigger.setRepeatCount(simpleTrigger.getRepeatCount());
            jobTrigger.setTimesTriggered(simpleTrigger.getTimesTriggered());
        }


        return jobTrigger;
    }
}
