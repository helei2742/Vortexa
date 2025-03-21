package cn.com.vortexa.control.util;

import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.protocol.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class RemotingCommandDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(in.readableBytes() >= 0) {
            byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);
            out.add(Serializer.Algorithm.Protostuff.deserialize(bytes, RemotingCommand.class));
        }
    }
}
