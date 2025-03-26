package cn.com.vortexa.db_layer.plugn.table_shard;


import cn.com.vortexa.db_layer.plugn.table_shard.strategy.ITableShardStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = {ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TableShard {

    // 表前缀名
    String tableNamePrefix();

    // 值
    String[] values() default {};

    // 是否是字段名，如果是需要解析请求参数改字段名的值（默认否）
    boolean fieldFlag() default false;


    Class<?> targetClass();

    // 对应的分表策略类
    Class<? extends ITableShardStrategy> shardStrategy();

}
