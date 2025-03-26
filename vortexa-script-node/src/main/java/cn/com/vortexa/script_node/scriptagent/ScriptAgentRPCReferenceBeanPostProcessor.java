package cn.com.vortexa.script_node.scriptagent;

import cn.com.vortexa.control.ScriptAgent;
import cn.com.vortexa.control.anno.RPCReference;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
@ConditionalOnBean(ScriptAgent.class)
public class ScriptAgentRPCReferenceBeanPostProcessor implements BeanPostProcessor {

    @Autowired
    private ScriptAgent scriptAgent;

    @Override
    public Object postProcessBeforeInitialization(Object bean, @NotNull String beanName) throws BeansException {
        // 获取 Bean 的所有字段
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(RPCReference.class)) {
                // 生成代理对象
                Object proxy = ScriptAgentRPCProxyFactory.createProxy(
                        field.getType(),
                        scriptAgent
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
