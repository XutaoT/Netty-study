package com.xutao.netty.http.src.protobuf.protoStudy.demo;

import com.xutao.netty.http.src.protobuf.proto.SubscribeReqProto;
import com.xutao.netty.http.src.protobuf.proto.SubscribeRespProto;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tau Hsu on 2017/5/26.
 */
public class SubReqClient {
    public void connect(String host, int port) throws Exception{
        EventLoopGroup group = new NioEventLoopGroup();
        try{
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY,true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //多个解码器共同作用
                            //处理半包
                            ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                            //ProtobufDecoder(SubscribeRespProto.SubscribeResp.getDefaultInstance())只负责解码，在此之前要有处理半包的解码器
                            ch.pipeline().addLast(new ProtobufDecoder(SubscribeRespProto.SubscribeResp.getDefaultInstance()));
                            //对protobuf协议的的消息头上加上一个长度为32的整形字段，用于标志这个消息的长度,不加会报
                            /*
                            While parsing a protocol message, the input ended unexpectedly in the middle of a field.
                            This could mean either that the input has been truncated or that an embedded message misreported its own length.
                             */
                            ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            //编码器
                            ch.pipeline().addLast(new ProtobufEncoder());
                            ch.pipeline().addLast(new SubReqClientHandler());
                        }
                    });
            ChannelFuture future = bootstrap.connect(host,port).sync();
            future.channel().closeFuture().sync();
        }finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception{
        int port = 8080;
        if(args != null && args.length > 0){
            try{
                port = Integer.valueOf(args[0]);
            }catch (NumberFormatException e){
                throw new RuntimeException("The input params must be integer!",e);
            }
        }
        new SubReqClient().connect("127.0.0.1", port);
    }
}

class SubReqClientHandler extends ChannelInboundHandlerAdapter{
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        for (int i = 0; i < 10; i++) {
            ctx.write(createrReq());
        }
        ctx.flush();
    }

    private SubscribeReqProto.SubscribeReq createrReq() {
        SubscribeReqProto.SubscribeReq.Builder builder = SubscribeReqProto.SubscribeReq.newBuilder();
        builder.setUserName("xutao");
        builder.setSubReqID(0);
        builder.setProductName("Netty book for protoBuf!");
        List<String> address = new ArrayList<String>();
        address.add("Beijing");
        address.add("Nanjing");
        address.add("Shanghai");
        builder.addAllAddress(address);
        return builder.build();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("Receive server response : \n [" + msg + "]");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("SubReqClient ReadComplete ...");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      cause.printStackTrace();
      ctx.close();
    }
}
