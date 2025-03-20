package cn.com.vortexa.nameserver.processor;

import cn.com.vortexa.nameserver.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.dto.RequestHandleResult;
import cn.com.vortexa.nameserver.exception.CustomCommandException;
import cn.com.vortexa.nameserver.handler.CustomRequestHandler;
import cn.com.vortexa.nameserver.protocol.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author helei
 * @since 2025/3/20 11:02
 */
@Slf4j
public class CustomCommandProcessor {

    private final ConcurrentMap<Integer, CustomRequestHandler> customCMDHandlerMap = new ConcurrentHashMap<>();

    /**
     * 添加自定义命令处理器
     *
     * @param commandFlag commandFlag
     * @param customRequestHandler customRequestHandler
     * @throws CustomCommandException CustomCommandException
     */
    public void addCustomCommandHandler(Integer commandFlag, CustomRequestHandler customRequestHandler)
            throws CustomCommandException {
        if (!checkCommandFlag(commandFlag)) {
            throw new CustomCommandException("illegal command flag " + commandFlag);
        }

        if (customCMDHandlerMap.putIfAbsent(commandFlag, customRequestHandler) != null) {
            throw new CustomCommandException("command [%s] exist".formatted(commandFlag));
        } else {
            log.info("custom command [{}] added", commandFlag);
        }
    }

    /**
     * 运行自定义命令处理器
     *
     * @param request request
     * @return RemotingCommand
     */
    public RemotingCommand tryInvokeCustomCommandHandler(RemotingCommand request) throws CustomCommandException {
        CustomRequestHandler handler = customCMDHandlerMap.get(request.getFlag());

        if (handler == null) {
            throw new CustomCommandException("custom request[%s] didn't exist".formatted(request.getFlag()));
        }

        RemotingCommand response = new RemotingCommand();
        response.setTransactionId(request.getTransactionId());
        response.setFlag(-1 * request.getFlag());

        // 执行注册的回调方法
        RequestHandleResult result = handler.handlerRequest(request);

        if (result == null || !result.getSuccess()) {
            response.setCode(RemotingCommandCodeConstants.FAIL);
        } else {
            response.setCode(RemotingCommandCodeConstants.SUCCESS);
            response.setBody(
                    Serializer.Algorithm.Protostuff.serialize(result.getData())
            );
        }

        return response;
    }

    /**
     * 检查命令flag
     *
     * @param commandFlag commandFlag
     * @return boolean
     */
    private static boolean checkCommandFlag(Integer commandFlag) {
        if (commandFlag == null || commandFlag <= 0 || commandFlag < 300) {
            return false;
        }

        int start = commandFlag;
        while (commandFlag > 10) {
            commandFlag = commandFlag / 10;
            start = commandFlag;
        }

        return start == 3;
    }

    public static void main(String[] args) {
        System.out.println(checkCommandFlag(3001));
    }
}
