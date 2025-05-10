package cn.com.vortexa.script_node.scriptagent;

import cn.com.vortexa.common.constants.BotExtFieldConstants;
import cn.com.vortexa.common.constants.BotRemotingCommandFlagConstants;
import cn.com.vortexa.common.constants.BotStatus;
import cn.com.vortexa.common.dto.BotACJobResult;
import cn.com.vortexa.common.dto.PageResult;
import cn.com.vortexa.common.dto.account.BotInstanceAccount;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.common.entity.BotInstance;
import cn.com.vortexa.common.entity.ScriptNode;
import cn.com.vortexa.common.vo.PageQuery;
import cn.com.vortexa.script_agent.ScriptAgent;
import cn.com.vortexa.script_agent.config.ScriptAgentConfig;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.dto.RPCArgsWrapper;
import cn.com.vortexa.control.dto.RPCServiceInfo;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.dto.RequestHandleResult;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.exception.CustomCommandInvokeException;
import cn.com.vortexa.common.util.protocol.Serializer;
import cn.com.vortexa.common.util.ServerInstanceUtil;
import cn.com.vortexa.control.util.RPCMethodUtil;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.script_node.config.ScriptNodeConfiguration;
import cn.com.vortexa.script_node.service.BotApi;
import cn.com.vortexa.script_node.util.ScriptBotLauncher;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLException;

/**
 * @author helei
 * @since 2025/3/26 16:56
 */
@Slf4j
public class BotScriptAgent extends ScriptAgent {
    private final AtomicInteger initCount = new AtomicInteger(0);   // 启动次数
    private final BotScriptAgentLogUploadService logUploadService = new BotScriptAgentLogUploadService(this);  // 日志上传服务
    private final Map<String, BotInstanceMetaInfo> runningBotMap = new HashMap<>(); // key map bot
    private final String scriptNodeName;
    private final List<RPCServiceInfo<?>> rpcServiceInfos;  // RPC方法信息
    private final ScriptNodeConfiguration scriptNodeConfiguration;

    @Setter
    private BotApi botApi;

    public BotScriptAgent(
            ScriptAgentConfig clientConfig,
            ScriptNodeConfiguration scriptNodeConfiguration,
            List<RPCServiceInfo<?>> rpcServiceInfos
    ) {
        super(clientConfig);
        super.setName(clientConfig.getServiceInstance().toString());
        super.setRegistryBodySetter(() -> {
            ServiceInstance serviceInstance = clientConfig.getServiceInstance();

            ScriptNode scriptNode = ScriptNode.generateFromServiceInstance(serviceInstance);
            scriptNode.setScriptNodeName(scriptNodeConfiguration.getScriptNodeName());
            scriptNode.setLoadedBotInfos(new ArrayList<>(scriptNodeConfiguration.getBotNameMetaInfoMap().keySet()));
            scriptNode.setNodeAppConfig(JSONObject.toJSONString(ScriptNodeConfiguration.RAW_CONFIG));

            return scriptNode;
        });

        this.scriptNodeName = scriptNodeConfiguration.getScriptNodeName();
        this.scriptNodeConfiguration = scriptNodeConfiguration;
        this.rpcServiceInfos = rpcServiceInfos;
    }

    @Override
    protected void init() throws SSLException, URISyntaxException {
        super.init();

        if (initCount.getAndIncrement() > 0) {
            return;
        }

        // Step 1 RPC命令处理
        initRPCMethod();

        // Step 2 其它命令处理
        customCommandInit();
    }

    /**
     * 初始化自定义命令
     */
    private void customCommandInit() {
        addCustomRemotingCommandHandler(
                BotRemotingCommandFlagConstants.START_UP_BOT_LOG,
                logUploadService::startUploadLogRCHandler
        );
        addCustomRemotingCommandHandler(
                BotRemotingCommandFlagConstants.STOP_UP_BOT_LOG,
                logUploadService::stopUploadLogRCHandler
        );
        addCustomRemotingCommandHandler(
                BotRemotingCommandFlagConstants.START_BOT_JOB,
                (channel, remotingCommand) -> startOrParsedBotJob(remotingCommand)
        );

        // 启动bot命令处理
        addCustomRemotingCommandHandler(
                BotRemotingCommandFlagConstants.START_BOT,
                this::startBotHandler
        );
        // 关闭bot命令处理
        addCustomRemotingCommandHandler(
                BotRemotingCommandFlagConstants.STOP_BOT,
                this::stopBotHandler
        );

        // 查询bot account命令
        addCustomRemotingCommandHandler(
                BotRemotingCommandFlagConstants.QUERY_BOT_ACCOUNT,
                this::pageQueryBotAccount
        );
    }

