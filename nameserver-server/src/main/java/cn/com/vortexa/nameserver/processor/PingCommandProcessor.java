package cn.com.vortexa.nameserver.processor;

import cn.com.vortexa.nameserver.dto.RemotingCommand;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author h30069248
 * @since 2025/3/18 9:46
 */
@Slf4j
public class PingCommandProcessor {
    public RemotingCommand handlerPing(String clientName, Channel channel, RemotingCommand remotingCommand) {
        log.debug("receive client[{}] ping", clientName);

        return null;
    }
}
