package cn.com.vortexa.rpc.api.platform;

import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.entity.AccountBaseInfo;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author helei
 * @since 2025/3/21 16:44
 */
public interface IAccountBaseInfoRPC {

    AccountBaseInfo queryByIdRPC(Serializable id);

    PageResult<AccountBaseInfo> conditionPageQueryRPC(int page, int limit, Map<String, Object> filterMap)
            throws SQLException;
}
