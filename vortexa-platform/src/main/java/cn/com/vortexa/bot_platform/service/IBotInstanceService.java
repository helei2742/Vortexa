package cn.com.vortexa.bot_platform.service;

import cn.com.vortexa.common.dto.control.RegisteredService;
import cn.com.vortexa.common.entity.BotInstance;
import cn.com.vortexa.db_layer.service.IBaseService;

import java.util.List;

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
}
