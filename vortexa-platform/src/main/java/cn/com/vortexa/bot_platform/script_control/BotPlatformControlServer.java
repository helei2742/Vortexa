package cn.com.vortexa.bot_platform.script_control;

import cn.com.vortexa.bot_platform.script_control.service.BotLogUploadService;
import cn.com.vortexa.bot_platform.wsController.FrontWebSocketServer;
import cn.com.vortexa.bot_platform.wsController.UIWSMessage;
import cn.com.vortexa.common.constants.*;
import cn.com.vortexa.common.dto.BotACJobResult;
import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.entity.ScriptNode;
import cn.com.vortexa.common.exception.BotStartException;
import cn.com.vortexa.common.vo.PageQuery;
import cn.com.vortexa.control_server.BotControlServer;
import cn.com.vortexa.control_server.config.ControlServerConfig;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.dto.*;
import cn.com.vortexa.control_server.dto.ConnectEntry;
import cn.com.vortexa.control_server.exception.ControlServerException;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.exception.CustomCommandInvokeException;
import cn.com.vortexa.common.util.protocol.Serializer;
import cn.com.vortexa.control_server.service.IConnectionService;
import cn.com.vortexa.control_server.service.IRegistryService;
import cn.com.vortexa.control.util.ControlServerUtil;
import cn.com.vortexa.control.util.RPCMethodUtil;
import cn.hutool.core.util.StrUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author helei
 * @since 2025/3/26 15:42
 */
@Slf4j
public class BotPlatformControlServer extends BotControlServer {
    @Getter
    private final ConcurrentMap<String, Set<String>> scriptNodeKey2BotInstanceKeyMap = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentMap<String, String> botInstanceKey2ScriptNodeKeyMap = new ConcurrentHashMap<>();

    private final List<RPCServiceInfo<?>> rpcServiceInfos;
    private final BotLogUploadService botLogUploadService;
    @Getter
    private final FrontWebSocketServer frontWebSocketServer;

    @Override
    public ChannelFuture start() throws ControlServerException {
        ChannelFuture start = super.start();
        FrontWebSocketServer.running = true;
        return start;
    }

    public BotPlatformControlServer(
            ControlServerConfig controlServerConfig,
            FrontWebSocketServer frontWebSocketServer,
            List<RPCServiceInfo<?>> rpcServiceInfos
    ) throws ControlServerException {
        super(controlServerConfig);
        this.rpcServiceInfos = rpcServiceInfos;
        this.frontWebSocketServer = frontWebSocketServer;
        this.botLogUploadService = new BotLogUploadService(this);
    }

    @Override
    public void init(IRegistryService registryService, IConnectionService connectionService) throws Exception {
        super.init(registryService, connectionService);

        // 1 RPC 服务
        initRPCMethodHandler();

        // 1 expose bot 注册、下线
        initScriptNodeExposeBotHandler();

        // 2 日志上传
        initScriptNodeUploadLogHandler();
    }

    /**
     * 初始化rpc调用方法处理器
     *
     * @throws CustomCommandException CustomCommandException
     */
    private void initRPCMethodHandler() throws CustomCommandException {
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
                        log.info("invoke rpc[{}] method[{}] return [{}]",
                                request.getTransactionId(), key, invoke == null ? "null" : "not null");
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
    }

    /**
     * 初始化script暴露bot命令
     */
    private void initScriptNodeExposeBotHandler() {
        addCustomRemotingCommandHandler(
                BotRemotingCommandFlagConstants.SCRIPT_BOT_ON_LINE,
                this::scriptNodeOnLineHandler
        );

        addCustomRemotingCommandHandler(
                BotRemotingCommandFlagConstants.SCRIPT_BOT_OFF_LINE,
                this::scriptBotOffLineHandler
        );
    }

