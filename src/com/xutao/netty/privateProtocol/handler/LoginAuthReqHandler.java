package com.xutao.netty.privateProtocol.handler;

import com.xutao.netty.privateProtocol.message.Header;
import com.xutao.netty.privateProtocol.message.MessageType;
import com.xutao.netty.privateProtocol.message.NettyMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Created by Tau Hsu on 2017/6/21.
 */
public class LoginAuthReqHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(buildLoginReq());
    }

    private NettyMessage buildLoginReq() {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.LOGIN_REQ);
        message.setHeader(header);
        return message;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;
        if (message.getHeader() != null
                && message.getHeader().getType() == MessageType.LOGIN_RESP) {
            byte loginResult = (Byte) message.getBody();
            //认证失败
            if (loginResult != (byte) 0) {
                ctx.close();
            } else {
                System.out.println("Login is ok : " + message);
                //?????
                ctx.fireChannelRead(msg);
            }
        } else {
            //?????
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireExceptionCaught(cause);
    }
}
