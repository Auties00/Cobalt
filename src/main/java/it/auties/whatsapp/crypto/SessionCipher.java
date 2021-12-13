package it.auties.whatsapp.crypto;

import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.protobuf.signal.message.PreKeySignalMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalMessage;
import it.auties.whatsapp.protobuf.signal.message.SignalProtocolMessage;
import it.auties.whatsapp.protobuf.signal.session.*;
import it.auties.whatsapp.util.Validate;
import lombok.NonNull;
import org.whispersystems.libsignal.ratchet.RatchetingSession;

public record SignalSession(@NonNull ProtocolAddress address, @NonNull WhatsappKeys keys) {
    public SignalProtocolMessage cipher(byte[] paddedMessage) {
        var sessionRecord = keys.sessions().get(address);
        var sessionState = sessionRecord.currentSession();
        var chainKey = sessionState.senderChain().chainKey();
        var messageKeys = chainKey.messageKeys();
        var senderEphemeral = sessionState.publicSenderRatchetKey();
        var previousCounter = sessionState.previousCounter();
        var sessionVersion = sessionState.sessionVersion();

        var ciphertextBody = getCiphertext(messageKeys, paddedMessage);
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

        var ourSignedPreKey = keys.signedIdentities().get(message.signedPreKeyId());

        var parameters= new BobSignalProtocolParameters()
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

        org.whispersystems.libsignal.ratchet.BobSignalProtocolParameters
        RatchetingSession.initializeSession(sessionRecord.getSessionState(), parameters.create());

        sessionRecord.getSessionState().setLocalRegistrationId(identityKeyStore.getLocalRegistrationId());
        sessionRecord.getSessionState().setRemoteRegistrationId(message.getRegistrationId());
        sessionRecord.getSessionState().setAliceBaseKey(message.getBaseKey().serialize());

        if (message.getPreKeyId().isPresent()) {
            return message.getPreKeyId();
        } else {
            return Optional.absent();
        }
    }

    public void process(PreKeyBundle preKey) throws InvalidKeyException, UntrustedIdentityException {
            if (!identityKeyStore.isTrustedIdentity(remoteAddress, preKey.getIdentityKey(), IdentityKeyStore.Direction.SENDING)) {
                throw new UntrustedIdentityException(remoteAddress.getName(), preKey.getIdentityKey());
            }

            if (preKey.getSignedPreKey() != null &&
                    !org.whispersystems.libsignal.ecc.Curve.verifySignature(preKey.getIdentityKey().getPublicKey(),
                            preKey.getSignedPreKey().serialize(),
                            preKey.getSignedPreKeySignature()))
            {
                throw new InvalidKeyException("Invalid signature on device key!");
            }

            if (preKey.getSignedPreKey() == null) {
                throw new InvalidKeyException("No signed prekey!");
            }

            SessionRecord         sessionRecord        = sessionStore.loadSession(remoteAddress);
            ECKeyPair             ourBaseKey           = org.whispersystems.libsignal.ecc.Curve.generateKeyPair();
            ECPublicKey theirSignedPreKey    = preKey.getSignedPreKey();
            Optional<ECPublicKey> theirOneTimePreKey   = Optional.fromNullable(preKey.getPreKey());
            Optional<Integer>     theirOneTimePreKeyId = theirOneTimePreKey.isPresent() ? Optional.of(preKey.getPreKeyId()) :
                    Optional.<Integer>absent();

            AliceSignalProtocolParameters.Builder parameters = AliceSignalProtocolParameters.newBuilder();

            parameters.setOurBaseKey(ourBaseKey)
                    .setOurIdentityKey(identityKeyStore.getIdentityKeyPair())
                    .setTheirIdentityKey(preKey.getIdentityKey())
                    .setTheirSignedPreKey(theirSignedPreKey)
                    .setTheirRatchetKey(theirSignedPreKey)
                    .setTheirOneTimePreKey(theirOneTimePreKey);

            if (!sessionRecord.isFresh()) sessionRecord.archiveCurrentState();

            RatchetingSession.initializeSession(sessionRecord.getSessionState(), parameters.create());

            sessionRecord.getSessionState().setUnacknowledgedPreKeyMessage(theirOneTimePreKeyId, preKey.getSignedPreKeyId(), ourBaseKey.getPublicKey());
            sessionRecord.getSessionState().setLocalRegistrationId(identityKeyStore.getLocalRegistrationId());
            sessionRecord.getSessionState().setRemoteRegistrationId(preKey.getRegistrationId());
            sessionRecord.getSessionState().setAliceBaseKey(ourBaseKey.getPublicKey().serialize());

            identityKeyStore.saveIdentity(remoteAddress, preKey.getIdentityKey());
            sessionStore.storeSession(remoteAddress, sessionRecord);
        }


