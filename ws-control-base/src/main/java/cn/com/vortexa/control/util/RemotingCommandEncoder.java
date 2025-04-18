package cn.com.vortexa.control.util;

import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.common.util.protocol.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


public class RemotingCommandEncoder extends MessageToByteEncoder<RemotingCommand> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RemotingCommand remotingCommand, ByteBuf out)throws Exception{
        out.writeBytes(Serializer.Algorithm.Protostuff.serialize(remotingCommand));
        remotingCommand.release();
    }
}
