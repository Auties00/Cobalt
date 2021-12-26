package it.auties.whatsapp.crypto;

import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.protobuf.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.protobuf.signal.message.PreKeySignalMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalProtocolMessage;
import it.auties.whatsapp.protobuf.signal.session.*;
import it.auties.whatsapp.util.Sessions;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;
import lombok.SneakyThrows;

import javax.crypto.Cipher;

public record SignalSession(@NonNull ProtocolAddress address, @NonNull WhatsappKeys keys) {
    private static final int MAX_MESSAGE_KEYS = 2000;
    private static final String AES = "AES/CBC/PKCS5Padding";

    public SignalProtocolMessage cipher(byte[] paddedMessage) {
        var sessionRecord = keys.findSessionByAddress(address);
        var sessionState = sessionRecord.currentSession();
        var chainKey = sessionState.senderChain().chainKey();
        var messageKeys = chainKey.messageKeys();
        var senderEphemeral = sessionState.publicSenderRatchetKey();
        var previousCounter = sessionState.previousCounter();
        var sessionVersion = sessionState.sessionVersion();

        var ciphertextBody = cipher(messageKeys, paddedMessage, true);
        var ciphertextMessage = getCipherTextMessage(sessionState, chainKey, messageKeys, senderEphemeral, previousCounter, sessionVersion, ciphertextBody);

        sessionState.senderChain().chainKey(chainKey.nextChainKey());
        Validate.isTrue(keys.hasTrust(address, sessionState.remoteIdentityKey()), "Untrusted key");
        keys.identities().put(address, sessionState.remoteIdentityKey());
        keys.sessions().put(address, sessionRecord);
        return ciphertextMessage;
    }

    private SignalProtocolMessage getCipherTextMessage(SessionStructure sessionState, ChainKey chainKey, MessageKeys messageKeys, byte[] senderEphemeral, int previousCounter, int sessionVersion, byte[] ciphertextBody) {
        var ciphertextMessage = new SignalMessage(sessionVersion, messageKeys.macKey(), senderEphemeral, chainKey.index(), previousCounter, ciphertextBody, sessionState.localIdentityPublic(), sessionState.remoteIdentityKey());
        if (!sessionState.hasUnacknowledgedPreKeyMessage()) {
            return ciphertextMessage;
        }

        var items = sessionState.unacknowledgedPreKeyMessageItems();
        var localRegistrationId = sessionState.localRegistrationId();
        return new PreKeySignalMessage(sessionVersion, localRegistrationId, items.preKeyId(), items.signedPreKeyId(), items.baseKey(), sessionState.localIdentityPublic(), ciphertextMessage);
    }


    private int process(SessionRecord sessionRecord, PreKeySignalMessage message) {
        var theirIdentityKey = message.identityKey();
        Validate.isTrue(keys.hasTrust(address, theirIdentityKey), "Untrusted key");
        var unsignedPreKeyId = processV3(sessionRecord, message);
        keys.identities().put(address, theirIdentityKey);
        return unsignedPreKeyId;
    }

    private int processV3(SessionRecord sessionRecord, PreKeySignalMessage message) {
        if (sessionRecord.hasSessionState(message.signalMessage().messageVersion(), message.baseKey())) {
            return -1;
        }

        var ourSignedPreKey = keys.findSignedIdentityByAddress(message.signedPreKeyId());
        var parameters = new BobSignalProtocolParameters()
                .theirBaseKey(message.baseKey())
                .theirIdentityKey(message.identityKey())
                .ourIdentityKey(keys.identityKeyPair())
                .ourSignedPreKey(ourSignedPreKey)
                .ourRatchetKey(ourSignedPreKey);
        if(message.preKeyId() != 0){
            parameters.ourOneTimePreKey(keys.findSignedIdentityByAddress(message.preKeyId()));
        }

        if (!sessionRecord.fresh()) {
            sessionRecord.archiveCurrentState();
        }

        Sessions.initializeSession(sessionRecord.currentSession(), parameters);

        sessionRecord.currentSession().localRegistrationId(keys.id());
        sessionRecord.currentSession().remoteRegistrationId(message.registrationId());
        sessionRecord.currentSession().aliceBaseKey(message.baseKey());
        return message.preKeyId() != 0 ? message.preKeyId() : -1;
    }

    public void process(PreKeyBundle preKey)  {
        Validate.isTrue(keys.hasTrust(address, preKey.identityKey().publicKey()), "Untrusted key");
        Validate.isTrue(preKey.signedPreKeyPublic() == null
                        || Curve.verifySignature(preKey.identityKey().publicKey(), preKey.signedPreKeyPublic(), preKey.signedPreKeySignature()),
                "No signed pre key");

        var sessionRecord = keys.findSessionByAddress(address);
        var ourBaseKey = SignalKeyPair.random();
        var theirSignedPreKey = preKey.signedPreKeyPublic();
        var theirOneTimePreKeyId = preKey.preKeyPublic() != null ? preKey.preKeyId() : 0;
        var parameters = new AliceSignalProtocolParameters()
                .ourBaseKey(ourBaseKey)
                .ourIdentityKey(keys.identityKeyPair())
                .theirIdentityKey(preKey.identityKey().publicKey())
                .theirSignedPreKey(theirSignedPreKey)
                .theirRatchetKey(theirSignedPreKey)
                .theirOneTimePreKey(preKey.preKeyPublic());

        if (!sessionRecord.fresh()) {
            sessionRecord.archiveCurrentState();
        }

        Sessions.initializeSession(sessionRecord.currentSession(), parameters);

        sessionRecord.currentSession().unacknowledgedPreKeyMessage(theirOneTimePreKeyId, preKey.signedPreKeyId(), ourBaseKey.publicKey());
        sessionRecord.currentSession().localRegistrationId(keys.id());
        sessionRecord.currentSession().remoteRegistrationId(preKey.registrationId());
        sessionRecord.currentSession().aliceBaseKey(ourBaseKey.publicKey());

        keys.identities().put(address, preKey.identityKey().publicKey());
        keys.sessions().put(address, sessionRecord);
    }