        public byte[] decrypt(PreKeySignalMessage ciphertext) {
        var sessionRecord = keys.sessions().get(address);
        var unsignedPreKeyId = sessionBuilder.process(sessionRecord, ciphertext);
        var plaintext = decrypt(sessionRecord, ciphertext.getWhisperMessage());

        sessionStore.storeSession(remoteAddress, sessionRecord);

        if (unsignedPreKeyId.isPresent()) {
            preKeyStore.removePreKey(unsignedPreKeyId.get());
        }

        return plaintext;
    }


    public byte[] decrypt(SignalMessage ciphertext) {
        if (!sessionStore.containsSession(remoteAddress)) {
            throw new NoSessionException("No session for: " + remoteAddress);
        }

        SessionRecord sessionRecord = sessionStore.loadSession(remoteAddress);
        byte[] plaintext = decrypt(sessionRecord, ciphertext);

        if (!identityKeyStore.isTrustedIdentity(remoteAddress, sessionRecord.getSessionState().getRemoteIdentityKey(), IdentityKeyStore.Direction.RECEIVING)) {
            throw new UntrustedIdentityException(remoteAddress.getName(), sessionRecord.getSessionState().getRemoteIdentityKey());
        }

        identityKeyStore.saveIdentity(remoteAddress, sessionRecord.getSessionState().getRemoteIdentityKey());

        callback.handlePlaintext(plaintext);

        sessionStore.storeSession(remoteAddress, sessionRecord);

        return plaintext;
    }

    private byte[] decrypt(SessionRecord sessionRecord, SignalMessage ciphertext) {
        Iterator<SessionState> previousStates = sessionRecord.getPreviousSessionStates().iterator();
        List<Exception> exceptions = new LinkedList<>();

        try {
            SessionState sessionState = new SessionState(sessionRecord.getSessionState());
            byte[] plaintext = decrypt(sessionState, ciphertext);

            sessionRecord.setState(sessionState);
            return plaintext;
        } catch (InvalidMessageException e) {
            exceptions.add(e);
        }

        while (previousStates.hasNext()) {
            try {
                SessionState promotedState = new SessionState(previousStates.next());
                byte[] plaintext = decrypt(promotedState, ciphertext);

                previousStates.remove();
                sessionRecord.promoteState(promotedState);

                return plaintext;
            } catch (InvalidMessageException e) {
                exceptions.add(e);
            }
        }

        throw new InvalidMessageException("No valid sessions.", exceptions);
    }

    private byte[] decrypt(SessionState sessionState, SignalMessage ciphertextMessage) {
        if (!sessionState.hasSenderChain()) {
            throw new InvalidMessageException("Uninitialized session!");
        }

        if (ciphertextMessage.getMessageVersion() != sessionState.getSessionVersion()) {
            throw new InvalidMessageException(String.format("Message version %d, but session version %d", ciphertextMessage.getMessageVersion(), sessionState.getSessionVersion()));
        }

        ECPublicKey theirEphemeral = ciphertextMessage.getSenderRatchetKey();
        int counter = ciphertextMessage.getCounter();
        ChainKey chainKey = getOrCreateChainKey(sessionState, theirEphemeral);
        MessageKeys messageKeys = getOrCreateMessageKeys(sessionState, theirEphemeral, chainKey, counter);

        ciphertextMessage.verifyMac(sessionState.getRemoteIdentityKey(), sessionState.getLocalIdentityKey(), messageKeys.getMacKey());

        byte[] plaintext = getPlaintext(messageKeys, ciphertextMessage.getBody());

        sessionState.clearUnacknowledgedPreKeyMessage();

        return plaintext;
    }

