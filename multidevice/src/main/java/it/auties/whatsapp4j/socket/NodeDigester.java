package it.auties.whatsapp4j.socket;

import com.google.protobuf.ByteString;
import it.auties.whatsapp4j.binary.BinaryUnpack;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.manager.WhatsappDataManager;
import it.auties.whatsapp4j.common.protobuf.model.misc.Node;
import it.auties.whatsapp4j.common.utils.Validate;
import it.auties.whatsapp4j.utils.Jid;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.whispersystems.curve25519.Curve25519;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static it.auties.whatsapp4j.common.utils.CypherUtils.hmacSha256;
import static it.auties.whatsapp4j.common.utils.CypherUtils.raw;

/**
 * This class is a singleton and holds all the data regarding a session with WhatsappWeb's WebSocket.
 * It also provides various methods to query this data.
 * It should not be used by multiple sessions as, being a singleton, it cannot determine and divide data coming from different sessions.
 * It should not be initialized manually.
 */
@UtilityClass
public class NodeDigester {
    /**
     * Singleton Instance for WhatsappDataManager
     */
    private static final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();

    /**
     * Digests a {@code node} adding the data it contains to the data this singleton holds
     *
     * @param node   the WhatsappNode to digest
     */
    public void digestWhatsappNode(@NonNull MultiDeviceSocket socket, @NonNull Node node) {
        System.out.printf("Received: %s%n", node);
        switch (node.description()){
            case "iq" -> {
                var children = node.childNodes();
                if(children.isEmpty()){
                    return;
                }

                var container = children.getFirst();
                switch (container.description()){
                    case "pair-device" -> generateQrCode(socket, node, container);
                    case "pair-success" -> confirmQrCode(socket, node, container);
                    default -> throw new IllegalArgumentException("Cannot handle iq request, unknown description. %s%n".formatted(container.description()));
                }
            }

            case "stream:error" -> {
                var code = node.attributes().getInt("code");
                if(code != 515){
                    return;
                }

                socket.reconnect();
            }
            default -> System.err.println("Unhandled");
        }
    }

    private void generateQrCode(MultiDeviceSocket socket, Node node, Node container) {
        var refHolder = container.findNodeByDescription("ref").orElseThrow();
        var qr = new String((byte[]) refHolder.content(), StandardCharsets.UTF_8);
        var matrix = socket.qrCode().generate(qr, raw(socket.keys().keyPair().getPublic()), socket.keys().signedIdentityKey().publicKey(), socket.keys().advSecretKey());
        MANAGER.callListeners(listener -> listener.onQRCode(matrix));
        var iq = new Node(
                "iq",
                Map.of(
                        "id", Objects.requireNonNull(node.attrs().get("id"), "Missing id"),
                        "to", Jid.WHATSAPP_SERVER,
                        "type", "result"
                ),
                null
        );

        socket.sendBinaryRequest(iq);
        ping(socket);
    }

    @SneakyThrows
    private void confirmQrCode(MultiDeviceSocket socket, Node node, Node container) {
        socket.keys().initializeUser(fetchJid(container));

        var curve = Curve25519.getInstance(Curve25519.BEST);
        var deviceIdentity = fetchDeviceIdentity(container);

        var advIdentity = Proto.ADVSignedDeviceIdentityHMAC.parseFrom(deviceIdentity);
        var advSecret = Base64.getDecoder().decode(socket.keys().advSecretKey());
        var advSign = hmacSha256(BinaryArray.forArray(advIdentity.getDetails().toByteArray()), BinaryArray.forArray(advSecret));
        Validate.isTrue(Arrays.equals(advIdentity.getHmac().toByteArray(), advSign.data()), "Cannot login: Hmac validation failed!", SecurityException.class);

        var account = Proto.ADVSignedDeviceIdentity.parseFrom(advIdentity.getDetails());
        var message = BinaryArray.forArray(new byte[]{6, 0})
                .append(account.getDetails().toByteArray())
                .append(socket.keys().signedIdentityKey().publicKey())
                .data();
        Validate.isTrue(curve.verifySignature(account.getAccountSignatureKey().toByteArray(), message, account.getAccountSignature().toByteArray()), "Cannot login: Hmac validation failed!", SecurityException.class);

        var deviceSignatureMessage = BinaryArray.forArray(new byte[]{6, 1})
                .append(account.getDetails().toByteArray())
                .append(socket.keys().signedIdentityKey().publicKey())
                .append(account.getAccountSignatureKey().toByteArray())
                .data();
        var deviceSignature = curve.calculateSignature(socket.keys().signedIdentityKey().privateKey(), deviceSignatureMessage);

        var keyIndex = Proto.ADVDeviceIdentity.parseFrom(account.getDetails()).getKeyIndex();
        var iq = new Node(
                "iq",
                Map.of(
                        "id", Objects.requireNonNull(node.attrs().get("id"), "Missing id"),
                        "to", Jid.WHATSAPP_SERVER,
                        "type", "result"
                ),
                List.of(
                        new Node(
                                "pair-device-sign",
                                Map.of(),
                                List.of(
                                        new Node(
                                                "device-identity",
                                                Map.of("key-index", keyIndex),
                                                 account.toBuilder().setDeviceSignature(ByteString.copyFrom(deviceSignature)).clearAccountSignatureKey().build().toByteArray()
                                        )
                                )
                        )
                )
        );
        socket.sendBinaryRequest(iq);
    }

    private byte[] fetchDeviceIdentity(Node container) {
        return container.findNodeByDescription("device-identity")
                .map(Node::content)
                .filter(data -> data instanceof byte[])
                .map(data -> (byte[]) data)
                .orElseThrow(() -> new NoSuchElementException("Cannot find device identity node for authentication in %s".formatted(container)));
    }

    private Jid fetchJid(Node container) {
        return container.findNodeByDescription("device")
                .map(Node::attributes)
                .orElseThrow(() -> new NoSuchElementException("Cannot find device node for authentication in %s".formatted(container)))
                .getObject("jid", Jid.class)
                .orElseThrow(() -> new NoSuchElementException("Cannot find jid attribute in %s".formatted(container)));
    }

    private void ping(MultiDeviceSocket socket) {
        var keepAlive = new Node(
                "iq",
                Map.of(
                        "id", BinaryUnpack.generateId(),
                        "to", Jid.WHATSAPP_SERVER,
                        "type", "get",
                        "xmlns", "w:p"
                ),
                List.of(new Node("ping"))
        );

        socket.sendBinaryRequest(keepAlive);
    }
}
