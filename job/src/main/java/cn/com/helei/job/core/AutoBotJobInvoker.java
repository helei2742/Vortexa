package cn.com.helei.job.core;

import cn.com.helei.common.dto.job.AutoBotJobParam;
import org.quartz.JobKey;

public interface AutoBotJobInvoker {


    void invokeJob(JobKey jobKey, AutoBotJobParam jobParam);

}
