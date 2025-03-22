package cn.com.vortexa.rpc.api.platform;

import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.entity.DiscordAccount;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author helei
 * @since 2025/3/21 16:47
 */
public interface IDiscordAccountRPC {

    DiscordAccount queryByIdRPC(Serializable id);

    PageResult<DiscordAccount> conditionPageQueryRPC(int page, int limit, Map<String, Object> filterMap) throws SQLException;
}
