package cn.com.vortexa.nameserver.processor;


import cn.com.vortexa.nameserver.constant.NameserverSystemConstants;
import cn.com.vortexa.nameserver.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.nameserver.dto.RemotingCommand;
import cn.com.vortexa.nameserver.handler.ClientInitMsgHandler;
import cn.com.vortexa.nameserver.handler.NettyBaseHandler;
import cn.com.vortexa.nameserver.handler.ResultCallBackHandler;
import cn.com.vortexa.nameserver.pool.RemotingCommandPool;
import cn.com.vortexa.nameserver.util.NettyChannelSendSupporter;
import cn.com.vortexa.websocket.netty.base.NettyClientEventHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;


public abstract class AbstractNettyProcessorAdaptor extends SimpleChannelInboundHandler<RemotingCommand> implements NettyBaseHandler {

    protected boolean useRemotingCommandPool = true;

    protected int heartbeatCount = 0;

    protected ClientInitMsgHandler clientInitMsgHandler;

    protected ResultCallBackHandler resultCallBackHandler;

    public NettyClientEventHandler eventHandler;

    public AbstractNettyProcessorAdaptor() {
        this.eventHandler = new NettyClientEventHandler() {
            @Override
            public void exceptionHandler(ChannelHandlerContext ctx, Throwable cause) {
            }
        };
    }

    public AbstractNettyProcessorAdaptor(NettyClientEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    public void init(ClientInitMsgHandler clientInitMsgHandler,
                     ResultCallBackHandler resultCallBackHandler,
                     NettyClientEventHandler eventHandler) {

        this.eventHandler = eventHandler;
        this.resultCallBackHandler = resultCallBackHandler;
        this.clientInitMsgHandler = clientInitMsgHandler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        eventHandler.activeHandler(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, RemotingCommand remotingCommand) throws Exception {
        Integer opt = remotingCommand.getFlag();
        if (opt.equals(RemotingCommandFlagConstants.PING)) {
            handlePing(context, remotingCommand);
        } else if (opt.equals(RemotingCommandFlagConstants.PONG)) {
            handlePong(context, remotingCommand);
        } else {
            handlerMessage(context, remotingCommand);
        }
    }

    protected void handlePing(ChannelHandlerContext context, RemotingCommand remotingCommand) {
        sendPongMsg(context);
    }


    protected void handlePong(ChannelHandlerContext context, RemotingCommand remotingCommand) {
        printLog(String.format("get pong msg from [%s][%s] ",
                context.channel().attr(NameserverSystemConstants.CLIENT_ID_KEY).get(),
                context.channel().remoteAddress()));
    }

    protected abstract void handlerMessage(ChannelHandlerContext context, RemotingCommand remotingCommand);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        eventHandler.exceptionHandler(ctx, cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // IdleStateHandler 所产生的 IdleStateEvent 的处理逻辑.
        if (evt instanceof IdleStateEvent e) {
            switch (e.state()) {
                case READER_IDLE:
                    handleReaderIdle(ctx);
                    break;
                case WRITER_IDLE:
                    handleWriterIdle(ctx);
                    break;
                case ALL_IDLE:
                    handleAllIdle(ctx);
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * 超过限定时间channel没有读时触发
     *
     * @param ctx ctx
     */
    protected void handleReaderIdle(ChannelHandlerContext ctx) {
    }

    /**
     * 超过限定时间channel没有写时触发
     *
     * @param ctx ctx
     */
    protected void handleWriterIdle(ChannelHandlerContext ctx) {
    }

    /**
     * 设备下线处理
     *
     * @param ctx ctx
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        eventHandler.closeHandler(ctx.channel());
    }

    /**
     * 超过限定时间channel没有读写时触发
     *
     * @param ctx ctx
     */
    protected void handleAllIdle(ChannelHandlerContext ctx) {
    }

    public void sendPingMsg(ChannelHandlerContext context) {
        RemotingCommand remotingCommand;
        if (useRemotingCommandPool) {
            remotingCommand = RemotingCommandPool.getObject();
        } else {
            remotingCommand = new RemotingCommand();
        }

        remotingCommand.setFlag(RemotingCommandFlagConstants.PING);
        sendMsg(context, remotingCommand);

        printLog(String.format("send ping msg to [%s], hear beat count [%d]",
                context.channel().remoteAddress(), heartbeatCount++));
    }

    public void sendPongMsg(ChannelHandlerContext context) {
        RemotingCommand remotingCommand;

        if (useRemotingCommandPool) {
            remotingCommand = RemotingCommandPool.getObject();
        } else {
            remotingCommand = new RemotingCommand();
        }

        remotingCommand.setFlag(RemotingCommandFlagConstants.PONG);
        sendMsg(context, remotingCommand);

        printLog(String.format("send pong msg to [%s], hear beat count [%d]",
                context.channel().remoteAddress(), heartbeatCount++));
    }

    @Override
    public void sendMsg(ChannelHandlerContext context, RemotingCommand remotingCommand) {
        NettyChannelSendSupporter.sendMessage(remotingCommand, context.channel());
    }

}
