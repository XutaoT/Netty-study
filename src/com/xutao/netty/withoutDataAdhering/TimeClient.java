package com.xutao.netty.withoutDataAdhering;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.util.logging.Logger;

/**
 * Created by Tau Hsu on 2017/5/14.
 */
public class TimeClient {
    private void connect(int port, String host) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).option(ChannelOption.TCP_NODELAY, true).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    System.out.println("client initChannel..");
                    ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                    ch.pipeline().addLast(new StringDecoder());
                    ch.pipeline().addLast(new TimeClientHandler());

                }
            });
            ChannelFuture future = bootstrap.connect(host, port).sync();
            future.channel().closeFuture().sync();

        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception{
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

class TimeClientHandler extends ChannelInboundHandlerAdapter{
    private static final Logger LOGGER = Logger.getLogger(TimeClientHandler.class.getName());
    private int counter;
    private byte[] req;

    public TimeClientHandler(){
        req = ("QUERY TIME ORDER" + System.getProperty("line.separator")).getBytes();
        System.out.println("rep length : "+ ("QUERY TIME ORDER" + System.getProperty("line.separator")).length());
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ByteBuf message = null;
        for (int i = 0; i < 100; i++){
            message = Unpooled.buffer(req.length);
            message.writeBytes(req);
            ctx.writeAndFlush(message);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String resp = (String)msg;//只会收到去除掉“line.separator”字符串
        System.out.println("resp length : " + resp.length());
        System.out.println("Now is : " + resp + "; the counter is :" + ++counter);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.warning("Unexpected exception from downstram : " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}
