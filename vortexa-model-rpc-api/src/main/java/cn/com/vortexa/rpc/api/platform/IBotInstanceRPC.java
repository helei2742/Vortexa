package cn.com.vortexa.rpc.api.platform;

import cn.com.vortexa.common.entity.BotInstance;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

/**
 * @author helei
 * @since 2025/3/21 17:11
 */
public interface IBotInstanceRPC {

    List<BotInstance> batchQueryByIdsRPC(List<Serializable> ids);

    Boolean existsBotInstanceRPC(BotInstance query);

    Integer insertOrUpdateRPC(BotInstance instance) throws SQLException;

    BotInstance selectOneRPC(BotInstance query);
}
