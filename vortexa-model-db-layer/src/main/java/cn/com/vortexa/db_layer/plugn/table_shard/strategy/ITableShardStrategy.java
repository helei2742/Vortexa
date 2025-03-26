package cn.com.vortexa.db_layer.plugn.table_shard.strategy;

import org.apache.commons.lang3.ObjectUtils;

public interface ITableShardStrategy {

    /**
     * 生成分表名
     *
     * @param tableNamePrefix 表前缀名
     * @param value           值
     * @return String
     */
    String generateTableName(String tableNamePrefix, Object[] value);

    /**
     * 验证tableNamePrefix
     *
     * @param tableNamePrefix 表前缀名
     */
    default void verificationTableNamePrefix(String tableNamePrefix) {
        if (ObjectUtils.isEmpty(tableNamePrefix)) {
            throw new RuntimeException("tableNamePrefix is null");
        }
    }
}
