package it.auties.whatsapp4j.serialization;

import com.southernstorm.noise.protocol.HandshakeState;
import it.auties.protobuf.decoder.ProtobufDecoder;
import it.auties.whatsapp4j.binary.message.MultiDeviceBinaryMessage;
import it.auties.whatsapp4j.binary.model.BinaryArray;
import it.auties.whatsapp4j.protobuf.message.server.HandshakeMessage;
import it.auties.whatsapp4j.protobuf.model.misc.Node;
import it.auties.whatsapp4j.request.model.BinaryRequest;
import it.auties.whatsapp4j.response.model.binary.BinaryResponse;
import it.auties.whatsapp4j.utils.internal.Validate;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.nio.ByteBuffer;
import java.util.Map;

public record MultiDeviceWhatsappSerializer(@NonNull HandshakeState handshakeState) implements WhatsappSerializer {

    @Override
    public ByteBuffer serialize(BinaryRequest<?> input) {
        throw new UnsupportedOperationException("Serializing is not supported as of now for multi device beta");
    }

    @Override
    @SneakyThrows
    public BinaryResponse deserialize(@NonNull BinaryArray input) {
        var msg = (MultiDeviceBinaryMessage) input.toMessage(true);
        if(input.size() < msg.messageLength() + 3){
            System.err.println("invalid length: " + msg.messageLength());
            return new BinaryResponse("!", new Node("", Map.of(), null));
        }

        var message = ProtobufDecoder.forType(HandshakeMessage.class).decode(msg.message().data());
        var mergedMessage = BinaryArray.forArray(message.serverHello().ephemeral())
                .merged(BinaryArray.forArray(message.serverHello()._static()))
                .merged(BinaryArray.forArray(message.serverHello().payload()));

        var payload = new byte[5096];
        var payloadLength = handshakeState.readMessage(mergedMessage.data(), 0, mergedMessage.size(), payload, 0);
        var realPayload = new byte[payloadLength];
        System.arraycopy(payload, 0, realPayload, 0, payloadLength);
        throw new UnsupportedOperationException(new String(realPayload));
    }
}
