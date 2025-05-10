package cn.com.vortexa.bot_platform.service;

import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.db_layer.service.IBaseService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-07
 */
public interface IBotInfoService extends IBaseService<BotInfo> {

    Result bindBotAccountBaseInfo(Integer botId, String botKey, List<Integer> bindAccountBaseInfoList);

    boolean exist(BotInfo build);

    BotInfo queryByName(String name);
}
