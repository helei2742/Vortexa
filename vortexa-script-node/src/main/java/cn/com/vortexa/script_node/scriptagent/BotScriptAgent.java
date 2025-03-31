package cn.com.vortexa.script_node.scriptagent;

import cn.com.vortexa.control.ScriptAgent;
import cn.com.vortexa.control.config.ScriptAgentConfig;
import cn.com.vortexa.control.dto.RPCArgsWrapper;
import cn.com.vortexa.control.dto.RPCServiceInfo;
import cn.com.vortexa.control.dto.RequestHandleResult;
import cn.com.vortexa.control.exception.CustomCommandException;
import cn.com.vortexa.control.exception.CustomCommandInvokeException;
import cn.com.vortexa.control.protocol.Serializer;
import cn.com.vortexa.control.util.RPCMethodUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
//        getCustomRemotingCommandHandlerMap().put(
//                BotRemotingCommandFlagConstants.START_UP_BOT_LOG,
//                logUploadService::startUploadLogRCHandler
//        );
//        getCustomRemotingCommandHandlerMap().put(
//                BotRemotingCommandFlagConstants.STOP_UP_BOT_LOG,
//                logUploadService::stopUploadLogRCHandler
//        );
    }
}
