package cn.com.vortexa.bot_platform.script_control;

import cn.com.vortexa.common.constants.BotRemotingCommandFlagConstants;
import cn.com.vortexa.control.BotControlServer;
import cn.com.vortexa.control.config.ControlServerConfig;
import cn.com.vortexa.control.dto.RPCArgsWrapper;
import cn.com.vortexa.control.dto.RPCServiceInfo;
import cn.com.vortexa.control.dto.RequestHandleResult;
import cn.com.vortexa.control.exception.ControlServerException;
import cn.com.vortexa.control.exception.CustomCommandInvokeException;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.control.service.IConnectionService;
import cn.com.vortexa.control.service.IRegistryService;
import cn.com.vortexa.control.util.RPCMethodUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author helei
 * @since 2025/3/26 15:42
 */
@Slf4j
public class BotPlatformControlServer extends BotControlServer {

    private final List<RPCServiceInfo<?>> rpcServiceInfos;
    private final BotLogUploadService botLogUploadService;

    public BotPlatformControlServer(ControlServerConfig controlServerConfig, List<RPCServiceInfo<?>> rpcServiceInfos) throws ControlServerException {
        super(controlServerConfig);
        this.rpcServiceInfos = rpcServiceInfos;
        this.botLogUploadService = new BotLogUploadService(this);
    }

    @Override
    public void init(IRegistryService registryService, IConnectionService connectionService) throws Exception {
        super.init(registryService, connectionService);

        // 1 RPC 服务
        for (RPCServiceInfo<?> rpcServiceInfo : rpcServiceInfos) {
            Class<?> interfaces = rpcServiceInfo.getInterfaces();

            Object ref = rpcServiceInfo.getRef();

            for (Method method : interfaces.getDeclaredMethods()) {
                method.setAccessible(true);
                String key = RPCMethodUtil.buildRpcMethodKey(interfaces.getName(), method);

                log.info("add custom command handler [{}]", key);

                super.addCustomCommandHandler(key, request -> {
                    RequestHandleResult result = new RequestHandleResult();

                    log.debug("invoke rpc method[{}]", method.getName());
                    try {
                        byte[] body = request.getBody();
                        RPCArgsWrapper params = Serializer.Algorithm.JDK.deserialize(body, RPCArgsWrapper.class);

                        Object invoke = method.invoke(ref, params.getArgs());
                        log.info("invoke rpc[{}] method[{}] return [{}]", request.getTransactionId(), key, invoke);
                        result.setData(invoke);
                        result.setSuccess(true);
                        return result;
                    } catch (Exception e) {
                        log.error("invoke rpc method [{}] error", method.getName());
                        throw new CustomCommandInvokeException(e);
                    }
                });
            }
        }

        // 2 自定义的请求
        // 2.1 浏览器客户端请求获取某个bot日志的命令处理器
        getCustomRemotingCommandHandlerMap().put(
                BotRemotingCommandFlagConstants.START_UP_BOT_LOG,
                botLogUploadService::browserRequestBotLogRCHandler
        );
        // 2.2 bot上传日志命令的处理器
        getCustomRemotingCommandHandlerMap().put(BotRemotingCommandFlagConstants.BOT_RUNTIME_LOG,
                botLogUploadService::botUploadLogRCHandler);
    }
}
