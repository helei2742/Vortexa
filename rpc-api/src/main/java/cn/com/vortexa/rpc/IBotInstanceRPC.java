package cn.com.vortexa.rpc;

import cn.com.vortexa.common.entity.BotInstance;

import java.sql.SQLException;

/**
 * @author helei
 * @since 2025/3/21 17:11
 */
public interface IBotInstanceRPC {

    Boolean existsBotInstanceRPC(BotInstance query);

    Integer insertOrUpdateRPC(BotInstance instance) throws SQLException;

}
