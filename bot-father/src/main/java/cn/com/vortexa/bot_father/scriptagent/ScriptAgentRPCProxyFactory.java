package cn.com.vortexa.bot_father.scriptagent;

import cn.com.vortexa.control.ScriptAgent;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.dto.ResultWrapper;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.rpc.RPCException;
import cn.com.vortexa.rpc.util.RPCMethodUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class ScriptAgentRPCProxyFactory {

    private static final ConcurrentMap<Class<?>, Object> referenceMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> interfaceClass, ScriptAgent scriptAgent) {
        return (T) referenceMap.compute(interfaceClass, (k, v) -> {
            if (v == null) {
                v = Proxy.newProxyInstance(
                        interfaceClass.getClassLoader(),
                        new Class<?>[]{interfaceClass},
                        new RPCInvocationHandler(interfaceClass, scriptAgent)
                );
            }
            return v;
        });
    }

    private static class RPCInvocationHandler implements InvocationHandler {
        private final Class<?> interfaceClass;
        private final ScriptAgent scriptAgent;
        private final Set<Method> rpcMethods;

        public RPCInvocationHandler(Class<?> interfaceClass, ScriptAgent scriptAgent) {
            this.interfaceClass = interfaceClass;
            this.scriptAgent = scriptAgent;
            this.rpcMethods = Set.of(interfaceClass.getDeclaredMethods());
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (rpcMethods.contains(method)) {
                try {
                    RemotingCommand request = RPCMethodUtil.buildRPCRequest(
                            scriptAgent.nextTxId(),
                            interfaceClass.getName(),
                            method,
                            args
                    );

                    RemotingCommand response = scriptAgent.sendRequest(request).get();
                    byte[] body = response.getBody();

                    if (response.getCode() == RemotingCommandCodeConstants.SUCCESS) {
                        if (method.getReturnType() != void.class) {
                            ResultWrapper resultWrapper = Serializer.Algorithm.JSON.deserialize(body, ResultWrapper.class);
                            log.debug("rpc [{}-{}] got result [{}]",
                                    interfaceClass.getName(), method.getName(), resultWrapper.getResult());
                            return resultWrapper.getResult();
                        }
                    } else if (response.getCode() == RemotingCommandCodeConstants.FAIL) {
                        String errorMsg = "rpc [%s-%s] error".formatted(interfaceClass.getName(), method.getName());
                        Exception exception = Serializer.Algorithm.Protostuff.deserialize(body, Exception.class);
                        throw new RPCException(errorMsg, exception);
                    } else {
                        return null;
                    }
                } catch (Exception e) {
                    String errorMsg = "rpc [%s-%s] error".formatted(interfaceClass.getName(), method.getName());
                    throw new RPCException(errorMsg, e);
                }
            }

            return method.invoke(proxy, args);
        }
    }
}
