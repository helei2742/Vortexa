package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.bot_platform.dto.BotInstanceAccountQuery;
import cn.com.vortexa.bot_platform.dto.BotJob;
import cn.com.vortexa.bot_platform.dto.BotInstanceUpdate;
import cn.com.vortexa.bot_platform.script_control.BotPlatformControlServer;
import cn.com.vortexa.bot_platform.service.IBotLaunchConfigService;
import cn.com.vortexa.bot_platform.service.IScriptNodeService;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import cn.com.vortexa.common.entity.ScriptNode;
import cn.com.vortexa.common.exception.BotStartException;
import cn.com.vortexa.common.vo.BotInstanceVO;
import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.control.RegisteredScriptNode;
import cn.com.vortexa.bot_platform.mapper.BotInfoMapper;
import cn.com.vortexa.control.constant.WSControlSystemConstants;
import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.common.entity.BotInstance;
import cn.com.vortexa.bot_platform.mapper.BotInstanceMapper;
import cn.com.vortexa.bot_platform.service.IBotInstanceService;

import cn.com.vortexa.common.dto.job.JobTrigger;
import cn.com.vortexa.job.service.BotJobService;
import cn.com.vortexa.job.util.TriggerConvertUtils;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import cn.com.vortexa.rpc.api.platform.IBotInstanceRPC;
import lombok.extern.slf4j.Slf4j;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-18
 */
