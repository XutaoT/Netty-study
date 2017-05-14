package com.xutao.io.bio.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by Tau Hsu on 2017/4/25.
 * @author Tau Hsu
 * @version 1.0
 */
public class TimeClient {
    public static void main(String[] args){
        int port = 8080;
        if(args != null && args.length > 0){
            try{
            port = Integer.valueOf(args[0]);
            }catch (NumberFormatException e){

            }
        }

            Socket socket = null;
            BufferedReader bufferedReader = null;
            PrintWriter printWriter = null;
            try{
                socket = new Socket("127.0.0.1", port);
                bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                printWriter = new PrintWriter(socket.getOutputStream(), true);
                printWriter.println("QUERY TIME ORDER");
                System.out.println("Send order 2 server succeed.");
                String resp = bufferedReader.readLine();
                System.out.print("Now is : " + resp);
            }catch (Exception e){
                e.printStackTrace();
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
