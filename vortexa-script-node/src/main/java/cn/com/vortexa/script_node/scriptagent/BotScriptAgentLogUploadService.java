package cn.com.vortexa.script_node.scriptagent;

import cn.com.vortexa.control.constant.ExtFieldsConstants;
import cn.com.vortexa.script_node.config.AutoBotConfig;
import cn.com.vortexa.common.constants.BotExtFieldConstants;
import cn.com.vortexa.common.constants.BotRemotingCommandFlagConstants;
import cn.com.vortexa.common.util.DiscardingBlockingQueue;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.script_node.util.log.AppendLogger;
import cn.hutool.core.util.StrUtil;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author helei
 * @since 2025/3/26 17:04
 */
@Slf4j
public class BotScriptAgentLogUploadService {

    public static final int LOG_SEND_MAX_INTERVAL = 30;

    private final BotScriptAgent botScriptAgent;

    private final DiscardingBlockingQueue<String> logCache = new DiscardingBlockingQueue<>(
            AutoBotConfig.LOG_CACHE_COUNT);

    private final AtomicBoolean upload = new AtomicBoolean(false);

    private volatile String bindUploadTXId;

    public BotScriptAgentLogUploadService(BotScriptAgent botScriptAgent) {
        this.botScriptAgent = botScriptAgent;
    }

    public void pushLog(String content) {
        try {
            logCache.put(content);
        } catch (InterruptedException e) {
            log.error("put script node log error", e);
        }
    }

    /**
     * 开始上传日志命令处理器
     *
     * @param channel channel
     * @param command command
     * @return RemotingCommand
     */
    public RemotingCommand startUploadLogRCHandler(Channel channel, RemotingCommand command) {
        // 开始上传日志
        String logUploadTXID = command.getExtFieldsValue(BotExtFieldConstants.LOG_UPLOAD_TX_ID);

        log.info("start upload log, logUploadTXID[{}]", logUploadTXID);
        RemotingCommand response = new RemotingCommand();
        response.setTransactionId(command.getTransactionId());
        response.setCode(RemotingCommandCodeConstants.FAIL);
        response.setFlag(BotRemotingCommandFlagConstants.START_UP_BOT_LOG_RESPONSE);

        // 开启日志上传任务
        if (startBotLogUploadTask(logUploadTXID)) {
            response.setCode(RemotingCommandCodeConstants.SUCCESS);
            log.info("start log upload task success, logUploadTXID[{}]", logUploadTXID);
        } else {
            log.error("start log upload task error, logUploadTXID[{}]", logUploadTXID);
            response.addExtField(ExtFieldsConstants.REQUEST_ERROR_MSG,
                    "script node log in uploading");
        }

        return response;
    }

    /**
     * 关闭上传日志命令处理器
     *
     * @param channel channel
     * @param command command
     * @return RemotingCommand
     */
    public RemotingCommand stopUploadLogRCHandler(Channel channel, RemotingCommand command) {
        upload.set(false);

        RemotingCommand response = new RemotingCommand();
        response.setTransactionId(command.getTransactionId());
        response.setCode(RemotingCommandCodeConstants.SUCCESS);
        response.setFlag(BotRemotingCommandFlagConstants.STOP_UP_BOT_LOG);

        return response;
    }

    /**
     * 开启bot日志上传任务
     *
     * @param logUploadTXID logUploadTXID
     * @return boolean
     */
    private boolean startBotLogUploadTask(String logUploadTXID) {

        if (judgeUploadAble(logUploadTXID)) {
            synchronized (this) {
                if (judgeUploadAble(logUploadTXID)) {
                    bindUploadTXId = logUploadTXID;
                    AppendLogger logger = botScriptAgent.getBot().logger;
                    logger.setBeforePrintHandler(this::pushLog);
                    String[] cachedLog = logger.getLogCache().toArray(new String[0]);

                    logCache.clear();
                    for (String logStr : cachedLog) {
                        try {
                            logCache.put(logStr);
                        } catch (InterruptedException e) {
                            log.error("put script node log error", e);
                            upload.set(false);
                            throw new RuntimeException(e);
                        }
                    }
                    upload.set(true);
                } else {
                    return false;
                }
            }

            botScriptAgent.getCallbackInvoker().execute(() -> {
                while (upload.get()) {
                    try {
                        String oriTxId = bindUploadTXId;
                        String logContent = logCache.poll(LOG_SEND_MAX_INTERVAL, TimeUnit.SECONDS);

                        // 线程醒后需判断当前要发的txId有没有变化
                        if (!bindUploadTXId.equals(oriTxId) || !upload.get()) {
                            upload.set(false);
                            continue;
                        }

                        RemotingCommand command = botScriptAgent.newRequestCommand(
                                BotRemotingCommandFlagConstants.BOT_RUNTIME_LOG, true);
                        command.addExtField(BotExtFieldConstants.LOG_UPLOAD_TX_ID, logUploadTXID);
                        command.setPayLoad(logContent);

                        log.debug("upload log, logUploadTXID[{}]", logUploadTXID);
                        RemotingCommand response = botScriptAgent.sendRequest(command).get();
                        if (response == null || !response.isSuccess()) {
                            log.error("upload log response error, {}", response);
                            upload.set(false);
                        }
                    } catch (InterruptedException e) {
                        upload.set(false);
                        log.error("log upload task interrupted");
                    } catch (ExecutionException e) {
                        upload.set(false);
                        log.error("upload log error", e);
                    }
                }
                log.warn("stopped upload log upload task");
            });
            return true;
        }
        return false;
    }

    private boolean judgeUploadAble(String logUploadTXID) {
        return StrUtil.isNotBlank(logUploadTXID) && (!logUploadTXID.equals(bindUploadTXId)
                || upload.compareAndSet(false, true));
    }
}
