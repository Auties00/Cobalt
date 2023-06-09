package it.auties.whatsapp.crypto;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.message.SignalMessage;
import it.auties.whatsapp.model.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.model.signal.session.Session;
import it.auties.whatsapp.model.signal.session.SessionAddress;
import it.auties.whatsapp.model.signal.session.SessionChain;
import it.auties.whatsapp.model.signal.session.SessionState;
import it.auties.whatsapp.util.BytesHelper;
import it.auties.whatsapp.util.KeyHelper;
import it.auties.whatsapp.util.Spec.Signal;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

import static it.auties.curve25519.Curve25519.sharedKey;
import static it.auties.whatsapp.util.Spec.Signal.*;

public record SessionCipher(@NonNull SessionAddress address, @NonNull Keys keys) {
    public GroupCipher.CipheredMessageResult encrypt(byte[] data) {
        if(data == null){
            return new GroupCipher.CipheredMessageResult(null, Signal.UNAVAILABLE);
        }
        var currentState = loadSession().currentState()
                .orElseThrow(() -> new NoSuchElementException("Missing session for address %s".formatted(address)));
        Validate.isTrue(keys.hasTrust(address, currentState.remoteIdentityKey()), "Untrusted key", SecurityException.class);
        var chain = currentState.findChain(currentState.ephemeralKeyPair().encodedPublicKey())
                .orElseThrow(() -> new NoSuchElementException("Missing chain for %s".formatted(address)));
        fillMessageKeys(chain, chain.counter().get() + 1);
        var currentKey = chain.messageKeys().get(chain.counter().get());
        var secrets = Hkdf.deriveSecrets(currentKey, "WhisperMessageKeys".getBytes(StandardCharsets.UTF_8));
        chain.messageKeys().remove(chain.counter().get());
        var iv = Arrays.copyOf(secrets[2], IV_LENGTH);
        var encrypted = AesCbc.encrypt(iv, data, secrets[0]);
        var encryptedMessageType = getMessageType(currentState);
        var encryptedMessage = encrypt(currentState, chain, secrets[1], encrypted);
        return new GroupCipher.CipheredMessageResult(encryptedMessage, encryptedMessageType);
    }

    private String getMessageType(SessionState currentState) {
        return currentState.hasPreKey() ? Signal.PKMSG : Signal.MSG;
    }

    private byte[] encrypt(SessionState state, SessionChain chain, byte[] key, byte[] encrypted) {
        var message = new SignalMessage(state.ephemeralKeyPair().encodedPublicKey(), chain.counter()
                .get(), state.previousCounter(), encrypted, encodedMessage -> createMessageSignature(state, key, encodedMessage));
        var serializedMessage = message.serialized();
        if (!state.hasPreKey()) {
            return serializedMessage;
        }
        var preKeyMessage = new SignalPreKeyMessage(
                state.pendingPreKey().preKeyId(),
                state.pendingPreKey().baseKey(),
                keys.identityKeyPair().encodedPublicKey(),
                serializedMessage,
                keys.registrationId(),
                state.pendingPreKey().signedKeyId()
        );
        return preKeyMessage.serialized();
    }

    private byte[] createMessageSignature(SessionState state, byte[] key, byte[] encodedMessage) {
        var macInput = BytesHelper.concat(
                keys.identityKeyPair().encodedPublicKey(),
                state.remoteIdentityKey(),
                encodedMessage
        );
        var sha256 = Hmac.calculateSha256(macInput, key);
        return Arrays.copyOfRange(sha256, 0, MAC_LENGTH);
    }

    private void fillMessageKeys(SessionChain chain, int counter) {
        if (chain.counter().get() >= counter) {
            return;
        }
        Validate.isTrue(counter - chain.counter()
                .get() <= MAX_MESSAGES, "Message overflow: expected <= %s, got %s", MAX_MESSAGES, counter - chain.counter()
                .get());
        Validate.isTrue(chain.key().get() != null, "Closed chain");
        var messagesHmac = Hmac.calculateSha256(new byte[]{1}, chain.key().get());
        chain.messageKeys().put(chain.counter().get() + 1, messagesHmac);
        var keyHmac = Hmac.calculateSha256(new byte[]{2}, chain.key().get());
        chain.key().set(keyHmac);
        chain.counter().getAndIncrement();
        fillMessageKeys(chain, counter);
    }

