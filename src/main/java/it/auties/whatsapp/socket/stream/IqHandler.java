package it.auties.whatsapp.socket.stream;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.WhatsappDisconnectReason;
import it.auties.whatsapp.api.WhatsappVerificationHandler;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.signal.auth.*;
import it.auties.whatsapp.socket.SocketConnection;
import it.auties.whatsapp.util.Bytes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.NoSuchElementException;

import static it.auties.whatsapp.api.WhatsappErrorHandler.Location.LOGIN;
import static it.auties.whatsapp.api.WhatsappErrorHandler.Location.STREAM;

final class IqHandler extends NodeHandler.Dispatcher {
    private static final int PING_INTERVAL = 30;
    private static final byte[] DEVICE_WEB_SIGNATURE_HEADER = {6, 1};
    private static final byte[] ACCOUNT_SIGNATURE_HEADER = {6, 0};

    IqHandler(SocketConnection socketConnection) {
        super(socketConnection, "iq");
    }

    @Override
    void execute(Node node) {
        if (node.attributes().hasValue("xmlns", "urn:xmpp:ping")) {
            socketConnection.sendQueryWithNoResponse("result", null);
            return;
        }

        var container = node.findChild().orElse(null);
        if (container == null) {
            return;
        }

        switch (container.description()) {
            case "pair-device" -> startWebPairing(node, container);
            case "pair-success" -> confirmPairing(node, container);
        }
    }

    private void startWebPairing(Node node, Node container) {
        switch (socketConnection.webVerificationHandler()) {
            case WhatsappVerificationHandler.Web.QrCode qrHandler -> {
                printQrCode(qrHandler, container);
                sendConfirmNode(node, null);
                schedulePing();
            }
            case WhatsappVerificationHandler.Web.PairingCode codeHandler -> {
                askPairingCode(codeHandler);
                schedulePing();
            }
            default -> throw new IllegalArgumentException("Cannot verify account: unknown verification method");
        }
    }

    private void printQrCode(WhatsappVerificationHandler.Web.QrCode qrHandler, Node container) {
        var ref = container.findChild("ref")
                .flatMap(Node::contentAsString)
                .orElseThrow(() -> new NoSuchElementException("Missing ref"));
        var qr = String.join(
                ",",
                ref,
                Base64.getEncoder().encodeToString(socketConnection.keys().noiseKeyPair().publicKey()),
                Base64.getEncoder().encodeToString(socketConnection.keys().identityKeyPair().publicKey()),
                Base64.getEncoder().encodeToString(socketConnection.keys().companionKeyPair().publicKey()),
                "1"
        );
        qrHandler.handle(qr);
    }

    private void sendConfirmNode(Node node, Node content) {
        var attributes = Attributes.of()
                .put("id", node.id())
                .put("type", "result")
                .put("to", JidServer.user().toJid())
                .toMap();
        var request = Node.of("iq", attributes, content);
        try {
            socketConnection.sendNodeWithNoResponse(request);
        } catch (Exception throwable) {
            socketConnection.handleFailure(STREAM, throwable);
        }
    }

    private void schedulePing() {
        socketConnection.scheduleAtFixedInterval(() -> {
            var result = socketConnection.sendPing();
            if(result == Node.empty()) {
                socketConnection.disconnect(WhatsappDisconnectReason.RECONNECTING);
            }else {
               socketConnection.serializeAsync();
            }
        }, PING_INTERVAL, PING_INTERVAL);
    }

    private void askPairingCode(WhatsappVerificationHandler.Web.PairingCode codeHandler) {
        var registration = Node.of(
                "link_code_companion_reg",
                Map.of("jid", getPhoneNumberAsJid(), "stage", "companion_hello", "should_show_push_notification", true),
                Node.of("link_code_pairing_wrapped_companion_ephemeral_pub", socketConnection.encryptPairingKey()),
                Node.of("companion_server_auth_key_pub", socketConnection.keys().noiseKeyPair().publicKey()),
                Node.of("companion_platform_id", 49),
                Node.of("companion_platform_display", "Chrome (Linux)".getBytes(StandardCharsets.UTF_8)),
                Node.of("link_code_pairing_nonce", 0)
        );
        socketConnection.sendQuery("set", "md", registration);
        socketConnection.handle(codeHandler);
    }

