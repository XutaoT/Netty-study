package com.xutao.netty.webSocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpUtil.setContentLength;

/**
 * Created by Tau Hsu on 2017/5/30.
 */
public class WebSocketServer {
    public void run(int port) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("http-codec", new HttpServerCodec());
                            pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));
                            pipeline.addLast("http-chunked", new ChunkedWriteHandler());
                            pipeline.addLast("webSocket-handler", new WebSocketServerHandler());
                        }
                    });
            ChannelFuture future = bootstrap.bind(port).sync();
            future.channel().closeFuture().sync();

        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                throw new RuntimeException("The input params must bu integer", e);
            }
        }
        new WebSocketServer().run(port);
    }
}

class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger LOGGER = Logger.getLogger(WebSocketServerHandler.class.getName());
    private WebSocketServerHandshaker serverHandshaker;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHtttpRequst(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception{
        if(frame instanceof CloseWebSocketFrame){
            serverHandshaker.close(ctx.channel(),(CloseWebSocketFrame)frame.retain());
            return;
        }
        if(frame instanceof PingWebSocketFrame){
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if(!(frame instanceof TextWebSocketFrame)){
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
        }

        String requst = ((TextWebSocketFrame)frame).text();
        if(LOGGER.isLoggable(Level.FINE)){
            LOGGER.fine(String.format("%s received %s", ctx.channel(),requst));
        }
        ctx.channel().write(new TextWebSocketFrame(requst + ", 欢迎使用Netty WebSocket 服务，现在的是时刻：" + new Date().toString()));

    }

    private void handleHtttpRequst(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception{
        if (!req.decoderResult().isSuccess()
                || (!"websocket".equalsIgnoreCase(req.headers().get("Upgrade")))) {
            sendHttpResponse(ctx,req,new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,BAD_REQUEST));
            return;
        }

        WebSocketServerHandshakerFactory handshakerFactory = new WebSocketServerHandshakerFactory(
                "ws://localhost:8080/websocket",null,false);
        serverHandshaker = handshakerFactory.newHandshaker(req);
        if(serverHandshaker == null){
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }else {
            serverHandshaker.handshake(ctx.channel(),req);
        }

    }

    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse resp) {
        if(resp.status().code() != 200){
            ByteBuf buf = Unpooled.copiedBuffer(resp.status().toString(), CharsetUtil.UTF_8);
            resp.content().writeBytes(buf);
            buf.release();
            setContentLength(resp,resp.content().readableBytes());
        }

        ChannelFuture f =  ctx.channel().writeAndFlush(resp);
        if(!isKeepAlive(req) || resp.status().code() != 200){
            f.addListener(ChannelFutureListener.CLOSE);
        }

    }

}