    public byte[] decrypt(SignalPreKeyMessage message) {
        var session = loadSession(this::createSession);
        var builder = new SessionBuilder(address, keys);
        builder.createIncoming(session, message);
        var state = session.findState(message.version(), message.baseKey())
                .orElseThrow(() -> new NoSuchElementException("Missing state"));
        return decrypt(message.signalMessage(), state);
    }

    private Optional<Session> createSession() {
        var newSession = new Session();
        keys.putSession(address, newSession);
        return Optional.of(newSession);
    }

    public byte[] decrypt(SignalMessage message) {
        var session = loadSession();
        return session.states()
                .stream()
                .map(state -> tryDecrypt(message, state))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cannot decrypt message: no suitable session found"));
    }

    private Optional<byte[]> tryDecrypt(SignalMessage message, SessionState state) {
        try {
            Validate.isTrue(keys.hasTrust(address, state.remoteIdentityKey()), "Untrusted key");
            return Optional.of(decrypt(message, state));
        } catch (Throwable throwable) {
            return Optional.empty();
        }
    }

    private byte[] decrypt(SignalMessage message, SessionState state) {
        maybeStepRatchet(message, state);
        var chain = state.findChain(message.ephemeralPublicKey())
                .orElseThrow(() -> new NoSuchElementException("Invalid chain"));
        fillMessageKeys(chain, message.counter());
        Validate.isTrue(chain.hasMessageKey(message.counter()), "Key used already or never filled");
        var messageKey = chain.messageKeys().get(message.counter());
        var secrets = Hkdf.deriveSecrets(messageKey, "WhisperMessageKeys".getBytes(StandardCharsets.UTF_8));
        var hmacValue = BytesHelper.concat(
                state.remoteIdentityKey(),
                keys.identityKeyPair().encodedPublicKey(),
                message.serialized()
        );
        var hmacInput = Arrays.copyOfRange(hmacValue, 0, hmacValue.length - MAC_LENGTH);;
        var hmacSha256 = Hmac.calculateSha256(hmacInput, secrets[1]);
        var hmac = Arrays.copyOf(hmacSha256, MAC_LENGTH);
        Validate.isTrue(Arrays.equals(message.signature(), hmac), "message_decryption", HmacValidationException.class);
        var iv = Arrays.copyOf(secrets[2], IV_LENGTH);
        var plaintext = AesCbc.decrypt(iv, message.ciphertext(), secrets[0]);
        state.pendingPreKey(null);
        return plaintext;
    }

    private void maybeStepRatchet(SignalMessage message, SessionState state) {
        if (state.hasChain(message.ephemeralPublicKey())) {
            return;
        }
        var previousRatchet = state.findChain(state.lastRemoteEphemeralKey());
        previousRatchet.ifPresent(chain -> {
            fillMessageKeys(chain, state.previousCounter());
            chain.key().set(null);
        });
        calculateRatchet(message, state, false);
        var previousCounter = state.findChain(state.ephemeralKeyPair().encodedPublicKey());
        previousCounter.ifPresent(chain -> {
            state.previousCounter(chain.counter().get());
            state.removeChain(state.ephemeralKeyPair().encodedPublicKey());
        });
        state.ephemeralKeyPair(SignalKeyPair.random());
        calculateRatchet(message, state, true);
        state.lastRemoteEphemeralKey(message.ephemeralPublicKey());
    }

    private void calculateRatchet(SignalMessage message, SessionState state, boolean sending) {
        var sharedSecret = sharedKey(KeyHelper.withoutHeader(message.ephemeralPublicKey()), state.ephemeralKeyPair()
                .privateKey());
        var masterKey = Hkdf.deriveSecrets(sharedSecret, state.rootKey(), "WhisperRatchet".getBytes(StandardCharsets.UTF_8), 2);
        var chainKey = sending ? state.ephemeralKeyPair().encodedPublicKey() : message.ephemeralPublicKey();
        state.addChain(chainKey, new SessionChain(-1, masterKey[1]));
        state.rootKey(masterKey[0]);
    }

    private Session loadSession() {
        return loadSession(() -> keys.findSessionByAddress(new SessionAddress(address.name(), 0)));
    }

    private Session loadSession(Supplier<Optional<Session>> defaultSupplier) {
        return keys.findSessionByAddress(address)
                .or(defaultSupplier)
                .orElseThrow(() -> new NoSuchElementException("Missing session for: %s".formatted(address)));
    }
}