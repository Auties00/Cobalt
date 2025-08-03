package it.auties.whatsapp.socket;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;
import it.auties.whatsapp.api.WhatsappClientType;
import it.auties.whatsapp.crypto.Hkdf;
import it.auties.whatsapp.io.BinaryNodeEncoder;
import it.auties.whatsapp.io.BinaryNodeLength;
import it.auties.whatsapp.io.BinaryNodeTokens;
import it.auties.whatsapp.model.mobile.CountryLocale;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.signal.auth.*;
import it.auties.whatsapp.model.sync.HistorySyncConfigBuilder;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.SignalConstants;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

final class EncryptionHandler {
    private static final byte[] NOISE_PROTOCOL = "Noise_XX_25519_AESGCM_SHA256\0\0\0\0".getBytes(StandardCharsets.UTF_8);
    private static final byte[] WHATSAPP_VERSION_HEADER = "WA".getBytes(StandardCharsets.UTF_8);
    private static final byte[] WEB_VERSION = new byte[]{6, BinaryNodeTokens.DICTIONARY_VERSION};
    private static final byte[] WEB_PROLOGUE = Bytes.concat(WHATSAPP_VERSION_HEADER, WEB_VERSION);
    private static final byte[] MOBILE_VERSION = new byte[]{5, BinaryNodeTokens.DICTIONARY_VERSION};
    private static final byte[] MOBILE_PROLOGUE = Bytes.concat(WHATSAPP_VERSION_HEADER, MOBILE_VERSION);
    public static final int HEADER_LENGTH = Integer.BYTES + Short.BYTES;

    private static GCMParameterSpec createGcmIv(long counter) {
        var iv = new byte[12];
        iv[4] = (byte) (counter >> 56);
        iv[5] = (byte) (counter >> 48);
        iv[6] = (byte) (counter >> 40);
        iv[7] = (byte) (counter >> 32);
        iv[8] = (byte) (counter >> 24);
        iv[9] = (byte) (counter >> 16);
        iv[10] = (byte) (counter >> 8);
        iv[11] = (byte) (counter);
        return new GCMParameterSpec(128, iv);
    }

    private final SocketHandler socketHandler;
    private final AtomicLong readCounter;
    private final AtomicLong writeCounter;
    private volatile SecretKeySpec readKey;
    private volatile SecretKeySpec writeKey;
    private final ReentrantLock readCipherLock;
    private final ReentrantLock writeCipherLock;

    EncryptionHandler(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
        this.readCounter = new AtomicLong();
        this.writeCounter = new AtomicLong();
        this.readCipherLock = new ReentrantLock(true);
        this.writeCipherLock = new ReentrantLock(true);
    }

    synchronized void startHandshake(byte[] publicKey) {
        if(readKey != null || writeKey != null) {
            throw new IllegalStateException("Handshake has already been completed");
        }
        var prologue = getHandshakePrologue();
        var clientHello = new ClientHelloBuilder()
                .ephemeral(publicKey)
                .build();
        var handshakeMessage = new HandshakeMessageBuilder()
                .clientHello(clientHello)
                .build();
        var requestLength = HandshakeMessageSpec.sizeOf(handshakeMessage);
        var message = new byte[prologue.length + HEADER_LENGTH + requestLength];
        System.arraycopy(prologue, 0, message, 0, prologue.length);
        var offset = writeRequestHeader(requestLength, message, prologue.length);
        HandshakeMessageSpec.encode(handshakeMessage, ProtobufOutputStream.toBytes(message, offset));
        socketHandler.sendBinary(message);
    }

    synchronized void finishHandshake(ByteBuffer serverHelloPayload) {
        if(readKey != null || writeKey != null) {
            throw new IllegalStateException("Handshake has already been completed");
        }
        var serverHandshake = HandshakeMessageSpec.decode(ProtobufInputStream.fromBuffer(serverHelloPayload));
        var serverHello = serverHandshake.serverHello();
        try(var handshake = new Handshake(getHandshakePrologue())) {
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
            var requestLength = HandshakeMessageSpec.sizeOf(clientHandshake);
            var message = new byte[HEADER_LENGTH + requestLength];
            var offset = writeRequestHeader(requestLength, message, 0);
            HandshakeMessageSpec.encode(clientHandshake, ProtobufOutputStream.toBytes(message, offset));
            socketHandler.sendBinary(message);
            var keys = handshake.finish();
            writeCounter.set(0);
            readCounter.set(0);
            writeKey = new SecretKeySpec(keys, 0, 32, "AES");
            readKey = new SecretKeySpec(keys, 32, 32, "AES");
        }
    }

    private byte[] getHandshakePrologue() {
        return switch (socketHandler.store().clientType()) {
            case WEB -> WEB_PROLOGUE;
            case MOBILE -> MOBILE_PROLOGUE;
        };
    }

