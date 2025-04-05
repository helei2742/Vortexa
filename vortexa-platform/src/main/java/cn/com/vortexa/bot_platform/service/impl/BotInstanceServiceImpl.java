package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.bot_platform.dto.BotJob;
import cn.com.vortexa.bot_platform.script_control.BotPlatformControlServer;
import cn.com.vortexa.common.vo.BotInstanceVO;
import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.exception.BotStartException;
import cn.com.vortexa.common.dto.control.RegisteredService;
import cn.com.vortexa.bot_platform.mapper.BotInfoMapper;
import cn.com.vortexa.control.constant.WSControlSystemConstants;
import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.common.entity.BotInstance;
import cn.com.vortexa.bot_platform.mapper.BotInstanceMapper;
import cn.com.vortexa.bot_platform.service.IBotInstanceService;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import cn.com.vortexa.rpc.api.platform.IBotInstanceRPC;
import lombok.extern.slf4j.Slf4j;

import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public BotInstanceServiceImpl() {
        super(botInstance -> {
            botInstance.setInsertDatetime(LocalDateTime.now());
            botInstance.setUpdateDatetime(LocalDateTime.now());
            botInstance.setIsValid(1);
        });
    }


    @Override
    public PageResult<BotInstanceVO> conditionPageQueryAllInfo(Integer page, Integer limit,
                                                               Map<String, Object> filterMap) throws SQLException, SchedulerException {

        PageResult<BotInstance> result = super.conditionPageQuery(page, limit, filterMap);

        // 填充botInfo
        List<Integer> botIds = result.getList().stream().map(BotInstance::getBotId).toList();
        Map<Integer, BotInfo> idMapBotInfo = botInfoMapper.selectBatchIds(botIds)
                .stream().collect(Collectors.toMap(BotInfo::getId, botInfo -> botInfo));


        // 查询运行中的任务
        Map<String, List<Trigger>> groupByBotKey = scheduler.getCurrentlyExecutingJobs()
                .stream()
                .map(JobExecutionContext::getTrigger)
                .collect(Collectors.groupingBy(trigger -> trigger.getJobKey().getGroup()));

        ArrayList<BotInstanceVO> voList = new ArrayList<>();
        for (BotInstance instance : result.getList()) {
            BotInstanceVO vo = new BotInstanceVO();

            vo.setBotInstance(instance);
            vo.setBotInfo(idMapBotInfo.get(instance.getBotId()));
            vo.setRunningJob(JSONObject.toJSONString(groupByBotKey.get(instance.getBotKey())));

            voList.add(vo);

            instance.addParam(BotInstance.BOT_INSTANCE_STATUS_KEY, botControlServer.getBotInstanceStatus(
                    WSControlSystemConstants.DEFAULT_GROUP, instance.getBotName(), instance.getBotKey()
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
    public Boolean existsBotInstance(BotInstance query) {
        return getBaseMapper().exists(new QueryWrapper<>(query));
    }

    @Override
    public List<RegisteredService> queryOnLineInstance() {
        List<RegisteredService> res = new ArrayList<>();
        List<String> keys = botControlServer.getConnectionService().queryOnlineInstanceKey();
        keys.forEach(key -> res.addAll(botControlServer.getRegistryService().queryServiceInstance(key)));
        return res;
    }

    @Override
    public Result startJob(BotJob botJob) throws BotStartException {
        try {
            return botControlServer.startJob(
                    botJob.getGroup(),
                    botJob.getBotName(),
                    botJob.getBotKey(),
                    botJob.getJobName()
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("[{}] start job error", botJob, e);
            return Result.fail(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
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
