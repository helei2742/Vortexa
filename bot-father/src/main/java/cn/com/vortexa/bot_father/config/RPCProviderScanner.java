package cn.com.vortexa.bot_father.config;

import cn.com.vortexa.rpc.anno.RPCMethod;
import cn.com.vortexa.rpc.dto.RPCMethodInfo;
import jakarta.annotation.PostConstruct;
import lombok.Getter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author helei
 * @since 2025-03-21
 */
@Getter
@Component
public class RPCProviderScanner {

    @Autowired
    private ApplicationContext applicationContext;

    private final Map<Integer, RPCMethodInfo> providerInfoMap = new HashMap<>();

    @PostConstruct
    public void scanRPCProviders() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        // 遍历所有 Bean，查找带有 @RPCProvider 注解的方法
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Method[] methods = bean.getClass().getDeclaredMethods();

            for (Method method : methods) {
                if (method.isAnnotationPresent(RPCMethod.class)) {
                    RPCMethod annotation = method.getAnnotation(RPCMethod.class);
                    int code = annotation.code();

                    // 将 Bean、方法和code信息封装到 RPCMethodInfo 对象中
                    RPCMethodInfo rpcMethodInfo = new RPCMethodInfo(code, bean, method);
                    providerInfoMap.put(code, rpcMethodInfo);
                }
            }
        }
    }
}
