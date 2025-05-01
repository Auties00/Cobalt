package it.auties.whatsapp.socket;

import it.auties.curve25519.Curve25519;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.model.mobile.CountryLocale;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.signal.auth.*;
import it.auties.whatsapp.model.sync.HistorySyncConfigBuilder;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.SignalConstants;

import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;


class AuthHandler {
    private final SocketHandler socketHandler;

    AuthHandler(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    protected CompletableFuture<Void> login(byte[] message) {
        try {
            var serverHandshake = HandshakeMessageSpec.decode(message);
            var serverHello = serverHandshake.serverHello();
            var handshake = new SocketHandshake(socketHandler.keys(), SocketHandshake.getPrologue(socketHandler.store().clientType()));
            handshake.updateHash(socketHandler.keys().ephemeralKeyPair().publicKey());
            handshake.updateHash(serverHello.ephemeral());
            var sharedEphemeral = Curve25519.sharedKey(serverHello.ephemeral(), socketHandler.keys().ephemeralKeyPair().privateKey());
            handshake.mixIntoKey(sharedEphemeral);
            var decodedStaticText = handshake.cipher(serverHello.staticText(), false);
            var sharedStatic = Curve25519.sharedKey(decodedStaticText, socketHandler.keys().ephemeralKeyPair().privateKey());
            handshake.mixIntoKey(sharedStatic);
            handshake.cipher(serverHello.payload(), false);
            var encodedKey = handshake.cipher(socketHandler.keys().noiseKeyPair().publicKey(), true);
            var sharedPrivate = Curve25519.sharedKey(serverHello.ephemeral(), socketHandler.keys().noiseKeyPair().privateKey());
            handshake.mixIntoKey(sharedPrivate);
            var payload = createUserClientPayload();
            var encodedPayload = handshake.cipher(ClientPayloadSpec.encode(payload), true);
            var clientFinish = new ClientFinish(encodedKey, encodedPayload);
            var clientHandshake = new HandshakeMessageBuilder()
                    .clientFinish(clientFinish)
                    .build();
            return socketHandler.sendBinaryWithNoResponse(HandshakeMessageSpec.encode(clientHandshake), false).thenRunAsync(() -> {
                socketHandler.keys().clearReadWriteKey();
                handshake.finish();
            });
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(throwable);
        }
    }

    private WebInfo createWebInfo() {
        return new WebInfoBuilder()
                .webSubPlatform(WebInfo.Platform.WEB_BROWSER)
                .build();
    }

    private UserAgent createUserAgent() {
        var mobile = socketHandler.store().clientType() == ClientType.MOBILE;
        return new UserAgentBuilder()
                .platform(socketHandler.store().device().platform())
                .appVersion(socketHandler.store().version())
                .mcc("000")
                .mnc("000")
                .osVersion(mobile ? socketHandler.store().device().osVersion().toString() : null)
                .manufacturer(mobile ? socketHandler.store().device().manufacturer() : null)
                .device(mobile ? socketHandler.store().device().model().replaceAll("_", " ") : null)
                .osBuildNumber(mobile ? socketHandler.store().device().osBuildNumber() : null)
                .phoneId(mobile ? socketHandler.keys().fdid().toUpperCase() : null)
                .releaseChannel(socketHandler.store().releaseChannel())
                .localeLanguageIso6391(socketHandler.store().locale().map(CountryLocale::languageValue).orElse("en"))
                .localeCountryIso31661Alpha2(socketHandler.store().locale().map(CountryLocale::languageCode).orElse("US"))
                .deviceType(UserAgent.DeviceType.PHONE)
                .deviceModelType(socketHandler.store().device().modelId())
                .build();
    }

    private ClientPayload createUserClientPayload() {
        var agent = createUserAgent();
        return switch (socketHandler.store().clientType()) {
            case MOBILE -> {
                var phoneNumber = socketHandler.store()
                        .phoneNumber()
                        .map(PhoneNumber::number)
                        .orElseThrow(() -> new NoSuchElementException("Missing phone number for mobile registration"));
                yield new ClientPayloadBuilder()
                        .username(phoneNumber)
                        .passive(false)
                        .pushName(socketHandler.keys().initialAppSync() ? socketHandler.store().name() : null)
                        .userAgent(agent)
                        .shortConnect(true)
                        .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                        .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                        .connectAttemptCount(0)
                        .device(0)
                        .oc(false)
                        .build();
            }
            case WEB -> {
                var jid = socketHandler.store().jid();
                if (jid.isPresent()) {
                    yield new ClientPayloadBuilder()
                            .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                            .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                            .userAgent(agent)
                            .webInfo(createWebInfo())
                            .username(Long.parseLong(jid.get().user()))
                            .passive(true)
                            .pull(true)
                            .device(jid.get().device())
                            .build();
                }

                yield new ClientPayloadBuilder()
                        .connectReason(ClientPayload.ClientPayloadConnectReason.USER_ACTIVATED)
                        .connectType(ClientPayload.ClientPayloadConnectType.WIFI_UNKNOWN)
                        .userAgent(agent)
                        .webInfo(createWebInfo())
                        .regData(createRegisterData())
                        .passive(false)
                        .pull(false)
                        .build();
            }
        };
    }

    private DNSSource getDnsSource() {
        return new DNSSourceBuilder()
                .dnsMethod(DNSSource.ResolutionMethod.SYSTEM)
                .appCached(false)
                .build();
    }

    private CompanionRegistrationData createRegisterData() {
        var companion = new CompanionRegistrationDataBuilder()
                .buildHash(socketHandler.store().version().toHash())
                .eRegid(socketHandler.keys().encodedRegistrationId())
                .eKeytype(Bytes.intToBytes(SignalConstants.KEY_TYPE, 1))
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
                var historyLength = socketHandler.store().webHistorySetting();
                var config = new HistorySyncConfigBuilder()
                        .inlineInitialPayloadInE2EeMsg(true)
                        .supportBotUserAgentChatHistory(true)
                        .supportCallLogHistory(true)
                        .storageQuotaMb(historyLength.size())
                        .fullSyncSizeMbLimit(historyLength.size())
                        .build();

                // Extended history sync won't work if we don't have a Desktop device
                if(socketHandler.store().webHistorySetting().isExtended() && !socketHandler.store().device().platform().isDesktop()) {
                    socketHandler.store().setDevice(socketHandler.store().device().withPlatform(UserAgent.PlatformType.WINDOWS));
                }

                var platformType = switch (socketHandler.store().device().platform()) {
                    case UNKNOWN, KAIOS -> CompanionProperties.PlatformType.UNKNOWN;
                    case IOS, IOS_BUSINESS -> CompanionProperties.PlatformType.IOS_PHONE;
                    case ANDROID, ANDROID_BUSINESS -> CompanionProperties.PlatformType.ANDROID_PHONE;
                    case WINDOWS -> CompanionProperties.PlatformType.UWP;
                    case MACOS -> CompanionProperties.PlatformType.CATALINA;
                    case WEB -> CompanionProperties.PlatformType.CHROME;
                };
                yield new CompanionPropertiesBuilder()
                        .os(socketHandler.store().name())
                        .platformType(platformType)
                        .requireFullSync(historyLength.isExtended())
                        .historySyncConfig(config)
                        .build();
            }
            case MOBILE -> null;
        };
    }
}
