package cn.com.vortexa.rpc.api.bot;

import cn.com.vortexa.common.dto.Result;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
public interface IBotAccountRPC{

    Result saveBotAccountContext(Integer botId, String botKey, List<Map<String, Object>> acKVMap);

    Boolean checkAndCreateShardedTable(Integer botId, String botKey, boolean existUpdate) throws SQLException;

}
