package cn.com.vortexa.job.core;

import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import org.quartz.JobKey;

public interface AutoBotJobInvoker {


    void invokeJob(JobKey jobKey, AutoBotJobParam jobParam);

}
