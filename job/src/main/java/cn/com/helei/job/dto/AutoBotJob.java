package cn.com.helei.job.dto;

import cn.com.helei.common.dto.job.AutoBotJobParam;
import cn.com.helei.job.core.AutoBotJobInvoker;
import cn.com.helei.job.service.BotJobService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class AutoBotJob implements Job {

    public static final String BOT_JOB_PARAM_KEY = "bot_job_param";


    @Autowired
    private BotJobService botJobService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobKey key = context.getJobDetail().getKey();

        AutoBotJobParam params = (AutoBotJobParam) context.getJobDetail().getJobDataMap().remove(BOT_JOB_PARAM_KEY);
        // 获取job调用者
        AutoBotJobInvoker invoker = botJobService.getJobInvoker(key);

        if (invoker != null) {
            // 调用invoke方法
            invoker.invokeJob(key, params);
        } else {
            log.warn("job[{}] invoker is null", key);
        }
    }

}
