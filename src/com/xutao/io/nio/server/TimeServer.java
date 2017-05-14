package com.xutao.io.nio.server;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Tau Hsu on 2017/4/25.
 *
 * @author Tau Hsu
 * @version 1.0s
 */
public class TimeServer {
    public static void main(String[] args) {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Input params must be String of Integer", e);
            }
        }
        MultiplexerTimerServer multiplexerTimerServer = new MultiplexerTimerServer(port);
        new Thread(multiplexerTimerServer, "nio-MultiplexerTimerServer-001").start();
    }
}

class MultiplexerTimerServer implements Runnable {

    private Selector selector;
    private ServerSocketChannel servChannel;
    private volatile boolean stop;

    public MultiplexerTimerServer(int port) {
        try {
            //多路复用器
            selector = Selector.open();
            //管道
            servChannel = ServerSocketChannel.open();
            //设置成非阻塞模式
            servChannel.configureBlocking(false);
            //绑定端口
            servChannel.socket().bind(new InetSocketAddress(port), 1024);
            //注册
            servChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println(servChannel.getLocalAddress().toString());
            System.out.println("The time server is starting in port : " + port);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void stop() {
        this.stop = true;
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                selector.select(1000);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                SelectionKey selectionKey = null;
                while (it.hasNext()) {
                    selectionKey = it.next();
                    it.remove();
                    try {
                        handleInput(selectionKey);
                    } catch (Exception e) {
//                        e.printStackTrace();
                        if (selectionKey != null) {
                            selectionKey.cancel();
                            if (selectionKey.channel() != null) {
                                selectionKey.channel().close();
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
                if (selector != null) {
                    try {
                        //多路复用器关闭后，所用注册在上面的Channel和pipe等资源都会自动去注册并关闭
                        selector.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    private void handleInput(SelectionKey selectionKey) throws IOException {
        if (selectionKey.isValid()) {
            //判断key是否是可接受状态的
            if (selectionKey.isAcceptable()) {
                ServerSocketChannel servChannel = (ServerSocketChannel) selectionKey.channel();
                SocketChannel channel = servChannel.accept();
                channel.configureBlocking(false);//如果要注册chanel此处必须为false,不然会报java.nio.channels.IllegalBlockingModeException
                channel.register(selector, SelectionKey.OP_READ);

            }

            if (selectionKey.isReadable()) {
                SocketChannel channel = (SocketChannel) selectionKey.channel();
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                int readBytes = channel.read(readBuffer);

                if (readBytes > 0) {

                    //将缓冲区的limit设置成position，position设置0，用于后续对缓冲区的读取操作
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String body = new String(bytes, "UTF-8");
                    System.out.println("The time server receive order : " + body);
                    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ?
                            new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
                    doWrite(channel, currentTime);
                } else if (readBytes < 0) {
                    selectionKey.cancel();
                    channel.close();
                }
            }
        }
    }

    private void doWrite(SocketChannel channel, String response) throws IOException {
        if (response != null && response.length() > 0) {
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            channel.write(writeBuffer);
        }

    }
}
