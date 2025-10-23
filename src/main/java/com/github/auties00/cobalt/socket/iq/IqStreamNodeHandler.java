package com.github.auties00.cobalt.socket.iq;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.api.WhatsappDisconnectReason;
import com.github.auties00.cobalt.api.WhatsappVerificationHandler;
import com.github.auties00.cobalt.model.proto.auth.SignedDeviceIdentity;
import com.github.auties00.cobalt.model.proto.auth.SignedDeviceIdentityHMAC;
import com.github.auties00.cobalt.exception.HmacValidationException;
import com.github.auties00.cobalt.model.node.Node;
import com.github.auties00.cobalt.model.node.NodeBuilder;
import com.github.auties00.cobalt.model.proto.auth.*;
import com.github.auties00.cobalt.model.proto.auth.UserAgent.PlatformType;
import com.github.auties00.cobalt.model.proto.contact.ContactBuilder;
import com.github.auties00.cobalt.model.proto.contact.ContactStatus;
import com.github.auties00.cobalt.model.proto.jid.Jid;
import com.github.auties00.cobalt.model.proto.jid.JidServer;
import com.github.auties00.cobalt.socket.SocketStream;
import com.github.auties00.cobalt.util.Bytes;
import com.github.auties00.cobalt.util.Clock;
import com.github.auties00.cobalt.util.PhonePairingCode;
import com.github.auties00.curve25519.Curve25519;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.github.auties00.cobalt.api.WhatsappErrorHandler.Location.LOGIN;

public final class IqStreamNodeHandler extends SocketStream.Handler {
    private static final int PING_INTERVAL = 30;
    private static final byte[] DEVICE_WEB_SIGNATURE_HEADER = {6, 1};
    private static final byte[] ACCOUNT_SIGNATURE_HEADER = {6, 0};

