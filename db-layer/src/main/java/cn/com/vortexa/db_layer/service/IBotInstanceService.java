package cn.com.vortexa.db_layer.service;

import cn.com.vortexa.common.entity.BotInstance;

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
}
