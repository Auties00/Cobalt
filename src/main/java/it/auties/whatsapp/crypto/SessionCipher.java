package it.auties.whatsapp.crypto;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.model.signal.message.SignalMessage;
import it.auties.whatsapp.model.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.model.signal.session.Session;
import it.auties.whatsapp.model.signal.session.SessionAddress;
import it.auties.whatsapp.model.signal.session.SessionChain;
import it.auties.whatsapp.model.signal.session.SessionState;
import it.auties.whatsapp.util.Keys;
import it.auties.whatsapp.util.SignalSpecification;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import static it.auties.curve25519.Curve25519.calculateAgreement;
import static it.auties.whatsapp.model.request.Node.with;
import static java.util.Map.of;
import static java.util.Objects.requireNonNull;

public record SessionCipher(@NonNull SessionAddress address, @NonNull WhatsappKeys keys) implements SignalSpecification {
    public Node encrypt(byte @NonNull [] data){
        var session = loadSession();
        Validate.isTrue(keys.hasTrust(address, session.currentState().remoteIdentityKey()),
                "Untrusted key", SecurityException.class);

        var currentState = session.currentState();
        var chain = currentState.findChain(currentState.ephemeralKeyPair().encodedPublicKey())
                .orElseThrow(() -> new NoSuchElementException("Missing chain for %s".formatted(address)));
        fillMessageKeys(chain, chain.counter() + 1);

        var currentKey = chain.messageKeys()
                .get(chain.counter())
                .publicKey();
        var whisperKeys = Hkdf.deriveSecrets(currentKey,
                "WhisperMessageKeys".getBytes(StandardCharsets.UTF_8));
        chain.messageKeys().remove(chain.counter());

        var encryptedIv = Bytes.of(whisperKeys[2])
                .cut(IV_LENGTH)
                .toByteArray();
        var encrypted = AesCbc.encrypt(encryptedIv, data, whisperKeys[0]);
        var encryptedMessage = encrypt(session, chain, whisperKeys, encrypted);
        return with("enc",
                of("v", "2", "type", hasPreKey(session) ? "pkmsg" : "msg"), encryptedMessage);
    }

    private byte[] encrypt(Session session, SessionChain chain, byte[][] whisperKeys, byte[] encrypted) {
        var ephemeralKey = session.currentState()
                .ephemeralKeyPair()
                .encodedPublicKey();
        var message = new SignalMessage(ephemeralKey, chain.counter(), session.currentState().previousCounter(),
                encrypted, encodedMessage -> createMessageSignature(session, whisperKeys, encodedMessage));
        keys.addSession(address, session);
        return hasPreKey(session) ? createPreKeyMessage(session, message)
                : message.serialized();
    }

    private byte[] createPreKeyMessage(Session session, SignalMessage message) {
        return SignalPreKeyMessage.builder()
                .identityKey(keys.identityKeyPair().encodedPublicKey())
                .registrationId(keys.id())
                .baseKey(session.currentState().pendingPreKey().baseKey())
                .signedPreKeyId(session.currentState().pendingPreKey().signedPreKeyId())
                .preKeyId(session.currentState().pendingPreKey().preKeyId())
                .serializedSignalMessage(message.serialized())
                .build()
                .serialized();
    }

    private byte[] createMessageSignature(Session session, byte[][] whisperKeys, byte[] encodedMessage) {
        var macInput = Bytes.of(keys.identityKeyPair().encodedPublicKey())
                .append(session.currentState().remoteIdentityKey())
                .append(encodedMessage)
                .assertSize(encodedMessage.length + 33 + 33)
                .toByteArray();
        return Bytes.of(Hmac.calculateSha256(macInput, whisperKeys[1]))
                .cut(8)
                .toByteArray();
    }

    private boolean hasPreKey(Session session) {
        return session.currentState().pendingPreKey() != null;
    }