    public int getRemoteRegistrationId() {
        synchronized (SESSION_LOCK) {
            SessionRecord record = sessionStore.loadSession(remoteAddress);
            return record.getSessionState().getRemoteRegistrationId();
        }
    }

    public int getSessionVersion() {
        if (!sessionStore.containsSession(remoteAddress)) {
            throw new IllegalStateException(String.format("No session for (%s)!", remoteAddress));
        }

        SessionRecord record = sessionStore.loadSession(remoteAddress);
        return record.getSessionState().getSessionVersion();
    }

    private ChainKey getOrCreateChainKey(SessionState sessionState, ECPublicKey theirEphemeral) throws InvalidMessageException {
        try {
            if (sessionState.hasReceiverChain(theirEphemeral)) {
                return sessionState.getReceiverChainKey(theirEphemeral);
            } else {
                RootKey rootKey = sessionState.getRootKey();
                ECKeyPair ourEphemeral = sessionState.getSenderRatchetKeyPair();
                Pair<RootKey, ChainKey> receiverChain = rootKey.createChain(theirEphemeral, ourEphemeral);
                ECKeyPair ourNewEphemeral = Curve.generateKeyPair();
                Pair<RootKey, ChainKey> senderChain = receiverChain.first().createChain(theirEphemeral, ourNewEphemeral);

                sessionState.setRootKey(senderChain.first());
                sessionState.addReceiverChain(theirEphemeral, receiverChain.second());
                sessionState.setPreviousCounter(Math.max(sessionState.getSenderChainKey().getIndex() - 1, 0));
                sessionState.setSenderChain(ourNewEphemeral, senderChain.second());

                return receiverChain.second();
            }
        } catch (InvalidKeyException e) {
            throw new InvalidMessageException(e);
        }
    }

    private MessageKeys getOrCreateMessageKeys(SessionState sessionState, ECPublicKey theirEphemeral, ChainKey chainKey, int counter) throws InvalidMessageException, DuplicateMessageException {
        if (chainKey.getIndex() > counter) {
            if (sessionState.hasMessageKeys(theirEphemeral, counter)) {
                return sessionState.removeMessageKeys(theirEphemeral, counter);
            } else {
                throw new DuplicateMessageException("Received message with old counter: " + chainKey.getIndex() + " , " + counter);
            }
        }

        if (counter - chainKey.getIndex() > 2000) {
            throw new InvalidMessageException("Over 2000 messages into the future!");
        }

        while (chainKey.getIndex() < counter) {
            MessageKeys messageKeys = chainKey.getMessageKeys();
            sessionState.setMessageKeys(theirEphemeral, messageKeys);
            chainKey = chainKey.getNextChainKey();
        }

        sessionState.setReceiverChainKey(theirEphemeral, chainKey.getNextChainKey());
        return chainKey.getMessageKeys();
    }

    private byte[] getCiphertext(MessageKeys messageKeys, byte[] plaintext) {
        try {
            Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, messageKeys.getCipherKey(), messageKeys.getIv());
            return cipher.doFinal(plaintext);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new AssertionError(e);
        }
    }

    private byte[] getPlaintext(MessageKeys messageKeys, byte[] cipherText) throws InvalidMessageException {
        try {
            Cipher cipher = getCipher(Cipher.DECRYPT_MODE, messageKeys.getCipherKey(), messageKeys.getIv());
            return cipher.doFinal(cipherText);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new InvalidMessageException(e);
        }
    }

    private Cipher getCipher(int mode, SecretKeySpec key, IvParameterSpec iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(mode, key, iv);
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | java.security.InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new AssertionError(e);
        }
    }
}