    /**
     * 初始化节点日志上传命令处理器
     */
    protected void initScriptNodeUploadLogHandler() {
        frontWebSocketServer.setCloseHandler((server, token) -> {
            botLogUploadService.stopFrontLogListener(token);
        });

        // 前端websocket server 监听命令
        frontWebSocketServer.addMessageHandler(
                BotRemotingCommandFlagConstants.START_UP_BOT_LOG,
                (token, session, request) -> {
                    String group = request.getParams().getString(BotExtFieldConstants.TARGET_GROUP_KEY);
                    String botName = request.getParams().getString(BotExtFieldConstants.TARGET_BOT_NAME_KEY);
                    String botKey = request.getParams().getString(BotExtFieldConstants.TARGET_BOT_KEY_KEY);

                    UIWSMessage response = botLogUploadService.browserRequestBotLogRCHandler(group, botName, botKey, token);
                    request.setCode(BotRemotingCommandFlagConstants.START_UP_BOT_LOG_RESPONSE);
                    return response;
                });

        // 后端websocket server 响应
        getCustomRemotingCommandHandlerMap().put(
                BotRemotingCommandFlagConstants.BOT_RUNTIME_LOG,
                botLogUploadService::botUploadLogRCHandler
        );
    }

    /**
     * 开启job
     *
     * @param scriptNode scriptNode
     * @param jobName    jobName
     * @param botName    botName
     * @param botKey     botKey
     * @return CompletableFuture<Result>
     */
    public CompletableFuture<Result> startJob(ScriptNode scriptNode, String botName, String botKey, String jobName)
            throws BotStartException {
        String scriptNodeName;
        String groupId;
        String serviceId;
        String instanceId;
        if (scriptNode == null
                || (StrUtil.isBlank(scriptNodeName = scriptNode.getScriptNodeName()))
                || (StrUtil.isBlank(groupId = scriptNode.getGroupId()))
                || (StrUtil.isBlank(serviceId = scriptNode.getServiceId()))
                || (StrUtil.isBlank(instanceId = scriptNode.getInstanceId()))
        ) {
            throw new IllegalArgumentException("scriptNodeName or groupId、serviceId、instanceId are required");
        }
        String scriptNodeKey = ControlServerUtil.generateServiceInstanceKey(groupId, serviceId, instanceId);

        // Step 1 判断目标Bot是否在线
        String botInstanceKey = ControlServerUtil.generateServiceInstanceKey(scriptNodeName, botName, botKey);

        BotInstanceStatus status = getBotInstanceStatus(botInstanceKey);
        if (status != BotInstanceStatus.RUNNING) {
            throw new BotStartException("bot[%s][%s][%s] status is[%s] not RUNNING".formatted(
                    scriptNodeName, botName, botKey, status
            ));
        }

        // Step 2 发送启动命令
        RemotingCommand command = newRemotingCommand(BotRemotingCommandFlagConstants.START_BOT_JOB, true);
        command.addExtField(BotExtFieldConstants.JOB_NAME, jobName);
        command.addExtField(BotExtFieldConstants.TARGET_BOT_NAME_KEY, botName);
        command.addExtField(BotExtFieldConstants.TARGET_BOT_KEY_KEY, botKey);

        return sendCommandToServiceInstance(
                scriptNodeKey,
                command
        ).thenApplyAsync(response -> {
            if (response.isSuccess()) {
                BotACJobResult result = response.getObjBody(BotACJobResult.class);
                log.info("[{}] start job[{}] success", botInstanceKey, jobName);
                return Result.ok(result.getData());
            } else {
                log.error("[{}] start job[{}] fail, {}", botInstanceKey, jobName, response);
                return Result.fail(response.getErrorMessage());
            }
        }).exceptionally(throwable -> {
            log.error("[{}] start job[{}] error", botInstanceKey, jobName, throwable);
            return Result.fail(throwable.getMessage());
        });
    }

    /**
     * 获取bot实例状态
     *
     * @param scriptNodeName scriptNodeName
     * @param botName        botName
     * @param botKey         botKey
     * @return BotInstanceStatus
     */
    public BotInstanceStatus getBotInstanceStatus(String scriptNodeName, String botName, String botKey) {
        String instanceKey = ControlServerUtil.generateServiceInstanceKey(scriptNodeName, botName, botKey);
        return getBotInstanceStatus(instanceKey);
    }

