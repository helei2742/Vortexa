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
public class PingCommandProcessor {

    private final NameserverService nameserverService;

    public PingCommandProcessor(NameserverService nameserverService) {
        this.nameserverService = nameserverService;
    }

    public RemotingCommand handlerPing(String clientName, Channel channel, RemotingCommand remotingCommand) {
        log.debug("receive client[{}] ping", clientName);
        // 刷新连接状态
        nameserverService.getConnectionService().freshServiceInstanceConnection(clientName, channel);

        // 返回pong
        RemotingCommand pong = RemotingCommand.generatePongCommand(clientName);
        pong.setTransactionId(remotingCommand.getTransactionId());
        return pong;
    }
}
