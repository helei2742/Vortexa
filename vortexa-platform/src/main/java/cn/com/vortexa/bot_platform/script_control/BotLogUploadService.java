package cn.com.vortexa.bot_platform.script_control;

import cn.com.vortexa.common.constants.BotRemotingCommandFlagConstants;
import cn.com.vortexa.common.constants.BotExtFieldConstants;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.control.BotControlServer;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.dto.ConnectEntry;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.util.ControlServerUtil;
import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import cn.hutool.core.util.StrUtil;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author helei
 * @since 2025/3/26 15:16
 */
@Slf4j
public class BotLogUploadService {

    private final BotControlServer botControlServer;

    private final Map<String, String> botLogTxIdToBrowserKeyMap = new HashMap<>();

    public BotLogUploadService(BotControlServer botControlServer) {
        this.botControlServer = botControlServer;
    }

    /**
     * 浏览器请求获取bot的日志命令处理器
     *
     * @param browserChannel browserChannel
     * @param request request
     * @return RemotingCommand
     */
    public RemotingCommand browserRequestBotLogRCHandler(Channel browserChannel, RemotingCommand request) {
        RemotingCommand response = new RemotingCommand();
        response.setFlag(BotRemotingCommandFlagConstants.START_UP_BOT_LOG_RESPONSE);
        String txId = request.getTransactionId();
        response.setTransactionId(txId);

        // Step 0 参数校验
        String browserInstanceKey = browserChannel.attr(NettyConstants.CLIENT_NAME).get();
        if (StrUtil.isBlank(browserInstanceKey)) {
            log.error("browserInstanceKey is blank, browser channel[{}]", browserChannel.id());

            response.setCode(RemotingCommandCodeConstants.FAIL);
            response.setObjBody(Result.fail("browserInstanceKey is blank"));
            return response;
        }

        //.收到开始获取bot的日志命令
        String group = request.getExtFieldsValue(BotExtFieldConstants.TARGET_GROUP_KEY);
        String botName = request.getExtFieldsValue(BotExtFieldConstants.TARGET_BOT_NAME_KEY);
        String botKey = request.getExtFieldsValue(BotExtFieldConstants.TARGET_BOT_KEY_KEY);
        String botInstanceKey = ControlServerUtil.generateServiceInstanceKey(group, botName, botKey);

        log.info("receive command[{}] to start get bot[{}] runtime log",
                BotRemotingCommandFlagConstants.START_UP_BOT_LOG,
                botInstanceKey
        );

        ConnectEntry connectEntry = botControlServer.getConnectionService().getServiceInstanceChannel(
                botInstanceKey
        );

        // Step 1 判断bot是否可用
        if (connectEntry == null || !connectEntry.isUsable()) {
            log.warn("bot [{}] not running, can't upload runtime log", botInstanceKey);

            response.setCode(RemotingCommandCodeConstants.FAIL);
            response.setObjBody(Result.fail("bot cannot use"));
            return response;
        }

        // Step 2 发送命令到该Bot，让它开始发
        try {
            request.addExtField(BotExtFieldConstants.LOG_UPLOAD_TX_ID, txId);
            RemotingCommand botStartResponse = botControlServer.sendCommandToServiceInstance(
                    group, botName, botKey, request
            ).get();

            if (botStartResponse.getCode() == RemotingCommandCodeConstants.SUCCESS) {
                // Step 2.1 成功
                botLogTxIdToBrowserKeyMap.put(txId, browserInstanceKey);
                response.setFlag(RemotingCommandCodeConstants.SUCCESS);

                log.info("bot [{}] start upload runtime log", botInstanceKey);
            } else {
                // Step 2.2 失败
                response.setFlag(RemotingCommandCodeConstants.FAIL);
                response.setBody(botStartResponse.getBody());
                botLogTxIdToBrowserKeyMap.remove(txId);
                log.error("bot [{}] start upload runtime log error, [{}]", botInstanceKey, botStartResponse);
            }

            return response;
        } catch (Exception e) {
            log.error("bot [{}] start upload runtime log error", botInstanceKey, e);

            response.setCode(RemotingCommandCodeConstants.FAIL);
            response.setObjBody(Result.fail("unknown error"));
            return response;
        }
    }

    /**
     * bot上传日志命令处理器
     *
     * @param channel channel
     * @param command command
     * @return RemotingCommand
     */
    public RemotingCommand botUploadLogRCHandler(Channel channel, RemotingCommand command) {
        String logUploadTxId = command.getExtFieldsValue(BotExtFieldConstants.LOG_UPLOAD_TX_ID);
        String browserInstanceKey = botLogTxIdToBrowserKeyMap.get(logUploadTxId);
        String botInstanceKey = channel.attr(NettyConstants.CLIENT_NAME).get();

        RemotingCommand response = botControlServer.newRemotingCommand(BotRemotingCommandFlagConstants.BOT_RUNTIME_LOG_RESPONSE, false);
        response.addExtField(BotExtFieldConstants.LOG_UPLOAD_TX_ID, logUploadTxId);
        response.setCode(RemotingCommandCodeConstants.FAIL);

        // Step 1 浏览器实例请求不在map里，让bot取消上传
        if (StrUtil.isBlank(browserInstanceKey)) {
            log.warn("browser instance[{}] upload log request canceled, interrupt bot[{}] upload",
                    browserInstanceKey, botInstanceKey);
            stopBotUploadLog(botInstanceKey, logUploadTxId);
            return response;
        }

        // Step 2 查找请求该txId的log的browser channel，推送给它
        ConnectEntry browserConnect = botControlServer.getConnectionService().getServiceInstanceChannel(browserInstanceKey);
        if (browserConnect != null && browserConnect.isUsable()) {
            try {
                // 发送给browser
                RemotingCommand message = botControlServer.newRemotingCommand(BotRemotingCommandFlagConstants.BOT_RUNTIME_LOG, false);
                botControlServer.sendCommandToServiceInstance(browserInstanceKey, message).get();

                // 发送给browser成功后，返回响应
                response.setCode(RemotingCommandCodeConstants.SUCCESS);
            } catch (Exception e) {
                log.error("reword bot[{}] log to browser[{}] error", botInstanceKey, browserInstanceKey, e);
            }
        } else {
            // channel不可用，让bot不发送
            log.warn("browser instance[{}] channel cannot use", browserInstanceKey);
            stopBotUploadLog(botInstanceKey, logUploadTxId);
        }

        return response;
    }

    /**
     * 停止Bot上传日志
     *
     * @param botInstanceKey botInstanceKey
     * @return CompletableFuture<RemotingCommand>
     */
    public CompletableFuture<RemotingCommand> stopBotUploadLog(String botInstanceKey, String txId) {
        RemotingCommand command = botControlServer.newRemotingCommand(BotRemotingCommandFlagConstants.STOP_UP_BOT_LOG, true);

        // 不管是否成功都把map里的txId删掉
        botLogTxIdToBrowserKeyMap.remove(txId);

        return botControlServer.sendCommandToServiceInstance(
                botInstanceKey, command
        ).whenComplete((response, throwable) -> {
            if (throwable != null) {
                log.error("stop bot[{}] log upload error", botInstanceKey, throwable);
            } else if (response == null || response.getCode() == RemotingCommandCodeConstants.FAIL) {
                log.error("stop bot[{}] log upload fail, {}", botInstanceKey, response, throwable);
            } else {
                log.info("stop bot[{}] log upload success", botInstanceKey, throwable);
            }
        });
    }
}
