package cn.com.vortexa.rpc;

import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.entity.ProxyInfo;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author helei
 * @since 2025/3/21 16:46
 */
public interface IProxyInfoRPC {

    ProxyInfo queryByIdRPC(Serializable id);

    PageResult<ProxyInfo> conditionPageQueryRPC(int page, int limit, Map<String, Object> filterMap)
            throws SQLException;
}
