package cn.com.vortexa.rpc.api.platform;

import cn.com.vortexa.common.entity.BotInfo;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author helei
 * @since 2025/3/21 16:10
 */
public interface IBotInfoRPC {
    List<BotInfo> batchQueryByIdsRPC(List<Serializable> ids);

    Integer insertOrUpdateRPC(BotInfo botInfo) throws SQLException;

    List<BotInfo> conditionQueryRPC(Map<String, Object> filterMap) throws SQLException;
}
