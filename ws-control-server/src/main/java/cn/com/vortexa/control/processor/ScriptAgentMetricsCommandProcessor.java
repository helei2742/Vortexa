package cn.com.vortexa.control.processor;

import cn.com.vortexa.control.BotControlServer;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.dto.ScriptAgentMetrics;

/**
 * Script Agent 指标上传命令处理器
 *
 * @author h30069248
 * @since 2025/3/24 15:47
 */
public class ScriptAgentMetricsCommandProcessor {
    private final BotControlServer botControlServer;

    public ScriptAgentMetricsCommandProcessor(BotControlServer botControlServer) {
        this.botControlServer = botControlServer;
    }

    /**
     * 处理script agent指标上传
     *
     * @param key key
     * @param command command
     * @return RemotingCommand
     */
    public RemotingCommand handlerScriptAgentMetricsUpload(String key, RemotingCommand command) {
        // Step 1 提取数据
        ScriptAgentMetrics agentMetrics = command.getObjBodY(ScriptAgentMetrics.class);

        int insert =  botControlServer.getMetricsService().saveAgentMetrics(agentMetrics);

        if (insert > 0) {
            throw new RuntimeException("save script agent metrics error");
        }

        RemotingCommand response = new RemotingCommand();
        response.setTransactionId(command.getTransactionId());
        response.setFlag(RemotingCommandFlagConstants.SCRIPT_AGENT_METRICS_UPLOAD_RESPONSE);
        response.setCode(RemotingCommandCodeConstants.SUCCESS);
        return response;
    }
}
