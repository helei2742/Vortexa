package cn.com.helei.db_layer.service;

import cn.com.helei.common.dto.Result;
import cn.com.helei.common.entity.BotInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-07
 */
public interface IBotInfoService extends IService<BotInfo>, IBaseService<BotInfo> {

    Result bindBotAccountBaseInfo(Integer botId, String botKey, List<Integer> bindAccountBaseInfoList);

}
