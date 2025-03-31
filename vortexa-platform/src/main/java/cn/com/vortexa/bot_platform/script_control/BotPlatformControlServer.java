package cn.com.vortexa.bot_platform.script_control;

import cn.com.vortexa.common.constants.BotInstanceStatus;
import cn.com.vortexa.common.constants.BotRemotingCommandFlagConstants;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.exception.BotStartException;
import cn.com.vortexa.control.BotControlServer;
import cn.com.vortexa.control.config.ControlServerConfig;
import cn.com.vortexa.control.dto.*;
import cn.com.vortexa.control.exception.ControlServerException;
import cn.com.vortexa.control.exception.CustomCommandInvokeException;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.control.service.IConnectionService;
import cn.com.vortexa.control.service.IRegistryService;
import cn.com.vortexa.control.util.ControlServerUtil;
import cn.com.vortexa.control.util.RPCMethodUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    /**
     * 开启job
     *
     * @param group   group
     * @param jobName jobName
     * @param botName botName
     * @param botKey  botKey
     * @return CompletableFuture<Result>
     */
    public CompletableFuture<Result> startJob(String group, String botName, String botKey, String jobName) throws BotStartException {
        // Step 1 判断目标Bot是否在线
        String key = ControlServerUtil.generateServiceInstanceKey(group, botName, botKey);
        BotInstanceStatus status = getBotInstanceStatus(key);

        if (status != BotInstanceStatus.RUNNING) {
            throw new BotStartException("bot[%s][%s][%s] status is[%s] not RUNNING".formatted(
                    group, botName, botKey, status
            ));
        }

        // Step 2 发送启动命令
        RemotingCommand command = newRemotingCommand(BotRemotingCommandFlagConstants.START_BOT_JOB, true);

        return sendCommandToServiceInstance(
                key,
                command
        ).thenApplyAsync(response->{
            if (response.isSuccess()) {
                log.error("[{}] start job[{}] success", key, jobName);
                return Result.ok();
            } else {
                log.error("[{}] start job[{}] fail, {}", key, jobName, response.getPayLoad());
                return Result.fail(String.valueOf(response.getPayLoad()));
            }
        }).exceptionally(throwable -> {
            log.error("[{}] start job[{}] error", key, jobName, throwable);
            return Result.fail(throwable.getMessage());
        });
    }

    /**
     * 获取bot实例状态
     *
     * @param group   group
     * @param botName botName
     * @param botKey  botKey
     * @return BotInstanceStatus
     */
    public BotInstanceStatus getBotInstanceStatus(String group, String botName, String botKey) {
        String instanceKey = ControlServerUtil.generateServiceInstanceKey(group, botName, botKey);
        return getBotInstanceStatus(instanceKey);
    }

    /**
     * 获取bot实例状态
     *
     * @param instanceKey instanceKey
     * @return BotInstanceStatus
     */
    public BotInstanceStatus getBotInstanceStatus(String instanceKey) {
        ConnectEntry connectEntry = getConnectionService().getServiceInstanceChannel(instanceKey);
        return connectEntry == null ? BotInstanceStatus.STOPPED :
                (connectEntry.isUsable() ? BotInstanceStatus.RUNNING : BotInstanceStatus.UN_USABLE);
    }
}
