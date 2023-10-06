package it.auties.whatsapp.socket;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.exception.ProtobufDeserializationException;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.mobile.CountryCode;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.signal.auth.*;
import it.auties.whatsapp.model.signal.auth.DNSSource.ResolutionMethod;
import it.auties.whatsapp.model.signal.auth.UserAgent.PlatformType;
import it.auties.whatsapp.model.sync.HistorySyncConfigBuilder;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.Specification;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


class AuthHandler {
    private final SocketHandler socketHandler;
    AuthHandler(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    protected CompletableFuture<Boolean> login(SocketSession session, byte[] message) {
        try {
            var serverHello = readHandshake(message);
            if(serverHello.isEmpty()){
                return CompletableFuture.completedFuture(false);
            }

            var handshake = new SocketHandshake(socketHandler.keys(), getPrologueData());
            handshake.updateHash(socketHandler.keys().ephemeralKeyPair().publicKey());
            handshake.updateHash(serverHello.get().ephemeral());
            var sharedEphemeral = Curve25519.sharedKey(serverHello.get().ephemeral(), socketHandler.keys().ephemeralKeyPair().privateKey());
            handshake.mixIntoKey(sharedEphemeral);
            var decodedStaticText = handshake.cipher(serverHello.get().staticText(), false);
            var sharedStatic = Curve25519.sharedKey(decodedStaticText, socketHandler.keys().ephemeralKeyPair().privateKey());
            handshake.mixIntoKey(sharedStatic);
            handshake.cipher(serverHello.get().payload(), false);
            var encodedKey = handshake.cipher(socketHandler.keys().noiseKeyPair().publicKey(), true);
            var sharedPrivate = Curve25519.sharedKey(serverHello.get().ephemeral(), socketHandler.keys().noiseKeyPair().privateKey());
            handshake.mixIntoKey(sharedPrivate);
            var payload = createUserClientPayload();
            var encodedPayload = handshake.cipher(ClientPayloadSpec.encode(payload), true);
            var clientFinish = new ClientFinish(encodedKey, encodedPayload);
            var handshakeMessage = new HandshakeMessageBuilder()
                    .clientFinish(clientFinish)
                    .build();
            return sendHandshake(session, handshake, handshakeMessage);
        }catch (Throwable throwable){
            return CompletableFuture.failedFuture(throwable);
        }
    }

    private byte[] getPrologueData() {
        return switch (socketHandler.store().clientType()) {
            case WEB -> Specification.Whatsapp.WEB_PROLOGUE;
            case MOBILE -> Specification.Whatsapp.MOBILE_PROLOGUE;
        };
    }

    private Optional<ServerHello> readHandshake(byte[] message) {
        try {
            var handshakeMessage = HandshakeMessageSpec.decode(message);
            return Optional.ofNullable(handshakeMessage.serverHello());
        }catch (ProtobufDeserializationException exception){
            return Optional.empty();
        }
    }

    private CompletableFuture<Boolean> sendHandshake(SocketSession session, SocketHandshake handshake, HandshakeMessage handshakeMessage) {
        return SocketRequest.of(HandshakeMessageSpec.encode(handshakeMessage))
                .sendWithNoResponse(session, socketHandler.keys(), socketHandler.store())
                .thenApplyAsync(result -> onHandshakeSent(handshake));
    }

    private boolean onHandshakeSent(SocketHandshake handshake) {
        socketHandler.keys().clearReadWriteKey();
        handshake.finish();
        return true;
    }

    private WebInfo createWebInfo() {
        return new WebInfoBuilder()
                .webSubPlatform(WebInfo.Platform.WEB_BROWSER)
                .build();
    }

    private UserAgent createUserAgent() {
        var mobile = socketHandler.store().clientType() == ClientType.MOBILE;
        var device = socketHandler.store().device();
        return new UserAgentBuilder()
                .appVersion(socketHandler.store().version())
                .platform(getUserAgentPlatform(mobile))
                .releaseChannel(socketHandler.store().releaseChannel())
                .mcc(getDeviceMcc(mobile))
                .mnc(getDeviceMnc(mobile))
                .osVersion(mobile ? device.orElseThrow().osVersion().toString() : null)
                .manufacturer(mobile ? device.orElseThrow().manufacturer() : null)
                .device(mobile ? device.orElseThrow().model() : null)
                .osBuildNumber(mobile ? device.orElseThrow().osVersion().toString() : null)
                .localeLanguageIso6391("en")
                .localeCountryIso31661Alpha2("US")
                .phoneId(mobile ? socketHandler.keys().phoneId() : null)
                .build();
    }

    private String getDeviceMcc(boolean mobile) {
        if(!mobile) {
            return "000";
        }

        return socketHandler.store()
                .phoneNumber()
                .map(PhoneNumber::countryCode)
                .map(CountryCode::mcc)
                .map(Object::toString)
                .orElse("000");
    }

    private String getDeviceMnc(boolean mobile) {
        if(!mobile) {
            return "000";
        }

        return socketHandler.store()
                .phoneNumber()
                .map(PhoneNumber::countryCode)
                .map(CountryCode::mnc)
                .orElse("000");
    }

    private PlatformType getUserAgentPlatform(boolean mobile) {
        if (!mobile) {
            return PlatformType.WEB;
        }

        var device = socketHandler.store()
                .device()
                .orElseThrow();
        return socketHandler.store().business() ? device.businessPlatform() : device.platform();
    }

    private ClientPayload createUserClientPayload() {
        var agent = createUserAgent();
        var builder = new ClientPayloadBuilder()
                .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                .userAgent(agent);
        return switch (socketHandler.store().clientType()){
            case MOBILE -> {
                var phoneNumber = socketHandler.store()
                        .phoneNumber()
                        .map(PhoneNumber::number)
                        .orElseThrow(() -> new NoSuchElementException("Missing phone number for mobile registration"));
                yield builder.sessionId(socketHandler.keys().registrationId())
                        .shortConnect(true)
                        .connectAttemptCount(0)
                        .device(0)
                        .dnsSource(getDnsSource())
                        .passive(false)
                        .pushName(socketHandler.store().name())
                        .username(phoneNumber)
                        .build();
            }
            case WEB -> {
                var jid = socketHandler.store().jid();
                if (jid.isPresent()) {
                    yield builder.webInfo(createWebInfo())
                            .username(Long.parseLong(jid.get().user()))
                            .passive(true)
                            .pull(true)
                            .device(jid.get().device())
                            .build();
                }

                yield builder.webInfo(createWebInfo())
                        .regData(createRegisterData())
                        .passive(false)
                        .pull(false)
                        .build();
            }
        };
    }

    private DNSSource getDnsSource() {
        return new DNSSourceBuilder()
                .appCached(false)
                .dnsMethod(ResolutionMethod.SYSTEM)
                .build();
    }

    private CompanionRegistrationData createRegisterData() {
        var companion = new CompanionRegistrationDataBuilder()
                .buildHash(socketHandler.store().version().toHash())
                .eRegid(socketHandler.keys().encodedRegistrationId())
                .eKeytype(BytesHelper.intToBytes(Specification.Signal.KEY_TYPE, 1))
                .eIdent(socketHandler.keys().identityKeyPair().publicKey())
                .eSkeyId(socketHandler.keys().signedKeyPair().encodedId())
                .eSkeyVal(socketHandler.keys().signedKeyPair().keyPair().publicKey())
                .eSkeySig(socketHandler.keys().signedKeyPair().signature());
        if (socketHandler.store().clientType() == ClientType.WEB) {
            var props = createCompanionProps();
            var encodedProps = props == null ? null : CompanionPropertiesSpec.encode(props);
            companion.companionProps(encodedProps);
        }

        return companion.build();
    }

    private CompanionProperties createCompanionProps() {
        return switch (socketHandler.store().clientType()) {
            case WEB -> {
                var historyLength = socketHandler.store().historyLength();
                var config = new HistorySyncConfigBuilder()
                        .inlineInitialPayloadInE2EeMsg(true)
                        .supportBotUserAgentChatHistory(true)
                        .storageQuotaMb(historyLength.size())
                        .build();
                yield new CompanionPropertiesBuilder()
                        .os(socketHandler.store().name())
                        .platformType(CompanionProperties.PlatformType.CHROME)
                        .requireFullSync(historyLength.isExtended())
                        .historySyncConfig(config)
                        .build();
            }
            case MOBILE -> null;
        };
    }
}
