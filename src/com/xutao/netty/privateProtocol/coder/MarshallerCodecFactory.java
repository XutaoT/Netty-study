package com.xutao.netty.privateProtocol.coder;

import org.jboss.marshalling.*;

import java.io.IOException;

/**
 * Created by Tau Hsu on 2017/6/20.
 */
public class MarshallerCodecFactory {

    protected static Marshaller buildMarshaller() throws IOException {
        final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
        final MarshallingConfiguration marshallingConfiguration = new MarshallingConfiguration();
        marshallingConfiguration.setVersion(5);
        Marshaller marshaller = marshallerFactory.createMarshaller(marshallingConfiguration);
        return marshaller;
    }

    protected static Unmarshaller buildUnmarshaller() throws IOException {
        final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
        final MarshallingConfiguration marshallingConfiguration = new MarshallingConfiguration();
        marshallingConfiguration.setVersion(5);
        Unmarshaller unmarshaller = marshallerFactory.createUnmarshaller(marshallingConfiguration);
        return unmarshaller;
    }
}
