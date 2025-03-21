package cn.com.vortexa.bot_platform.config;

import cn.com.vortexa.rpc.anno.RPCMethod;
import cn.com.vortexa.rpc.anno.RPCService;
import cn.com.vortexa.rpc.dto.RPCMethodInfo;
import jakarta.annotation.PostConstruct;
import lombok.Getter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author helei
 * @since 2025-03-21
 */
@Slf4j
@Getter
@Component
public class RPCProviderScanner {

    private static final String basePackage = "cn.com.vortexa.bot_platform.service.impl"; // 指定扫描包

    @Autowired
    private ApplicationContext applicationContext;

    private final Map<String, RPCMethodInfo> providerInfoMap = new HashMap<>();

    @PostConstruct
    public void scanRPCProviders() {
        // 1. 创建扫描器，只扫描 @RPCService 注解的类
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RPCService.class));

        Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(basePackage);

        for (BeanDefinition beanDefinition : beanDefinitions) {
            try {
                // 3. 获取类名，并实例化对象
                String className = beanDefinition.getBeanClassName();
                if (className == null) continue;

                Class<?> clazz = Class.forName(className);
                Object bean = applicationContext.getBean(clazz);

                // 4. 存储到 RPC 服务提供者 Map（按类名注册）
                // 将 Bean、方法和code信息封装到 RPCMethodInfo 对象中
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(RPCMethod.class)) {
                        RPCMethodInfo rpcMethodInfo = RPCMethodInfo.generate(className, bean, method);
                        providerInfoMap.put(rpcMethodInfo.getRpcMethodKey(), rpcMethodInfo);
                    }
                }
                log.info("注册 RPC 服务: {}", clazz.getName());
            } catch (Exception e) {
                log.error("注册 RPC 服务失败: {}", beanDefinition.getBeanClassName(), e);
            }
        }
    }
}
