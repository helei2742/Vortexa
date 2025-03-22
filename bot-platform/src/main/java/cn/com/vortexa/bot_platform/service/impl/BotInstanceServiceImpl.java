package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.db_layer.mapper.BotInfoMapper;
import cn.com.vortexa.db_layer.service.AbstractBaseService;
import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.common.entity.BotInstance;
import cn.com.vortexa.db_layer.mapper.BotInstanceMapper;
import cn.com.vortexa.db_layer.service.IBotInstanceService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import cn.com.vortexa.rpc.api.platform.IBotInstanceRPC;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    public BotInstanceServiceImpl() {
        super(botInstance -> {
            botInstance.setInsertDatetime(LocalDateTime.now());
            botInstance.setUpdateDatetime(LocalDateTime.now());
            botInstance.setIsValid(1);
        });
    }

    @Override
    public PageResult<BotInstance> conditionPageQuery(int page, int limit, Map<String, Object> filterMap) throws SQLException {

        PageResult<BotInstance> result = super.conditionPageQuery(page, limit, filterMap);

        // 填充botInfo
        List<Integer> botIds = result.getList().stream().map(BotInstance::getBotId).toList();
        Map<Integer, BotInfo> idMapBotInfo = botInfoMapper.selectBatchIds(botIds)
                .stream().collect(Collectors.toMap(BotInfo::getId, botInfo -> botInfo));

        for (BotInstance instance : result.getList()) {
            instance.setBotInfo(idMapBotInfo.get(instance.getBotId()));
        }

        return result;
    }


    @Override
    public Boolean existsBotInstance(BotInstance query) {
        return getBaseMapper().exists(new QueryWrapper<>(query));
    }

    @Override
    public Boolean existsBotInstanceRPC(BotInstance query) {
        return existsBotInstance(query);
    }

    @Override
    public Integer insertOrUpdateRPC(BotInstance instance) throws SQLException {
        return insertOrUpdate(instance);
    }
}
