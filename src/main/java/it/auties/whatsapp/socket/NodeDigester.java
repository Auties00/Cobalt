package it.auties.whatsapp.socket;

import com.google.protobuf.ByteString;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.binary.BinaryUnpack;
import it.auties.whatsapp.manager.WhatsappStore;
import it.auties.whatsapp.protobuf.model.misc.Node;
import it.auties.whatsapp.utils.Validate;
import it.auties.whatsapp.utils.Jid;
import it.auties.whatsapp.utils.MultiDeviceCypher;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.whispersystems.curve25519.Curve25519;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

import static it.auties.whatsapp.binary.BinaryArray.*;
import static it.auties.whatsapp.utils.CypherUtils.hmacSha256;
import static it.auties.whatsapp.utils.CypherUtils.raw;

/**
 * This class is a singleton and holds all the data regarding a session with WhatsappWeb's WebSocket.
 * It also provides various methods to query this data.
 * It should not be used by multiple sessions as, being a singleton, it cannot determine and divide data coming from different sessions.
 * It should not be initialized manually.
 */
@UtilityClass
public class NodeDigester {
    /**
     * Digests a {@code node} adding the data it contains to the data this singleton holds
     *
     * @param node   the WhatsappNode to digest
     */
    public void digestWhatsappNode(@NonNull MultiDeviceSocket socket, @NonNull Node node) {
        System.out.printf("Received: %s%n", node);
        switch (node.description()){
            case "success" -> handleSuccess(socket);
            case "iq" -> handleIq(socket, node);
            case "stream:error" -> handleStreamError(socket, node);
            case "failure" -> handleStreamFailure(socket, node);
            default -> System.err.println("Unhandled");
        }
    }

    private void handleStreamFailure(MultiDeviceSocket socket, Node node) {
        if (node.attributes().getInt("reason") != 401) {
            throw new RuntimeException("WhatsappWeb failure at %s, status code: %s".formatted(node.attributes().getString("location"), node.attributes().getString("reason")));
        }

        socket.reconnect();
    }

    private void handleSuccess(MultiDeviceSocket socket) {
        sendPreKeys(socket);
        startConnection(socket);
    }

    private void handleIq(MultiDeviceSocket socket, Node node) {
        var children = node.childNodes();
        if(children.isEmpty()){
            return;
        }

        var container = children.getFirst();
        switch (container.description()){
            case "pair-device" -> generateQrCode(socket, node, container);
            case "pair-success" -> confirmQrCode(socket, node, container);
            case "active" -> socket.manager().callListeners(listener -> listener.onLoggedIn(socket.keys().me()));
            default -> throw new IllegalArgumentException("Cannot handle iq request, unknown description. %s%n".formatted(container.description()));
        }
    }

    private void handleStreamError(MultiDeviceSocket socket, Node node) {
        var code = node.attributes().getInt("code");
        if(code != 515){
            return;
        }

        socket.reconnect();
    }

    private void startConnection(MultiDeviceSocket socket) {
        var stanza = new Node(
                "iq",
                Map.of(
                        "to", Jid.WHATSAPP_SERVER,
                        "xmlns", "passive",
                        "type", "set",
                        "id", BinaryUnpack.generateId()
                ),
                List.of(new Node("active"))
        );
        socket.sendBinaryRequest(stanza);
    }

    private void sendPreKeys(MultiDeviceSocket socket) {
        if(socket.keys().preKeys()){
            return;
        }

        var preKeys = createPreKeys();
        var stanza = new Node(
                "iq",
                Map.of(
                        "id", BinaryUnpack.generateId(),
                        "xmlns", "encrypt",
                        "type", "set",
                        "to", Jid.WHATSAPP_SERVER
                ),
                List.of(
                        new Node(
                                "registration",
                                Map.of(),
                                BinaryArray.of(socket.keys().registrationId(), 4).data()
                        ),
                        new Node(
                                "type",
                                Map.of(),
                                ""
                        ),
                        new Node(
                                "identity",
                                Map.of(),
                                socket.keys().signedIdentityKey().publicKey()
                        ),
                        new Node(
                                "list",
                                Map.of(),
                                preKeys
                        ),
                        new Node(
                                "skey",
                                Map.of(),
                                List.of(
                                        new Node("id",
                                                Map.of(),
                                                BinaryArray.of(socket.keys().signedPreKey().id(), 3).data()
                                        ),
                                        new Node(
                                                "value",
                                                Map.of(),
                                                socket.keys().signedPreKey().keyPair().publicKey()
                                        ),
                                        new Node(
                                                "signature",
                                                Map.of(),
                                                socket.keys().signedPreKey().signature()
                                        )
                                )
                        )
                )
        );

        socket.sendBinaryRequest(stanza);
        socket.keys().preKeys(true);
    }

