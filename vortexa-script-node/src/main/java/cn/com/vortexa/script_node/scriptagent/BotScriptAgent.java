package cn.com.vortexa.script_node.scriptagent;

import cn.com.vortexa.common.constants.BotExtFieldConstants;
import cn.com.vortexa.common.constants.BotRemotingCommandFlagConstants;
import cn.com.vortexa.common.dto.BotACJobResult;
import cn.com.vortexa.control.ScriptAgent;
import cn.com.vortexa.control.config.ScriptAgentConfig;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.dto.RPCArgsWrapper;
import cn.com.vortexa.control.dto.RPCServiceInfo;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.dto.RequestHandleResult;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.exception.CustomCommandInvokeException;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.control.util.RPCMethodUtil;
import cn.com.vortexa.script_node.bot.AutoLaunchBot;
import cn.hutool.core.util.BooleanUtil;
import io.netty.channel.Channel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import javax.net.ssl.SSLException;

/**
 * @author helei
 * @since 2025/3/26 16:56
 */
@Slf4j
public class BotScriptAgent extends ScriptAgent {

    private final AtomicInteger initCount = new AtomicInteger(0);

    private final List<RPCServiceInfo<?>> rpcServiceInfos;

    private final BotScriptAgentLogUploadService logUploadService;
    @Setter
    private AutoLaunchBot<?> bot;

    public BotScriptAgent(ScriptAgentConfig clientConfig, List<RPCServiceInfo<?>> rpcServiceInfos) {
        super(clientConfig);
        this.rpcServiceInfos = rpcServiceInfos;
        this.logUploadService = new BotScriptAgentLogUploadService(this);
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

        // Step 3 其它初始化
        bot.logger.setBeforePrintHandler(logUploadService::pushLog);
    }

    private RemotingCommand startOrParsedBotJob(RemotingCommand remotingCommand) {
        String jobName = remotingCommand.getExtFieldsValue(BotExtFieldConstants.JOB_NAME);

        BotACJobResult botACJobResult = bot.startBotJob(jobName);

        RemotingCommand response = new RemotingCommand();
        if (BooleanUtil.isTrue(botACJobResult.getSuccess())) {
            response.setCode(RemotingCommandCodeConstants.SUCCESS);
            bot.logger.debug("start/parsed job[%s] success".formatted(jobName));
        } else {
            response.setCode(RemotingCommandCodeConstants.FAIL);
            bot.logger.error("start/parsed job[%s] fail".formatted(jobName));
        }

        response.setBody(Serializer.Algorithm.JDK.serialize(botACJobResult));
        return response;
    }
}
