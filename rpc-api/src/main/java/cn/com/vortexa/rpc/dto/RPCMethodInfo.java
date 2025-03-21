package cn.com.vortexa.rpc.dto;

import cn.com.vortexa.rpc.util.RPCMethodKeyBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;

/**
 * @author helei
 * @since 2025/3/21 11:05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RPCMethodInfo {

    private String rpcMethodKey;

    private Object bean;

    private Method method;

    public static RPCMethodInfo generate(String className, Object bean, Method method) {
        RPCMethodInfo rpcMethodInfo = new RPCMethodInfo();
        rpcMethodInfo.setRpcMethodKey(
                RPCMethodKeyBuilder.build(className, method)
        );
        rpcMethodInfo.setBean(bean);
        rpcMethodInfo.setMethod(method);
        return rpcMethodInfo;
    }
}
