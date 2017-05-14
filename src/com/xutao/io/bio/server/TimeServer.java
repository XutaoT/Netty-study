package com.xutao.io.bio.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Tau Hsu on 2017/4/25.
 * @author Tau Hsu
 * @date 2017/4/25
 * @version 1.0
 */
public class TimeServer {

    public static void main(String[] args){
        int port = 8080;
        if(args != null && args.length > 0){
            try{
                port = Integer.valueOf(args[0]);
            }catch (NumberFormatException e){
                throw new RuntimeException("Input params must be String of Integer");
            }
        }

        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket(port);
            System.out.println("The time server is starting in port :" + port);
            Socket socket = null;
            TimeServerHandlerExecutePool timeServerHandlerExecutePool = new TimeServerHandlerExecutePool(50,1000);
            while (true){
                socket = serverSocket.accept();
//                new Thread(new TimeServerHandler(socket)).start();
                timeServerHandlerExecutePool.excute(new TimeServerHandler(socket));
            }
        }catch (IOException e){
            throw new RuntimeException("serverSocket IOException");
        }finally {
            if(serverSocket != null){
                System.out.println("The time server be closed");
                try {
                    serverSocket.close();
                } catch (IOException e) {
                }
                serverSocket = null;
            }
        }
    }
}

class TimeServerHandlerExecutePool{
    private ExecutorService executorService;

    public TimeServerHandlerExecutePool(int maxPoolSize, int queueSize){
        executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),maxPoolSize,
                120L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(queueSize));
    }

    public void excute(Runnable task){
        executorService.execute(task);
    }
}

class TimeServerHandler implements Runnable{
    private Socket socket = null;

    public TimeServerHandler(Socket socket){
        this.socket = socket;
    }
    @Override
    public void run() {
        BufferedReader bufferedReader = null;
        PrintWriter printWriter = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            printWriter = new PrintWriter(this.socket.getOutputStream(), true);
            String currentTime = null;
            String body = null;
            while (true){
                body = bufferedReader.readLine();
                if(body == null)
                    break;
                System.out.println("The time server receive order : " + body);
                currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ?
                        new Date(System.currentTimeMillis()).toString():"BAD ORDER";
                printWriter.println(currentTime);
            }
        }catch (IOException e){
            if(bufferedReader != null){
                try {
                    bufferedReader.close();
                    bufferedReader = null;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            if(printWriter != null){
                printWriter.close();
                printWriter = null;
            }

            if(socket != null){
                try {
                    socket.close();
                    socket = null;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }
        }finally {
            if(bufferedReader != null){
                try {
                    bufferedReader.close();
                    bufferedReader = null;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            if(printWriter != null){
                printWriter.close();
                printWriter = null;
            }

            if(socket != null){
                try {
                    socket.close();
                    socket = null;
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }
        }
    }
}