    private List<Node> createPreKeys() {
        return IntStream.range(0, 30)
                .mapToObj(index -> new Node("key", Map.of(), List.of(new Node("id", Map.of(), BinaryArray.of(index, 3).data()), new Node("value", Map.of(), MultiDeviceCypher.createKeyPair().publicKey()))))
                .toList();
    }

    private void generateQrCode(MultiDeviceSocket socket, Node node, Node container) {
        var refHolder = container.findNodeByDescription("ref")
                .filter(ref -> ref.content() instanceof byte[])
                .orElseThrow(() -> new NoSuchElementException("Pairing error: missing ref!"));
        var qr = new String((byte[]) refHolder.content(), StandardCharsets.UTF_8);
        var matrix = socket.qrCode().generate(qr, raw(socket.keys().keyPair().getPublic()), socket.keys().signedIdentityKey().publicKey(), socket.keys().advSecretKey());
        MANAGER.callListeners(listener -> listener.onQRCode(matrix));
        sendConfirmNode(socket, node, null);
        ping(socket);
    }

    private void sendConfirmNode(MultiDeviceSocket socket, Node node, Object content) {
        var iq = new Node(
                "iq",
                Map.of(
                        "id", Objects.requireNonNull(node.attrs().get("id"), "Missing id"),
                        "to", Jid.WHATSAPP_SERVER,
                        "type", "result"
                ),
                content
        );

        socket.sendBinaryRequest(iq);
    }

    @SneakyThrows
    private void confirmQrCode(MultiDeviceSocket socket, Node node, Node container) {
        socket.keys().initializeUser(fetchJid(container));

        var curve = Curve25519.getInstance(Curve25519.BEST);
        var deviceIdentity = fetchDeviceIdentity(container);

        var advIdentity = Proto.ADVSignedDeviceIdentityHMAC.parseFrom(deviceIdentity);
        var advSecret = Base64.getDecoder().decode(socket.keys().advSecretKey());
        var advSign = hmacSha256(forArray(advIdentity.getDetails().toByteArray()), of(advSecret));
        Validate.isTrue(Arrays.equals(advIdentity.getHmac().toByteArray(), advSign.data()), "Cannot login: Hmac validation failed!", SecurityException.class);

        var account = Proto.ADVSignedDeviceIdentity.parseFrom(advIdentity.getDetails());
        var message = of(new byte[]{6, 0})
                .append(account.getDetails().toByteArray())
                .append(socket.keys().signedIdentityKey().publicKey())
                .data();
        Validate.isTrue(curve.verifySignature(account.getAccountSignatureKey().toByteArray(), message, account.getAccountSignature().toByteArray()), "Cannot login: Hmac validation failed!", SecurityException.class);

        var deviceSignatureMessage = of(new byte[]{6, 1})
                .append(account.getDetails().toByteArray())
                .append(socket.keys().signedIdentityKey().publicKey())
                .append(account.getAccountSignatureKey().toByteArray())
                .data();
        var deviceSignature = curve.calculateSignature(socket.keys().signedIdentityKey().privateKey(), deviceSignatureMessage);

        var keyIndex = Proto.ADVDeviceIdentity.parseFrom(account.getDetails()).getKeyIndex();
        var content = new Node(
                "pair-device-sign",
                Map.of(),
                List.of(
                        new Node(
                                "device-identity",
                                Map.of("key-index", keyIndex),
                                account.toBuilder().setDeviceSignature(ByteString.copyFrom(deviceSignature)).clearAccountSignatureKey().build().toByteArray()
                        )
                )
        );
        sendConfirmNode(socket, node, List.of(content));
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
