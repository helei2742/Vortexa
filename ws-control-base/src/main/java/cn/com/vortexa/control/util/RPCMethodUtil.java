package cn.com.vortexa.control.util;


import cn.com.vortexa.control.constant.ExtFieldsConstants;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.control.dto.RPCArgsWrapper;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.common.util.protocol.Serializer;

import java.lang.reflect.Method;

/**
 * @author helei
 * @since 2025-03-22
 */
public class RPCMethodUtil {

    public static String buildRpcMethodKey(String className, Method method) {
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

    public static RemotingCommand buildRPCRequest(
            String txId,
            String className,
            Method method,
            Object[] args
    ) {
        String key = buildRpcMethodKey(className, method);

        RemotingCommand remotingCommand = new RemotingCommand();
        remotingCommand.setFlag(RemotingCommandFlagConstants.CUSTOM_COMMAND);
        remotingCommand.setCode(RemotingCommandCodeConstants.SUCCESS);
        remotingCommand.setTransactionId(txId);

        remotingCommand.addExtField(
                ExtFieldsConstants.CUSTOM_COMMAND_HANDLER_KEY,
                key
        );

        remotingCommand.setBody(Serializer.Algorithm.JDK.serialize(
                new RPCArgsWrapper(args)
        ));

        return remotingCommand;
    }
}
