package com.xutao.netty.privateProtocol.message;

/**
 * Created by Tau Hsu on 2017/6/21.
 */
public class MessageType {
    //请求类型
    public final static byte LOGIN_REQ = 4;
    //响应类型
    public final static byte LOGIN_RESP = 3;
    //心跳请求类型
    public final static byte HERTBEAT_REQ = 5;
    //心跳相应类型
    public final static byte HERTBEAT_RESP = 6;

}
