package cn.com.vortexa.db_layer.plugn.table_shard.strategy;

import java.util.Arrays;


public class BotIdBasedTableShardStrategy implements ITableShardStrategy {
    @Override
    public String generateTableName(String tableNamePrefix, Object[] value) {
        verificationTableNamePrefix(tableNamePrefix);

        if (value == null || value.length != 2 || value[0] == null || value[1] == null) {
            throw new IllegalArgumentException("table shard params[%s] illegal".formatted(Arrays.toString(value)));
        }

        long bot_id = Long.parseLong(value[0].toString());
        String bot_key = (String) value[1];

        // 特殊字段过滤
        bot_key = bot_key.replace("-", "_");

        //此处可以缓存优化
        return tableNamePrefix + "_" + bot_id + "_" + bot_key;
    }
}
