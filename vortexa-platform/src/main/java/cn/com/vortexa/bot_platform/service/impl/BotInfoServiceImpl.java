package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.bot_platform.mapper.BotInfoMapper;
import cn.com.vortexa.bot_platform.service.IBotInfoService;
import cn.com.vortexa.rpc.api.platform.IBotInfoRPC;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-07
 */
@Slf4j
@Service
public class BotInfoServiceImpl extends AbstractBaseService<BotInfoMapper, BotInfo> implements IBotInfoService, IBotInfoRPC {


    public BotInfoServiceImpl() {
        super(botInfo -> {
            botInfo.setInsertDatetime(LocalDateTime.now());
            botInfo.setUpdateDatetime(LocalDateTime.now());
            botInfo.setIsValid(1);
        });
    }

    @Override
    public Result bindBotAccountBaseInfo(Integer botId, String botKey, List<Integer> bindAccountBaseInfoList) {

        // Step 1 参数校验
        if (botId == null || StrUtil.isBlank(botKey)) {
            log.error("Bot[{}]-[{}]绑定账户时参数botId/botKey为空", botId, botKey);
            return Result.fail("参数botId/botKey不能为空");
        }

        BotInfo dbBotInfo = query().eq("id", botId).one();
        if (dbBotInfo == null) {
            log.error("Bot[{}]-[{}] 不存在该bot", botId, botKey);
            return Result.fail("不存在该bot");
        }

        if (bindAccountBaseInfoList == null) bindAccountBaseInfoList = List.of();

        return Result.ok(bindAccountBaseInfoList);
    }


    @Override
    public Integer insertOrUpdateRPC(BotInfo botInfo) throws SQLException {
        return insertOrUpdate(botInfo);
    }

    @Override
    public List<BotInfo> conditionQueryRPC(Map<String, Object> filterMap) throws SQLException {
        return conditionQuery(filterMap);
    }
}
