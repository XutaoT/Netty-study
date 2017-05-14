package com.xutao.netty.withError;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.logging.Logger;

/**
 * Created by Tau Hsu on 2017/5/12.
 * TCP粘包
 */
public class TimeClient {

    public void connect(int port, String host) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true).handler(
                    new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            System.out.println("client initChannel..");
                            ch.pipeline().addLast(new TimeClientHandler());
                        }
                    });
            ChannelFuture future = b.connect(host, port).sync();
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]).intValue();
            } catch (NumberFormatException e) {
                throw new RuntimeException("Input params must be String of Integer", e);
            }
        }

        new TimeClient().connect(port, "127.0.0.1");
    }
}

class TimeClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = Logger.getLogger(TimeClientHandler.class.getName());
    private int counter;
    byte[] req;

    public TimeClientHandler() {
        req = ("QUERY TIME ORDER" + System.getProperty("line.separator")).getBytes();

    }

    //当客户端和服务端的TCP连接上时，Netty的Nio线程会调用此方法
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
       ByteBuf message = null;
        for(int i= 0; i < 100; i++){
            message = Unpooled.buffer(req.length);
            message.writeBytes(req);
            ctx.writeAndFlush(message);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String body = new String(req, "UTF-8");
        System.out.println("Now is : " + body + "; the counter is :" + ++counter);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.warning("Unexpected exception from downstram : " + cause.getMessage());
        ctx.close();
    }
}

