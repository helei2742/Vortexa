package cn.com.vortexa.rpc.api.platform;

import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.entity.TwitterAccount;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author helei
 * @since 2025/3/21 16:44
 */
public interface ITwitterAccountRPC {
    List<TwitterAccount> batchQueryByIdsRPC(List<Serializable> ids);

    TwitterAccount queryByIdRPC(Serializable id);

    PageResult<TwitterAccount> conditionPageQueryRPC(int page, int limit, Map<String, Object> filterMap) throws
            SQLException;
}
