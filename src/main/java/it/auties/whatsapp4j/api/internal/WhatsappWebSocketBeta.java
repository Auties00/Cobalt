package it.auties.whatsapp4j.api.internal;

import it.auties.protobuf.encoder.ProtobufEncoder;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.binary.model.BinaryArray;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.protobuf.message.server.HandshakeMessage;
import it.auties.whatsapp4j.protobuf.model.client.ClientHello;
import it.auties.whatsapp4j.serialization.MultiDeviceWhatsappSerializer;
import it.auties.whatsapp4j.serialization.WhatsappSerializer;
import jakarta.websocket.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import static it.auties.whatsapp4j.utils.internal.CypherUtils.calculateHandshake;
import static it.auties.whatsapp4j.utils.internal.CypherUtils.multiDeviceEncrypt;

@RequiredArgsConstructor
@ClientEndpoint(configurator = WhatsappSocketConfiguration.class)
public class WhatsappWebSocketBeta {
    private WhatsappSerializer serializer;
    private final @NonNull WebSocketContainer webSocketContainer;
    private final @NonNull WhatsappConfiguration options;
    private final @NonNull WhatsappKeysManager whatsappKeys;
    public WhatsappWebSocketBeta(@NonNull WhatsappConfiguration options, @NonNull WhatsappKeysManager whatsappKeys) {
        this(ContainerProvider.getWebSocketContainer(), options, whatsappKeys);
    }

    @OnOpen
    @SneakyThrows
    public void onOpen(@NonNull Session session){
        var handshake = calculateHandshake(whatsappKeys.keyPair());
        var publicKey = new byte[handshake.getFixedEphemeralKey().getPublicKeyLength()];
        handshake.getFixedEphemeralKey().getPublicKey(publicKey, 0);

        var handshakeMessage = new HandshakeMessage(new ClientHello(publicKey));
        var encodedMessage = ProtobufEncoder.encode(handshakeMessage);
        var parsedMessage = multiDeviceEncrypt(encodedMessage);

        this.serializer = new MultiDeviceWhatsappSerializer(handshake);
        session.getBasicRemote().sendBinary(parsedMessage.toBuffer());
    }

    @OnMessage
    public void onBinary(byte @NonNull [] msg) {
        System.out.println(Arrays.toString(msg));
        System.out.println(msg.length);
    }

    public void openConnection() {
        try{
            webSocketContainer.connectToServer(this, URI.create(options.whatsappUrlBeta()));
        }catch (IOException | DeploymentException exception){
            throw new RuntimeException("Cannot connect to WhatsappWeb's WebServer", exception);
        }
    }
}
