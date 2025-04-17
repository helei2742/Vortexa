package cn.com.vortexa.bot_platform.script_control.rpc;

import cn.com.vortexa.control_server.BotControlServer;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.dto.RPCResultWrapper;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.control.exception.RPCException;
import cn.com.vortexa.common.util.protocol.Serializer;
import cn.com.vortexa.control.util.RPCMethodUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Slf4j
public class ControlServerRPCProxyFactory {

    private static final ConcurrentMap<Class<?>, Object> referenceMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> interfaceClass, BotControlServer botControlServer) {
        return (T) referenceMap.compute(interfaceClass, (k, v) -> {
            if (v == null) {
                v = Proxy.newProxyInstance(
                        interfaceClass.getClassLoader(),
                        new Class<?>[]{interfaceClass},
                        new RPCInvocationHandler(interfaceClass, botControlServer)
                );
            }
            return v;
        });
    }

    private static class RPCInvocationHandler implements InvocationHandler {
        private final Class<?> interfaceClass;
        private final BotControlServer botControlServer;
        private final Set<Method> rpcMethods;

        public RPCInvocationHandler(Class<?> interfaceClass, BotControlServer botControlServer) {
            this.interfaceClass = interfaceClass;
            this.botControlServer = botControlServer;
            this.rpcMethods = Set.of(interfaceClass.getDeclaredMethods());
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (rpcMethods.contains(method)) {
                try {
                    // Step 1 交易方法参数
                    if (args.length < 1 || args[0].getClass() != ServiceInstance.class) {
                        throw new RPCException("rpc method[%s.%s] first param must be ServiceInstance(target service instance)".formatted(
                                interfaceClass.getName(), method.getName()
                        ));
                    }
                    ServiceInstance serviceInstance = (ServiceInstance) args[0];

                    // Step 2 构建请求命令
                    RemotingCommand request = RPCMethodUtil.buildRPCRequest(
                            botControlServer.nextTxId(),
                            interfaceClass.getName(),
                            method,
                            args
                    );

                    log.debug("send RPC request[{}][{}]", request.getTransactionId(), method.getName());

                    // Step 3 发送请求，等待响应
                    RemotingCommand response = botControlServer.sendCommandToServiceInstance(
                            serviceInstance.getGroupId(),
                            serviceInstance.getServiceId(),
                            serviceInstance.getInstanceId(),
                            request
                    ).get();

                    // Step 4 解析响应结果
                    byte[] body = response.getBody();
                    if (response.getCode() == RemotingCommandCodeConstants.SUCCESS) {
                        if (method.getReturnType() != void.class) {
                            RPCResultWrapper<?> RPCResultWrapper = Serializer.Algorithm.JDK.deserialize(body, RPCResultWrapper.class);
                            log.debug("rpc [{}-{}] got result [{}]",
                                    interfaceClass.getName(), method.getName(), RPCResultWrapper.getResult());
                            return RPCResultWrapper.getResult();
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
            }

            return method.invoke(proxy, args);
        }
    }
}
