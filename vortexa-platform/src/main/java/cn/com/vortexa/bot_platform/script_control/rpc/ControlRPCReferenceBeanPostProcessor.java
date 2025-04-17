package cn.com.vortexa.bot_platform.script_control.rpc;

import cn.com.vortexa.control_server.BotControlServer;
import cn.com.vortexa.control.anno.RPCReference;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
@ConditionalOnBean(BotControlServer.class)
public class ControlRPCReferenceBeanPostProcessor implements BeanPostProcessor {

    @Autowired
    private BotControlServer botControlServer;

    @Override
    public Object postProcessBeforeInitialization(Object bean, @NotNull String beanName) throws BeansException {
        // 获取 Bean 的所有字段
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(RPCReference.class)) {
                // 生成代理对象
                Object proxy = ControlServerRPCProxyFactory.createProxy(
                        field.getType(),
                        botControlServer
                );

                // 设置访问权限，允许修改 private 字段
                field.setAccessible(true);
                try {
                    field.set(bean, proxy); // 注入代理对象
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("autowired RPC proxy error", e);
                }
            }
        }
        return bean;
    }
}
