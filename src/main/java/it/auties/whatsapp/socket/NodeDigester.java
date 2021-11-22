package it.auties.whatsapp.socket;

import com.google.protobuf.ByteString;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.protobuf.contact.ContactId;
import it.auties.whatsapp.protobuf.model.misc.Node;
import it.auties.whatsapp.utils.*;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.whispersystems.curve25519.Curve25519;

import java.util.*;
import java.util.stream.IntStream;

/**
 * This class is a singleton and holds all the data regarding a session with WhatsappWeb's WebSocket.
 * It also provides various methods to query this data.
 * It should not be used by multiple sessions as, being a singleton, it cannot determine and divide data coming from different sessions.
 * It should not be initialized manually.
 */
public record NodeDigester(@NonNull WhatsappSocket socket, @NonNull WhatsappQRCode generator) {
    /**
     * Digests a {@code node} adding the data it contains to the data this singleton holds
     *
     * @param node   the WhatsappNode to digest
     */
    public void digest(@NonNull Node node) {
        System.out.printf("Received: %s%n", node);
        switch (node.description()){
            case "success" -> handleSuccess();
            case "iq" -> handleIq(node);
            case "stream:error" -> handleStreamError(node);
            case "failure" -> handleStreamFailure(node);
            default -> System.err.println("Unhandled");
        }
    }

    private void handleStreamFailure(Node node) {
        if (node.attributes().getInt("reason") != 401) {
            throw new RuntimeException("WhatsappWeb failure at %s, status code: %s".formatted(node.attributes().getString("location"), node.attributes().getString("reason")));
        }

        socket.reconnect();
    }

    private void handleSuccess() {
        sendPreKeys();
        startConnection();
    }

    private void handleIq(Node node) {
        var children = node.childNodes();
        if(children.isEmpty()){
            return;
        }

        var container = children.getFirst();
        switch (container.description()){
            case "pair-device" -> generateQrCode(node, container);
            case "pair-success" -> confirmQrCode(node, container);
            case "active" -> socket.store().callListeners(listener -> listener.onLoggedIn(socket.keys().me()));
            default -> throw new IllegalArgumentException("Cannot handle iq request, unknown description. %s%n".formatted(container.description()));
        }
    }

    private void handleStreamError(Node node) {
        var code = node.attributes().getInt("code");
        if(code != 515){
            return;
        }

        socket.reconnect();
    }

    private void startConnection() {
        var stanza = new Node(
                "iq",
                Map.of(
                        "to", ContactId.WHATSAPP_SERVER,
                        "xmlns", "passive",
                        "type", "set",
                        "id", WhatsappUtils.buildRequestTag(socket.options())
                ),
                List.of(new Node("active"))
        );
        socket.sendBinaryRequest(stanza);
    }

    private void sendPreKeys() {
        if(socket.keys().preKeys()){
            return;
        }

        var preKeys = createPreKeys();
        var stanza = new Node(
                "iq",
                Map.of(
                        "id", WhatsappUtils.buildRequestTag(socket.options()),
                        "xmlns", "encrypt",
                        "type", "set",
                        "to", ContactId.WHATSAPP_SERVER
                ),
                List.of(
                        new Node(
                                "registration",
                                Map.of(),
                                BinaryArray.of(socket.keys().id(), 4).data()
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

    private void generateQrCode(Node node, Node container) {
        var qr = decodeQrCode(container);
        var matrix = generator.generate(qr, CypherUtils.raw(socket.keys().keyPair().getPublic()), socket.keys().signedIdentityKey().publicKey(), socket.keys().advSecretKey());
        socket.store().callListeners(listener -> listener.onQRCode(matrix));
        sendConfirmNode(node, null);
        ping();
    }

    private String decodeQrCode(Node container) {
        return container.findNodeByDescription("ref")
                .filter(ref -> ref.content() instanceof byte[])
                .map(ref -> (byte[]) ref.content())
                .map(String::new)
                .orElseThrow(() -> new NoSuchElementException("Pairing error: missing qr code reference"));
    }

    private void sendConfirmNode(Node node, Object content) {
        var iq = new Node(
                "iq",
                Map.of(
                        "id", Objects.requireNonNull(node.attrs().get("id"), "Missing id"),
                        "to", ContactId.WHATSAPP_SERVER,
                        "type", "result"
                ),
                content
        );

        socket.sendBinaryRequest(iq);
    }

    @SneakyThrows
    private void confirmQrCode(Node node, Node container) {
        socket.keys().me(fetchJid(container));

        var curve = Curve25519.getInstance(Curve25519.BEST);
        var deviceIdentity = fetchDeviceIdentity(container);

        var advIdentity = Proto.ADVSignedDeviceIdentityHMAC.parseFrom(deviceIdentity);
        var advSecret = Base64.getDecoder().decode(socket.keys().advSecretKey());
        var advSign = CypherUtils.hmacSha256(BinaryArray.of(advIdentity.getDetails().toByteArray()), BinaryArray.of(advSecret));
        Validate.isTrue(Arrays.equals(advIdentity.getHmac().toByteArray(), advSign.data()), "Cannot login: Hmac validation failed!", SecurityException.class);

        var account = Proto.ADVSignedDeviceIdentity.parseFrom(advIdentity.getDetails());
        var message = BinaryArray.of(new byte[]{6, 0})
                .append(account.getDetails().toByteArray())
                .append(socket.keys().signedIdentityKey().publicKey())
                .data();
        Validate.isTrue(curve.verifySignature(account.getAccountSignatureKey().toByteArray(), message, account.getAccountSignature().toByteArray()), "Cannot login: Hmac validation failed!", SecurityException.class);

        var deviceSignatureMessage = BinaryArray.of(new byte[]{6, 1})
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
        sendConfirmNode(node, List.of(content));
    }

    private byte[] fetchDeviceIdentity(Node container) {
        return container.findNodeByDescription("device-identity")
                .map(Node::content)
                .filter(data -> data instanceof byte[])
                .map(data -> (byte[]) data)
                .orElseThrow(() -> new NoSuchElementException("Cannot find device identity node for authentication in %s".formatted(container)));
    }

    private ContactId fetchJid(Node container) {
        return container.findNodeByDescription("device")
                .map(Node::attributes)
                .orElseThrow(() -> new NoSuchElementException("Cannot find device node for authentication in %s".formatted(container)))
                .getObject("jid", ContactId.class)
                .orElseThrow(() -> new NoSuchElementException("Cannot find jid attribute in %s".formatted(container)));
    }

    private void ping() {
        var keepAlive = new Node(
                "iq",
                Map.of(
                        "id", WhatsappUtils.buildRequestTag(socket.options()),
                        "to", ContactId.WHATSAPP_SERVER,
                        "type", "get",
                        "xmlns", "w:p"
                ),
                List.of(new Node("ping"))
        );

        socket.sendBinaryRequest(keepAlive);
    }
}
