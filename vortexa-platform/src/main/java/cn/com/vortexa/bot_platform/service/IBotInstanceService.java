package cn.com.vortexa.bot_platform.service;

import cn.com.vortexa.bot_platform.dto.BotJob;
import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.control.RegisteredService;
import cn.com.vortexa.common.entity.BotInstance;
import cn.com.vortexa.common.exception.BotStartException;
import cn.com.vortexa.common.vo.BotInstanceVO;
import cn.com.vortexa.db_layer.service.IBaseService;
import org.quartz.SchedulerException;

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
    List<RegisteredService> queryOnLineInstance();

    /**
     * 启动job
     *
     * @param botJob botJob
     * @return boolean
     */
    Result startJob(BotJob botJob) throws BotStartException;

    /**
     * 分页查询bot instance 的全信息
     *
     * @param page page
     * @param limit limit
     * @param filterMap filterMap
     * @return PageResult<BotInstanceVO>
     */
    PageResult<BotInstanceVO> conditionPageQueryAllInfo(Integer page, Integer limit, Map<String, Object> filterMap)
            throws SQLException, SchedulerException;
}