    public byte[] decipher(PreKeySignalMessage ciphertext) {
        var sessionRecord = keys.findSessionByAddress(address);
        var unsignedPreKeyId = process(sessionRecord, ciphertext);
        var plaintext = decryptRecursively(sessionRecord, ciphertext.signalMessage());
        keys.sessions().put(address, sessionRecord);
        if (unsignedPreKeyId != 0) {
            keys.signedIdentities().remove(unsignedPreKeyId);
        }

        return plaintext;
    }

    public byte[] decipher(SignalMessage ciphertext) {
        var sessionRecord = keys.findSessionByAddress(address);
        var plaintext = decryptRecursively(sessionRecord, ciphertext);
        Validate.isTrue(keys.hasTrust(address, sessionRecord.currentSession().remoteIdentityKey()), "Untrusted key");
        keys.identities().put(address, sessionRecord.currentSession().remoteIdentityKey());
        keys.sessions().put(address, sessionRecord);
        return plaintext;
    }

    private byte[] decryptRecursively(SessionRecord sessionRecord, SignalMessage ciphertext) {
        try {
            var sessionState = new SessionRecord(sessionRecord.currentSession());
            var plaintext = decryptMessage(sessionState, ciphertext);
            sessionRecord.currentSession(sessionState.currentSession());
            return plaintext;
        } catch (Exception ignored) {

        }

        var previousStates = sessionRecord.previousSessions().iterator();
        while (previousStates.hasNext()) {
            try {
                var promotedState = new SessionRecord(previousStates.next());
                var plaintext = decryptMessage(promotedState, ciphertext);
                previousStates.remove();
                sessionRecord.promoteState(promotedState.currentSession());
                return plaintext;
            } catch (Exception ignored) {

            }
        }

        throw new RuntimeException("No valid sessions");
    }

    private byte[] decryptMessage(SessionRecord sessionState, SignalMessage ciphertextMessage) {
        var theirEphemeral = ciphertextMessage.ratchetKey();
        var counter = ciphertextMessage.counter();
        var chainKey = getOrCreateChainKey(sessionState, theirEphemeral);
        var messageKeys = getOrCreateMessageKeys(sessionState, theirEphemeral, chainKey, counter);
        ciphertextMessage.verifyMac(sessionState.currentSession().remoteIdentityKey(), sessionState.currentSession().localIdentityPublic(), messageKeys.macKey());
        var plaintext = cipher(messageKeys, ciphertextMessage.ciphertext(), false);
        sessionState.currentSession().pendingPreKey(null);
        return plaintext;
    }

    private ChainKey getOrCreateChainKey(SessionRecord sessionState, byte[] theirEphemeral) {
        var chainKey = sessionState.currentSession().receiverChainKey(theirEphemeral);
        if (chainKey.isPresent()) {
            return chainKey.get();
        }

        var rootKey = sessionState.currentSession().rootKey();
        var ourEphemeral = sessionState.currentSession().senderRatchetKeyPair();
        var receiverChain = new RootKey(rootKey).createChain(theirEphemeral, ourEphemeral);
        var ourNewEphemeral = SignalKeyPair.random();
        var senderChain = receiverChain.rootKey().createChain(theirEphemeral, ourNewEphemeral);

        sessionState.currentSession().rootKey(senderChain.rootKey().key());
        sessionState.currentSession().addReceiverChain(theirEphemeral, receiverChain.chainKey());
        sessionState.currentSession().previousCounter(Math.max(sessionState.currentSession().senderChain().chainKey().index() - 1, 0));
        sessionState.currentSession().senderChain(ourNewEphemeral, senderChain.chainKey());

        return receiverChain.chainKey();
    }

    private MessageKeys getOrCreateMessageKeys(SessionRecord sessionState, byte[] theirEphemeral, ChainKey chainKey, int counter) {
        if (chainKey.index() > counter) {
            Validate.isTrue(sessionState.currentSession().hasMessageKeys(theirEphemeral, counter), "Received message with old counter: %s,%s".formatted(chainKey.index(), counter));
            return sessionState.currentSession().removeMessageKeys(theirEphemeral, counter)
                    .orElseThrow(() -> new RuntimeException("Cannot create MessageKeys"));
        }

        Validate.isTrue(counter - chainKey.index() <= MAX_MESSAGE_KEYS, "Message overflow");
        while (chainKey.index() < counter) {
            var messageKeys = chainKey.messageKeys();
            sessionState.currentSession().messageKeys(theirEphemeral, messageKeys);
            chainKey = chainKey.nextChainKey();
        }

        sessionState.currentSession().receiverChainKey(theirEphemeral, chainKey.nextChainKey());
        return chainKey.messageKeys();
    }

    @SneakyThrows
    private byte[] cipher(MessageKeys messageKeys, byte[] plaintext, boolean encrypt) {
        var cipher = Cipher.getInstance(AES);
        cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, messageKeys.cipherKey(), messageKeys.iv());
        return cipher.doFinal(plaintext);
    }
}