    /**
     * 获取bot实例所在的Script Node状态
     *
     * @param botInstanceKey botInstanceKey
     * @return BotInstanceStatus
     */
    public BotInstanceStatus getBotInstanceStatus(String botInstanceKey) {
        String scriptNodeKey = botInstanceKey2ScriptNodeKeyMap.get(botInstanceKey);
        if (scriptNodeKey == null) return BotInstanceStatus.STOPPED;
        ConnectEntry connectEntry = getConnectionService().getServiceInstanceChannel(scriptNodeKey);
        return connectEntry == null ? BotInstanceStatus.STOPPED :
                (connectEntry.isUsable() ? BotInstanceStatus.RUNNING : BotInstanceStatus.UN_USABLE);
    }

    /**
     * 脚本上线命令处理
     *
     * @param channel         channel
     * @param remotingCommand remotingCommand
     * @return RemotingCommand
     */
    public RemotingCommand scriptNodeOnLineHandler(Channel channel, RemotingCommand remotingCommand) {
        String group = remotingCommand.getGroup();
        String serviceId = remotingCommand.getServiceId();
        String instanceId = remotingCommand.getInstanceId();

        String scriptNodeKey = ControlServerUtil.generateServiceInstanceKey(group, serviceId, instanceId);

        String botGroup = remotingCommand.getExtFieldsValue(BotExtFieldConstants.TARGET_GROUP_KEY);
        String botName = remotingCommand.getExtFieldsValue(BotExtFieldConstants.TARGET_BOT_NAME_KEY);
        String botKey = remotingCommand.getExtFieldsValue(BotExtFieldConstants.TARGET_BOT_KEY_KEY);

        String botInstanceKey = ControlServerUtil.generateServiceInstanceKey(botGroup, botName, botKey);

        scriptNodeKey2BotInstanceKeyMap.compute(scriptNodeKey, (k, v) -> {
            if (v == null) {
                v = new HashSet<>();
            }
            v.add(botInstanceKey);
            return v;
        });
        botInstanceKey2ScriptNodeKeyMap.put(botInstanceKey, scriptNodeKey);

        log.info("script node[{}] add bot instance[{}]", scriptNodeKey, botInstanceKey);

        RemotingCommand response = new RemotingCommand();
        response.setFlag(BotRemotingCommandFlagConstants.SCRIPT_BOT_ON_LINE_RESPONSE);
        response.setCode(RemotingCommandCodeConstants.SUCCESS);
        response.setTransactionId(remotingCommand.getTransactionId());

        return response;
    }

    /**
     * 脚本下线命令处理
     *
     * @param channel         channel
     * @param remotingCommand remotingCommand
     * @return RemotingCommand
     */
    private RemotingCommand scriptBotOffLineHandler(Channel channel, RemotingCommand remotingCommand) {
        String group = remotingCommand.getGroup();
        String serviceId = remotingCommand.getServiceId();
        String instanceId = remotingCommand.getInstanceId();

        String scriptNodeKey = ControlServerUtil.generateServiceInstanceKey(group, serviceId, instanceId);

        String botGroup = remotingCommand.getExtFieldsValue(BotExtFieldConstants.TARGET_GROUP_KEY);
        String botName = remotingCommand.getExtFieldsValue(BotExtFieldConstants.TARGET_BOT_NAME_KEY);
        String botKey = remotingCommand.getExtFieldsValue(BotExtFieldConstants.TARGET_BOT_KEY_KEY);

        String botInstanceKey = ControlServerUtil.generateServiceInstanceKey(botGroup, botName, botKey);

        Set<String> onLineBotInstanceKeys = scriptNodeKey2BotInstanceKeyMap.get(scriptNodeKey);
        if (onLineBotInstanceKeys == null || !onLineBotInstanceKeys.contains(botInstanceKey)) {
            log.info("script node[{}] remove bot instance[{}] fail, bot instance not exist", scriptNodeKey, botInstanceKey);
        } else {
            onLineBotInstanceKeys.remove(botInstanceKey);
            botInstanceKey2ScriptNodeKeyMap.remove(botInstanceKey);
            log.info("script node[{}] remove bot instance[{}]", scriptNodeKey, botInstanceKey);
        }

        RemotingCommand response = new RemotingCommand();
        response.setFlag(BotRemotingCommandFlagConstants.SCRIPT_BOT_OFF_LINE_RESPONSE);
        response.setCode(RemotingCommandCodeConstants.SUCCESS);
        response.setTransactionId(remotingCommand.getTransactionId());

        return response;
    }

