package com.xutao.netty.http.fileServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.LOCATION;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpUtil.setContentLength;

/**
 * Created by Tau Hsu on 2017/5/28.
 */
public class HttpFileServer {
    private static final String DEFAULT_URI = "/src/com/xutao/netty/http/src";

    public void run(final int port, final String uri) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workGroup).channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //Http解码器在每个Http消息中会生成多个消息对象：HttpRequest/HttpResponse,HttpContent,LastHttpContent
                            ch.pipeline().addLast("http-decoder", new HttpRequestDecoder());
                            //HttpObjectAggregator解码器，将多个信息转化为单一的FullHttpRequest或者FullHttpResponse
                            ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
                            ch.pipeline().addLast("http-encode", new HttpResponseEncoder());
                            //ChunkedWriteHandler，支持异步发送大的码流（例如大文件传输），不会占用过多的内存，防止发生java内存溢出
                            ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                            ch.pipeline().addLast("fileServerHandler", new HttpFileServerHandler(uri));
                        }
                    });
            ChannelFuture future = bootstrap.bind("192.168.0.14", port).sync();
            System.out.println("Http 文件目录服务器启动，网址是：http://192.168.0.14:" + port + uri);
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 80;
        String uri = DEFAULT_URI;
        new HttpFileServer().run(port, uri);
    }
}

class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final String uri;

    public HttpFileServerHandler(String uri) {
        this.uri = uri;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        if (request.method() != HttpMethod.GET) {
            sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }

        final String uri = request.uri();
        final String path = sanitizeUri(uri);
        if (path == null) {
            sendError(ctx, FORBIDDEN);
            return;
        }
        File file = new File(path);
        if (file.isHidden() || !file.exists()) {
            sendError(ctx, NOT_FOUND);
        }

        if (file.isDirectory()) {
            if (uri.endsWith("/")) {
                sendListing(ctx, file);
            } else {
                sendRedirect(ctx, uri + "/");
            }
            return;
        }
        if (!file.isFile()) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            sendError(ctx, NOT_FOUND);
            return;
        }
        long fileLength = randomAccessFile.length();
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, FOUND);
        setContentLength(response, fileLength);
        setContentTypeHeader(response, file);
        if (isKeepAlive(request)) {
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }
        ctx.write(response);
        ChannelFuture sendChannelFuture;
        sendChannelFuture = ctx.write(
                new ChunkedFile(randomAccessFile, 0, fileLength, 8192)
                , ctx.newProgressivePromise());
        sendChannelFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
                if (total < 0) {
                    System.err.println("Transfer progress: " + progress);
                } else {
                    System.err.println("Transfer progress: " + progress + "/" + total);
                }

            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) throws Exception {
                System.out.println("Transfer Complete ... ");
            }
        });
        ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!isKeepAlive(request)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        if(ctx.channel().isActive()){
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }

    private static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
        response.headers().set(CONTENT_TYPE, mimetypesFileTypeMap.getContentType(file.getPath()));
    }

    private void sendRedirect(ChannelHandlerContext ctx, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, FOUND);
        response.headers().set(LOCATION, newUri);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static final Pattern ALLOWEN_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");

    private void sendListing(ChannelHandlerContext ctx, File file) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, OK);
        response.headers().set(CONTENT_TYPE, "text/html;charset=UTF-8");
        StringBuffer buffer = new StringBuffer();
        String dir = file.getPath();
        buffer.append("<!DOCTYPE html>\r\n");
        buffer.append("<html><head><title>");
        buffer.append(dir);
        buffer.append(" 目录: ");
        buffer.append("</title></head><body>\r\n");
        buffer.append("<h3>");
        buffer.append(dir).append(" 目录: ");
        buffer.append("</h3>");
        buffer.append("<ul>");
        buffer.append("<li>连接：<a href=\"../\">..</a></li>\r\n");
        for (File f : file.listFiles()) {
            if (f.isHidden() || !f.canRead()) {
                continue;
            }
            String name = f.getName();
            if (!ALLOWEN_FILE_NAME.matcher(name).matches()) {
                continue;
            }
            buffer.append("<li>连接：<a href=\"");
            buffer.append(name);
            buffer.append("\">");
            buffer.append(name);
            buffer.append("</a></li>\r\n");
        }
        buffer.append("</ul></body></html>\r\n");
        ByteBuf buf = Unpooled.copiedBuffer(buffer, CharsetUtil.UTF_8);
        response.content().writeBytes(buf);
        buf.release();
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

    private String sanitizeUri(String uri) {
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            try {
                uri = URLDecoder.decode(uri, "ISO-8859-1");
            } catch (UnsupportedEncodingException e1) {
                throw new Error("UnsupportedEncoding ...");
            }
        }
        if (!uri.startsWith(this.uri)) {
            return null;
        }

        if (!uri.startsWith("/")) {
            return null;
        }
        uri = uri.replace('/', File.separatorChar);
        if (uri.contains(File.separator + ".")
                || uri.contains("." + File.separator)
                || uri.startsWith(".")
                || uri.endsWith(".")
                || INSECURE_URI.matcher(uri).matches()) {
            return null;
        }
        String path = System.getProperty("user.dir") + uri;
        System.out.println("src path: " + path);
        return path;
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
