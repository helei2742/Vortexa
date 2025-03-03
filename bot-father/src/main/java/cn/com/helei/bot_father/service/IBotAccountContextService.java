package cn.com.helei.bot_father.service;

import cn.com.helei.common.dto.Result;
import cn.com.helei.common.entity.AccountContext;
import cn.com.helei.db_layer.service.IBaseService;
import cn.com.helei.db_layer.service.ImportService;
import com.baomidou.mybatisplus.extension.service.IService;

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
public interface IBotAccountContextService extends IService<AccountContext>, IBaseService<AccountContext>, ImportService {

    Result saveBotAccountContext(Integer botId, String botKey, List<Map<String, Object>> acKVMap);

    Boolean checkAndCreateShardedTable(Integer botId, String botKey, boolean existUpdate) throws SQLException;

}
