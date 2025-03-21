package cn.com.vortexa.rpc.util;


import java.lang.reflect.Method;

/**
 * @author helei
 * @since 2025-03-22
 */
public class RPCMethodKeyBuilder {

    public static String build(String className, Method method) {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        StringBuilder keyBuilder = new StringBuilder(className);
        keyBuilder.append(".").append(methodName).append("(");
        for (int i = 0; i < parameterTypes.length; i++) {
            keyBuilder.append(parameterTypes[i].getName());
            if (i < parameterTypes.length - 1) {
                keyBuilder.append(",");
            }
        }
        keyBuilder.append(")");
        return keyBuilder.toString();
    }
}
