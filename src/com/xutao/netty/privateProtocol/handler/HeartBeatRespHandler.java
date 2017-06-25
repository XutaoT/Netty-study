package com.xutao.netty.privateProtocol.handler;

import com.xutao.netty.privateProtocol.message.Header;
import com.xutao.netty.privateProtocol.message.MessageType;
import com.xutao.netty.privateProtocol.message.NettyMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Created by Tau Hsu on 2017/6/21.
 */
public class HeartBeatRespHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;
        if (message.getHeader() != null
                && message.getHeader().getType() == MessageType.HERTBEAT_REQ) {
            System.out.println("Receive client heart beat message : ---> " + message);
            message = buildHeartBeat();
            System.out.println("Send heart beat response message to client : ---> " + message);
            ctx.writeAndFlush(message);
        }else {
            ctx.fireChannelRead(msg);
        }
    }

    private NettyMessage buildHeartBeat() {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.HERTBEAT_RESP);
        message.setHeader(header);
        return message;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireExceptionCaught(cause);
    }
}
