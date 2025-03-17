package cn.com.vortexa.websocket.netty.base;

import cn.com.vortexa.websocket.netty.constants.NettyConstants;
import io.netty.channel.*;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


/**
 * WebSocket客户端处理器抽象类
 * 能够处理请求响应类型的消息。
 * 其它类型的消息要通过handleOtherMessage()抽象方法处理
 *
 * @param <Resp>
 */
@Slf4j
@ChannelHandler.Sharable
public abstract class AbstractWebSocketClientHandler<Req, Resp, T> extends BaseWebSocketClientHandler<Req, Resp, T> {

    /**
     * 存放请求响应的回调
     */
    protected final ConcurrentMap<Object, HandlerEntity<Resp>> requestIdMap = new ConcurrentHashMap<>();


    /**
     * 注册request
     *
     * @param request request
     * @return 是否注册成功
     */
    public boolean registryRequest(Req request, Consumer<Resp> callback) {
        AtomicBoolean res = new AtomicBoolean(false);
        Object requestId = getRequestId(request);

        if (requestId == null) return false;

        requestIdMap.compute(requestId, (k, v) -> {
            if (v == null) {
                res.set(true);
                long expireTime = System.currentTimeMillis() + NettyConstants.REQUEST_WAITE_SECONDS * 1000;
                v = new HandlerEntity<>(expireTime, callback);
                log.debug("registry request id[{}] success, expire time [{}]", requestId, expireTime);
            }
            return v;
        });

        return res.get();
    }

    /**
     * 获取请求id
     *
     * @param request request
     * @return id
     */
    public abstract Object getRequestId(Req request) ;
    /**
     * 获取响应id
     *
     * @param response 响应
     * @return id
     */
    public abstract Object getResponseId(Resp response);
}
