package cn.com.vortexa.control_server.processor;

import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control_server.BotControlServer;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author helei
 * @since 2025/3/18 9:46
 */
@Slf4j
public class PingCommandProcessor {

    private final BotControlServer botControlServer;

    public PingCommandProcessor(BotControlServer botControlServer) {
        this.botControlServer = botControlServer;
    }

    public RemotingCommand handlerPing(String clientName, Channel channel, RemotingCommand remotingCommand) {
        log.debug("receive client[{}] ping", clientName);
        botControlServer.handleServiceInstancePingCommand(channel, clientName, remotingCommand);
        // 返回pong
        RemotingCommand pong = RemotingCommand.generatePongCommand(clientName);
        pong.setTransactionId(remotingCommand.getTransactionId());
        return pong;
    }
}
