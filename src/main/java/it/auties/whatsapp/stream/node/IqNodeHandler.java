package it.auties.whatsapp.stream.node;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappDisconnectReason;
import it.auties.whatsapp.api.WhatsappVerificationHandler;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.io.node.Node;
import it.auties.whatsapp.model.auth.*;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidServer;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.PhonePairingCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.NoSuchElementException;

import static it.auties.whatsapp.api.WhatsappErrorHandler.Location.LOGIN;

public final class IqNodeHandler extends AbstractNodeHandler {
    private static final int PING_INTERVAL = 30;
    private static final byte[] DEVICE_WEB_SIGNATURE_HEADER = {6, 1};
    private static final byte[] ACCOUNT_SIGNATURE_HEADER = {6, 0};

    private final WhatsappVerificationHandler.Web webVerificationHandler;
    private final PhonePairingCode pairingCode;

    public IqNodeHandler(Whatsapp whatsapp, WhatsappVerificationHandler.Web webVerificationHandler, PhonePairingCode pairingCode) {
        super(whatsapp, "iq");
        this.webVerificationHandler = webVerificationHandler;
        this.pairingCode = pairingCode;
    }

    @Override
    void handle(Node node) {
        if (node.attributes().hasValue("xmlns", "urn:xmpp:ping")) {
            whatsapp.sendQueryWithNoResponse("result", null);
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
        switch (webVerificationHandler) {
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
                Base64.getEncoder().encodeToString(whatsapp.keys().noiseKeyPair().publicKey().toEncodedPoint()),
                Base64.getEncoder().encodeToString(whatsapp.keys().identityKeyPair().publicKey().toEncodedPoint()),
                Base64.getEncoder().encodeToString(whatsapp.keys().companionKeyPair().publicKey().toEncodedPoint()),
                "1"
        );
        qrHandler.handle(qr);
    }

    private void sendConfirmNode(Node node, Node content) {
        var attributes = NodeAttributes.of()
                .put("id", node.id())
                .put("type", "result")
                .put("to", JidServer.user().toJid())
                .toMap();
        var request = Node.of("iq", attributes, content);
        whatsapp.sendNodeWithNoResponse(request);
    }

    private void schedulePing() {
        whatsapp.scheduleAtFixedInterval(() -> {
            var result = whatsapp.sendPing();
            if(result == Node.empty()) {
                whatsapp.disconnect(WhatsappDisconnectReason.RECONNECTING);
            }else {
               whatsapp.serializeAsync();
            }
        }, PING_INTERVAL, PING_INTERVAL);
    }

    private void askPairingCode(WhatsappVerificationHandler.Web.PairingCode codeHandler) {
        var companionEphemeralPublicKey = pairingCode.encrypt(whatsapp.keys().companionKeyPair().publicKey());
        var registration = Node.of(
                "link_code_companion_reg",
                Map.of("value", getPhoneNumberAsJid(), "stage", "companion_hello", "should_show_push_notification", true),
                Node.of("link_code_pairing_wrapped_companion_ephemeral_pub", companionEphemeralPublicKey),
                Node.of("companion_server_auth_key_pub", whatsapp.keys().noiseKeyPair().publicKey()),
                Node.of("companion_platform_id", 49),
                Node.of("companion_platform_display", "Chrome (Linux)".getBytes(StandardCharsets.UTF_8)),
                Node.of("link_code_pairing_nonce", 0)
        );
        whatsapp.sendQuery("set", "md", registration);
        pairingCode.accept(codeHandler);
    }

    private Jid getPhoneNumberAsJid() {
        return whatsapp.store()
                .phoneNumber()
                .map(PhoneNumber::toJid)
                .orElseThrow(() -> new IllegalArgumentException("Missing phone value while registering via OTP"));
    }

    private void confirmPairing(Node node, Node container) {
        try {
            saveCompanion(container);
            var deviceIdentity = container.findChild("device-identity")
                    .orElseThrow(() -> new NoSuchElementException("Missing device identity"));
            var advIdentity = SignedDeviceIdentityHMACSpec.decode(deviceIdentity.contentAsBytes().orElseThrow());
            var localMac = Mac.getInstance("HmacSHA256");
            var macKey = new SecretKeySpec(whatsapp.keys().companionKeyPair().publicKey().toEncodedPoint(), "HmacSHA256");
            localMac.init(macKey);
            var advSign = localMac.doFinal(advIdentity.details());
            if (!Arrays.equals(advIdentity.hmac(), advSign)) {
                whatsapp.handleFailure(LOGIN, new HmacValidationException("adv_sign"));
                return;
            }
            var account = SignedDeviceIdentitySpec.decode(advIdentity.details());
            whatsapp.keys().setCompanionIdentity(account);
            var identityPublicKey = whatsapp.keys().identityKeyPair().publicKey().toEncodedPoint();
            var message = Bytes.concat(
                    ACCOUNT_SIGNATURE_HEADER,
                    account.details(),
                    identityPublicKey
            );
            if (!Curve25519.verifySignature(account.accountSignatureKey(), message, account.accountSignature())) {
                whatsapp.handleFailure(LOGIN, new HmacValidationException("message_header"));
                return;
            }
            var deviceSignatureMessage = Bytes.concat(
                    DEVICE_WEB_SIGNATURE_HEADER,
                    account.details(),
                    identityPublicKey,
                    account.accountSignatureKey()
            );
            var result = new SignedDeviceIdentityBuilder()
                    .accountSignature(account.accountSignature())
                    .accountSignatureKey(account.accountSignatureKey())
                    .details(account.details())
                    .deviceSignature(Curve25519.sign(whatsapp.keys().identityKeyPair().privateKey().toEncodedPoint(), deviceSignatureMessage))
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
            whatsapp.keys().setCompanionIdentity(result);
            var device = whatsapp.store().device();
            var platform = getWebPlatform(node);
            whatsapp.store().setDevice(device.withPlatform(platform));
            sendConfirmNode(node, devicePairNode);
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot confirm pairing", exception);
        }
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
                .getOptionalJid("value")
                .orElseThrow(() -> new NoSuchElementException("Missing companion"));
        whatsapp.store().setJid(companion);
        PhoneNumber.of(companion.user())
                .ifPresent(phoneNumber -> whatsapp.store().setPhoneNumber(phoneNumber));
        whatsapp.addMe(companion.withoutData());
    }
}
