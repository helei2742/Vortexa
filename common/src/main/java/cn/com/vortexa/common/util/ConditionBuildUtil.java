package cn.com.vortexa.common.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ConditionBuildUtil {

    private final static Map<Class<?>, Map<String, Field>> classFieldMap = new ConcurrentHashMap<>();


    public static <T> T getMapContainsCondition(
            Map<String, Object> filterMap,
            String mapParamsFieldName,
            Class<T> tClass
    ) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<T> constructor = tClass.getConstructor();
        T condition = constructor.newInstance();

        if (filterMap == null || filterMap.isEmpty()) {
            return condition;
        }

        Map<String, Field> fields = getClassFieldMap(tClass);
        if (!fields.containsKey(mapParamsFieldName)) {
            throw new IllegalAccessException("目标类字段没有[" + mapParamsFieldName + "]");
        }

        for (Map.Entry<String, Object> entry : filterMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (fields.containsKey(key)) {
                // 是原生字段
                Field field = fields.get(key);
                field.setAccessible(true);
                try {
                    field.set(condition, value);
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessException("参数类型[%s]值[%s]设置错误".formatted(key, value));
                }
            } else {
                // 是params里的字段
                Field field = fields.get(mapParamsFieldName);
                field.setAccessible(true);
                Object obj = field.get(condition);
                Map<String, Object> map = (Map<String, Object>) obj;
                map.put(key, value);
            }
        }

        return condition;
    }

    private static Map<String, Field> getClassFieldMap(Class<?> tClass) {
        return classFieldMap.compute(tClass, (k, v) -> {
            if (v == null) {
                v = Arrays.stream(tClass.getDeclaredFields())
                        .collect(Collectors.toMap(Field::getName, field -> field));
            }
            return v;
        });
    }
}
