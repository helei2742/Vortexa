package cn.com.vortexa.bot_platform.service;

import cn.com.vortexa.bot_platform.dto.BotInstanceAccountQuery;
import cn.com.vortexa.bot_platform.dto.BotJob;
import cn.com.vortexa.bot_platform.dto.BotInstanceUpdate;
import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.control.RegisteredScriptNode;
import cn.com.vortexa.common.entity.BotInstance;
import cn.com.vortexa.common.vo.BotInstanceVO;
import cn.com.vortexa.db_layer.service.IBaseService;
import org.quartz.SchedulerException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-18
 */
public interface IBotInstanceService extends IBaseService<BotInstance> {

    Boolean existsBotInstance(BotInstance query);

    /**
     * 查询正在运行的bot实例
     *
     * @return List<BotInfo>
     */
    List<RegisteredScriptNode> queryOnLineInstance();

    /**
     * 启动job
     *
     * @param botJob botJob
     * @return boolean
     */
    Result startJob(BotJob botJob) throws SchedulerException;

    /**
     * 启动远程script node 中bot的job
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey         botKey
     * @param jobName        jobName
     * @return Result
     */
    Result startRemoteScriptNodeBotJob(String scriptNodeName, String botKey, String jobName);

    /**
     * 暂停Job
     *
     * @param botJob botJob
     * @return Result
     */
    Result pauseJob(BotJob botJob) throws SchedulerException;

    /**
     * 删除Job
     *
     * @param botJob botJob
     * @return Result
     */
    Result deleteJob(BotJob botJob) throws SchedulerException;

    /**
     * 分页查询bot instance 的全信息
     *
     * @param page      page
     * @param limit     limit
     * @param filterMap filterMap
     * @return PageResult<BotInstanceVO>
     */
    PageResult<BotInstanceVO> conditionPageQueryAllInfo(Integer page, Integer limit, Map<String, Object> filterMap)
            throws SQLException, SchedulerException;

    /**
     * 查询详情
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey         botKey
     * @return BotInstanceVO
     */
    BotInstanceVO detail(String scriptNodeName, String botKey) throws IOException, SchedulerException;

    /**
     * 保存Job参数
     *
     * @param saveBotJobParamParam saveBotJobParamParam
     * @return Result
     */
    Result updateJobParam(BotInstanceUpdate saveBotJobParamParam);

    /**
     * 保存bot instance 启动配置
     *
     * @param scriptNodeName  scriptNodeName
     * @param botKey          botKey
     * @param botLaunchConfig botLaunchConfig
     * @return Result
     */
    Result saveBotInstanceLaunchConfig(String scriptNodeName, String botKey, String botLaunchConfig) throws IOException;

    /**
     * 条件条件查询bot instance 账户
     *
     * @param accountQuery accountQuery
     * @return Result
     */
    Result conditionPageQueryAccount(BotInstanceAccountQuery accountQuery);

    /**
     * 判断是否存在
     *
     * @param query query
     * @return boolean
     */
    boolean exist(BotInstance query);
}
