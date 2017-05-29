package com.xutao.netty.http.src.protobuf.protoStudy;

import com.google.protobuf.InvalidProtocolBufferException;
import com.xutao.netty.http.src.protobuf.proto.SubscribeReqProto;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tau Hsu on 2017/5/26.
 */
public class TestSubsribeReqProto {
    private static byte[] encode(SubscribeReqProto.SubscribeReq req) {
        return req.toByteArray();
    }

    private static SubscribeReqProto.SubscribeReq decode(byte[] body) throws InvalidProtocolBufferException {
        return SubscribeReqProto.SubscribeReq.parseFrom(body);
    }

    private static SubscribeReqProto.SubscribeReq createSubscribeReq(){
        SubscribeReqProto.SubscribeReq.Builder builder = SubscribeReqProto.SubscribeReq.newBuilder();
        builder.setSubReqID(1);
        builder.setUserName("xutao");
        builder.setProductName("Netty Book");
        List<String> address = new ArrayList<String>();
        address.add("Nanjing");
        address.add("Shanghai");
        address.add("Beijing");
        builder.addAllAddress(address);
        return builder.build();
    }

    public static void main(String[] args) throws InvalidProtocolBufferException {
        SubscribeReqProto.SubscribeReq subscribeReq = createSubscribeReq();
        System.out.println("Before encode : \n" + subscribeReq.toString());
        SubscribeReqProto.SubscribeReq req = decode(encode(subscribeReq));
        System.out.println("After decode : \n" + req.toString());
        System.out.println("Assert equals : --> " + req.equals(subscribeReq));
    }
}
