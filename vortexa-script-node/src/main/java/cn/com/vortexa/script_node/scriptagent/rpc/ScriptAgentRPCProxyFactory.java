package cn.com.vortexa.script_node.scriptagent.rpc;

import cn.com.vortexa.script_agent.ScriptAgent;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.dto.RPCResultWrapper;
import cn.com.vortexa.common.util.protocol.Serializer;
import cn.com.vortexa.control.exception.RPCException;
import cn.com.vortexa.control.util.RPCMethodUtil;
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
                    log.debug("send RPC request[{}][{}]", request.getTransactionId(), method.getName());

                    RemotingCommand response = scriptAgent.sendRequest(request).get();
                    byte[] body = response.getBody();

                    if (response.getCode() == RemotingCommandCodeConstants.SUCCESS) {
                        if (method.getReturnType() != void.class) {
                            RPCResultWrapper<?> RPCResultWrapper = Serializer.Algorithm.JDK.deserialize(body, RPCResultWrapper.class);
                            log.debug("rpc [{}-{}] got result [{}]",
                                    interfaceClass.getName(), method.getName(), RPCResultWrapper.getResult());
                            return RPCResultWrapper.getResult();
                        } else {
                            return null;
                        }
                    } else if (response.getCode() == RemotingCommandCodeConstants.FAIL) {
                        String errorMsg = "rpc [%s-%s] error".formatted(interfaceClass.getName(), method.getName());
                        RPCResultWrapper<?> RPCResultWrapper = Serializer.Algorithm.JDK.deserialize(body, RPCResultWrapper.class);
                        throw new RPCException(errorMsg, RPCResultWrapper.getException());
                    } else {
                        return null;
                    }
                } catch (Exception e) {
                    String errorMsg = "rpc [%s-%s] error".formatted(interfaceClass.getName(), method.getName());
                    throw new RPCException(errorMsg, e);
                }
            } else {
                return method.invoke(proxy, args);
            }
        }
    }
}