    private void fillMessageKeys(SessionChain chain, int counter) {
        if (chain.counter() >= counter) {
            return;
        }

        Validate.isTrue(counter - chain.counter() <= 2000,
                "Message overflow: expected <= 2000, got %s", counter - chain.counter());
        Validate.isTrue(chain.key() != null,
                "Closed chain");

        var messagesHmac = Hmac.calculateSha256(new byte[]{1}, chain.key());
        var keyPair = new SignalPreKeyPair(chain.counter() + 1, messagesHmac, null);
        chain.messageKeys().put(chain.counter() + 1, keyPair);

        var keyHmac = Hmac.calculateSha256(new byte[]{2}, chain.key());
        chain.key(keyHmac);
        chain.incrementCounter();
        fillMessageKeys(chain, counter);
    }

    public byte[] decrypt(SignalPreKeyMessage message) {
        var session = loadSession(() -> {
            Validate.isTrue(message.registrationId() != 0, "Missing registration jid");
            return new Session();
        });

        var builder = new SessionBuilder(address, keys);
        builder.createIncoming(session, message);
        var state = session.findState(message.version(), message.baseKey())
                .orElseThrow(() -> new NoSuchElementException("Missing state"));
        var plaintext = decrypt(message.signalMessage(), state);
        keys.addSession(address, session);
        return plaintext;
    }

    public byte[] decrypt(SignalMessage message) {
        var session = loadSession();
        for(var state : session.states()){
            try {
                Validate.isTrue(keys.hasTrust(address, state.remoteIdentityKey()),
                        "Untrusted key");
                var result = decrypt(message, state);
                keys.addSession(address, session);
                return result;
            }catch (Throwable ignored){

            }
        }

        throw new RuntimeException("Cannot decrypt message");
    }

    @SneakyThrows
    private byte[] decrypt(SignalMessage message, SessionState state) {
        maybeStepRatchet(message, state);

        var chain = state.findChain(message.ephemeralPublicKey())
                .orElseThrow(() -> new NoSuchElementException("Invalid chain"));
        fillMessageKeys(chain, message.counter());

        Validate.isTrue(chain.hasMessageKey(message.counter()),
                "Key used already or never filled");
        var messageKey = chain.messageKeys().get(message.counter());
        chain.messageKeys().remove(message.counter());

        var secrets = Hkdf.deriveSecrets(messageKey.publicKey(),
                "WhisperMessageKeys".getBytes(StandardCharsets.UTF_8));

        var hmacInput = Bytes.of(state.remoteIdentityKey())
                .append(keys.identityKeyPair().encodedPublicKey())
                .append(message.serialized())
                .cut(-SignalMessage.MAC_LENGTH)
                .toByteArray();
        var hmac = Bytes.of(Hmac.calculateSha256(hmacInput, secrets[1]))
                .cut(SignalMessage.MAC_LENGTH)
                .toByteArray();
        Validate.isTrue(Arrays.equals(message.signature(), hmac),
                "Cannot decode message: Hmac validation failed", SecurityException.class);

        var iv = Bytes.of(secrets[2])
                .cut(IV_LENGTH)
                .toByteArray();
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
            chain.key(null);
        });

        calculateRatchet(message, state, false);
        var previousCounter = state.findChain(state.ephemeralKeyPair().encodedPublicKey());
        previousCounter.ifPresent(chain -> {
            state.previousCounter(chain.counter());
            state.chains().remove(chain);
        });

        state.ephemeralKeyPair(SignalKeyPair.random());
        calculateRatchet(message, state, true);
        state.lastRemoteEphemeralKey(message.ephemeralPublicKey());
    }

    private void calculateRatchet(SignalMessage message, SessionState state, boolean sending) {
        var sharedSecret = calculateAgreement(Keys.withoutHeader(message.ephemeralPublicKey()),
                state.ephemeralKeyPair().privateKey());
        var masterKey = Hkdf.deriveSecrets(sharedSecret, state.rootKey(),
                "WhisperRatchet".getBytes(StandardCharsets.UTF_8), 2);
        var chainKey = sending ? state.ephemeralKeyPair().encodedPublicKey() : message.ephemeralPublicKey();
        state.addChain(new SessionChain(-1, masterKey[1], chainKey));
        state.rootKey(masterKey[0]);
    }

    private Session loadSession() {
        return loadSession(() -> null);
    }

    private Session loadSession(Supplier<Session> defaultSupplier) {
        return keys.findSessionByAddress(address)
                .orElseGet(() -> requireNonNull(defaultSupplier.get(), "Missing session for %s. Known sessions: %s".formatted(address, keys.sessions())));
    }
}