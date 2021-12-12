package it.auties.whatsapp.crypto;

import it.auties.whatsapp.manager.WhatsappKeys;
import it.auties.whatsapp.protobuf.signal.group.ProtocolAddress;
import lombok.NonNull;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.libsignal.state.SessionRecord;

public record SignalSession(@NonNull ProtocolAddress address, @NonNull WhatsappKeys keys) {
    public void cipher(byte[] paddedMessage) {
        SessionRecord sessionRecord = sessionStore.loadSession(remoteAddress);
        SessionState sessionState = sessionRecord.getSessionState();
        ChainKey chainKey = sessionState.getSenderChainKey();
        MessageKeys messageKeys = chainKey.getMessageKeys();
        ECPublicKey senderEphemeral = sessionState.getSenderRatchetKey();
        int previousCounter = sessionState.getPreviousCounter();
        int sessionVersion = sessionState.getSessionVersion();

        byte[] ciphertextBody = getCiphertext(messageKeys, paddedMessage);
        CiphertextMessage ciphertextMessage = new SignalMessage(sessionVersion, messageKeys.getMacKey(), senderEphemeral, chainKey.getIndex(), previousCounter, ciphertextBody, sessionState.getLocalIdentityKey(), sessionState.getRemoteIdentityKey());

        if (sessionState.hasUnacknowledgedPreKeyMessage()) {
            UnacknowledgedPreKeyMessageItems items = sessionState.getUnacknowledgedPreKeyMessageItems();
            int localRegistrationId = sessionState.getLocalRegistrationId();

            ciphertextMessage = new PreKeySignalMessage(sessionVersion, localRegistrationId, items.getPreKeyId(), items.getSignedPreKeyId(), items.getBaseKey(), sessionState.getLocalIdentityKey(), (SignalMessage) ciphertextMessage);
        }

        sessionState.setSenderChainKey(chainKey.getNextChainKey());

        if (!identityKeyStore.isTrustedIdentity(remoteAddress, sessionState.getRemoteIdentityKey(), IdentityKeyStore.Direction.SENDING)) {
            throw new UntrustedIdentityException(remoteAddress.getName(), sessionState.getRemoteIdentityKey());
        }

        identityKeyStore.saveIdentity(remoteAddress, sessionState.getRemoteIdentityKey());
        sessionStore.storeSession(remoteAddress, sessionRecord);
        return ciphertextMessage;
    }


    public byte[] decrypt(PreKeySignalMessage ciphertext) throws DuplicateMessageException, LegacyMessageException, InvalidMessageException, InvalidKeyIdException, InvalidKeyException, UntrustedIdentityException {
        return decrypt(ciphertext, new NullDecryptionCallback());
    }


    public byte[] decrypt(PreKeySignalMessage ciphertext, DecryptionCallback callback) throws DuplicateMessageException, LegacyMessageException, InvalidMessageException, InvalidKeyIdException, InvalidKeyException, UntrustedIdentityException {
        synchronized (SESSION_LOCK) {
            SessionRecord sessionRecord = sessionStore.loadSession(remoteAddress);
            Optional<Integer> unsignedPreKeyId = sessionBuilder.process(sessionRecord, ciphertext);
            byte[] plaintext = decrypt(sessionRecord, ciphertext.getWhisperMessage());

            callback.handlePlaintext(plaintext);

            sessionStore.storeSession(remoteAddress, sessionRecord);

            if (unsignedPreKeyId.isPresent()) {
                preKeyStore.removePreKey(unsignedPreKeyId.get());
            }

            return plaintext;
        }
    }


    public byte[] decrypt(SignalMessage ciphertext) throws InvalidMessageException, DuplicateMessageException, LegacyMessageException, NoSessionException, UntrustedIdentityException {
        return decrypt(ciphertext, new NullDecryptionCallback());
    }


    public byte[] decrypt(SignalMessage ciphertext, DecryptionCallback callback) throws InvalidMessageException, DuplicateMessageException, LegacyMessageException, NoSessionException, UntrustedIdentityException {
        synchronized (SESSION_LOCK) {

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
    }

    private byte[] decrypt(SessionRecord sessionRecord, SignalMessage ciphertext) throws DuplicateMessageException, LegacyMessageException, InvalidMessageException {
        synchronized (SESSION_LOCK) {
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
    }

    private byte[] decrypt(SessionState sessionState, SignalMessage ciphertextMessage) throws InvalidMessageException, DuplicateMessageException, LegacyMessageException {
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
        synchronized (SESSION_LOCK) {
            if (!sessionStore.containsSession(remoteAddress)) {
                throw new IllegalStateException(String.format("No session for (%s)!", remoteAddress));
            }

            SessionRecord record = sessionStore.loadSession(remoteAddress);
            return record.getSessionState().getSessionVersion();
        }
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