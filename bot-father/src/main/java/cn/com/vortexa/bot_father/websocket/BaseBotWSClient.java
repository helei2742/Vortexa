package cn.com.vortexa.bot_father.websocket;

import cn.com.vortexa.common.constants.ConnectStatus;
import cn.com.vortexa.common.entity.AccountContext;
import cn.com.vortexa.websocket.netty.base.AbstractWebsocketClient;
import cn.com.vortexa.websocket.netty.constants.WebsocketClientStatus;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public abstract class BaseBotWSClient<T> extends AbstractWebsocketClient<T> {

    private static final int MAX_FRAME_SIZE = 10 * 1024 * 1024;  // 10 MB or set to your desired size

    /**
     * client对应的账号
     */
    private final AccountContext accountContext;

    public BaseBotWSClient(
            AccountContext accountContext,
            String connectUrl,
            BaseBotWSClientHandler<T> handler
    ) {
        super(connectUrl, accountContext.getName(), handler);

        super.setProxy(accountContext.getProxy());
        super.setClientStatusChangeHandler(this::whenClientStatusChange);

        this.accountContext = accountContext;

        updateClientStatus(WebsocketClientStatus.NEW);
    }

    /**
     * 获取心跳消息
     *
     * @return T
     */
    public abstract T getHeartbeatMessage();

    /**
     * 账户收到响应时的处理
     *
     * @param id       id
     * @param response response
     */
    public abstract void whenAccountReceiveResponse(Object id, T response);

    /**
     * 账户收到消息处理
     *
     * @param message message
     */
    public abstract void whenAccountReceiveMessage(T message);

    /**
     * ws链接状态改变处理
     *
     * @param clientStatus clientStatus
     */
    public void whenAccountClientStatusChange(WebsocketClientStatus clientStatus) {
    }

    @Override
    public void addPipeline(ChannelPipeline p) {
        p.addLast("http-chunked", new ChunkedWriteHandler()); // 支持大数据流

        p.addLast(new HttpClientCodec());
        p.addLast(new HttpObjectAggregator(81920));
        p.addLast(new IdleStateHandler(0, 0, getAllIdleTimeSecond(), TimeUnit.SECONDS));
        p.addLast(new ChunkedWriteHandler());

        p.addLast(new WebSocketFrameAggregator(MAX_FRAME_SIZE));  // 设置聚合器的最大帧大小

        p.addLast(getHandler());
    }

    /**
     * ws客户端状态改变，同步更新账户状态
     *
     * @param newClientStatus 最新的客户端状态
     */
    public void whenClientStatusChange(WebsocketClientStatus newClientStatus) {
        ConnectStatus connectStatus = switch (newClientStatus) {
            case WebsocketClientStatus.NEW -> {
                accountContext.getConnectStatusInfo().setStartDateTime(LocalDateTime.now());
                accountContext.getConnectStatusInfo().setUpdateDateTime(LocalDateTime.now());
                yield ConnectStatus.NEW;
            }
            case WebsocketClientStatus.STARTING -> {
                accountContext.getConnectStatusInfo().setUpdateDateTime(LocalDateTime.now());
                yield ConnectStatus.STARTING;
            }
            case WebsocketClientStatus.RUNNING -> {
                accountContext.getConnectStatusInfo().setUpdateDateTime(LocalDateTime.now());
                yield ConnectStatus.RUNNING;
            }
            case WebsocketClientStatus.STOP, WebsocketClientStatus.SHUTDOWN -> {
                accountContext.getConnectStatusInfo().setUpdateDateTime(LocalDateTime.now());
                yield ConnectStatus.STOP;
            }
        };

        accountContext.getConnectStatusInfo().setConnectStatus(connectStatus);

        whenAccountClientStatusChange(newClientStatus);
    }

}