    /**
     * 查询script node所有在线的bot
     *
     * @param scriptNodeKey scriptNodeKey
     * @return List<String>
     */
    public List<String> selectScriptNodeOnlineBot(String scriptNodeKey) {
        Set<String> onLineBotInstanceKeys = scriptNodeKey2BotInstanceKeyMap.get(scriptNodeKey);
        if (onLineBotInstanceKeys != null && !onLineBotInstanceKeys.isEmpty()) {
            return new ArrayList<>(onLineBotInstanceKeys);
        }
        return List.of();
    }

    /**
     * 查询script node下的botKey是否在线
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey        botKey
     * @return boolean
     */
    public boolean isScriptNodeBotOnline(String scriptNodeName, String botName, String botKey) {
        String botInstanceKey = ControlServerUtil.generateServiceInstanceKey(scriptNodeName, botName, botKey);
        String scriptNodeKey =  botInstanceKey2ScriptNodeKeyMap.get(botInstanceKey);
        if (StrUtil.isBlank(scriptNodeKey)) {
            return false;
        }
        ConnectEntry connectEntry = getConnectionService().getServiceInstanceChannel(scriptNodeKey);
        return connectEntry != null && connectEntry.isUsable();
    }

    /**
     * 查询scriptNode的status
     *
     * @param groupId    groupId
     * @param serviceId  serviceId
     * @param instanceId instanceId
     * @return WebsocketClientStatus
     */
    public ScriptNodeStatus queryScriptNodeStatus(String groupId, String serviceId, String instanceId) {
        String scriptNodeKey = ControlServerUtil.generateServiceInstanceKey(groupId, serviceId, instanceId);
        ConnectEntry connectEntry = getConnectionService().getServiceInstanceChannel(scriptNodeKey);
        if (connectEntry == null) {
            return ScriptNodeStatus.UNKNOWN;
        } else if (connectEntry.isUsable()) {
            return ScriptNodeStatus.ONLINE;
        } else {
            return ScriptNodeStatus.UNUSABLE;
        }
    }

    /**
     * 启动scriptNode下的Bot
     *
     * @param groupId    groupId
     * @param serviceId  serviceId
     * @param instanceId instanceId
     * @param botKey     botKey
     * @return Result
     */
    public CompletableFuture<Result> startScriptNodeBot(String groupId, String serviceId, String instanceId, String botKey) {
        String scriptNodeKey = ControlServerUtil.generateServiceInstanceKey(groupId, serviceId, instanceId);

        RemotingCommand command = newRemotingCommand(BotRemotingCommandFlagConstants.START_BOT, true);
        command.addExtField(BotExtFieldConstants.TARGET_BOT_KEY_KEY, botKey);

        return sendCommandToServiceInstance(
                scriptNodeKey,
                command
        ).thenApplyAsync(response -> {
            if (response.isSuccess()) {
                log.info("[{}] start bot[{}] success", scriptNodeKey, botKey);
                BotStatus botStatus = response.getObjBody(BotStatus.class);
                return Result.ok(botStatus);
            } else {
                log.error("[{}] start bot[{}] fail, {}", scriptNodeKey, botKey, response.getPayLoad());
                return Result.fail(response.getErrorMessage());
            }
        }).exceptionally(throwable -> {
            log.error("[{}] start job[{}] error", scriptNodeKey, botKey, throwable);
            return Result.fail(throwable.getMessage());
        });
    }

