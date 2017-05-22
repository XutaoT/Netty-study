package com.xutao.serializableWithJDK;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

/**
 * Created by Tau Hsu on 2017/5/22.
 */
public class TestUserInfo {
    public static void main(String[] args) throws IOException{
//        beforeModify();
        afterModify();
    }

    static void beforeModify() throws IOException{
            UserInfo userInfo = new UserInfo();
            userInfo.buildUserID(100).buildUserName("Welcome to Netty");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bos);
            os.writeObject(userInfo);
            os.flush();
            os.close();
            byte[] b = bos.toByteArray();
            System.out.println("The JDK serializable length is : " + b.length);
            bos.close();
            System.out.println("The byte array serializable length is :" + userInfo.codeC().length);
    }

    static void afterModify() throws IOException{
        UserInfo userInfo = new UserInfo();
        userInfo.buildUserID(100).buildUserName("Welcome to Netty");
        int loop = 1000000;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream os = null;
        long startTime = System.currentTimeMillis();
        for(int i = 0; i < loop; i++){
            bos = new ByteArrayOutputStream();
            os = new ObjectOutputStream(bos);
            os.writeObject(userInfo);
            os.flush();
            os.close();
            byte[] b = bos.toByteArray();
            bos.close();

        }
        long endTime = System.currentTimeMillis();
        System.out.println("The JDK serializable cost time is : " + (endTime - startTime) + " ms");

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        startTime = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            byte[] b = userInfo.codeC(buffer);
        }
        endTime = System.currentTimeMillis();
        System.out.println("The byte array serializable cost time is : " + (endTime - startTime) + " ms");
    }
}
