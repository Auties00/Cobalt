package com.github.auties00.cobalt.socket;

import com.github.auties00.cobalt.client.WhatsAppClientType;
import com.github.auties00.cobalt.client.WhatsAppWebClientHistory;
import com.github.auties00.cobalt.client.version.WhatsAppClientVersion;
import com.github.auties00.cobalt.io.node.NodeEncoder;
import com.github.auties00.cobalt.io.node.NodeTokens;
import com.github.auties00.cobalt.model.node.Node;
import com.github.auties00.cobalt.model.proto.auth.*;
import com.github.auties00.cobalt.model.proto.sync.HistorySyncConfigBuilder;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.util.SecureBytes;
import com.github.auties00.curve25519.Curve25519;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.protobuf.stream.ProtobufOutputStream;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public final class SocketEncryption {
    private static final byte[] NOISE_PROTOCOL = "Noise_XX_25519_AESGCM_SHA256\0\0\0\0".getBytes(StandardCharsets.UTF_8);
    private static final byte[] WHATSAPP_VERSION_HEADER = "WA".getBytes(StandardCharsets.UTF_8);
    private static final byte[] WEB_VERSION = new byte[]{6, NodeTokens.DICTIONARY_VERSION};
    private static final byte[] WEB_PROLOGUE = SecureBytes.concat(WHATSAPP_VERSION_HEADER, WEB_VERSION);
    private static final byte[] MOBILE_VERSION = new byte[]{5, NodeTokens.DICTIONARY_VERSION};
    private static final byte[] MOBILE_PROLOGUE = SecureBytes.concat(WHATSAPP_VERSION_HEADER, MOBILE_VERSION);
    private static final int HEADER_LENGTH = Integer.BYTES + Short.BYTES;

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

    private final WhatsappStore store;
    private final Consumer<ByteBuffer> sendBinary;
    private final AtomicLong readCounter;
    private final AtomicLong writeCounter;
    private volatile SecretKeySpec readKey;
    private volatile SecretKeySpec writeKey;
    private final ReentrantLock readCipherLock;
    private final ReentrantLock writeCipherLock;
    private SignalIdentityKeyPair ephemeralKeyPair;

    public SocketEncryption(WhatsappStore store, Consumer<ByteBuffer> sendBinary) {
        this.store = store;
        this.sendBinary = sendBinary;
        this.readCounter = new AtomicLong();
        this.writeCounter = new AtomicLong();
        this.readCipherLock = new ReentrantLock(true);
        this.writeCipherLock = new ReentrantLock(true);
    }

    public synchronized void startHandshake() {
        if(ephemeralKeyPair != null) {
            throw new IllegalStateException("Handshake has already started");
        }

        if(readKey != null || writeKey != null) {
            throw new IllegalStateException("Handshake has already been completed");
        }

        this.ephemeralKeyPair = SignalIdentityKeyPair.random();

        var prologue = getHandshakePrologue();
        var clientHello = new ClientHelloBuilder()
                .ephemeral(ephemeralKeyPair.publicKey().toEncodedPoint())
                .build();
        var handshakeMessage = new HandshakeMessageBuilder()
                .clientHello(clientHello)
                .build();
        var requestLength = HandshakeMessageSpec.sizeOf(handshakeMessage);
        var message = new byte[prologue.length + HEADER_LENGTH + requestLength];
        System.arraycopy(prologue, 0, message, 0, prologue.length);
        var offset = writeRequestHeader(requestLength, message, prologue.length);
        HandshakeMessageSpec.encode(handshakeMessage, ProtobufOutputStream.toBytes(message, offset));
        sendBinary.accept(ByteBuffer.wrap(message));
    }

    public synchronized void finishHandshake(ByteBuffer serverHelloPayload) {
        if(readKey != null || writeKey != null) {
            throw new IllegalStateException("Handshake has already been completed");
        }
        var serverHandshake = HandshakeMessageSpec.decode(ProtobufInputStream.fromBuffer(serverHelloPayload));
        var serverHello = serverHandshake.serverHello();
        try(var handshake = new Handshake(getHandshakePrologue())) {
            handshake.updateHash(ephemeralKeyPair.publicKey().toEncodedPoint());
            handshake.updateHash(serverHello.ephemeral());
            var sharedEphemeral = Curve25519.sharedKey(ephemeralKeyPair.privateKey().toEncodedPoint(), serverHello.ephemeral());
            handshake.mixIntoKey(sharedEphemeral);
            var decodedStaticText = handshake.cipher(serverHello.staticText(), false);
            var sharedStatic = Curve25519.sharedKey(ephemeralKeyPair.privateKey().toEncodedPoint(), decodedStaticText);
            handshake.mixIntoKey(sharedStatic);
            handshake.cipher(serverHello.payload(), false);
            var encodedKey = handshake.cipher(store.noiseKeyPair().publicKey().toEncodedPoint(), true);
            var sharedPrivate = Curve25519.sharedKey(store.noiseKeyPair().privateKey().toEncodedPoint(), serverHello.ephemeral());
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
            sendBinary.accept(ByteBuffer.wrap(message));
            var keys = handshake.finish();
            writeCounter.set(0);
            readCounter.set(0);
            writeKey = new SecretKeySpec(keys, 0, 32, "AES");
            readKey = new SecretKeySpec(keys, 32, 32, "AES");
            ephemeralKeyPair = null;
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot finish handshake", exception);
        }
    }

    private byte[] getHandshakePrologue() {
        return switch (store.clientType()) {
            case WEB -> WEB_PROLOGUE;
            case MOBILE -> MOBILE_PROLOGUE;
        };
    }

    public boolean sendCiphered(Node node) {
        var writeKey = this.writeKey;
        if(writeKey == null) {
            return false;
        }

        try {
            writeCipherLock.lock();
            var writeCipher = Cipher.getInstance("AES/GCM/NoPadding");
            writeCipher.init(
                    Cipher.ENCRYPT_MODE,
                    writeKey,
                    createGcmIv(writeCounter.getAndIncrement())
            );
            var plaintextLength = NodeEncoder.sizeOf(node);
            var ciphertextLength = writeCipher.getOutputSize(plaintextLength);
            var ciphertext = new byte[HEADER_LENGTH + ciphertextLength];
            var offset = writeRequestHeader(ciphertextLength, ciphertext, 0);
            NodeEncoder.encode(node, ciphertext, offset);
            writeCipher.doFinal(ciphertext, offset, plaintextLength, ciphertext, offset);
            if (this.writeKey != writeKey) {
                // Session changed
                return false;
            }
            sendBinary.accept(ByteBuffer.wrap(ciphertext));
            return true;
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot encrypt data", throwable);
        }finally {
            writeCipherLock.unlock();
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

    public ByteBuffer receiveDeciphered(ByteBuffer message) {
        var readKey = this.readKey;
        if(readKey == null) {
            throw new IllegalStateException("Handshake has not been completed");
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
        }catch (GeneralSecurityException exception) {
            throw new RuntimeException("Cannot decrypt data", exception);
        } finally {
            if(!releasedLock) {
                readCipherLock.unlock();
            }
        }
    }

    private UserAgent createUserAgent() {
        var clientInfo = WhatsAppClientVersion.of(store.device().platform());
        var mobile = store.clientType() == WhatsAppClientType.MOBILE;
        return new UserAgentBuilder()
                .platform(store.device().platform())
                .appVersion(clientInfo.latest())
                .mcc("000")
                .mnc("000")
                .osVersion(mobile ? store.device().osVersion().toString() : null)
                .manufacturer(mobile ? store.device().manufacturer() : null)
                .device(mobile ? store.device().model().replaceAll("_", " ") : null)
                .osBuildNumber(mobile ? store.device().osBuildNumber() : null)
                .phoneId(mobile ? store.fdid().toString().toUpperCase() : null)
                .releaseChannel(store.releaseChannel())
                .localeLanguageIso6391("en")
                .localeCountryIso31661Alpha2("US")
                .deviceType(UserAgent.DeviceType.PHONE)
                .deviceModelType(store.device().modelId())
                .build();
    }

    private ClientPayload createUserClientPayload() {
        var agent = createUserAgent();
        return switch (store.clientType()) {
            case MOBILE -> {
                var phoneNumber = store
                        .phoneNumber()
                        .orElseThrow(() -> new InternalError("Phone number was not set"));
                yield new ClientPayloadBuilder()
                        .username(phoneNumber)
                        .passive(false)
                        .pushName(store.registered() ? store.name() : null)
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
                var jid = store.jid();
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
        var clientInfo = WhatsAppClientVersion.of(store.device().platform());
        var companion = new CompanionRegistrationDataBuilder()
                .buildHash(clientInfo.latest().toHash())
                .eRegid(SecureBytes.intToBytes(store.registrationId(), 4))
                .eKeytype(SecureBytes.intToBytes(SignalIdentityPublicKey.type(), 1))
                .eIdent(store.identityKeyPair().publicKey().toEncodedPoint())
                .eSkeyId(SecureBytes.intToBytes(store.signedKeyPair().id(), 3))
                .eSkeyVal(store.signedKeyPair().publicKey().toEncodedPoint())
                .eSkeySig(store.signedKeyPair().signature());
        if (store.clientType() == WhatsAppClientType.WEB) {
            var props = createCompanionProps();
            var encodedProps = props == null ? null : CompanionPropertiesSpec.encode(props);
            companion.companionProps(encodedProps);
        }

        return companion.build();
    }

    private CompanionProperties createCompanionProps() {
        return switch (store.clientType()) {
            case WEB -> {
                var historyLength = store.webHistoryPolicy()
                        .orElse(WhatsAppWebClientHistory.standard(true));
                var config = new HistorySyncConfigBuilder()
                        .inlineInitialPayloadInE2EeMsg(true)
                        .supportBotUserAgentChatHistory(true)
                        .supportCallLogHistory(true)
                        .storageQuotaMb(historyLength.size())
                        .fullSyncSizeMbLimit(historyLength.size())
                        .build();
                var platformType = switch (store.device().platform()) {
                    case IOS, IOS_BUSINESS -> CompanionProperties.PlatformType.IOS_PHONE;
                    case ANDROID, ANDROID_BUSINESS -> CompanionProperties.PlatformType.ANDROID_PHONE;
                    case WINDOWS -> CompanionProperties.PlatformType.UWP;
                    case MACOS -> CompanionProperties.PlatformType.IOS_CATALYST;
                };
                yield new CompanionPropertiesBuilder()
                        .os(store.name())
                        .platformType(platformType)
                        .requireFullSync(historyLength.isExtended())
                        .historySyncConfig(config)
                        .build();
            }
            case MOBILE -> null;
        };
    }

    public synchronized void reset() {
        this.readKey = null;
        this.writeKey = null;
    }

    private static final class Handshake implements AutoCloseable {
        public static final SecretKeySpec FINISH_KEY = new SecretKeySpec(new byte[0], "AES");

        private final KDF kdf;
        private final MessageDigest hashDigest;
        private final Cipher cipher;

        private byte[] hash;
        private SecretKeySpec salt;
        private SecretKeySpec cryptoKey;
        private long counter;

        private Handshake(byte[] prologue) throws NoSuchAlgorithmException, NoSuchPaddingException {
            this.kdf = KDF.getInstance("HKDF-SHA256");
            this.hashDigest = MessageDigest.getInstance("SHA-256");
            this.cipher = Cipher.getInstance("AES/GCM/NoPadding");
            this.hash = NOISE_PROTOCOL;
            this.salt = new SecretKeySpec(NOISE_PROTOCOL, "AES");
            this.cryptoKey = new SecretKeySpec(NOISE_PROTOCOL, 0, 32, "AES");
            this.counter = 0;
            updateHash(prologue);
        }

        private void updateHash(byte[] data) {
            hashDigest.update(hash);
            hashDigest.update(data);
            this.hash = hashDigest.digest();
        }

        private byte[] cipher(byte[] text, boolean encrypt) throws IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
            cipher.init(
                    encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
                    cryptoKey,
                    createGcmIv(counter++)
            );
            cipher.updateAAD(hash);
            var result = cipher.doFinal(text);
            updateHash(encrypt ? result : text);
            return result;
        }

        private byte[] finish() throws GeneralSecurityException {
            var params = HKDFParameterSpec.ofExtract()
                    .addSalt(salt)
                    .addIKM(FINISH_KEY)
                    .thenExpand(null, 64);
            return kdf.deriveData(params);
        }

        private void mixIntoKey(byte[] bytes) throws GeneralSecurityException {
            var params = HKDFParameterSpec.ofExtract()
                    .addSalt(salt)
                    .addIKM(new SecretKeySpec(bytes, "AES"))
                    .thenExpand(null, 64);
            var expanded = kdf.deriveData(params);
            this.salt = new SecretKeySpec(expanded, 0, 32, "AES");
            this.cryptoKey = new SecretKeySpec(expanded, 32, 32, "AES");
            this.counter = 0;
        }

        @Override
        public void close() {
            this.hash = null;
            this.salt = null;
            try {
                cryptoKey.destroy();
            } catch (DestroyFailedException _) {

            }
            this.cryptoKey = null;
            this.counter = 0;
        }
    }
}
