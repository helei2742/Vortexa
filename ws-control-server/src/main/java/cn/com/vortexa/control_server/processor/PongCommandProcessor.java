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
public class PongCommandProcessor {
    private final BotControlServer botControlServer;

    public PongCommandProcessor(BotControlServer botControlServer) {
        this.botControlServer = botControlServer;
    }

    public RemotingCommand handlerPong(String key, Channel channel, RemotingCommand remotingCommand) {
        log.debug("receive client[{}] pong", key);

        // 刷新链接
        botControlServer.getConnectionService().freshServiceInstanceConnection(
                key,
                channel
        );

        return null;
    }
}
