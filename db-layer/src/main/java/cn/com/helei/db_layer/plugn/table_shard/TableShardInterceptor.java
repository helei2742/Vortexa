package cn.com.helei.db_layer.plugn.table_shard;

import cn.com.helei.db_layer.plugn.table_shard.strategy.ITableShardStrategy;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.HashMap;

@Intercepts({
        @Signature(
                type = StatementHandler.class,
                method = "prepare",
                args = {Connection.class, Integer.class}
        )
})
public class TableShardInterceptor implements Interceptor {

    private static final ReflectorFactory defaultReflectorFactory = new DefaultReflectorFactory();

    private final ITableShardStrategy tableShardStrategy;

    public TableShardInterceptor(ITableShardStrategy tableShardStrategy) {
        this.tableShardStrategy = tableShardStrategy;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        // MetaObject是mybatis里面提供的一个工具类，类似反射的效果
        MetaObject metaObject = getMetaObject(invocation);
        BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

        // 获取Mapper执行方法
        Method method = invocation.getMethod();

        // 获取分表注解
        TableShard tableShard = getTableShard(method, mappedStatement);

        // 如果method与class都没有TableShard注解或执行方法不存在，执行下一个插件逻辑
        if (tableShard == null) {
            return invocation.proceed();
        }

        //获取值
        String[] values = tableShard.values();
        //value是否字段名，如果是，需要解析请求参数字段名的值
        boolean fieldFlag = tableShard.fieldFlag();

        Class<?> targetClass = tableShard.targetClass();

        if (fieldFlag) {
            //获取请求参数
            Object parameterObject = boundSql.getParameterObject();
            Object[] valueObjects = new Object[values.length];

            for (int i = 0; i < values.length; i++) {
                String value = values[i];

                // 1 传入QueryWrapper的情况
                if (parameterObject instanceof MapperMethod.ParamMap paramMap) {
                    // 根据字段名获取参数值
                    valueObjects[i] = resolveValueObject(paramMap, boundSql, value, tableShard);
                } else if (targetClass.isInstance(parameterObject)) {
                    // 2 传入目标对象当query
                    Field field = targetClass.getDeclaredField(value);
                    field.setAccessible(true);
                    valueObjects[i] = field.get(parameterObject);
                } else if (parameterObject instanceof HashMap map) {

                    valueObjects[i] = map.get(value);
                }
            }

            // 替换表名
            replaceSql(tableShard, valueObjects, metaObject, boundSql);
        }
        //执行下一个插件逻辑
        return invocation.proceed();
    }

    private static Object resolveValueObject(MapperMethod.ParamMap paramMap, BoundSql boundSql, String value, TableShard tableShard) throws NoSuchFieldException, IllegalAccessException {
        if (boundSql.getParameterMappings().isEmpty()) {
            return paramMap.get(value);
        }

        for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
            String property = parameterMapping.getProperty();
            if (property.endsWith("." + value)) {
                String[] split = property.split("\\.");
                Object o = paramMap.get(split[0]);

                if (o instanceof QueryWrapper<?> queryWrapper) {
                    Object entity = queryWrapper.getEntity();
                    Class<?> targetClass = tableShard.targetClass();
                    if (targetClass.isInstance(entity)) {
                        Field field = targetClass.getDeclaredField(value);
                        field.setAccessible(true);
                        return field.get(entity);
                    }
                }
                break;
            }
        }
        return null;
    }

    /**
     * @param target target
     * @return Object
     */
    @Override
    public Object plugin(Object target) {
        // 当目标类是StatementHandler类型时，才包装目标类，否者直接返回目标本身, 减少目标被代理的次数
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
    }

    /**
     * 基本数据类型验证，true是，false否
     *
     * @param object object
     * @return boolean
     */
    private static boolean isBaseType(Object object) {
        return object.getClass().isPrimitive()
                || object instanceof String
                || object instanceof Integer
                || object instanceof Double
                || object instanceof Float
                || object instanceof Long
                || object instanceof Boolean
                || object instanceof Byte
                || object instanceof Short;
    }

    /**
     * @param tableShard 分表注解
     * @param values     值
     * @param metaObject mybatis反射对象
     * @param boundSql   sql信息对象
     */
    private void replaceSql(TableShard tableShard, Object[] values, MetaObject metaObject, BoundSql boundSql) {
        String tableNamePrefix = tableShard.tableNamePrefix();
        // 获取策略class
        Class<? extends ITableShardStrategy> strategyClazz = tableShard.shardStrategy();
        // 生成分表名
        String shardTableName = tableShardStrategy.generateTableName(tableNamePrefix, values);
        // 获取sql
        String sql = boundSql.getSql();
        // 完成表名替换
        metaObject.setValue("delegate.boundSql.sql", sql.replaceAll(tableNamePrefix, shardTableName));
    }

    /**
     * 获取MetaObject对象-mybatis里面提供的一个工具类，类似反射的效果
     *
     * @param invocation invocation
     * @return MetaObject
     */
    private MetaObject getMetaObject(Invocation invocation) {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        // MetaObject是mybatis里面提供的一个工具类，类似反射的效果

        return MetaObject.forObject(statementHandler,
                SystemMetaObject.DEFAULT_OBJECT_FACTORY,
                SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY,
                defaultReflectorFactory
        );
    }

    /**
     * 获取分表注解
     *
     * @param method          method
     * @param mappedStatement mappedStatement
     * @return TableShard
     */
    private TableShard getTableShard(Method method, MappedStatement mappedStatement) throws ClassNotFoundException {
        String id = mappedStatement.getId();
        // 获取Class
        final String className = id.substring(0, id.lastIndexOf("."));
        // 分表注解
        TableShard tableShard = null;
        // 获取Mapper执行方法的TableShard注解
        tableShard = method.getAnnotation(TableShard.class);
        // 如果方法没有设置注解，从Mapper接口上面获取TableShard注解
        if (tableShard == null) {
            // 获取TableShard注解
            tableShard = Class.forName(className).getAnnotation(TableShard.class);
        }
        return tableShard;
    }
}