    /**
     * 初始化rpc方法
     *
     * @return 是否初始化成功
     */
    private boolean initRPCMethod() {
        if (rpcServiceInfos == null) {
            return true;
        }
        log.info("start registry rpc services");
        for (RPCServiceInfo<?> rpcServiceInfo : rpcServiceInfos) {
            Class<?> interfaces = rpcServiceInfo.getInterfaces();
            Object ref = rpcServiceInfo.getRef();

            for (Method method : interfaces.getDeclaredMethods()) {
                method.setAccessible(true);
                String key = RPCMethodUtil.buildRpcMethodKey(interfaces.getName(), method);

                try {
                    super.addCustomCommandHandler(key, request -> {
                        RequestHandleResult result = new RequestHandleResult();

                        log.debug("invoke rpc method[{}]", method.getName());
                        try {
                            byte[] body = request.getBody();
                            RPCArgsWrapper params = Serializer.Algorithm.JDK.deserialize(body, RPCArgsWrapper.class);
                            result.setData(method.invoke(ref, params.getArgs()));
                            result.setSuccess(true);
                            return result;
                        } catch (Exception e) {
                            log.error("invoke rpc method [{}] error", method.getName());
                            throw new CustomCommandInvokeException(e);
                        }
                    });
                } catch (CustomCommandException e) {
                    log.error("registry custom method error", e);
                }
            }
        }
        return false;
    }

    /**
     * 添加运行的bot 到script agent中
     *
     * @param botName botName
     * @param botKey  botKey
     * @param bot     bot
     */
    public void addRunningBot(String botName, String botKey, AutoLaunchBot<?> bot) {
        String key = ServerInstanceUtil.generateServiceInstanceKey(scriptNodeName, botName, botKey);

        if (runningBotMap.containsKey(key)) {
            throw new IllegalArgumentException("bot[%s][%s] exist".formatted(botName, botKey));
        }
        runningBotMap.put(key, new BotInstanceMetaInfo(bot));

        // 上报bot上线
        reportScriptBotOnLine(scriptNodeName, botName, botKey)
                .exceptionally(throwable -> {
                    log.error("bot[{}][{}]expose error", botName, botKey, throwable);
                    return false;
                });
    }

    /**
     * 移除正在运行的bot
     *
     * @param botName botName
     * @param botKey  botKey
     */
    public void removeRunningBot(String botName, String botKey) {
        String key = ServerInstanceUtil.generateServiceInstanceKey(scriptNodeName, botName, botKey);
        runningBotMap.remove(key);

        // 上报bot下线
        reportScriptBotOffLine(scriptNodeName, botName, botKey)
                .exceptionally(throwable -> {
                    log.error("bot[{}][{}] ffLine error", botName, botKey, throwable);
                    return false;
                });
    }


    /**
     * 根据bot实例key获取bot
     */
    public BotInstanceMetaInfo getBotMetaInfo(String botInstanceKey) {
        return runningBotMap.get(botInstanceKey);
    }

