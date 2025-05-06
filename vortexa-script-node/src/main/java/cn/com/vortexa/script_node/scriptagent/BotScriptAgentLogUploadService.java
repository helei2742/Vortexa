package cn.com.vortexa.script_node.scriptagent;

import cn.com.vortexa.common.util.FileUtil;

import cn.com.vortexa.control.constant.ExtFieldsConstants;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.constants.BotExtFieldConstants;
import cn.com.vortexa.common.constants.BotRemotingCommandFlagConstants;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.hutool.core.util.StrUtil;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
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

    private final BlockingQueue<String> logBuffer
            = new LinkedBlockingQueue<>(AutoBotConfig.LOG_CACHE_COUNT * 10);

    private final AtomicBoolean upload = new AtomicBoolean(false);

    private volatile String bindUploadTXId;

    public BotScriptAgentLogUploadService(BotScriptAgent botScriptAgent) {
        this.botScriptAgent = botScriptAgent;
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
        String scriptNodeName = command.getExtFieldsValue(BotExtFieldConstants.TARGET_GROUP_KEY);
        String botName = command.getExtFieldsValue(BotExtFieldConstants.TARGET_BOT_NAME_KEY);
        String botKey = command.getExtFieldsValue(BotExtFieldConstants.TARGET_BOT_KEY_KEY);

        log.info("start upload log, logUploadTXID[{}]", logUploadTXID);
        RemotingCommand response = new RemotingCommand();
        response.setTransactionId(command.getTransactionId());
        response.setCode(RemotingCommandCodeConstants.FAIL);
        response.setFlag(BotRemotingCommandFlagConstants.START_UP_BOT_LOG_RESPONSE);

        // 开启日志上传任务
        if (startBotLogUploadTask(scriptNodeName, botKey, logUploadTXID)) {
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
     * @param botKey        botKey
     * @return boolean
     */
    private boolean startBotLogUploadTask(String scriptNodeName, String botKey, String logUploadTXID) {
        if (judgeUploadAble(logUploadTXID)) {
            synchronized (this) {
                if (judgeUploadAble(logUploadTXID)) {
                    bindUploadTXId = logUploadTXID;
                    logBuffer.clear();
                    startLogFileTailTask(scriptNodeName, botKey, logUploadTXID);
                    upload.set(true);
                } else {
                    return false;
                }
            }

            botScriptAgent.getCallbackInvoker().execute(() -> {
                while (upload.get()) {
                    try {
                        String oriTxId = bindUploadTXId;
                        String logContent = logBuffer.poll(LOG_SEND_MAX_INTERVAL, TimeUnit.SECONDS);

                        // 线程醒后需判断当前要发的txId有没有变化
                        if (!bindUploadTXId.equals(oriTxId) || !upload.get()) {
                            upload.set(false);
                            continue;
                        } else if (logContent == null) {
                            continue;
                        }

                        RemotingCommand command = botScriptAgent.newRequestCommand(
                                BotRemotingCommandFlagConstants.BOT_RUNTIME_LOG, true);
                        command.addExtField(BotExtFieldConstants.LOG_UPLOAD_TX_ID, logUploadTXID);
                        command.addExtField(BotExtFieldConstants.TARGET_BOT_KEY_KEY, botKey);
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

    /**
     * 开启读取日志文件线程
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey         botKey
     * @param logUploadTXID  logUploadTXID
     */
    private void startLogFileTailTask(String scriptNodeName, String botKey, String logUploadTXID) {
        botScriptAgent.getCallbackInvoker().execute(() -> {
            String logPath = FileUtil.getBotInstanceCurrentLogPath(scriptNodeName, botKey);
            File file = new File(logPath);

            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                long pointer = findStartOffsetFromLastLines(file, AutoBotConfig.LOG_CACHE_COUNT);
                raf.seek(pointer);

                // 把 FileDescriptor 包装成支持 UTF-8 的 reader
                try (
                        FileInputStream fis = new FileInputStream(raf.getFD());
                        InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                        BufferedReader reader = new BufferedReader(isr)
                ) {
                    String line;
                    while (true) {
                        while ((line = reader.readLine()) != null) {
                            if (!logUploadTXID.equals(this.bindUploadTXId)) {
                                log.warn("[{}] upload txId not used, stop tail log file [{}]", logUploadTXID, logPath);
                                return;
                            }
                            if (!logBuffer.offer(line, LOG_SEND_MAX_INTERVAL, TimeUnit.SECONDS)) {
                                log.warn("offer log content into buffer fail");
                                break;
                            }
                        }
                        TimeUnit.MILLISECONDS.sleep(500);
                    }
                }
            } catch (Exception e) {
                log.error("start log file tail task error", e);
            }
            if (logUploadTXID.equals(this.bindUploadTXId)) {
                upload.set(false);
            }
        });
    }

    public long findStartOffsetFromLastLines(File file, int lineCount) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long fileLength = raf.length();
            long pointer = fileLength - 1;
            int lines = 0;

            // 向前找换行符
            while (pointer >= 0) {
                raf.seek(pointer);
                if (raf.readByte() == '\n') {
                    lines++;
                    if (lines == lineCount + 1) {
                        break;
                    }
                }
                pointer--;
            }

            return Math.max(0, pointer + 1); // 行起始位置
        }
    }

    private boolean judgeUploadAble(String logUploadTXID) {
        return StrUtil.isNotBlank(logUploadTXID) && (!logUploadTXID.equals(bindUploadTXId)
                || upload.compareAndSet(false, true));
    }
}
