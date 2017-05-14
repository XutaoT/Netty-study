package com.xutao.netty.server;

//lunix执行命令
//java -cp .:./netty-all-4.1.9.Final.jar  com.xutao.netty.server.TimeServer
//java -cp ./Netty-study/:./Netty-study/netty-all-4.1.9.Final.jar  com.xutao.netty.server.TimeServer
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.Date;

/**
 * Created by Tau Hsu on 2017/5/3.
 */
public class TimeServer {

    public void bind(int port) throws Exception {
        //配置服务端的NIO线程组,Reactor线程组，包含一组Nio线程，一个用于接受客户端连接，一个用于SocketChannel网络读写
        EventLoopGroup bossGroup = new NioEventLoopGroup();

        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            //Netty用于启动NIO服务端的启动类
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)//创建一个NioServerSocketChannel的channel,功能对应与JDK中的ServerSocketChannel
                    .option(ChannelOption.SO_BACKLOG, 1024)//设置TCP的参数，此处将backlog设置成1024
                    .childHandler(new ChildChannelHandler());//绑定I/O事件的处理对象，ChannelHandler，作用类似于Reactor的handler模式
            //绑定端口，同步阻塞等待成功
            ChannelFuture future = bootstrap.bind(port).sync();//ChannelFuture类似与JDK中java.util.concurrent.Future
            //等待服务端监听端口关闭
            future.channel().closeFuture().sync();
        }finally {
            //优雅退出，释放线程池资源
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }

    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel>{
        //只有连接连接上时，此方法才会执行
        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            System.out.println("server initChannel..");
            socketChannel.pipeline().addLast(new TimeServerHandler());
        }
    }

    public static void main(String[] args) throws Exception{
        int port = 8080;
        if(args != null && args.length > 0){
            try{
                port = Integer.valueOf(args[0]);
            }catch (NumberFormatException e){
                throw new RuntimeException("Input params must be String of Integer");
            }
        }
        new TimeServer().bind(port);
    }
}

//书中ChannelHandlerAdapter
class  TimeServerHandler extends ChannelInboundHandlerAdapter{


    @Override
    public void channelRead(ChannelHandlerContext context, Object msg) throws Exception{
        ByteBuf buf = (ByteBuf) msg;//类似于ByteBufer,但功能更强大灵活
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String body = new String(req,"UTF-8");
        System.out.println("The time server recevie order : " + body);
        String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body)?
                new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
        ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
        context.write(resp);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("server channelReadComplete..");
        ctx.flush();//刷新后才将数据发出到SocketChannel
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        System.out.println("server exceptionCaught..");
        ctx.close();
    }

}
