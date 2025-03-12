package cn.com.vortexa.rpc;

import cn.com.vortexa.common.service.IBaseService;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.BotInfo;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-07
 */
public interface IBotInfoRPC extends IBaseService<BotInfo> {

    Result bindBotAccountBaseInfo(Integer botId, String botKey, List<Integer> bindAccountBaseInfoList);

}
