package cn.com.vortexa.bot_platform.script_control.service;

import cn.com.vortexa.bot_platform.script_control.BotPlatformControlServer;
import cn.com.vortexa.bot_platform.wsController.FrontWSException;
import cn.com.vortexa.bot_platform.wsController.FrontWebSocketServer;
import cn.com.vortexa.bot_platform.wsController.UIWSMessage;
import cn.com.vortexa.common.constants.BotRemotingCommandFlagConstants;
import cn.com.vortexa.common.constants.BotExtFieldConstants;
import cn.com.vortexa.common.entity.ScriptNode;
import cn.com.vortexa.control.constant.ExtFieldsConstants;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.util.ControlServerUtil;
import cn.com.vortexa.control_server.dto.ConnectEntry;
import cn.hutool.core.util.StrUtil;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author helei
 * @since 2025/3/26 15:16
 */
@Slf4j
public class BotLogUploadService {

    private final BotPlatformControlServer platformControlServer;

    private final Map<String, String> botLogTxIdToFrontTokenMap = new HashMap<>();
    private final Map<String, String> botLogTxIdToFrontInstanceKeyMap = new HashMap<>();

    public BotLogUploadService(BotPlatformControlServer platformControlServer) {
        this.platformControlServer = platformControlServer;
    }

    /**
     * 浏览器请求获取bot的日志命令处理器
     *
     * @return RemotingCommand
     */
    public UIWSMessage browserRequestBotLogRCHandler(
            String group, String botName, String botKey, String token
    ) {
        UIWSMessage.UIWSMessageBuilder responseBuilder = UIWSMessage.builder();

        // Step 0 参数校验
        if (StrUtil.isBlank(botName) || StrUtil.isBlank(botKey)) {
            log.error("botName/botKey is blank");

            responseBuilder.success(false);
            responseBuilder.errorMsg("browserInstanceKey is blank");
            return responseBuilder.build();
        }

        //.收到开始获取bot的日志命令
        String botInstanceKey = ControlServerUtil.generateServiceInstanceKey(group, botName, botKey);

        log.info("receive command[{}] to start get bot[{}] runtime log",
                BotRemotingCommandFlagConstants.START_UP_BOT_LOG,
                botInstanceKey
        );

        ScriptNode scriptNode = platformControlServer.getScriptNodeService().queryByScriptNodeName(group);
        String scriptNodeKey = null;
        if (scriptNode == null) {
            responseBuilder.success(false);
            responseBuilder.errorMsg("scriptNode not found");
            return responseBuilder.build();
        } else {
            scriptNodeKey =  ControlServerUtil.generateServiceInstanceKey(
                    scriptNode.getGroupId(), scriptNode.getServiceId(), scriptNode.getInstanceId()
            );
            ConnectEntry connectEntry = platformControlServer.getConnectionService().getServiceInstanceChannel(
                    scriptNodeKey
            );
            if (connectEntry == null || !connectEntry.isUsable()) {
                responseBuilder.success(false);
                responseBuilder.errorMsg("scriptNode connection can't use");
                return responseBuilder.build();
            }
        }
//        BotInstanceStatus status = platformControlServer.getBotInstanceStatus(botInstanceKey);

//        // Step 1 判断bot是否可用
//        if (status != BotInstanceStatus.RUNNING) {
//            log.warn("bot [{}] not running, can't upload runtime log", botInstanceKey);
//
//            responseBuilder.success(false);
//            responseBuilder.errorMsg("browser instance status is " + status);
//            return responseBuilder.build();
//        }


        // Step 2 发送命令到该Bot，让它开始发
        try {
            RemotingCommand command = platformControlServer.newRemotingCommand(
                    BotRemotingCommandFlagConstants.START_UP_BOT_LOG,
                    true
            );
            String logUploadTxId = platformControlServer.nextTxId();
            command.addExtField(BotExtFieldConstants.LOG_UPLOAD_TX_ID, logUploadTxId);
            command.addExtField(BotExtFieldConstants.TARGET_GROUP_KEY, group);
            command.addExtField(BotExtFieldConstants.TARGET_BOT_NAME_KEY, botName);
            command.addExtField(BotExtFieldConstants.TARGET_BOT_KEY_KEY, botKey);

            botLogTxIdToFrontTokenMap.put(logUploadTxId, token);
            botLogTxIdToFrontInstanceKeyMap.put(logUploadTxId, botInstanceKey);

            // 发送，并等待结果
            RemotingCommand botStartResponse = platformControlServer.sendCommandToServiceInstance(
                    scriptNodeKey, command
            ).get();

            if (botStartResponse.getCode() == RemotingCommandCodeConstants.SUCCESS) {
                // Step 2.1 成功,
                responseBuilder.success(true);

                log.info("bot [{}] start upload runtime log", botInstanceKey);
            } else {
                // Step 2.2 失败
                responseBuilder.success(false);
                responseBuilder.errorMsg(botStartResponse.getExtFieldsValue(
                        ExtFieldsConstants.REQUEST_ERROR_MSG
                ));
                botLogTxIdToFrontTokenMap.remove(logUploadTxId);
                botLogTxIdToFrontInstanceKeyMap.remove(logUploadTxId);

                log.error("bot [{}] start upload runtime log error, [{}]", botInstanceKey, botStartResponse);
            }

            return responseBuilder.build();
        } catch (Exception e) {
            log.error("bot [{}] start upload runtime log error", botInstanceKey, e);

            responseBuilder.success(false);
            responseBuilder.errorMsg("unknown error");
            return responseBuilder.build();
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
        String token = botLogTxIdToFrontTokenMap.get(logUploadTxId);
        String botInstanceKey = botLogTxIdToFrontInstanceKeyMap.get(logUploadTxId);

        RemotingCommand response = platformControlServer.newRemotingCommand(BotRemotingCommandFlagConstants.BOT_RUNTIME_LOG_RESPONSE, false);
        response.addExtField(BotExtFieldConstants.LOG_UPLOAD_TX_ID, logUploadTxId);
        response.setCode(RemotingCommandCodeConstants.FAIL);

        FrontWebSocketServer frontWebSocketServer = platformControlServer.getFrontWebSocketServer();

        // Step 1 浏览器实例请求不在map里，让bot取消上传
        if (!frontWebSocketServer.isSessionOnline(token)) {
            log.warn("browser instance[{}] upload log request canceled, interrupt bot[{}] upload",
                    token, botInstanceKey);
            stopBotUploadLog(botInstanceKey, logUploadTxId);

            response.addExtField(
                    ExtFieldsConstants.REQUEST_ERROR_MSG,
                    "log upload canceled"
            );
            return response;
        }

        // Step 2 推送给前端
        try {
            frontWebSocketServer.sendMessage(
                    token,
                    UIWSMessage.builder()
                            .code(BotRemotingCommandFlagConstants.BOT_RUNTIME_LOG)
                            .success(true)
                            .message(String.valueOf(command.getPayLoad()))
                            .build()
            );

            response.setCode(RemotingCommandCodeConstants.SUCCESS);
        } catch (FrontWSException e) {
            // channel不可用，让bot不发送
            log.error("push bot[{}] log to front[{}] error", botInstanceKey, token, e);
            stopBotUploadLog(botInstanceKey, logUploadTxId);

            response.addExtField(
                    ExtFieldsConstants.REQUEST_ERROR_MSG,
                    e.getCause() == null ? e.getCause().getMessage() : e.getMessage()
            );
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
        // 不管是否成功都把map里的txId删掉
        String token = botLogTxIdToFrontTokenMap.remove(txId);
        botLogTxIdToFrontInstanceKeyMap.remove(txId);

        if (token == null) {
            return CompletableFuture.completedFuture(null);
        }

        RemotingCommand command = platformControlServer.newRemotingCommand(BotRemotingCommandFlagConstants.STOP_UP_BOT_LOG, true);

        return platformControlServer.sendCommandToServiceInstance(
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

    /**
     * 去除监听
     *
     * @param token token
     */
    public void stopFrontLogListener(String token) {
        Iterator<Map.Entry<String, String>> iterator = botLogTxIdToFrontTokenMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (entry.getValue().equals(token)) {
                stopBotUploadLog(botLogTxIdToFrontInstanceKeyMap.get(entry.getKey()), token);

                iterator.remove();
            }
        }
    }
}