    boolean sendCiphered(Node node) {
        var writeKey = this.writeKey;
        if(writeKey == null) {
            return false;
        }

        var releasedLock = false;
        try {
            writeCipherLock.lock();
            var writeCipher = Cipher.getInstance("AES/GCM/NoPadding");
            writeCipher.init(
                    Cipher.ENCRYPT_MODE,
                    writeKey,
                    createGcmIv(writeCounter.getAndIncrement())
            );
            var plaintextLength = BinaryNodeLength.sizeOf(node);
            var ciphertextLength = writeCipher.getOutputSize(plaintextLength);
            var ciphertext = new byte[HEADER_LENGTH + ciphertextLength];
            var offset = writeRequestHeader(ciphertextLength, ciphertext, 0);
            BinaryNodeEncoder.encode(node, ciphertext, offset);
            writeCipher.doFinal(ciphertext, offset, plaintextLength, ciphertext, offset);
            writeCipherLock.unlock();
            releasedLock = true;
            if (this.writeKey != writeKey) {
                // Session changed
                return false;
            }
            socketHandler.sendBinary(ciphertext);
            return true;
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot encrypt data", throwable);
        }finally {
            if(!releasedLock) {
                writeCipherLock.unlock();
            }
        }
    }

    private int writeRequestHeader(int requestLength, byte[] message, int offset) {
        var mss = requestLength >> 16;
        message[offset++] = (byte) (mss >> 24);
        message[offset++] = (byte) (mss >> 16);
        message[offset++] = (byte) (mss >> 8);
        message[offset++] = (byte) mss;
        var lss = requestLength & 65535;
        message[offset++] = (byte) (lss >> 8);
        message[offset++] = (byte) lss;
        return offset;
    }

    ByteBuffer receiveDeciphered(ByteBuffer message) {
        var readKey = this.readKey;
        if(readKey == null) {
            return null;
        }

        var releasedLock = false;
        try {
            readCipherLock.lock();
            var output = message.duplicate();
            var readCipher = Cipher.getInstance("AES/GCM/NoPadding");
            readCipher.init(
                    Cipher.DECRYPT_MODE,
                    readKey,
                    createGcmIv(readCounter.getAndIncrement())
            );
            readCipher.doFinal(message, output);
            readCipherLock.unlock();
            releasedLock = true;
            output.flip();
            return output;
        } catch (GeneralSecurityException exception) {
            return null;
        } finally {
            if(!releasedLock) {
                readCipherLock.unlock();
            }
        }
    }

    private UserAgent createUserAgent() {
        var mobile = socketHandler.store().clientType() == WhatsappClientType.MOBILE;
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
                        .regData(createRegisterData())
                        .passive(false)
                        .pull(false)
                        .build();
            }
        };
    }

    private CompanionRegistrationData createRegisterData() {
        var companion = new CompanionRegistrationDataBuilder()
                .buildHash(socketHandler.store().version().toHash())
                .eRegid(socketHandler.keys().encodedRegistrationId())
                .eKeytype(Bytes.intToBytes(SignalConstants.KEY_TYPE, 1))
                .eIdent(socketHandler.keys().identityKeyPair().publicKey())
                .eSkeyId(socketHandler.keys().signedKeyPair().encodedId())
                .eSkeyVal(socketHandler.keys().signedKeyPair().publicKey())
                .eSkeySig(socketHandler.keys().signedKeyPair().signature());
        if (socketHandler.store().clientType() == WhatsappClientType.WEB) {
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
                var platformType = switch (socketHandler.store().device().platform()) {
                    case IOS, IOS_BUSINESS -> CompanionProperties.PlatformType.IOS_PHONE;
                    case ANDROID, ANDROID_BUSINESS -> CompanionProperties.PlatformType.ANDROID_PHONE;
                    case WINDOWS -> CompanionProperties.PlatformType.UWP;
                    case MACOS -> CompanionProperties.PlatformType.IOS_CATALYST;
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

    synchronized void reset() {
        this.readKey = null;
        this.writeKey = null;
    }

    private static final class Handshake implements AutoCloseable {
        private byte[] hash;
        private byte[] salt;
        private SecretKeySpec cryptoKey;
        private long counter;

        private Handshake(byte[] prologue) {
            this.hash = NOISE_PROTOCOL;
            this.salt = NOISE_PROTOCOL;
            this.cryptoKey = new SecretKeySpec(NOISE_PROTOCOL, 0, 32, "AES");
            this.counter = 0;
            updateHash(prologue);
        }

        private void updateHash(byte[] data) {
            try {
                var digest = MessageDigest.getInstance("SHA-256");
                digest.update(hash);
                digest.update(data);
                this.hash = digest.digest();
            } catch (NoSuchAlgorithmException exception) {
                throw new UnsupportedOperationException("Missing sha256 implementation");
            }
        }

        private byte[] cipher(byte[] bytes, boolean encrypt) {
            try {
                var cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(
                        encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
                        cryptoKey,
                        createGcmIv(counter++)
                );
                cipher.updateAAD(hash);
                var result = cipher.doFinal(bytes);
                updateHash(encrypt ? result : bytes);
                return result;
            }catch (GeneralSecurityException exception) {
                throw new IllegalArgumentException("Cannot encrypt data", exception);
            }
        }

        private byte[] finish() {
            return Hkdf.extractAndExpand(new byte[0], salt, null, 64);
        }

        private void mixIntoKey(byte[] bytes) {
            var expanded = Hkdf.extractAndExpand(bytes, salt, null, 64);
            this.salt = Arrays.copyOfRange(expanded, 0, 32);
            this.cryptoKey = new SecretKeySpec(expanded, 32, 32, "AES");
            this.counter = 0;
        }

        @Override
        public void close() {
            this.hash = null;
            this.salt = null;
            this.cryptoKey = null;
            this.counter = 0;
        }
    }
}
