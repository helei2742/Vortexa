package cn.com.helei.rpc;

import cn.com.helei.common.service.IBaseService;
import cn.com.helei.common.dto.Result;
import cn.com.helei.common.entity.BotInfo;

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
