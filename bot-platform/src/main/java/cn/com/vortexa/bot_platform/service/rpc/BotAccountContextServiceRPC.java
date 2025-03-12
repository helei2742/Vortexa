package cn.com.vortexa.bot_platform.service.rpc;

import cn.com.vortexa.bot_platform.service.DynamicDubboRPCService;
import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.rpc.bot.IBotAccountRPC;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;


@Service
public class BotAccountContextServiceRPC extends DynamicDubboRPCService<IBotAccountRPC> {

    public BotAccountContextServiceRPC() {
        super(IBotAccountRPC.class);
    }

    public Result saveBotAccountContext(Integer botId, String botKey, List<Map<String, Object>> acKVMap) {
        return getRPCInstance("", String.valueOf(botId), botKey).saveBotAccountContext(botId, botKey, acKVMap);
    }

    public Boolean checkAndCreateShardedTable(Integer botId, String botKey, boolean existUpdate) throws SQLException {
        return getRPCInstance("", String.valueOf(botId), botKey).checkAndCreateShardedTable(botId, botKey, existUpdate);
    }

    public Integer insertOrUpdate(Integer botId, String botKey, AccountContext accountContext) throws SQLException {
        return getRPCInstance("", String.valueOf(botId), botKey).insertOrUpdate(accountContext);
    }

    public Integer insertOrUpdateBatch(Integer botId, String botKey, List<AccountContext> accountContexts) throws SQLException {
        return getRPCInstance("", String.valueOf(botId), botKey).insertOrUpdateBatch(accountContexts);
    }

    public PageResult<AccountContext> conditionPageQuery(
            Integer botId, String botKey, int page, int limit, String params, Map<String, Object> filterMap
    ) throws SQLException {
        return getRPCInstance("", String.valueOf(botId), botKey).conditionPageQuery(page, limit, params, filterMap);
    }

    public PageResult<AccountContext> conditionPageQuery(
            Integer botId, String botKey, int page, int limit, Map<String, Object> filterMap
    ) throws SQLException {
        return getRPCInstance("", String.valueOf(botId), botKey).conditionPageQuery(page, limit, filterMap);
    }

    public List<AccountContext> conditionQuery(Integer botId, String botKey, Map<String, Object> filterMap) throws SQLException {
        return getRPCInstance("", String.valueOf(botId), botKey).conditionQuery(filterMap);
    }

    public List<AccountContext> conditionQuery(Integer botId, String botKey, String params, Map<String, Object> filterMap) throws SQLException {
        return getRPCInstance("", String.valueOf(botId), botKey).conditionQuery(params, filterMap);
    }

    public AccountContext queryById(Integer botId, String botKey, Serializable id) {
        return getRPCInstance("", String.valueOf(botId), botKey).queryById(id);
    }

    public Boolean delete(Integer botId, String botKey, List<Integer> ids) {
        return getRPCInstance("", String.valueOf(botId), botKey).delete(ids);
    }

    public Integer importFromExcel(Integer botId, String botKey, String fileBotConfigPath) throws SQLException {
        return getRPCInstance("", String.valueOf(botId), botKey).importFromExcel(fileBotConfigPath);
    }

    public Integer importFromRaw(Integer botId, String botKey, List<Map<String, Object>> rawLines) throws SQLException {
        return getRPCInstance("", String.valueOf(botId), botKey).importFromRaw(rawLines);
    }
}
