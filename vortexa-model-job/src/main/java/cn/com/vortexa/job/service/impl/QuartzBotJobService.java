package cn.com.vortexa.job.service.impl;

import cn.com.vortexa.common.constants.BotJobType;
import cn.com.vortexa.common.dto.BotACJobResult;
import cn.com.vortexa.common.constants.JobStatus;
import cn.com.vortexa.job.core.AutoBotJobInvoker;
import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import cn.com.vortexa.job.dto.AutoBotJob;
import cn.com.vortexa.common.dto.job.JobTrigger;
import cn.com.vortexa.job.service.BotJobService;
import cn.com.vortexa.job.util.TriggerConvertUtils;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.utils.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static cn.com.vortexa.common.dto.job.AutoBotJobParam.START_AT;
import static cn.com.vortexa.job.dto.AutoBotJob.BOT_JOB_PARAM_KEY;


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
    public void registerJobInvoker(String scriptNodeName, String botKey, String jobName, AutoBotJobInvoker invoker) {
        String group = BotJobService.botQuartzGroupBuilder(scriptNodeName, botKey);
        JobKey jobKey = new JobKey(jobName, group);
        registerJobInvoker(jobKey, invoker);
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
            String scriptNodeName,
            String botKey,
            String jobName,
            Collection<AutoBotJobParam> autoBotJobParams,
            AutoBotJobInvoker invoker
    ) {
        List<BotACJobResult> resultList = new ArrayList<>(autoBotJobParams.size());

        for (AutoBotJobParam autoBotJob : autoBotJobParams) {
            resultList.add(startJob(scriptNodeName, botKey, jobName, autoBotJob, invoker));
        }

        return resultList;
    }


    @Override
    public BotACJobResult startJob(
            String scriptNodeName,
            String botKey,
            String jobName,
            AutoBotJobParam jobParam,
            AutoBotJobInvoker invoker,
            boolean refreshTrigger
    ) {
        String group = BotJobService.botQuartzGroupBuilder(scriptNodeName, botKey);

        JobKey jobKey = new JobKey(jobName, group);
        TriggerKey triggerKey = new TriggerKey(jobName, group);

        registerJobInvoker(jobKey, invoker);

        BotACJobResult result = BotACJobResult
                .builder()
                .group(group)
                .jobName(jobName)
                .success(true)
                .build();

        try {
            JobStatus status = queryJobStatus(jobKey);

            switch (status) {
                case PAUSED -> {
                    log.warn("job [{}] paused, will resume it", jobKey);
                    resumeJob(jobKey);
                }
                case COMPLETE -> {
                    log.warn("job [{}] already complete, will restart it", jobKey);
                    scheduler.deleteJob(jobKey);
                    registerJobInvoker(jobKey, invoker);
                }
                case BLOCKED, NORMAL -> {
                    log.warn("job [{}] already started, will delete it", jobKey);
                    scheduler.deleteJob(jobKey);
                }
                case ERROR -> {
                    log.warn("job [{}] error, will resume trigger it", jobKey);
                    scheduler.resetTriggerFromErrorState(triggerKey);
                    scheduler.resumeTrigger(triggerKey);
                }
                case NONE -> registryAndStartJob(jobKey, jobParam);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMsg(e.getMessage());

            log.error("注册[{}]job发生异常", jobKey, e);
        }

        return result;
    }


    @Override
    public BotACJobResult startJob(String scriptNodeName, String botKey, String jobName, AutoBotJobParam autoBotJobParam, AutoBotJobInvoker invoker) {
        return startJob(scriptNodeName, botKey, jobName, autoBotJobParam, invoker, true);
    }

    @Override
    public Boolean pauseJob(JobKey jobKey) throws SchedulerException {
        TriggerKey triggerKey = new TriggerKey(jobKey.getName(), jobKey.getGroup());
        Trigger.TriggerState state = scheduler.getTriggerState(triggerKey);
        return switch (state) {
            case NORMAL, BLOCKED -> {
                scheduler.pauseTrigger(triggerKey);
                yield true;
            }
            case ERROR -> {
                scheduler.resetTriggerFromErrorState(triggerKey);
                scheduler.pauseTrigger(triggerKey);
                yield true;
            }
            case PAUSED, COMPLETE, NONE -> false;
        };
    }


    @Override
    public Boolean pauseJob(String scriptNodeName, String botKey, String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, BotJobService.botQuartzGroupBuilder(scriptNodeName, botKey));
        return pauseJob(jobKey);
    }

    @Override
    public void parseGroupJob(String scriptNodeName, String botKey) throws SchedulerException {
        String group = BotJobService.botQuartzGroupBuilder(scriptNodeName, botKey);
        scheduler.pauseJobs(GroupMatcher.jobGroupEquals(group));
        log.info("{} all job parsed", group);
    }

    @Override
    public void resumeJob(String scriptNodeName, String botKey, String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, BotJobService.botQuartzGroupBuilder(scriptNodeName, botKey));
        resumeJob(jobKey);
    }

    @Override
    public void resumeJob(JobKey jobKey) throws SchedulerException {
        if (scheduler.getJobDetail(jobKey) == null) {
            throw new SchedulerException(jobKey + "not exist, cancel resume");
        }
        scheduler.resumeJob(jobKey);
        log.info("[{}] resumed", jobKey);
    }

    @Override
    public Boolean deleteJob(String scriptNodeName, String botKey, String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, BotJobService.botQuartzGroupBuilder(scriptNodeName, botKey));
        return deleteJob(jobKey);
    }

    @Override
    public Boolean deleteJob(JobKey jobKey) throws SchedulerException {
        if (scheduler.getJobDetail(jobKey) == null) {
            throw new SchedulerException(jobKey + "not exist, cancel delete");
        }
        return scheduler.deleteJob(jobKey);
    }

    @Override
    public JobStatus queryJobStatus(String scriptNodeName, String botKey, String jobName) throws SchedulerException {
        JobKey jobKey = new JobKey(jobName, BotJobService.botQuartzGroupBuilder(scriptNodeName, botKey));
        return queryJobStatus(jobKey);
    }

    @Override
    public JobStatus queryJobStatus(JobKey jobKey) throws SchedulerException {
        TriggerKey triggerKey = new TriggerKey(jobKey.getName(), jobKey.getGroup());
        Trigger.TriggerState state = scheduler.getTriggerState(triggerKey);
        return switch (state) {
            case NONE -> JobStatus.NONE;
            case NORMAL -> JobStatus.NORMAL;
            case PAUSED -> JobStatus.PAUSED;
            case COMPLETE -> JobStatus.COMPLETE;
            case ERROR -> JobStatus.ERROR;
            case BLOCKED -> JobStatus.BLOCKED;
        };
    }

    @Override
    public Map<String, List<JobTrigger>> queryScriptNodeBotJobs(String scriptNodeName, String botKey) throws SchedulerException {
        Set<JobKey> jobKeys = scheduler.getJobKeys(
                GroupMatcher.jobGroupEquals(BotJobService.botQuartzGroupBuilder(scriptNodeName, botKey))
        );
        return jobKeys.stream().collect(Collectors.toMap(
                Key::getName,
                jobKey -> {
                    try {
                        return scheduler.getTriggersOfJob(jobKey).stream().map(trigger -> {
                            try {
                                JobTrigger jobTrigger = TriggerConvertUtils.fromQuartzTrigger(trigger);
                                jobTrigger.setJobStatus(queryJobStatus(scriptNodeName, botKey, jobKey.getName()));
                                return jobTrigger;
                            } catch (SchedulerException e) {
                                throw new RuntimeException(e);
                            }
                        }).toList();
                    } catch (SchedulerException e) {
                        throw new RuntimeException(e);
                    }
                }
        ));
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
