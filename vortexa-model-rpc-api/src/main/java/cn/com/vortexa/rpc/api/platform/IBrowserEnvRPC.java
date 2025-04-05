package cn.com.vortexa.rpc.api.platform;

import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.entity.BrowserEnv;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author helei
 * @since 2025/3/21 16:47
 */
public interface IBrowserEnvRPC {
    List<BrowserEnv> batchQueryByIdsRPC(List<Serializable> ids);

    BrowserEnv queryByIdRPC(Serializable id);

    PageResult<BrowserEnv> conditionPageQueryRPC(int page, int limit, Map<String, Object> filterMap)
            throws SQLException;
}