    /**
     * 将bot 上线上报到ControlServer
     *
     * @param scriptNodeName scriptNodeName
     * @param botName        botName
     * @param botKey         botKey
     * @return boolean
     */
    private CompletableFuture<Boolean> reportScriptBotOnLine(String scriptNodeName, String botName, String botKey) {
        log.info("send report bot[{}][{}] on line command", scriptNodeName, botName);
        // Step 1 生成命令
        RemotingCommand command = newRequestCommand(BotRemotingCommandFlagConstants.SCRIPT_BOT_ON_LINE, true);
        command.addExtField(BotExtFieldConstants.TARGET_GROUP_KEY, scriptNodeName);
        command.addExtField(BotExtFieldConstants.TARGET_BOT_NAME_KEY, botName);
        command.addExtField(BotExtFieldConstants.TARGET_BOT_KEY_KEY, botKey);

        return sendRequest(command)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        log.info("report bot[{}][{}] online success", scriptNodeName, botName);
                        return true;
                    } else {
                        return false;
                    }
                });
    }

    /**
     * 上报bot下线
     *
     * @param botGroup botGroup
     * @param botName  botName
     * @param botKey   botKey
     * @return CompletableFuture<Boolean>
     */
    private CompletableFuture<Boolean> reportScriptBotOffLine(String botGroup, String botName, String botKey) {
        log.warn("send report bot[{}][{}] offline command", botGroup, botName);
        // Step 1 生成命令
        RemotingCommand command = newRequestCommand(BotRemotingCommandFlagConstants.SCRIPT_BOT_OFF_LINE, true);
        command.addExtField(BotExtFieldConstants.TARGET_GROUP_KEY, botGroup);
        command.addExtField(BotExtFieldConstants.TARGET_BOT_NAME_KEY, botName);
        command.addExtField(BotExtFieldConstants.TARGET_BOT_KEY_KEY, botKey);

        return sendRequest(command)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        log.warn("report bot[{}][{}] offline success", botGroup, botName);
                        return true;
                    } else {
                        return false;
                    }
                });
    }


    /**
     * 启动或暂停bot
     *
     * @param remotingCommand remotingCommand
     * @return RemotingCommand
     */
    private RemotingCommand startOrParsedBotJob(RemotingCommand remotingCommand) {
        String botName = remotingCommand.getExtFieldsValue(BotExtFieldConstants.TARGET_BOT_NAME_KEY);
        String botKey = remotingCommand.getExtFieldsValue(BotExtFieldConstants.TARGET_BOT_KEY_KEY);
        String jobName = remotingCommand.getExtFieldsValue(BotExtFieldConstants.JOB_NAME);

        // Step 1 生成key，从map中查找存在的bot
        String key = ServerInstanceUtil.generateServiceInstanceKey(scriptNodeName, botName, botKey);

        RemotingCommand response = new RemotingCommand();
        response.setCode(RemotingCommandCodeConstants.FAIL);

        BotInstanceMetaInfo botMetaInfo = runningBotMap.get(key);

        // 不存在，返回
        if (botMetaInfo == null) {
            response.setErrorMessage(key + " not found in script node");
            return response;
        }

        AutoLaunchBot<?> bot = botMetaInfo.getBot();

        // 存在则尝试调用方法启动
        BotACJobResult botACJobResult = bot.startBotJob(jobName);
        if (BooleanUtil.isTrue(botACJobResult.getSuccess())) {
            response.setCode(RemotingCommandCodeConstants.SUCCESS);
            bot.logger.debug("start/parsed job[%s] success".formatted(jobName));
        } else {

            bot.logger.error("start/parsed job[%s] fail".formatted(jobName));
        }

        response.setBody(Serializer.Algorithm.JDK.serialize(botACJobResult));
        return response;
    }

    /**
     * 远程命令启动Bot
     *
     * @param channel channel
     * @param request request
     * @return RemotingCommand
     */
    private RemotingCommand startBotHandler(Channel channel, RemotingCommand request) {
        RemotingCommand response = new RemotingCommand();
        response.setTransactionId(request.getTransactionId());
        response.setCode(RemotingCommandCodeConstants.SUCCESS);
        response.setFlag(BotRemotingCommandFlagConstants.START_BOT_RESPONSE);


        String botKey = request.getExtFieldsValue(BotExtFieldConstants.TARGET_BOT_KEY_KEY);
        String group = request.getGroup();
        String serviceId = request.getServiceId();
        String instanceId = request.getInstanceId();
        if (StrUtil.isBlank(group) || StrUtil.isBlank(serviceId)
                || StrUtil.isBlank(instanceId) || StrUtil.isBlank(botKey)) {
            response.setCode(RemotingCommandCodeConstants.FAIL);
            response.setErrorMessage("params error");
        } else {
            log.info("remote[{}]-[{}]-[{}] try launch bot[{}]", group, serviceId, instanceId, botKey);
            try {
                AutoBotConfig launchConfig = request.getObjBody(AutoBotConfig.class);
                launchConfig.setMetaInfo(
                    scriptNodeConfiguration.getBotNameMetaInfoMap().get(launchConfig.getBotName())
                );
                BotStatus botStatus = ScriptBotLauncher.INSTANCE.loadAndLaunchBot(launchConfig);
                response.setObjBody(botStatus);
                log.info("remote[{}]-[{}]-[{}] try launch bot[{}] success", group, serviceId, instanceId, botKey);
            } catch (Exception e) {
                response.setCode(RemotingCommandCodeConstants.FAIL);
                response.setErrorMessage(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
                log.error("remote[{}]-[{}]-[{}] launch bot[{}] failed", group, serviceId, instanceId, botKey, e);
            }
        }
        return response;
    }

    /**
     * 远程命令停止bot
     *
     * @param channel channel
     * @param request request
     * @return RemotingCommand
     */
    private RemotingCommand stopBotHandler(Channel channel, RemotingCommand request) {
        RemotingCommand response = new RemotingCommand();
        response.setTransactionId(request.getTransactionId());
        response.setCode(RemotingCommandCodeConstants.SUCCESS);
        response.setFlag(BotRemotingCommandFlagConstants.STOP_BOT_RESPONSE);

        String botKey = request.getExtFieldsValue(BotExtFieldConstants.TARGET_BOT_KEY_KEY);
        String group = request.getGroup();
        String serviceId = request.getServiceId();
        String instanceId = request.getInstanceId();
        if (StrUtil.isBlank(group) || StrUtil.isBlank(serviceId)
                || StrUtil.isBlank(instanceId) || StrUtil.isBlank(botKey)) {
            response.setCode(RemotingCommandCodeConstants.FAIL);
            response.setErrorMessage("params error");
        } else {
            log.info("remote[{}]-[{}]-[{}] try stop bot[{}]", group, serviceId, instanceId, botKey);
            try {
                AutoLaunchBot<?> bot = ScriptBotLauncher.INSTANCE.getBotByBotKey(botKey);
                if (bot != null) {
                    bot.stop();
                    response.setObjBody(bot.getStatus());
                } else {
                    response.setCode(RemotingCommandCodeConstants.FAIL);
                    response.setErrorMessage("bot not found");
                }
                log.info("remote[{}]-[{}]-[{}] try stop bot[{}] success", group, serviceId, instanceId, botKey);
            } catch (Exception e) {
                response.setCode(RemotingCommandCodeConstants.FAIL);
                response.setErrorMessage(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
                log.error("remote[{}]-[{}]-[{}] v bot[{}] failed", group, serviceId, instanceId, botKey, e);
            }
        }
        return response;
    }

    /**
     * 查询bot 账户
     *
     * @param channel channel
     * @param request request
     * @return response
     */
    private RemotingCommand pageQueryBotAccount(Channel channel, RemotingCommand request) {
        log.info("remote page query bot account...");
        RemotingCommand response = new RemotingCommand();
        response.setTransactionId(request.getTransactionId());
        response.setCode(RemotingCommandCodeConstants.SUCCESS);
        response.setFlag(BotRemotingCommandFlagConstants.QUERY_BOT_ACCOUNT_RESPONSE);

        String botKey = request.getExtFieldsValue(BotExtFieldConstants.TARGET_BOT_KEY_KEY);
        Integer botId = request.getExtFieldsInt(BotExtFieldConstants.TARGET_BOT_ID_KEY);
        PageQuery pageQuery = request.getObjBody(PageQuery.class);

        Map<String, Object> filterMap = new HashMap<>();
        filterMap.put("botId", botId);
        filterMap.put("botKey", botKey);
        if (pageQuery.getFilterMap() != null) {
            filterMap.putAll(pageQuery.getFilterMap());
        }
        try {
            PageResult<AccountContext> pageResult = botApi.getBotAccountService().conditionPageQuery(
                    pageQuery.getPage(),
                    pageQuery.getLimit(),
                    filterMap
            );
            List<AccountContext> list = pageResult.getList();

            PageResult<BotInstanceAccount> result = new PageResult<>(
                    pageResult.getTotal(),
                    list == null ? List.of(): list.stream().map(BotInstanceAccount::fromAccountContext).toList(),
                    pageResult.getPages(),
                    pageResult.getPageNum(),
                    pageResult.getPageSize()
            );
            response.setBody(Serializer.Algorithm.JDK.serialize(result));
        } catch (SQLException e) {
            response.setCode(RemotingCommandCodeConstants.FAIL);
            response.setErrorMessage("query bot account fail");
            log.error("page query bot account error", e);
        }
        return response;
    }

    @Override
    public RemotingCommand buildPingCommand() {
        RemotingCommand ping =  super.buildPingCommand();
        List<String> list = runningBotMap.values().stream().map(instanceMetaInfo -> {
            AutoLaunchBot<?> bot = instanceMetaInfo.getBot();
            if (bot == null) {
                return null;
            }
            BotInstance botInstance = bot.getBotInstance();
            return ServerInstanceUtil.generateServiceInstanceKey(
                    botInstance.getScriptNodeName(),
                    botInstance.getBotName(),
                    botInstance.getBotKey()
            );
        }).filter(Objects::nonNull).toList();
        // 添加当前运行的botKey
        ping.addExtField(
                BotExtFieldConstants.RUNNING_BOT_INSTANCE_KEYS, String.join(",", list)
        );
        return ping;
    }
}
