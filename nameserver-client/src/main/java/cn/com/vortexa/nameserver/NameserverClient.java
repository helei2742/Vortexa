package cn.com.vortexa.nameserver;


import cn.com.vortexa.nameserver.config.NameserverClientConfig;
import cn.com.vortexa.nameserver.constant.NameserverSystemConstants;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.processor.NameServerWSClientProcessor;
import cn.com.vortexa.nameserver.util.RemotingCommandDecoder;
import cn.com.vortexa.nameserver.util.RemotingCommandEncoder;
import cn.com.vortexa.websocket.netty.base.AbstractWebsocketClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author helei
 * @since 2025-03-13
 */
@Slf4j
public class NameserverClient extends AbstractWebsocketClient<RemotingCommand, RemotingCommand> {

    private final NameserverClientConfig clientConfig;


    public NameserverClient(NameserverClientConfig clientConfig) {
        super(
                clientConfig.getRegistryCenterUrl(),
                new NameServerWSClientProcessor(clientConfig)
        );
        this.clientConfig = clientConfig;

        setName(clientConfig.getServiceInstance().toString());
    }

    @Override
    protected void init() {
        bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override//链接建立后被调用，进行初始化
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new IdleStateHandler(0,
                                0, clientConfig.getServiceOfflineTtl()));

                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(NameserverSystemConstants.MAX_FRAME_LENGTH,
                                0, 4, 0, 4));
                        ch.pipeline().addLast(new LengthFieldPrepender(4));

                        ch.pipeline().addLast(new RemotingCommandEncoder());
                        ch.pipeline().addLast(new RemotingCommandDecoder());

                        ch.pipeline().addLast(handler);
                    }
                });
    }

    /**
     * 转换为写入channel的数据
     *
     */
    protected void convertToChannelWriteData(Channel channel, RemotingCommand request) {
        channel.writeAndFlush(request);
    }
}