    private Jid getPhoneNumberAsJid() {
        return socketConnection.store()
                .phoneNumber()
                .map(PhoneNumber::toJid)
                .orElseThrow(() -> new IllegalArgumentException("Missing phone number while registering via OTP"));
    }

    private void confirmPairing(Node node, Node container) {
        saveCompanion(container);
        var deviceIdentity = container.findChild("device-identity")
                .orElseThrow(() -> new NoSuchElementException("Missing device identity"));
        var advIdentity = SignedDeviceIdentityHMACSpec.decode(deviceIdentity.contentAsBytes().orElseThrow());
        var advSign = Hmac.calculateSha256(advIdentity.details(), socketConnection.keys().companionKeyPair().publicKey());
        if (!Arrays.equals(advIdentity.hmac(), advSign)) {
            socketConnection.handleFailure(LOGIN, new HmacValidationException("adv_sign"));
            return;
        }
        var account = SignedDeviceIdentitySpec.decode(advIdentity.details());
        socketConnection.keys().setCompanionIdentity(account);
        var message = Bytes.concat(
                ACCOUNT_SIGNATURE_HEADER,
                account.details(),
                socketConnection.keys().identityKeyPair().publicKey()
        );
        if (!Curve25519.verifySignature(account.accountSignatureKey(), message, account.accountSignature())) {
            socketConnection.handleFailure(LOGIN, new HmacValidationException("message_header"));
            return;
        }
        var deviceSignatureMessage = Bytes.concat(
                DEVICE_WEB_SIGNATURE_HEADER,
                account.details(),
                socketConnection.keys().identityKeyPair().publicKey(),
                account.accountSignatureKey()
        );
        var result = new SignedDeviceIdentityBuilder()
                .accountSignature(account.accountSignature())
                .accountSignatureKey(account.accountSignatureKey())
                .details(account.details())
                .deviceSignature(Curve25519.sign(socketConnection.keys().identityKeyPair().privateKey(), deviceSignatureMessage))
                .build();
        var keyIndex = DeviceIdentitySpec.decode(result.details()).keyIndex();
        var outgoingDeviceIdentity = SignedDeviceIdentitySpec.encode(new SignedDeviceIdentity(result.details(), null, result.accountSignature(), result.deviceSignature()));
        var devicePairNode = Node.of(
                "pair-device-sign",
                Node.of(
                        "device-identity",
                        Map.of("key-index", keyIndex),
                        outgoingDeviceIdentity
                )
        );
        socketConnection.keys().setCompanionIdentity(result);
        var device = socketConnection.store().device();
        var platform = getWebPlatform(node);
        socketConnection.store().setDevice(device.withPlatform(platform));
        sendConfirmNode(node, devicePairNode);
    }

    private UserAgent.PlatformType getWebPlatform(Node node) {
        var name = node.findChild("platform")
                .flatMap(entry -> entry.attributes().getOptionalString("name"))
                .orElse(null);
        return switch (name) {
            case "smbi" -> UserAgent.PlatformType.IOS_BUSINESS;
            case "smba" -> UserAgent.PlatformType.ANDROID_BUSINESS;
            case "android" -> UserAgent.PlatformType.ANDROID;
            case "ios" -> UserAgent.PlatformType.IOS;
            case null, default -> null;
        };
    }

    private void saveCompanion(Node container) {
        var node = container.findChild("device")
                .orElseThrow(() -> new NoSuchElementException("Missing device"));
        var companion = node.attributes()
                .getOptionalJid("jid")
                .orElseThrow(() -> new NoSuchElementException("Missing companion"));
        socketConnection.store().setJid(companion);
        PhoneNumber.of(companion.user())
                .ifPresent(phoneNumber -> socketConnection.store().setPhoneNumber(phoneNumber));
        socketConnection.addMe(companion.withoutData());
    }
}
