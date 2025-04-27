package cn.com.vortexa.bot_platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import cn.com.vortexa.common.entity.RewordInfo;
import cn.com.vortexa.db_layer.plugn.table_shard.TableShard;
import cn.com.vortexa.db_layer.plugn.table_shard.strategy.BotIdBasedTableShardStrategy;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author helei
 * @since 2025/4/27 15:41
 */
@TableShard(
        tableNamePrefix = RewordInfo.BOT_REWORD_INFO_TABLE_PREFIX,
        values = {"botId", "botKey"},
        fieldFlag = true,
        shardStrategy = BotIdBasedTableShardStrategy.class,
        targetClass = RewordInfo.class
)
public interface RewordInfoMapper extends BaseMapper<RewordInfo> {

    /**
     * 不存在分表则创建
     *
     * @param botId botId
     * @param botKey botKey
     * @return Integer
     */
    Integer createIfTableNotExist(@Param("botId") Integer botId, @Param("botKey") String botKey);

    /**
     * 批量保存
     *
     * @param botId botId
     * @param botKey botKey
     * @param rewordInfos rewordInfos
     * @return int
     */
    int saveBatch(@Param("botId") Integer botId, @Param("botKey") String botKey, @Param("list")List<RewordInfo> rewordInfos);
}
