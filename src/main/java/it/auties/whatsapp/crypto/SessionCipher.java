package it.auties.whatsapp.crypto;

import it.auties.curve25519.Curve25519;
import it.auties.protobuf.stream.ProtobufOutputStream;
import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.message.SignalMessage;
import it.auties.whatsapp.model.signal.message.SignalMessageSpec;
import it.auties.whatsapp.model.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.model.signal.session.Session;
import it.auties.whatsapp.model.signal.session.SessionAddress;
import it.auties.whatsapp.model.signal.session.SessionChain;
import it.auties.whatsapp.model.signal.session.SessionState;
import it.auties.whatsapp.util.SignalConstants;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;

import static it.auties.whatsapp.util.SignalConstants.*;

public record SessionCipher(SessionAddress address, Keys keys) {
    public CipheredMessageResult encrypt(byte[] data) {
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
                return new CipheredMessageResult(
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
                return new CipheredMessageResult(
                        preKeyMessage.serialized(),
                        currentState.hasPreKey() ? PKMSG : MSG
                );
            }
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot encrypt data", exception);
        }
    }

    public byte[] decrypt(SignalPreKeyMessage message) {
        var session = keys.findSessionByAddress(address).orElseGet(() -> {
            var newSession = new Session();
            keys.addSession(address, newSession);
            return newSession;
        });
        var builder = new SessionBuilder(address, keys);
        builder.createIncoming(session, message);
        var state = session.findState(message.version(), message.baseKey())
                .orElseThrow(() -> new NoSuchElementException("Missing state"));
        return decrypt(message.signalMessage(), state);
    }

    // FIXME: Is this the best way to do this?
    public byte[] decrypt(SignalMessage message) {
        return keys.findSessionByAddress(address)
                .orElseThrow(() -> new NoSuchElementException("Missing session for " + address))
                .states()
                .stream()
                .map(state -> {
                    try {
                        if (!keys.hasTrust(address, state.remoteIdentityKey())) {
                            throw new IllegalArgumentException("Untrusted key");
                        }
                        return Optional.of(decrypt(message, state));
                    } catch (Throwable throwable) {
                        return Optional.<byte[]>empty();
                    }
                })
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot decrypt message: no suitable session found"));
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
            if(!Arrays.equals(expectedSignature, 0, MAC_LENGTH, actualSignature, 0, MAC_LENGTH)) {
                throw new HmacValidationException("message_decryption");
            }
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var keySpec = new SecretKeySpec(secrets[0], "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(secrets[2], 0, IV_LENGTH));
            var plaintext = cipher.doFinal(message.ciphertext());
            state.pendingPreKey(null);
            return plaintext;
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot decrypt data", exception);
        }
    }

    private byte[] computeDecryptionMac(SignalMessage message, byte[] signalRemoteIdentityKey, byte[] signalIdentityPublicKey, byte[] hmacKey) {
        if(signalRemoteIdentityKey == null || signalRemoteIdentityKey.length != KEY_LENGTH + 1 || signalRemoteIdentityKey[0] != KEY_TYPE) {
            throw new IllegalArgumentException("Invalid signalRemoteIdentityKey");
        }

        if(signalIdentityPublicKey == null || signalIdentityPublicKey.length != KEY_LENGTH + 1 || signalIdentityPublicKey[0] != KEY_TYPE) {
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
            state.previousCounter(chain.counter());
            state.removeChain(state.ephemeralKeyPair().signalPublicKey());
        });
        state.ephemeralKeyPair(SignalKeyPair.random());
        calculateRatchet(message, state, true);
        state.lastRemoteEphemeralKey(message.ephemeralPublicKey());
    }

    private void calculateRatchet(SignalMessage message, SessionState state, boolean sending) {
        var sharedSecret = Curve25519.sharedKey(SignalConstants.createCurveKey(message.ephemeralPublicKey()), state.ephemeralKeyPair().privateKey());
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
}