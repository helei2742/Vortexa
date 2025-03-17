package cn.com.vortexa.nameserver.handler;


import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.websocket.netty.base.NettyClientEventHandler;

/**
 * @author helei
 * @since 2025-03-15
 */
public interface NameserverClientEventHandler extends NettyClientEventHandler {

    default void initSuccessHandler(RemotingCommand remotingCommand){}

    default void initFailHandler(){}
}
