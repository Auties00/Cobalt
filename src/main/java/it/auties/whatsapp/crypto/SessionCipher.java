package it.auties.whatsapp.crypto;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.protobuf.signal.message.SignalMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalPreKeyMessage;
import it.auties.whatsapp.protobuf.signal.session.*;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import static java.util.Arrays.copyOfRange;
import static java.util.Objects.requireNonNull;

public record SessionCipher(@NonNull SessionAddress address, @NonNull WhatsappKeys keys) {
    @SneakyThrows
    public byte[] encrypt(byte[] data){
        var session = loadSession();
        Validate.isTrue(keys.hasTrust(address, session.state().remoteIdentityKey()),
                "Untrusted key", SecurityException.class);

        var chain = session.state()
                .findReceiverChain(session.state().senderChain().publicKey())
                .orElseThrow(() -> new NoSuchElementException("Missing chain for %s".formatted(address)));
        fillMessageKeys(chain, chain.key().index() + 1);

        var currentKeyIndex = chain.key().index();
        var currentKey = chain.messageKeys()
                .get(currentKeyIndex)
                .key();
        var whisperKeys = Hkdf.deriveSecrets(currentKey, "WhisperMessageKeys".getBytes(StandardCharsets.UTF_8));
        chain.messageKeys().remove(currentKeyIndex);

        var message = SignalMessage.builder()
                .ratchetKey(session.state().senderChain().publicKey())
                .counter(chain.key().index())
                .previousCounter(session.state().previousCounter())
                .ciphertext(AesCbc.cipher(whisperKeys[0], data, copyOfRange(whisperKeys[2], 0, 16), true))
                .build()
                .serialized();

        var macStream = new ByteArrayOutputStream();
        macStream.write(SignalHelper.appendKeyHeader(keys.identityKeyPair().publicKey()));
        macStream.write(SignalHelper.appendKeyHeader(session.state().remoteIdentityKey()));
        macStream.write(message);

        var mac = Hmac.calculate(macStream.toByteArray(), whisperKeys[1]);
        var result = BinaryArray.of(message)
                .append(mac.cut(8))
                .data();
       keys.addSession(address, session);
       if(session.state().pendingPreKey() == null){
           return result;
       }

       return SignalPreKeyMessage.builder()
               .identityKey(keys.identityKeyPair().publicKey())
               .registrationId(keys.id())
               .baseKey(session.state().pendingPreKey().baseKey())
               .signedPreKeyId(session.state().pendingPreKey().signedPreKeyId())
               .preKeyId(session.state().pendingPreKey().preKeyId())
               .serializedSignalMessage(result)
               .build()
               .serialized();
  }

    private void fillMessageKeys(SessionChain chain, int index) {
        if (chain.key().index() >= index) {
            return;
        }

        Validate.isTrue(index - chain.key().index() <= 2000,
                "Message overflow: expected <= 2000, got %s", index - chain.key().index());
        Validate.isTrue(chain.key().key() != null,
                "Closed chain");
        var messagesHmac = Hmac.calculate(new byte[]{1}, chain.key().key());
        chain.messageKeys().add(chain.key().index() + 1, new SessionChainKey(chain.key().index() + 1, messagesHmac.data()));
        var keyHmac = Hmac.calculate(new byte[]{2}, chain.key().key());
        chain.key().key(keyHmac.data());
        chain.key().index(chain.key().index() + 1);
        fillMessageKeys(chain, index);
    }

    public byte[] decrypt(SignalMessage message) {
        var session = loadSession();
        var result = decryptWithPreviousStates(message);
        Validate.isTrue(keys.hasTrust(address, result.state().remoteIdentityKey()),
                "Untrusted key");
        keys.addSession(address, session);
        return result.plaintext();
    }

