package cn.com.vortexa.control.processor;

import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.dto.RequestHandleResult;
import cn.com.vortexa.control.dto.RPCResultWrapper;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.handler.CustomRequestHandler;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import cn.hutool.core.util.StrUtil;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static cn.com.vortexa.control.constant.ExtFieldsConstants.CUSTOM_COMMAND_HANDLER_KEY;

/**
 * @author helei
 * @since 2025/3/20 11:02
 */
@Slf4j
public class CustomCommandProcessor {

    private final ConcurrentMap<String, CustomRequestHandler> customCMDHandlerMap = new ConcurrentHashMap<>();

    /**
     * 添加自定义命令处理器
     *
     * @param commandKey           commandKey
     * @param customRequestHandler customRequestHandler
     * @throws CustomCommandException CustomCommandException
     */
    public void addCustomCommandHandler(String commandKey, CustomRequestHandler customRequestHandler)
            throws CustomCommandException {
        if (checkCommandKey(commandKey)) {
            throw new CustomCommandException("illegal command key " + commandKey);
        }

        if (customCMDHandlerMap.putIfAbsent(commandKey, customRequestHandler) != null) {
            throw new CustomCommandException("command [%s] exist".formatted(commandKey));
        } else {
            log.debug("custom command [{}] added", commandKey);
        }
    }

    /**
     * 运行自定义命令处理器
     *
     * @param channel channel
     * @param request request
     * @return RemotingCommand
     */
    public RemotingCommand tryInvokeCustomCommandHandler(Channel channel, RemotingCommand request) throws CustomCommandException {
        String commandKey = request.getExtFieldsValue(CUSTOM_COMMAND_HANDLER_KEY);
        String clientName = channel.attr(NettyConstants.CLIENT_NAME).get();
        CustomRequestHandler handler = null;

        if (StrUtil.isBlank(commandKey) || (handler = customCMDHandlerMap.get(commandKey)) == null) {
            throw new CustomCommandException("custom request[%s] didn't exist".formatted(CUSTOM_COMMAND_HANDLER_KEY));
        }
        log.debug("client[{}] start invoke [{}]", clientName, handler);

        RemotingCommand response = new RemotingCommand();
        response.setTransactionId(request.getTransactionId());
        response.setFlag(-1 * request.getFlag());

        // 执行注册的回调方法
        RequestHandleResult result = handler.handlerRequest(request);

        if (result == null || !result.getSuccess()) {
            response.setCode(RemotingCommandCodeConstants.FAIL);
        } else {
            response.setCode(RemotingCommandCodeConstants.SUCCESS);
            response.setBody(Serializer.Algorithm.JDK.serialize(
                    new RPCResultWrapper<>(result.getData(), null)
            ));
        }
        return response;
    }

    /**
     * 检查命令flag
     *
     * @param commandKey commandKey
     * @return boolean
     */
    private static boolean checkCommandKey(String commandKey) {
        return StrUtil.isBlank(commandKey);
    }
}
