package cn.com.vortexa.nameserver.handler;

import cn.com.vortexa.nameserver.dto.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;


public interface NettyBaseHandler {

    /**
     * 发送消息
     *
     * @param context         context
     * @param remotingCommand 消息体
     */
    void sendMsg(ChannelHandlerContext context, RemotingCommand remotingCommand);

    /**
     * 打印日志
     *
     * @param logStr 日志字符串
     */
    void printLog(String logStr);
}
