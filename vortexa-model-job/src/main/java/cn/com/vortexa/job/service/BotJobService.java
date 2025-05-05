package cn.com.vortexa.job.service;


import cn.com.vortexa.common.dto.BotACJobResult;
import cn.com.vortexa.common.constants.JobStatus;
import cn.com.vortexa.job.core.AutoBotJobInvoker;
import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import cn.com.vortexa.common.dto.job.JobTrigger;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface BotJobService {

    static String botQuartzGroupBuilder(String scriptNodeName, String botKey) {
        return "node{" + scriptNodeName + "}bot{" + botKey + "}";
    }

    /**
     * 注册Job的调用者
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey         botKey
     * @param jobName        jobName
     * @param invoker        invoker
     */
    void registerJobInvoker(
            String scriptNodeName,
            String botKey,
            String jobName, AutoBotJobInvoker invoker
    );

    /**
     * 注册Job的调用者
     *
     * @param jobKey  jobKey
     * @param invoker invoker
     */
    void registerJobInvoker(JobKey jobKey, AutoBotJobInvoker invoker);

    /**
     * 获取job的调用者
     *
     * @param jobKey jobKey
     * @return invoker
     */
    AutoBotJobInvoker getJobInvoker(JobKey jobKey);

    /**
     * 批量注册job
     *
     * @param botKey           botKey
     * @param autoBotJobParams autoBotJobParams
     * @return Result
     */
    List<BotACJobResult> startJobList(
            String scriptNodeName,
            String botKey,
            String jobName,
            Collection<AutoBotJobParam> autoBotJobParams,
            AutoBotJobInvoker invoker
    );


    /**
     * 注册job，开始定时执行
     *
     * @param group           group
     * @param jobName         jobName
     * @param autoBotJobParam autoBotJobParam
     * @param invoker         invoker
     * @param refreshTrigger  refreshTrigger
     * @return BotACJobResult
     */
    BotACJobResult startJob(
            String scriptNodeName,
            String group,
            String jobName,
            AutoBotJobParam autoBotJobParam,
            AutoBotJobInvoker invoker,
            boolean refreshTrigger
    );

    /**
     * 注册job，开始定时执行
     *
     * @param scriptNodeName  scriptNodeName
     * @param botKey          botKey
     * @param jobName         jobName
     * @param autoBotJobParam autoBotJobParam
     * @param invoker         invoker
     * @return BotACJobResult
     */
    BotACJobResult startJob(
            String scriptNodeName,
            String botKey,
            String jobName,
            AutoBotJobParam autoBotJobParam,
            AutoBotJobInvoker invoker
    );

    Boolean pauseJob(JobKey jobKey) throws SchedulerException;

    /**
     * 暂停Job
     *
     * @param botKey  botKey
     * @param jobName jobName
     * @return  Boolean
     */
    Boolean pauseJob(String scriptNodeName, String botKey, String jobName) throws SchedulerException;

    /**
     * 暂停Bot的全部任务
     *
     * @param botKey botKey
     * @throws SchedulerException SchedulerException
     */
    void parseGroupJob(String scriptNodeName, String botKey) throws SchedulerException;


    /**
     * 重新启动Job
     *
     * @param botKey  botKey
     * @param jobName jobName
     * @throws SchedulerException SchedulerException
     */
    void resumeJob(String scriptNodeName, String botKey, String jobName) throws SchedulerException;

    /**
     * 重新启动Job
     *
     * @param jobKey jobKey
     * @throws SchedulerException SchedulerException
     */
    void resumeJob(JobKey jobKey) throws SchedulerException;

    /**
     * 删除Job
     *
     * @param botKey  botKey
     * @param jobName jobName
     * @return Boolean
     * @throws SchedulerException SchedulerException
     */
    Boolean deleteJob(String scriptNodeName, String botKey, String jobName) throws SchedulerException;

    /**
     * 删除Job
     *
     * @param jobKey jobKey
     * @return Boolean
     * @throws SchedulerException SchedulerException
     */
    Boolean deleteJob(JobKey jobKey) throws SchedulerException;

    /**
     * 查询job状态
     *
     * @param botKey  botKey
     * @param jobName jobName
     * @return JobStatus
     */
    JobStatus queryJobStatus(String scriptNodeName, String botKey, String jobName) throws SchedulerException;

    /**
     * 查询Job状态
     *
     * @param jobKey jobKey
     * @return JobStatus
     * @throws SchedulerException SchedulerException
     */
    JobStatus queryJobStatus(JobKey jobKey) throws SchedulerException;

    /**
     * 查询scriptNodeName下botKey运行的job
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey         botKey
     * @return List>
     * @throws SchedulerException SchedulerException
     */
    Map<String, List<JobTrigger>> queryScriptNodeBotJobs(String scriptNodeName, String botKey) throws SchedulerException;
}
