package cn.com.helei.bot_platform.service.rpc;

import cn.com.helei.bot_platform.service.DynamicDubboRPCService;
import cn.com.helei.common.dto.PageResult;
import cn.com.helei.common.entity.RewordInfo;
import cn.com.helei.rpc.bot.IRewordInfoRPC;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Service
public class RewordInfoServiceRPC extends DynamicDubboRPCService<IRewordInfoRPC> {
    public RewordInfoServiceRPC() {
        super(IRewordInfoRPC.class);
    }

    public Integer insertOrUpdate(Integer botId, String botKey, RewordInfo rewordInfo) throws SQLException {
        return getRPCInstance(String.valueOf(botId), botKey).insertOrUpdate(rewordInfo);
    }

    public Integer insertOrUpdateBatch(Integer botId, String botKey, List<RewordInfo> rewordInfos) throws SQLException {
        return getRPCInstance(String.valueOf(botId), botKey).insertOrUpdateBatch(rewordInfos);
    }

    public PageResult<RewordInfo> conditionPageQuery(
            Integer botId, String botKey, int page, int limit, String params, Map<String, Object> filterMap
    ) throws SQLException {
        return getRPCInstance(String.valueOf(botId), botKey).conditionPageQuery(page, limit, params, filterMap);
    }

    public PageResult<RewordInfo> conditionPageQuery(
            Integer botId, String botKey, int page, int limit, Map<String, Object> filterMap
    ) throws SQLException {
        return getRPCInstance(String.valueOf(botId), botKey).conditionPageQuery(page, limit, filterMap);
    }

    public List<RewordInfo> conditionQuery(Integer botId, String botKey, Map<String, Object> filterMap) throws SQLException {
        return getRPCInstance(String.valueOf(botId), botKey).conditionQuery(filterMap);
    }

    public List<RewordInfo> conditionQuery(Integer botId, String botKey, String params, Map<String, Object> filterMap) throws SQLException {
        return getRPCInstance(String.valueOf(botId), botKey).conditionQuery(params, filterMap);
    }

    public RewordInfo queryById(Integer botId, String botKey, Serializable id) {
        return getRPCInstance(String.valueOf(botId), botKey).queryById(id);
    }

    public Boolean delete(Integer botId, String botKey, List<Integer> ids) {
        return getRPCInstance(String.valueOf(botId), botKey).delete(ids);
    }
}
