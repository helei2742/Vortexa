package cn.com.vortexa.script_node.service;

import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.db_layer.service.IBaseService;
import cn.com.vortexa.db_layer.service.ImportService;
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
public interface IBotAccountContextService extends IService<AccountContext>, IBaseService<AccountContext> {

    Result saveBotAccountContext(Integer botId, String botKey, List<Map<String, Object>> acKVMap);

    Boolean checkAndCreateShardedTable(Integer botId, String botKey, boolean existUpdate) throws SQLException;

    Integer importFromExcel(Integer botId, String botKey, String fileBotConfigPath) throws SQLException;

    Integer importFromRaw(Integer botId, String botKey, List<Map<String, Object>> rawLines) throws SQLException;
}