    private DecryptResult decryptWithPreviousStates(SignalMessage message) {
        var session = loadSession();
        for(var state : session.previousStates()){
            try {
                Validate.isTrue(keys.hasTrust(address, state.remoteIdentityKey()),
                        "Untrusted key");
                var result = decrypt(message, state);
                return new DecryptResult(state, result);
            }catch (Throwable ignored){

            }
        }

        throw new NoSuchElementException("Cannot decrypt message using older states");
    }

    private record DecryptResult(SessionState state, byte[] plaintext) {

    }

    public byte[] decrypt(SignalPreKeyMessage message) {
        var session = loadSession(() -> {
            Validate.isTrue(message.registrationId() != 0, "Missing registration id");
            return new Session();
        });

        var builder = new SessionBuilder(address, keys);
        var preKeyId = builder.createIncoming(session, message);
        var state = session.findState(message.version(), message.baseKey())
                .orElseThrow(() -> new NoSuchElementException("Missing state"));
        var plaintext = decrypt(message.signalMessage(), state);
        keys.addSession(address, session);
        if(preKeyId != 0) {
            keys.preKeys().remove(preKeyId);
        }

        return plaintext;
    }

    @SneakyThrows
    private byte[] decrypt(SignalMessage message, SessionState state) {
        maybeStepRatchet(message, state);
        var chain = state.findReceiverChain(message.ratchetKey())
                .orElseThrow(() -> new NoSuchElementException("Invalid chain"));
        fillMessageKeys(chain, message.counter());
        Validate.isTrue(chain.hasMessageKey(message.counter()),
                "Key used already or never filled");
        var messageKey = chain.messageKeys().get(message.counter());
        chain.messageKeys().remove(messageKey);
        var secrets = Hkdf.deriveSecrets(messageKey.key(),
                "WhisperMessageKeys".getBytes(StandardCharsets.UTF_8));
        var macInput = new ByteArrayOutputStream();
        macInput.write(state.remoteIdentityKey());
        macInput.write(keys.identityKeyPair().publicKey());
        macInput.write(message.serialized());

        var plaintext = AesCbc.cipher(secrets[0], message.ciphertext(),
                copyOfRange(secrets[2], 0, 16), false);
        state.pendingPreKey(null);
        return plaintext;
    }

    private void maybeStepRatchet(SignalMessage message, SessionState state) {
        if (state.hasReceiverChain(message.ratchetKey())) {
            return;
        }

        var previousRatchet = state.findReceiverChain(state.localIdentityPublic());
        previousRatchet.ifPresent(chain -> {
            fillMessageKeys(chain, state.previousCounter());
            chain.key().key(null);
        });

        calculateRatchet(message, state, false);
        var previousCounter = state.findReceiverChain(state.senderChain().publicKey());
        previousCounter.ifPresent(chain -> {
            state.previousCounter(chain.key().index());
            state.receiverChains().remove(chain);
        });

        var keyPair = SignalKeyPair.random();
        state.senderChain()
                .publicKey(keyPair.publicKey())
                .privateKey(keyPair.privateKey());
        calculateRatchet(message, state, true);
        state.localIdentityPublic(message.ratchetKey());
    }

    private void calculateRatchet(SignalMessage message, SessionState state, boolean sending) {
        var sharedSecret = Curve.calculateSharedSecret(message.ratchetKey(),
                state.senderChain().privateKey());
        var masterKey = Hkdf.deriveSecrets(sharedSecret.data(), state.rootKey(),
                "WhisperRatchet".getBytes(StandardCharsets.UTF_8), 2);
        var chainKey = sending ? state.senderChain().publicKey() : message.ratchetKey();
        state.addReceiverChain(chainKey, new SessionChainKey(-1, masterKey[1]));
        state.rootKey(masterKey[0]);
    }

    private Session loadSession() {
        return loadSession(() -> null);
    }

    private Session loadSession(Supplier<Session> defaultSupplier) {
        return keys.findSessionByAddress(address)
                .orElseGet(() -> requireNonNull(defaultSupplier.get(), "Missing session for %s".formatted(address)));
    }
}