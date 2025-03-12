package cn.com.vortexa.bot_father.mapper;

import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.db_layer.mapper.IBaseMapper;
import cn.com.vortexa.db_layer.plugn.table_shard.TableShard;
import cn.com.vortexa.db_layer.plugn.table_shard.strategy.BotIdBasedTableShardStrategy;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import static cn.com.vortexa.bot_father.service.impl.BotAccountContextServiceImpl.BOT_ACCOUNT_CONTEXT_TABLE_PREFIX;


/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author com.helei
 * @since 2025-02-05
 */
@TableShard(
        tableNamePrefix = BOT_ACCOUNT_CONTEXT_TABLE_PREFIX,
        values = {"botId", "botKey"},
        fieldFlag = true,
        shardStrategy = BotIdBasedTableShardStrategy.class,
        targetClass = AccountContext.class
)
public interface BotAccountContextMapper extends IBaseMapper<AccountContext> {

    Boolean createIfTableNotExist(@Param("botId") Integer botId, @Param("botKey") String botKey);

    List<String> queryBotAccountTableNames(@Param("botId") Integer botId);
}
