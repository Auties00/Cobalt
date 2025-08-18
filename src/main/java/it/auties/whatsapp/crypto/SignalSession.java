package it.auties.whatsapp.crypto;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.stream.ProtobufOutputStream;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.message.*;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.signal.sender.SenderKeyRecord;
import it.auties.whatsapp.model.signal.sender.SenderKeyState;
import it.auties.whatsapp.model.signal.sender.SenderMessageKey;
import it.auties.whatsapp.model.signal.session.*;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.SignalConstants;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import static it.auties.whatsapp.util.SignalConstants.*;

public final class SignalSession {
    private final Keys keys;

    public SignalSession(Keys keys) {
        this.keys = keys;
    }

    public Result encrypt(SessionAddress address, byte[] data) {
        try {
            var currentState = keys.findSessionByAddress(address)
                    .orElseThrow(() -> new NoSuchElementException("Missing session for " + address))
                    .currentState()
                    .orElseThrow(() -> new NoSuchElementException("Missing state for address " + address));
            if (!keys.hasTrust(address, currentState.remoteIdentityKey())) {
                throw new IllegalArgumentException("Untrusted key");
            }
            var chain = currentState.findChain(currentState.ephemeralKeyPair().signalPublicKey())
                    .orElseThrow(() -> new NoSuchElementException("Missing chain for " + address));
            fillMessageKeys(chain, chain.counter() + 1);
            var currentKeyCounter = chain.counter();
            var currentKey = chain.getMessageKey(currentKeyCounter)
                    .orElseThrow(() -> new NoSuchElementException("Missing key in chain for counter " + currentKeyCounter));
            var secrets = Hkdf.deriveSecrets(currentKey, "WhisperMessageKeys".getBytes(StandardCharsets.UTF_8));
            chain.removeMessageKey(chain.counter());
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(secrets[0], "AES"),
                    new IvParameterSpec(secrets[2], 0, IV_LENGTH)
            );
            var encrypted = cipher.doFinal(data);
            var message = SignalMessage.ofSigned(
                    currentState.version(),
                    currentState.ephemeralKeyPair().signalPublicKey(),
                    chain.counter(),
                    currentState.previousCounter(),
                    encrypted,
                    keys.identityKeyPair().publicKey(),
                    currentState.remoteIdentityKey(),
                    secrets[1]
            );
            if (!currentState.hasPreKey()) {
                return new Result(
                        message.serialized(),
                        currentState.hasPreKey() ? PKMSG : MSG
                );
            } else {
                var preKeyMessage = new SignalPreKeyMessage(
                        message.version(),
                        currentState.pendingPreKey().preKeyId(),
                        currentState.pendingPreKey().baseKey(),
                        keys.identityKeyPair().signalPublicKey(),
                        message.serialized(),
                        keys.registrationId(),
                        currentState.pendingPreKey().signedKeyId()
                );
                return new Result(
                        preKeyMessage.serialized(),
                        currentState.hasPreKey() ? PKMSG : MSG
                );
            }
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot encrypt data", exception);
        }
    }

    public byte[] decrypt(SessionAddress address, SignalPreKeyMessage message) {
        var session = keys.findSessionByAddress(address).orElseGet(() -> {
            var newSession = new Session();
            keys.addSession(address, newSession);
            return newSession;
        });
        createIncoming(address, session, message);
        var state = session.findState(message.version(), message.baseKey())
                .orElseThrow(() -> new NoSuchElementException("Missing state for address " + address));
        return decrypt(message.signalMessage(), state);
    }

    public byte[] decrypt(SessionAddress address, SignalMessage message) {
        var state = keys.findSessionByAddress(address)
                .orElseThrow(() -> new NoSuchElementException("Missing session for " + address))
                .currentState()
                .orElseThrow(() -> new NoSuchElementException("Missing state for address " + address));
        if (!keys.hasTrust(address, state.remoteIdentityKey())) {
            throw new IllegalArgumentException("Untrusted key");
        }
        return decrypt(message, state);
    }

    private byte[] decrypt(SignalMessage message, SessionState state) {
        try {
            maybeStepRatchet(message, state);
            var chain = state.findChain(message.ephemeralPublicKey())
                    .orElseThrow(() -> new NoSuchElementException("Invalid chain"));
            fillMessageKeys(chain, message.counter());
            if (!chain.hasMessageKey(message.counter())) {
                throw new IllegalArgumentException("Key used already or never filled");
            }
            var messageKeyCounter = message.counter();
            var messageKey = chain.getMessageKey(messageKeyCounter)
                    .orElseThrow(() -> new NoSuchElementException("Missing key in chain for counter " + messageKeyCounter));
            var secrets = Hkdf.deriveSecrets(messageKey, "WhisperMessageKeys".getBytes(StandardCharsets.UTF_8));
            var expectedSignature = message.signature();
            var remoteIdentityKey = state.remoteIdentityKey();
            var identityPublicKey = keys.identityKeyPair().signalPublicKey();
            var actualSignature = computeDecryptionMac(message, remoteIdentityKey, identityPublicKey, secrets[1]);
            if (!Arrays.equals(expectedSignature, 0, MAC_LENGTH, actualSignature, 0, MAC_LENGTH)) {
                throw new HmacValidationException("message_decryption");
            }
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var keySpec = new SecretKeySpec(secrets[0], "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(secrets[2], 0, IV_LENGTH));
            var plaintext = cipher.doFinal(message.ciphertext());
            state.setPendingPreKey(null);
            return plaintext;
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot decrypt data", exception);
        }
    }

    private byte[] computeDecryptionMac(SignalMessage message, byte[] signalRemoteIdentityKey, byte[] signalIdentityPublicKey, byte[] hmacKey) {
        if (signalRemoteIdentityKey == null || signalRemoteIdentityKey.length != KEY_LENGTH + 1 || signalRemoteIdentityKey[0] != KEY_TYPE) {
            throw new IllegalArgumentException("Invalid signalRemoteIdentityKey");
        }

        if (signalIdentityPublicKey == null || signalIdentityPublicKey.length != KEY_LENGTH + 1 || signalIdentityPublicKey[0] != KEY_TYPE) {
            throw new IllegalArgumentException("Invalid signalIdentityPublicKey");
        }

        var messageLength = SignalMessageSpec.sizeOf(message);
        var hmacValue = new byte[signalRemoteIdentityKey.length + signalIdentityPublicKey.length + 1 + messageLength];
        System.arraycopy(signalRemoteIdentityKey, 0, hmacValue, 0, signalRemoteIdentityKey.length);
        System.arraycopy(signalIdentityPublicKey, 0, hmacValue, signalRemoteIdentityKey.length, signalIdentityPublicKey.length);
        hmacValue[signalRemoteIdentityKey.length + signalIdentityPublicKey.length] = message.serializedVersion();
        SignalMessageSpec.encode(message, ProtobufOutputStream.toBytes(hmacValue, signalRemoteIdentityKey.length + signalIdentityPublicKey.length + 1));

        try {
            var localMac = Mac.getInstance("HmacSHA256");
            localMac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return localMac.doFinal(hmacValue);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot calculate hmac", exception);
        }
    }

    private void maybeStepRatchet(SignalMessage message, SessionState state) {
        if (state.hasChain(message.ephemeralPublicKey())) {
            return;
        }

        state.findChain(state.lastRemoteEphemeralKey()).ifPresent(chain -> {
            fillMessageKeys(chain, state.previousCounter());
            chain.close();
        });
        calculateRatchet(message, state, false);
        state.findChain(state.ephemeralKeyPair().signalPublicKey()).ifPresent(chain -> {
            state.setPreviousCounter(chain.counter());
            state.removeChain(state.ephemeralKeyPair().signalPublicKey());
        });
        state.setEphemeralKeyPair(SignalKeyPair.random());
        calculateRatchet(message, state, true);
        state.setLastRemoteEphemeralKey(message.ephemeralPublicKey());
    }

    private void calculateRatchet(SignalMessage message, SessionState state, boolean sending) {
        var sharedSecret = Curve25519.sharedKey(createCurveKey(message.ephemeralPublicKey()), state.ephemeralKeyPair().privateKey());
        var masterKey = Hkdf.deriveSecrets(sharedSecret, state.rootKey(), "WhisperRatchet".getBytes(StandardCharsets.UTF_8), 2);
        var chainKey = sending ? state.ephemeralKeyPair().signalPublicKey() : message.ephemeralPublicKey();
        var sessionChain = new SessionChain(-1, masterKey[1]);
        state.addChain(chainKey, sessionChain);
        state.setRootKey(masterKey[0]);
    }

    private void fillMessageKeys(SessionChain chain, int counter) {
        while (chain.counter() < counter) {
            var chainCounter = chain.counter();
            var delta = counter - chainCounter;
            if (delta > MAX_MESSAGES) {
                throw new IllegalArgumentException("Message overflow: expected <= %s, got %s".formatted(MAX_MESSAGES, delta));
            }

            var chainKey = chain.key();
            if (chainKey == null) {
                throw new IllegalStateException("Closed chain");
            }

            var messagesHmac = Hmac.calculateSha256(new byte[]{1}, chainKey);
            var nextChainCounter = chainCounter + 1;
            chain.addMessageKey(nextChainCounter, messagesHmac);
            var keyHmac = Hmac.calculateSha256(new byte[]{2}, chainKey);
            chain.setKey(keyHmac);
            chain.setCounter(nextChainCounter);
        }
    }

    public void createOutgoing(SessionAddress address, int id, byte[] identityKey, SignalSignedKeyPair signedPreKey, SignalSignedKeyPair preKey) {
        if (!keys.hasTrust(address, identityKey)) {
            throw new IllegalArgumentException("Untrusted key");
        }
        if (!Curve25519.verifySignature(SignalConstants.createCurveKey(identityKey), signedPreKey.signalPublicKey(), signedPreKey.signature())) {
            throw new IllegalArgumentException("Signature mismatch");
        }
        var baseKey = SignalKeyPair.random();
        var state = createState(
                true,
                baseKey,
                null,
                identityKey,
                preKey == null ? null : preKey.signalPublicKey(),
                signedPreKey.signalPublicKey(),
                id,
                SignalConstants.CURRENT_VERSION
        );
        var pendingPreKey = new SessionPreKey(
                preKey == null ? null : preKey.id(),
                baseKey.signalPublicKey(),
                signedPreKey.id()
        );
        state.setPendingPreKey(pendingPreKey);
        var session = keys.findSessionByAddress(address)
                .orElse(null);
        if (session != null) {
            session.closeCurrentState();
            session.addState(state);
        } else {
            var newSession = new Session();
            keys.addSession(address, newSession);
            newSession.addState(state);
        }
    }

    private byte[][] computeMasterKey(boolean isInitiator, byte[] signedSecret, byte[] identitySecret, byte[] signedIdentitySecret, byte[] ephemeralSecret) {
        var result = new byte[KEY_LENGTH + signedSecret.length + identitySecret.length + signedIdentitySecret.length + (ephemeralSecret == null ? 0 : ephemeralSecret.length)];
        Arrays.fill(result, 0, KEY_LENGTH, (byte) 0xff);
        if (isInitiator) {
            System.arraycopy(signedSecret, 0, result, KEY_LENGTH, signedSecret.length);
            System.arraycopy(identitySecret, 0, result, KEY_LENGTH + signedSecret.length, identitySecret.length);
        } else {
            System.arraycopy(identitySecret, 0, result, KEY_LENGTH, identitySecret.length);
            System.arraycopy(signedSecret, 0, result, KEY_LENGTH + identitySecret.length, signedSecret.length);
        }
        System.arraycopy(signedIdentitySecret, 0, result, KEY_LENGTH + signedSecret.length + identitySecret.length, signedIdentitySecret.length);
        if (ephemeralSecret != null) {
            System.arraycopy(ephemeralSecret, 0, result, KEY_LENGTH + signedSecret.length + identitySecret.length + signedIdentitySecret.length, ephemeralSecret.length);
        }
        return Hkdf.deriveSecrets(result, "WhisperText".getBytes(StandardCharsets.UTF_8));
    }

    private SessionState calculateSendingRatchet(SessionState state, byte[] theirSignedPubKey) {
        var initSecret = Curve25519.sharedKey(SignalConstants.createCurveKey(theirSignedPubKey), state.ephemeralKeyPair().privateKey());
        var initKey = Hkdf.deriveSecrets(initSecret, state.rootKey(), "WhisperRatchet".getBytes(StandardCharsets.UTF_8));
        var key = state.ephemeralKeyPair().signalPublicKey();
        var chain = new SessionChain(-1, initKey[1]);
        state.addChain(key, chain);
        state.setRootKey(initKey[0]);
        return state;
    }

    public void createIncoming(SessionAddress address, Session session, SignalPreKeyMessage message) {
        if (!keys.hasTrust(address, message.identityKey())) {
            throw new IllegalArgumentException("Untrusted key");
        }
        if (session.hasState(message.version(), message.baseKey())) {
            return;
        }
        var preKeyPair = keys.findPreKeyById(message.preKeyId())
                .orElse(null);
        var signedKeyPair = keys.findSignedKeyPairById(message.signedPreKeyId())
                .orElseThrow(() -> new NoSuchElementException("Cannot find signed key with id %s".formatted(message.signedPreKeyId())));
        session.closeCurrentState();
        var nextState = createState(
                false,
                preKeyPair == null ? null : preKeyPair.toGenericKeyPair(),
                signedKeyPair.toGenericKeyPair(),
                message.identityKey(),
                message.baseKey(),
                null,
                message.registrationId(),
                message.version()
        );
        session.addState(nextState);
    }

    private SessionState createState(
            boolean isInitiator,
            SignalKeyPair ourEphemeralKey,
            SignalKeyPair ourSignedKey,
            byte[] theirIdentityPubKey, byte[] theirEphemeralPubKey, byte[] theirSignedPubKey,
            int registrationId, int version
    ) {
        if (isInitiator) {
            if (ourSignedKey != null) {
                throw new IllegalArgumentException("Our signed key should be null");
            }
            ourSignedKey = ourEphemeralKey;
        } else {
            if (theirSignedPubKey != null) {
                throw new IllegalArgumentException("Their signed public key should be null");
            }
            theirSignedPubKey = theirEphemeralPubKey;
        }
        var signedSecret = Curve25519.sharedKey(SignalConstants.createCurveKey(theirSignedPubKey), keys.identityKeyPair().privateKey());
        var identitySecret = Curve25519.sharedKey(SignalConstants.createCurveKey(theirIdentityPubKey), ourSignedKey.privateKey());
        var signedIdentitySecret = Curve25519.sharedKey(SignalConstants.createCurveKey(theirSignedPubKey), ourSignedKey.privateKey());
        var ephemeralSecret = theirEphemeralPubKey == null || ourEphemeralKey == null ? null : Curve25519.sharedKey(SignalConstants.createCurveKey(theirEphemeralPubKey), ourEphemeralKey.privateKey());
        var masterKey = computeMasterKey(isInitiator, signedSecret, identitySecret, signedIdentitySecret, ephemeralSecret);
        var state = new SessionState(
                version,
                registrationId,
                isInitiator ? ourEphemeralKey.signalPublicKey() : theirEphemeralPubKey,
                theirIdentityPubKey,
                new ConcurrentHashMap<>(),
                masterKey[0],
                null,
                isInitiator ? SignalKeyPair.random() : ourSignedKey,
                theirSignedPubKey,
                0,
                false
        );
        if (isInitiator) {
            return calculateSendingRatchet(state, theirSignedPubKey);
        } else {
            return state;
        }
    }

    public Result encrypt(SenderKeyName name, byte[] data) {
        try {
            var currentState = keys.findSenderKeyByName(name)
                    .orElseThrow(() -> new NoSuchElementException("Cannot find sender key for " + name))
                    .firstState()
                    .orElseThrow(() -> new NoSuchElementException("No sender key state for " + name));
            var messageKey = currentState.chainKey()
                    .toMessageKey();
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var keySpec = new SecretKeySpec(messageKey.cipherKey(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(messageKey.iv()));
            var ciphertext = cipher.doFinal(data);
            var senderKeyMessage = new SenderKeyMessage(
                    CURRENT_VERSION,
                    currentState.id(),
                    messageKey.iteration(),
                    ciphertext,
                    serialized -> Curve25519.sign(currentState.signingKey().privateKey(), serialized, null)
            );
            var next = currentState.chainKey().next();
            currentState.setChainKey(next);
            return new Result(senderKeyMessage.serialized(), SignalConstants.SKMSG);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot encrypt data", exception);
        }
    }

    public byte[] decrypt(SenderKeyName name, byte[] data) {
        var record = keys.findSenderKeyByName(name)
                .orElseThrow(() -> new NoSuchElementException("Cannot find sender key for " + name));
        var senderKeyMessage = SenderKeyMessage.ofSerialized(data);
        var senderKeyState = record.findStateById(senderKeyMessage.id())
                .orElseThrow(() -> new NoSuchElementException("Cannot find sender key state for " + name + " with id " + senderKeyMessage.id()));
        try {
            var senderKey = getSenderKey(senderKeyState, senderKeyMessage.iteration());
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var keySpec = new SecretKeySpec(senderKey.cipherKey(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(senderKey.iv()));
            return cipher.doFinal(senderKeyMessage.cipherText());
        } catch (Throwable throwable) {
            throw new RuntimeException("Cannot decrypt data for " + name +  " with id " + senderKeyMessage.id(), throwable);
        }
    }

    private SenderMessageKey getSenderKey(SenderKeyState senderKeyState, int iteration) {
        if (senderKeyState.chainKey().iteration() > iteration) {
            return senderKeyState.findSenderMessageKey(iteration)
                    .orElseThrow(() -> new NoSuchElementException("Received message with old counter: got %s, expected more than %s".formatted(iteration, senderKeyState.chainKey().iteration())));
        }
        var lastChainKey = senderKeyState.chainKey();
        while (lastChainKey.iteration() < iteration) {
            senderKeyState.addSenderMessageKey(lastChainKey.toMessageKey());
            lastChainKey = lastChainKey.next();
        }
        senderKeyState.setChainKey(lastChainKey.next());
        return lastChainKey.toMessageKey();
    }

    public byte[] createOutgoing(SenderKeyName name) {
        var record = keys.findSenderKeyByName(name).orElseGet(() -> {
            var newRecord = new SenderKeyRecord();
            keys.addSenderKey(name, newRecord);
            return newRecord;
        });
        var state = record.firstState().orElseGet(() -> record.addState(
                randomId(),
                SignalKeyPair.random(),
                0,
                Bytes.random(32)
        ));
        var message = new SignalDistributionMessage(
                CURRENT_VERSION,
                state.id(),
                state.chainKey().iteration(),
                state.chainKey().seed(),
                state.signingKey().signalPublicKey()
        );
        return message.serialized();
    }

    public void createIncoming(SenderKeyName name, SignalDistributionMessage message) {
        var record = keys.findSenderKeyByName(name).orElseGet(() -> {
            var newRecord = new SenderKeyRecord();
            keys.addSenderKey(name, newRecord);
            return newRecord;
        });
        record.addState(
                message.id(),
                message.signingKey(),
                message.iteration(),
                message.chainKey()
        );
    }

    private int randomId() {
        try {
            return SecureRandom.getInstanceStrong()
                    .nextInt();
        }catch (GeneralSecurityException exception) {
            return new SecureRandom()
                    .nextInt();
        }
    }

    public static final class Result {
        private final byte[] message;
        private final String type;

        Result(byte[] message, String type) {
            this.message = message;
            this.type = type;
        }

        public byte[] message() {
            return message;
        }

        public String type() {
            return type;
        }
    }
}