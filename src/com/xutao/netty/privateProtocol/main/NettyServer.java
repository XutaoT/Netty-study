package com.xutao.netty.privateProtocol.main;


import com.xutao.netty.privateProtocol.coder.NettyMessageDecoder;
import com.xutao.netty.privateProtocol.coder.NettyMessageEncoder;
import com.xutao.netty.privateProtocol.handler.HeartBeatRespHandler;
import com.xutao.netty.privateProtocol.handler.LoginAuthRespHandler;
import com.xutao.netty.privateProtocol.message.NettyConstant;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
 * Created by Tau Hsu on 2017/6/22.
 */
public class NettyServer {
    public void bind() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workGroup).channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 100)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast("MessageDecoder", new NettyMessageDecoder(1204 * 1204, 4, 4));
                        ch.pipeline().addLast("MessageEncoder", new NettyMessageEncoder());
                        ch.pipeline().addLast("readTimeOutHandler", new ReadTimeoutHandler(50));
                        ch.pipeline().addLast("LoginAuthHandler", new LoginAuthRespHandler());
                        ch.pipeline().addLast("HeartBeatHandler", new HeartBeatRespHandler());
                    }
                });
        bootstrap.bind(NettyConstant.REMOTE_IP, NettyConstant.REMOTE_PORT).sync();
        System.out.println("Netty server start ok : " + (NettyConstant.REMOTE_IP + " : " + NettyConstant.REMOTE_PORT));
    }

    public static void main(String[] args) throws Exception {
        new NettyServer().bind();
    }

}
