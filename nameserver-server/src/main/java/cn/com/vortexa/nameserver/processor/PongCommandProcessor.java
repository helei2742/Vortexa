package cn.com.vortexa.nameserver.processor;

import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.server.NameserverService;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author helei
 * @since 2025/3/18 9:46
 */
@Slf4j
public class PongCommandProcessor {
    private final NameserverService nameserverService;

    public PongCommandProcessor(NameserverService nameserverService) {
        this.nameserverService = nameserverService;
    }

    public RemotingCommand handlerPong(String key, Channel channel, RemotingCommand remotingCommand) {
        log.debug("receive client[{}] pong", key);

        // 刷新链接
        nameserverService.getConnectionService().freshServiceInstanceConnection(
                key,
                channel
        );

        return null;
    }
}
