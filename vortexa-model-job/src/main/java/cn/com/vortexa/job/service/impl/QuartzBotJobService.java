package cn.com.vortexa.job.service.impl;

import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.common.dto.BotACJobResult;
import cn.com.vortexa.job.constants.JobStatus;
import cn.com.vortexa.job.core.AutoBotJobInvoker;
import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import cn.com.vortexa.job.AutoBotJob;
import cn.com.vortexa.job.service.BotJobService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static cn.com.vortexa.common.dto.job.AutoBotJobParam.START_AT;
import static cn.com.vortexa.job.AutoBotJob.BOT_JOB_PARAM_KEY;


@Slf4j
@Service
public class QuartzBotJobService implements BotJobService {

    private final ConcurrentMap<JobKey, AutoBotJobInvoker> invokerMap;

    @Autowired
    private Scheduler scheduler;

    public QuartzBotJobService() {
        this.invokerMap = new ConcurrentHashMap<>();
    }

    @Override
    public void registerJobInvoker(JobKey jobKey, AutoBotJobInvoker invoker) {
        this.invokerMap.put(jobKey, invoker);
    }


    @Override
    public AutoBotJobInvoker getJobInvoker(JobKey jobKey) {
        return invokerMap.get(jobKey);
    }

    @Override
    public List<BotACJobResult> startJobList(
            String botKey,
            String jobName,
            Collection<AutoBotJobParam> autoBotJobParams,
            AutoBotJobInvoker invoker
    ) {
        List<BotACJobResult> resultList = new ArrayList<>(autoBotJobParams.size());

        for (AutoBotJobParam autoBotJob : autoBotJobParams) {
            resultList.add(startJob(botKey, jobName, autoBotJob, invoker));
        }

        return resultList;
    }


    @Override
    public BotACJobResult startJob(
            String botKey,
            String jobName,
            AutoBotJobParam jobParam,
            AutoBotJobInvoker invoker,
            boolean refreshTrigger
    ) {

        JobKey jobKey = new JobKey(jobName, botKey);

        registerJobInvoker(jobKey, invoker);

        BotACJobResult result = BotACJobResult
                .builder()
                .group(botKey)
                .jobName(jobName)
                .success(true)
                .build();

        try {

            JobStatus status = queryJobStatus(jobKey);

            switch (status) {
                case PARSED -> resumeJob(jobKey);
                case STARTED -> {
                    scheduler.deleteJob(jobKey);
                    registerJobInvoker(jobKey, invoker);
//                    updateTrigger(jobParam, refreshTrigger, jobKey, result);
                }
                case NOT_REGISTER -> registryAndStartJob(jobKey, jobParam);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMsg(e.getMessage());

            log.error("注册[{}]job发生异常", jobKey, e);
        }

        return result;
    }


    @Override
    public BotACJobResult startJob(String group, String jobName, AutoBotJobParam autoBotJobParam, AutoBotJobInvoker invoker) {
        return startJob(group, jobName, autoBotJobParam, invoker, true);
    }

    @Override
    public void parseJob(JobKey jobKey) throws SchedulerException {
        if (scheduler.getJobDetail(jobKey) == null) {
            log.warn("[{}] not exist, cancel parse", jobKey);
            return;
        }

        scheduler.pauseJob(jobKey);
        log.info("[{}] parsed", jobKey);
    }


    @Override
    public void parseJob(String botKey, String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, botKey);
        parseJob(jobKey);
    }

    @Override
    public void parseGroupJob(String botKey) throws SchedulerException {
        scheduler.pauseJobs(GroupMatcher.jobGroupEquals(botKey));
        log.info("bot[{}] all job parsed", botKey);
    }

    @Override
    public void resumeJob(String botKey, String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, botKey);
        resumeJob(jobKey);
    }

    @Override
    public void resumeJob(JobKey jobKey) throws SchedulerException {
        if (scheduler.getJobDetail(jobKey) == null) {
            log.warn("[{}] not exist, cancel resume", jobKey);
            return;
        }
        scheduler.resumeJob(jobKey);
        log.info("[{}] resumed", jobKey);
    }

    @Override
    public JobStatus queryJobStatus(String botKey, String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, botKey);
        return queryJobStatus(jobKey);
    }

    @Override
    public JobStatus queryJobStatus(JobKey jobKey) throws SchedulerException {

        if (!scheduler.checkExists(jobKey)) {
            return JobStatus.NOT_REGISTER;
        }
        if (scheduler.isStarted()) {
            return JobStatus.STARTED;
        }
        return JobStatus.PARSED;
    }


    /**
     * 注册并启动job
     *
     * @param jobKey   jobKey
     * @param jobParam jobParam
     * @throws SchedulerException SchedulerException
     */
    private void registryAndStartJob(JobKey jobKey, AutoBotJobParam jobParam) throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(BOT_JOB_PARAM_KEY, jobParam);

        JobDetail jobDetail = JobBuilder.newJob(AutoBotJob.class)
                .withIdentity(jobKey)
                .withDescription(jobParam.getDescription())
                .setJobData(jobDataMap)
//                        .storeDurably()
                .build();

        Trigger trigger = generateTriggerFromParam(jobKey, jobParam);

        scheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * 解析参数，生成trigger
     *
     * @param jobKey   jobKey
     * @param jobParam jobParam
     * @return Trigger
     */
    private static Trigger generateTriggerFromParam(JobKey jobKey, AutoBotJobParam jobParam) {
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                .withIdentity(jobParam.getJobName(), jobKey.getGroup())
                .startNow();

        if (BotJobType.ONCE_TASK.equals(jobParam.getJobType())
                || BotJobType.ACCOUNT_SPLIT_JOB.equals(jobParam.getJobType())
        ) {
            Map<String, Object> params = jobParam.getParams();

            Object start;
            if (params == null || (start = params.get(START_AT)) == null) {
                triggerBuilder.startNow();
            } else {
                triggerBuilder.startAt(new Date((Long) start));
            }
        } else if (jobParam.getIntervalInSecond() != null) {
            triggerBuilder
                    .withSchedule(SimpleScheduleBuilder
                            .simpleSchedule()
                            .withIntervalInSeconds(jobParam.getIntervalInSecond())
                            .repeatForever()
                    );
        } else if (jobParam.getCronExpression() != null) {
            triggerBuilder
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobParam.getCronExpression()));
        }
        return triggerBuilder.build();
    }


    private void updateTrigger(AutoBotJobParam jobParam, boolean refreshTrigger, JobKey jobKey, BotACJobResult result) throws SchedulerException {
        // 检查是否发生变化
        JobDetail jobDetail = scheduler.getJobDetail(jobKey);
        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        // 提取JobDataMap里的参数
        AutoBotJobParam dbParam = (AutoBotJobParam) jobDataMap.get(BOT_JOB_PARAM_KEY);

        // 发生变化，修改trigger，和jobDetail重新启动
        if (refreshTrigger && !dbParam.equals(jobParam)) {

            jobDataMap.put(BOT_JOB_PARAM_KEY, jobParam);

            // 更新 JobDetail 到调度器
            scheduler.deleteJob(jobKey);

            Trigger trigger = generateTriggerFromParam(jobKey, jobParam);
            scheduler.scheduleJob(jobDetail, trigger);

            // 重新调度，确保 JobDataMap 更新
            log.info("[{}] trigger 修改成功 new trigger [{}]", jobKey, trigger);
        } else {
            result.setSuccess(false);
            result.setErrorMsg("job exist");
        }
    }
}
