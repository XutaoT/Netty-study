package com.xutao.netty.privateProtocol.message;

/**
 * Created by Tau Hsu on 2017/6/18.
 */
public final  class NettyMessage {
    private Header header;
    private Object body;

    public final Header getHeader() {
        return header;
    }

    public final Object getBody() {
        return body;
    }

    public final void setHeader(Header header) {
        this.header = header;
    }

    public final void setBody(Object body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "NettyMessage{" +
                "header=" + header +
                '}';
    }
}
