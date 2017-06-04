package com.xutao.netty.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

/**
 * Created by Tau Hsu on 2017/6/4.
 */
public class ChineseProverbClient {
    public void run(int port) throws Exception{
        EventLoopGroup group = new NioEventLoopGroup();
        try{
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST,true)
                    .handler(new ChineseProverbClientHandler());
            Channel channel = bootstrap.bind(0).sync().channel();
            //向网段内所有机器广播UDP消息
            channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("谚语字典查询？", CharsetUtil.UTF_8),new InetSocketAddress("255.255.255.255",port))).sync();
            if(!channel.closeFuture().await(15000)){
                System.out.println("查询超时");
            }
        }finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception{
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                throw new RuntimeException("The input params must bu integer", e);
            }
        }
        new ChineseProverbClient().run(port);
    }
}

class ChineseProverbClientHandler extends SimpleChannelInboundHandler<DatagramPacket>{

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
        //此处必须添加CharsetUtil.UTF_8，不然得不到满足要求的response
        String response = packet.content().toString(CharsetUtil.UTF_8);
//        System.out.println(response);
        if(response.startsWith("谚语查询结果：")){
            System.out.println(response);
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