    /**
     * 关闭Bot
     *
     * @param groupId    groupId
     * @param serviceId  serviceId
     * @param instanceId instanceId
     * @param botKey     botKey
     * @return CompletableFuture<Result>
     */
    public CompletableFuture<Result> stopScriptNodeBot(String groupId, String serviceId, String instanceId, String botKey) {
        String scriptNodeKey = ControlServerUtil.generateServiceInstanceKey(groupId, serviceId, instanceId);

        RemotingCommand command = newRemotingCommand(BotRemotingCommandFlagConstants.STOP_BOT, true);
        command.addExtField(BotExtFieldConstants.TARGET_BOT_KEY_KEY, botKey);

        return sendCommandToServiceInstance(
                scriptNodeKey,
                command
        ).thenApplyAsync(response -> {
            BotStatus botStatus = response.getObjBody(BotStatus.class);
            if (response.isSuccess()) {
                log.info("[{}] stop bot[{}] success", scriptNodeKey, botKey);
                return Result.ok(botStatus);
            } else {
                log.error("[{}] stop bot[{}] fail, {}", scriptNodeKey, botKey, response.getPayLoad());
                return Result.fail(response.getErrorMessage());
            }
        }).exceptionally(throwable -> {
            log.error("[{}] v job[{}] error", scriptNodeKey, botKey, throwable);
            return Result.fail(throwable.getMessage());
        });
    }

    /**
     * 查询bot instance中运行的账户
     *
     * @param scriptNode scriptNode
     * @param botId      botId
     * @param botKey     botKey
     * @param pageQuery  pageQuery
     * @return Result
     */
    public CompletableFuture<Result> queryBotInstanceAccount(
            ScriptNode scriptNode, Integer botId, String botName, String botKey, PageQuery pageQuery
    ) {
        log.info("query[{}] bot[{}] account, params[{}]", scriptNode, botKey, pageQuery);
        String groupId;
        String serviceId;
        String instanceId;
        if (scriptNode == null
                || botId == null
                || pageQuery == null
                || StrUtil.isBlank(botKey)
                || StrUtil.isBlank(botName)
                || (StrUtil.isBlank(groupId = scriptNode.getGroupId()))
                || (StrUtil.isBlank(serviceId = scriptNode.getServiceId()))
                || (StrUtil.isBlank(instanceId = scriptNode.getInstanceId()))
        ) {
            throw new IllegalArgumentException("scriptNodeName or groupId、serviceId、instanceId are required");
        }
        String scriptNodeKey = ControlServerUtil.generateServiceInstanceKey(groupId, serviceId, instanceId);

        RemotingCommand command = newRemotingCommand(BotRemotingCommandFlagConstants.QUERY_BOT_ACCOUNT, true);
        command.addExtField(BotExtFieldConstants.TARGET_BOT_KEY_KEY, botKey);
        command.addExtField(BotExtFieldConstants.TARGET_BOT_NAME_KEY, botName);
        command.addExtField(BotExtFieldConstants.TARGET_BOT_ID_KEY, String.valueOf(botId));
        command.setObjBody(new PageQuery(pageQuery.getPage(), pageQuery.getLimit(), pageQuery.getFilterMap()));

        return sendCommandToServiceInstance(
                scriptNodeKey,
                command
        ).thenApply(response -> {
            PageResult<?> result = null;
            if (response.isSuccess() && (result = response.getObjBody(PageResult.class)) != null) {
                log.info("query[{}] bot[{}] account success, total:{}", scriptNode, botKey, result.getTotal());
                return Result.ok(result);
            } else {
                log.error("query[{}] bot[{}] account fail, {}", scriptNode, botKey, response.getErrorMessage());
                return Result.fail(response.getErrorMessage());
            }
        });
    }
}
