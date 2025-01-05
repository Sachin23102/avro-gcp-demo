package com.practice.avrodemo.messageconverter;

import com.google.cloud.spring.pubsub.support.converter.PubSubMessageConverter;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.springframework.stereotype.Component;

@Component
class AvroPubSubMessageConverter implements PubSubMessageConverter {
    @Override
    public PubsubMessage toPubSubMessage(Object payload, Map<String, String> headers) {
        try {
            var outputStream = new ByteArrayOutputStream();
            var encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            var writer = new SpecificDatumWriter<>((Class<Object>) payload.getClass());
            writer.write(payload, encoder);
            encoder.flush();
            return byteStringToPubSubMessage(ByteString.copyFrom(outputStream.toByteArray()), headers);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T fromPubSubMessage(PubsubMessage message, Class<T> payloadType) {
        var decoder = DecoderFactory.get().binaryDecoder(message.getData().toByteArray(), null);
        var reader = new SpecificDatumReader<>(payloadType);
        try {
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
