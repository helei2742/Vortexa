package cn.com.vortexa.script_node.scriptagent;

import cn.com.vortexa.common.constants.BotExtFieldConstants;
import cn.com.vortexa.common.constants.BotRemotingCommandFlagConstants;
import cn.com.vortexa.common.dto.BotACJobResult;
import cn.com.vortexa.common.dto.control.ServiceInstance;
import cn.com.vortexa.common.entity.ScriptNode;
import cn.com.vortexa.control.ScriptAgent;
import cn.com.vortexa.control.config.ScriptAgentConfig;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.dto.RPCArgsWrapper;
import cn.com.vortexa.control.dto.RPCServiceInfo;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.dto.RequestHandleResult;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.exception.CustomCommandInvokeException;
import cn.com.vortexa.common.util.protocol.Serializer;
import cn.com.vortexa.control.util.ControlServerUtil;
import cn.com.vortexa.control.util.RPCMethodUtil;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.com.vortexa.script_node.config.ScriptNodeConfiguration;
import cn.hutool.core.util.BooleanUtil;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

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
    private final String botGroup;
    private final List<RPCServiceInfo<?>> rpcServiceInfos;  // RPC方法信息

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
            scriptNode.setBotGroup(scriptNodeConfiguration.getBotGroup());
            scriptNode.setBotConfigMap(scriptNodeConfiguration.getBotKeyConfigMap());

            return scriptNode;
        });

        this.botGroup = scriptNodeConfiguration.getBotGroup();
        this.rpcServiceInfos = rpcServiceInfos;

    }

    @Override
    protected void init() throws SSLException, URISyntaxException {
        super.init();

        // Step 1 RPC命令处理
        if (rpcServiceInfos == null) {
            return;
        }
        if (initCount.getAndIncrement() > 0) {
            return;
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

        // Step 2 其它命令处理
        Map<Integer, BiFunction<Channel, RemotingCommand, RemotingCommand>> handlerMap
                = getCustomRemotingCommandHandlerMap();
        handlerMap.put(
                BotRemotingCommandFlagConstants.START_UP_BOT_LOG,
                logUploadService::startUploadLogRCHandler
        );
        handlerMap.put(
                BotRemotingCommandFlagConstants.STOP_UP_BOT_LOG,
                logUploadService::stopUploadLogRCHandler
        );
        handlerMap.put(
                BotRemotingCommandFlagConstants.START_BOT_JOB,
                (channel, remotingCommand) -> startOrParsedBotJob(remotingCommand)
        );
    }

    /**
     * 添加运行的bot 到script agent中
     *
     * @param botName botName
     * @param botKey  botKey
     * @param bot     bot
     */
    public void addRunningBot(String botName, String botKey, AutoLaunchBot<?> bot) {
        String key = ControlServerUtil.generateServiceInstanceKey(botGroup, botName, botKey);

        if (runningBotMap.containsKey(key)) {
            throw new IllegalArgumentException("bot[%s][%s] exist".formatted(botName, botKey));
        }
        runningBotMap.put(key, new BotInstanceMetaInfo(bot));

        // 上报bot上线
        reportScriptBotOnLine(botGroup, botName, botKey);
    }

    /**
     * 移除正在运行的bot
     *
     * @param botName botName
     * @param botKey  botKey
     */
    public void removeRunningBot(String botName, String botKey) {
        String key = ControlServerUtil.generateServiceInstanceKey(botGroup, botName, botKey);
        runningBotMap.remove(key);

        // 上报bot下线
        reportScriptBotOffLine(botGroup, botName, botKey);
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
     * @param botGroup botGroup
     * @param botName  botName
     * @param botKey   botKey
     * @return boolean
     */
    private CompletableFuture<Boolean> reportScriptBotOnLine(String botGroup, String botName, String botKey) {
        log.info("send report bot[{}][{}] on line command", botGroup, botName);
        // Step 1 生成命令
        RemotingCommand command = newRequestCommand(BotRemotingCommandFlagConstants.SCRIPT_BOT_ON_LINE, true);
        command.addExtField(BotExtFieldConstants.TARGET_GROUP_KEY, botGroup);
        command.addExtField(BotExtFieldConstants.TARGET_BOT_NAME_KEY, botName);
        command.addExtField(BotExtFieldConstants.TARGET_BOT_KEY_KEY, botKey);

        return sendRequest(command)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        log.info("report bot[{}][{}] online success", botGroup, botName);
                        return true;
                    } else {
                        return false;
                    }
                })
                .exceptionally(throwable -> {
                    log.error("[{}] expose bot[{}][{}] error", botGroup, botName, botKey, throwable);
                    return false;
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
                })
                .exceptionally(throwable -> {
                    log.error("[{}] offLine bot[{}][{}] error", botGroup, botName, botKey, throwable);
                    return false;
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
        String key = ControlServerUtil.generateServiceInstanceKey(botGroup, botName, botKey);

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
}