@Slf4j
@Service
public class BotInstanceServiceImpl extends AbstractBaseService<BotInstanceMapper, BotInstance>
        implements IBotInstanceRPC, IBotInstanceService {

    @Autowired
    private BotInfoMapper botInfoMapper;

    @Lazy
    @Autowired
    private BotPlatformControlServer botControlServer;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private IScriptNodeService scriptNodeService;

    @Autowired
    private BotJobService botJobService;

    @Lazy
    @Autowired
    private IBotLaunchConfigService botLaunchConfigService;

    @Override
    public PageResult<BotInstanceVO> conditionPageQueryAllInfo(Integer page, Integer limit,
                                                               Map<String, Object> filterMap) throws SQLException, SchedulerException {

        PageResult<BotInstance> result = super.conditionPageQuery(page, limit, filterMap);

        // 填充botInfo
        List<Integer> botIds = result.getList().stream().map(BotInstance::getBotId).toList();
        Map<Integer, BotInfo> idMapBotInfo = botIds.isEmpty() ? new HashMap<>() : botInfoMapper.selectBatchIds(botIds)
                .stream().collect(Collectors.toMap(BotInfo::getId, botInfo -> botInfo));

        // 查询运行中的任务
        Map<String, List<JobTrigger>> groupByBotKey = scheduler.getCurrentlyExecutingJobs()
                .stream()
                .map(context -> TriggerConvertUtils.fromQuartzTrigger(context.getTrigger()))

                .collect(Collectors.groupingBy(JobTrigger::getJobGroup));

        // 运行的bot
        ArrayList<BotInstanceVO> voList = new ArrayList<>();
        for (BotInstance instance : result.getList()) {

            String botName = instance.getBotName();
            String botKey = instance.getBotKey();

            BotInstanceVO vo = new BotInstanceVO();

            vo.setBotInstance(instance);
            vo.setBotInfo(idMapBotInfo.get(instance.getBotId()));
            Map<String, List<JobTrigger>> triggerMap
                    = botJobService.queryScriptNodeBotJobs(instance.getScriptNodeName(), botKey);
            vo.setJobTriggers(triggerMap);

            voList.add(vo);

            instance.addParam(BotInstance.BOT_INSTANCE_STATUS_KEY, botControlServer.getBotInstanceStatus(
                    WSControlSystemConstants.DEFAULT_GROUP, botName, botKey
            ));
        }

        return new PageResult<>(
                result.getTotal(),
                voList,
                result.getPages(),
                result.getPageNum(),
                result.getPageSize()
        );
    }

    @Override
    public BotInstanceVO detail(String scriptNodeName, String botKey) throws IOException, SchedulerException {
        if (StrUtil.isBlank(scriptNodeName) || StrUtil.isBlank(botKey)) {
            throw new IllegalArgumentException("scriptNodeName or botKey should not be blank");
        }
        // Step 1 查询实例
        BotInstance instance = getOne(new QueryWrapper<>(BotInstance.builder().scriptNodeName(scriptNodeName).botKey(botKey).build()));
        if (instance == null) {
            throw new RuntimeException("bot instance not found");
        }
        // Step 2 查询实例的botInfo
        BotInfo botInfo = botInfoMapper.selectById(instance.getBotId());
        if (botInfo == null) {
            throw new RuntimeException("bot info not found");
        }
        // Step 3 查询实例的启动yaml配置
//        AutoBotConfig botLaunchConfig = botLaunchConfigService.queryScriptNodeBotLaunchConfig(scriptNodeName, botKey);
        // Step 4 查询是否正在运行
        boolean online = botControlServer.isScriptNodeBotOnline(
                scriptNodeName,
                instance.getBotName(),
                botKey
        );

        // Step 5 查开始的任务
        Map<String, List<JobTrigger>> triggerMap
                = botJobService.queryScriptNodeBotJobs(scriptNodeName, botKey);

        return BotInstanceVO.builder()
                .botInstance(instance)
                .botInfo(botInfo)
                .botLaunchConfig(null)
                .online(online)
                .jobTriggers(triggerMap)
                .build();
    }

    @Override
    public Result updateJobParam(BotInstanceUpdate saveBotJobParamParam) {
        String scriptNodeName;
        String botKey;
        AutoBotJobParam jobParam;
        if (saveBotJobParamParam == null
                || (scriptNodeName = saveBotJobParamParam.getScriptNodeName()) == null
                || (botKey = saveBotJobParamParam.getBotKey()) == null
                || (jobParam = saveBotJobParamParam.getBotJobParam()) == null
        ) {
            return Result.fail("params error");
        }

        BotInstance botInstance = getOne(new QueryWrapper<>(BotInstance.builder().scriptNodeName(scriptNodeName).botKey(botKey).build()));
        if (botInstance == null) {
            return Result.fail("bot instance not found");
        }

        Map<String, AutoBotJobParam> jobParams = botInstance.getJobParams();
        if (!jobParams.containsKey(jobParam.getJobName())) {
            return Result.fail(jobParam.getJobName() + " job not found");
        }

        jobParams.put(jobParam.getJobName(), jobParam);
        BotInstance update = BotInstance.builder().id(botInstance.getId()).jobParams(jobParams).build();

        if (!updateById(update)) {
            return Result.fail("update failed");
        }

        return Result.ok();
    }

    @Override
    public Result saveBotInstanceLaunchConfig(String scriptNodeName, String botKey, String botLaunchConfig) throws IOException {
        scriptNodeService.updateScriptNodeBotLaunchConfig(scriptNodeName, botKey, botLaunchConfig);
        return Result.ok();
    }

    @Override
    public Result conditionPageQueryAccount(BotInstanceAccountQuery accountQuery) {
        BotInstance instance = getOne(
                new QueryWrapper<>(BotInstance.builder()
                        .scriptNodeName(accountQuery.getScriptNodeName())
                        .botKey(accountQuery.getBotKey())
                        .build())
        );
        if (instance == null) {
            return Result.fail("bot instance not found");
        }
        ScriptNode scriptNode = scriptNodeService.queryByScriptNodeName(accountQuery.getScriptNodeName());

        try {
            return botControlServer.queryBotInstanceAccount(
                    scriptNode,
                    instance.getBotId(),
                    instance.getBotName(),
                    accountQuery.getBotKey(),
                    accountQuery
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
            return Result.fail(e.getMessage());
        }
    }

    @Override
    public boolean exist(BotInstance query) {
        return baseMapper.exists(new QueryWrapper<>(query));
    }

    @Override
    public Boolean existsBotInstance(BotInstance query) {
        return getBaseMapper().exists(new QueryWrapper<>(query));
    }

    @Override
    public List<RegisteredScriptNode> queryOnLineInstance() {
        List<RegisteredScriptNode> res = new ArrayList<>();
        List<String> keys = botControlServer.getConnectionService().queryOnlineInstanceKey();
        keys.forEach(key -> res.addAll(botControlServer.getRegistryService().queryServiceInstance(key)));
        return res;
    }

    @Override
    public Result startJob(BotJob botJob) throws SchedulerException {
        String scriptNodeName;
        String botKey;
        String jobName;
        if (botJob == null || (scriptNodeName = botJob.getScriptNodeName()) == null
                || (botKey = botJob.getBotKey()) == null || (jobName = botJob.getJobName()) == null
        ) {
            return Result.fail("params error");
        }
        String jobGroup = BotJobService.botQuartzGroupBuilder(scriptNodeName, botKey);
        JobKey jobKey = new JobKey(jobName, jobGroup);
        TriggerKey triggerKey = new TriggerKey(jobName, jobGroup);

        return switch (botJobService.queryJobStatus(jobKey)) {
            case NONE, COMPLETE -> startRemoteScriptNodeBotJob(scriptNodeName, botKey, jobName);
            case ERROR -> {
                scheduler.resetTriggerFromErrorState(triggerKey);
                scheduler.resumeTrigger(triggerKey);
                yield Result.ok();
            }
            case PAUSED -> {
                botJobService.resumeJob(jobKey);
                yield Result.ok();
            }
            default -> Result.fail("job started");
        };

    }

    @Override
    public Result startRemoteScriptNodeBotJob(String scriptNodeName, String botKey, String jobName) {
        try {
            BotInstance instance = getOne(new QueryWrapper<>(BotInstance.builder().scriptNodeName(scriptNodeName).botKey(botKey).build()));
            if (instance == null) {
                return Result.fail("bot instance not found");
            }
            ScriptNode scriptNode = scriptNodeService.queryByScriptNodeName(scriptNodeName);

            return botControlServer.startJob(
                    scriptNode,
                    instance.getBotName(),
                    instance.getBotKey(),
                    jobName
            ).get();
        } catch (InterruptedException | ExecutionException | BotStartException e) {
            log.error("scriptNode[{}]botKey[{}]jobName[{}] start error", scriptNodeName, botKey, jobName, e);
            return Result.fail(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
        }
    }


    @Override
    public Result pauseJob(BotJob botJob) throws SchedulerException {
        if (botJobService.pauseJob(
                botJob.getScriptNodeName(),
                botJob.getBotKey(),
                botJob.getJobName()
        )) {
            return Result.ok();
        }
        return Result.fail("pause failed");
    }

    @Override
    public Result deleteJob(BotJob botJob) throws SchedulerException {
        if (botJobService.deleteJob(
                botJob.getScriptNodeName(),
                botJob.getBotKey(),
                botJob.getJobName()
        )) {
            return Result.ok();
        } else {
            return Result.fail("delete failed");
        }
    }

    @Override
    public List<BotInstance> batchQueryByIdsRPC(List<Serializable> ids) {
        return super.batchQueryByIds(ids);
    }

    @Override
    public Boolean existsBotInstanceRPC(BotInstance query) {
        return existsBotInstance(query);
    }

    @Override
    public Integer insertOrUpdateRPC(BotInstance instance) throws SQLException {
        return insertOrUpdate(instance);
    }

    @Override
    public BotInstance selectOneRPC(BotInstance query) {
        return getOne(new QueryWrapper<>(query));
    }
}