    private final WhatsappVerificationHandler.Web webVerificationHandler;
    private final PhonePairingCode pairingCode;
    private final Executor pingExecutor;
    public IqStreamNodeHandler(Whatsapp whatsapp, WhatsappVerificationHandler.Web webVerificationHandler, PhonePairingCode pairingCode) {
        super(whatsapp, "iq");
        this.webVerificationHandler = webVerificationHandler;
        this.pairingCode = pairingCode;
        this.pingExecutor = CompletableFuture.delayedExecutor(PING_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    public void handle(Node node) {
        if (node.hasAttribute("xmlns", "urn:xmpp:ping")) {
            handlePing();
        }else {
            handlePairing(node);
        }
    }

    private void handlePing() {
        var result = new NodeBuilder()
                .description("result")
                .build();
        var response = new NodeBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("type", "result")
                .content(result)
                .build();
        whatsapp.sendNodeWithNoResponse(response);
    }

    private void handlePairing(Node node) {
        var container = node.getChild().orElse(null);
        if (container == null) {
            return;
        }

        switch (container.description()) {
            case "pair-device" -> handlePairInit(node, container);
            case "pair-success" -> handlePairSuccess(node, container);
        }
    }

    private void handlePairInit(Node node, Node container) {
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
        var ref = container.getChild("ref")
                .flatMap(Node::toContentString)
                .orElseThrow(() -> new NoSuchElementException("Missing ref"));
        var companionKeyPair = SignalIdentityKeyPair.random();
        whatsapp.store().setCompanionKeyPair(companionKeyPair);
        var qr = String.join(
                ",",
                ref,
                Base64.getEncoder().encodeToString(whatsapp.store().noiseKeyPair().publicKey().toEncodedPoint()),
                Base64.getEncoder().encodeToString(whatsapp.store().identityKeyPair().publicKey().toEncodedPoint()),
                Base64.getEncoder().encodeToString(companionKeyPair.publicKey().toEncodedPoint()),
                "1"
        );
        qrHandler.handle(qr);
    }

    private void sendConfirmNode(Node node, Node content) {
        var nodeId = node.getRequiredAttributeAsString("id");
        var request = new NodeBuilder()
                .description("iq")
                .attribute("id", nodeId)
                .attribute("type", "result")
                .attribute("to", JidServer.user())
                .content(content)
                .build();
        whatsapp.sendNodeWithNoResponse(request);
    }

    private void schedulePing() {
        pingExecutor.execute(() -> {
            var result = sendPing();
            if(result == Node.empty()) {
                whatsapp.disconnect(WhatsappDisconnectReason.RECONNECTING);
            }else {
                var store = whatsapp.store();
                store.serialize();
            }
            schedulePing();
        });
    }

    private Node sendPing() {
        try {
            var pingBody = new NodeBuilder()
                    .description("ping")
                    .build();
            var pingRequest = new NodeBuilder()
                    .description("iq")
                    .attribute("to", JidServer.user())
                    .attribute("xmlns", "w:p")
                    .attribute("type", "get")
                    .content(pingBody);
            return whatsapp.sendNode(pingRequest);
        }catch (Throwable throwable) {
            return Node.empty();
        }
    }

    private void askPairingCode(WhatsappVerificationHandler.Web.PairingCode codeHandler) {
        var phoneNumber = whatsapp.store()
                .phoneNumber()
                .orElseThrow(() -> new InternalError("Phone number was not set"));
        var companionKeyPair = SignalIdentityKeyPair.random();
        whatsapp.store().setCompanionKeyPair(companionKeyPair);
        var linkCodePairingWrappedCompanionEphemeralPub = new NodeBuilder()
                .description("link_code_pairing_wrapped_companion_ephemeral_pub")
                .content(pairingCode.encrypt(companionKeyPair.publicKey()))
                .build();
        var companionServerAuthKeyPub = new NodeBuilder()
                .description("companion_server_auth_key_pub")
                .content(whatsapp.store().noiseKeyPair().publicKey().toEncodedPoint())
                .build();
        var companionPlatformId = new NodeBuilder()
                .description("companion_platform_id")
                .content(49)
                .build();
        var companionPlatformDisplay = new NodeBuilder()
                .description("companion_platform_display")
                .content("Chrome (Linux)".getBytes(StandardCharsets.UTF_8))
                .build();
        var linkCodePairingNonce = new NodeBuilder()
                .description("link_code_pairing_nonce")
                .content(0)
                .build();
        var registrationBody = new NodeBuilder()
                .description("link_code_companion_reg_request")
                .attribute("jid", Jid.of(phoneNumber))
                .attribute("stage", "companion_hello")
                .attribute("should_show_push_notification", true)
                .content(linkCodePairingWrappedCompanionEphemeralPub, companionServerAuthKeyPub, companionPlatformId, companionPlatformDisplay, linkCodePairingNonce)
                .build();
        var registrationRequest = new NodeBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("xmlns", "md")
                .attribute("type", "set")
                        .content(registrationBody);
        whatsapp.sendNode(registrationRequest);
        pairingCode.accept(codeHandler);
    }

    private void handlePairSuccess(Node node, Node container) {
        saveCompanion(container);
        var deviceIdentity = container.getChild("device-identity")
                .orElseThrow(() -> new InternalError("Missing device identity"))
                .toContentBytes()
                .orElseThrow(() -> new InternalError("Missing device identity content"));
        var advIdentity = SignedDeviceIdentityHMACSpec.decode(deviceIdentity);
        var advSign = getAdvSign(advIdentity);
        if (!Arrays.equals(advIdentity.hmac(), advSign)) {
            whatsapp.handleFailure(LOGIN, new HmacValidationException("adv_sign"));
            return;
        }
        var account = SignedDeviceIdentitySpec.decode(advIdentity.details());
        whatsapp.store().setCompanionIdentity(account);
        var identityPublicKey = whatsapp.store().identityKeyPair().publicKey().toEncodedPoint();
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
                .deviceSignature(Curve25519.sign(whatsapp.store().identityKeyPair().privateKey().toEncodedPoint(), deviceSignatureMessage))
                .build();
        whatsapp.store().setCompanionIdentity(result);
        var platform = getWebPlatform(node);
        var device = whatsapp.store()
                .device()
                .withPlatform(platform);
        whatsapp.store()
                .setDevice(device);
        var keyIndex = DeviceIdentitySpec.decode(result.details()).keyIndex();
        var outgoingDeviceIdentity = SignedDeviceIdentitySpec.encode(new SignedDeviceIdentity(result.details(), null, result.accountSignature(), result.deviceSignature()));
        var deviceIdentityNode = new NodeBuilder()
                .description("device-identity")
                .attribute("key-index", keyIndex)
                .content(outgoingDeviceIdentity)
                .build();
        var devicePairRequest = new NodeBuilder()
                .description("pair-device-sign")
                .content(deviceIdentityNode)
                .build();
        sendConfirmNode(node, devicePairRequest);
    }

    private byte[] getAdvSign(SignedDeviceIdentityHMAC advIdentity) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            var companionKey = whatsapp.store()
                    .companionKeyPair()
                    .orElseThrow(() -> new InternalError("Missing companion key pair"))
                    .publicKey()
                    .toEncodedPoint();
            var companionSecretKey = new SecretKeySpec(companionKey, "HmacSHA256");
            mac.init(companionSecretKey);
            return mac.doFinal(advIdentity.details());
        }catch (GeneralSecurityException exception) {
            throw new InternalError("Cannot get adv sign", exception);
        }
    }

    private PlatformType getWebPlatform(Node node) {
        var name = node.getChild("platform")
                .flatMap(entry -> entry.getAttributeAsString("name"))
                .orElse(null);
        return switch (name) {
            case "smbi" -> PlatformType.IOS_BUSINESS;
            case "smba" -> PlatformType.ANDROID_BUSINESS;
            case "android" -> PlatformType.ANDROID;
            case "ios" -> PlatformType.IOS;
            case null, default -> null;
        };
    }

    private void saveCompanion(Node container) {
        var node = container.getRequiredChild("device");
        var companion = node.getRequiredAttributeAsJid("jid");
        whatsapp.store()
                .setJid(companion);
        whatsapp.store()
                .setPhoneNumber(Long.parseUnsignedLong(companion.user()));
        var contact = new ContactBuilder()
                .jid(companion)
                .chosenName(whatsapp.store().name())
                .lastKnownPresence(ContactStatus.AVAILABLE)
                .lastSeenSeconds(Clock.nowSeconds())
                .blocked(false)
                .build();
        whatsapp.store()
                .addContact(contact);
    }
}