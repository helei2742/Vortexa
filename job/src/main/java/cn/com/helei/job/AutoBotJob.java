package cn.com.helei.job;

import cn.com.helei.common.dto.job.AutoBotJobParam;
import cn.com.helei.job.core.AutoBotJobInvoker;
import cn.com.helei.job.service.BotJobService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static cn.com.helei.common.constants.BotJobType.ACCOUNT_SPLIT_JOB;
import static cn.com.helei.common.constants.BotJobType.ONCE_TASK;

@Slf4j
@Component
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class AutoBotJob implements Job {

    public static final String BOT_JOB_PARAM_KEY = "bot_job_param";

    @Autowired
    private BotJobService botJobService;

    @Autowired
    private Scheduler scheduler;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobKey key = context.getJobDetail().getKey();

        // 获取job调用者
        AutoBotJobInvoker invoker = botJobService.getJobInvoker(key);
        AutoBotJobParam param = (AutoBotJobParam) context.getJobDetail().getJobDataMap().get(BOT_JOB_PARAM_KEY);

        try {
            if (invoker != null) {
                // 调用invoke方法
                invoker.invokeJob(key, param);
            } else {
                log.warn("job[{}] invoker is null, cancel job execute", key);
            }
        } finally {
            if (ACCOUNT_SPLIT_JOB.equals(param.getJobType()) || ONCE_TASK.equals(param.getJobType())) {
                try {
                    scheduler.deleteJob(key);
                } catch (SchedulerException e) {
                    log.error("delete once job[{}] error, e", key, e);
                }
            }
        }
    }
}
