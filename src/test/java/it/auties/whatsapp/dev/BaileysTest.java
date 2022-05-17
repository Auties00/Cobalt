package it.auties.whatsapp.dev;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.bytes.Bytes;
import it.auties.whatsapp.api.WhatsappOptions;
import it.auties.whatsapp.binary.BinarySocket;
import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.controller.WhatsappStore;
import it.auties.whatsapp.crypto.GroupBuilder;
import it.auties.whatsapp.crypto.GroupCipher;
import it.auties.whatsapp.crypto.SessionBuilder;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MessageContainer;
import it.auties.whatsapp.model.message.model.MessageKey;
import it.auties.whatsapp.model.message.standard.TextMessage;
import it.auties.whatsapp.model.signal.auth.SignedDeviceIdentity;
import it.auties.whatsapp.model.signal.keypair.SignalKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalPreKeyPair;
import it.auties.whatsapp.model.signal.keypair.SignalSignedKeyPair;
import it.auties.whatsapp.model.signal.sender.SenderKeyMessage;
import it.auties.whatsapp.model.signal.sender.SenderKeyName;
import it.auties.whatsapp.model.signal.sender.SenderKeyRecord;
import it.auties.whatsapp.model.signal.sender.SenderKeyState;
import it.auties.whatsapp.model.signal.session.SessionAddress;
import it.auties.whatsapp.model.signal.session.SessionChain;
import it.auties.whatsapp.model.signal.session.SessionPreKey;
import it.auties.whatsapp.model.signal.session.SessionState;
import it.auties.whatsapp.model.sync.AppStateSyncKeyData;
import it.auties.whatsapp.model.sync.AppStateSyncKeyFingerprint;
import it.auties.whatsapp.model.sync.AppStateSyncKeyId;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import static it.auties.whatsapp.util.SignalSpecification.CURRENT_VERSION;

public class BaileysTest {
    public static final String MESSAGE_ID = "ABCABCABCABCABCC";
    public static final String MESSAGE_CONTENT = "Ciao";

    @Test
    @SneakyThrows
    public void toWW4J()  {
        var whatsappKeys = createKeys();
        var whatsappStore = WhatsappStore.newStore(33);

        var contact = ContactJid.of("15414367949@s.whatsapp.net");
        var key = MessageKey.newMessageKey()
                .id(MESSAGE_ID)
                .chatJid(contact)
                .fromMe(true)
                .create();
        var info = MessageInfo.newMessageInfo()
                .storeId(whatsappStore.id())
                .key(key)
                .message(MessageContainer.of(TextMessage.of(MESSAGE_CONTENT)))
                .create();

        var address = ContactJid.of("120363020967782887@g.us");
        var sender = ContactJid.of("393495089819:86@s.whatsapp.net");
        var name = new SenderKeyName(address.toString(), sender.toSignalAddress());
        var cipher = new GroupCipher(name, whatsappKeys);
        var ww4j = SenderKeyMessage.ofSerialized(cipher.encrypt(Bytes.ofHex("0a054369616f2101").toByteArray()).bytes());
        var baileys = SenderKeyMessage.ofSerialized(Bytes.ofHex("3308b59fc8f50310011a1044e6182c4644a698abe887c43d3831ec9a033b8a647d60e9f801ebfe0f7f3d6bda7bd1e6419f4d3c89589a6cf286a0b6ccf310d6a3cbd7c5734bfcf9b92b1160477f3a60b1381e58c492de803fabef06").toByteArray());
        System.out.println(ww4j.equals(baileys));
    }

    private WhatsappKeys createKeys() throws URISyntaxException, IOException {
        var path = Path.of(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("test.json")).toURI());
        var baileysKeys = new ObjectMapper().readValue(path.toFile(), BaileysKeys.class);
        return WhatsappKeys.builder()
                .id(baileysKeys.creds().registrationId())
                .noiseKeyPair(new SignalKeyPair(baileysKeys.creds().noiseKey().publicKey().data(), baileysKeys.creds().noiseKey().privateKey().data()))
                .ephemeralKeyPair(new SignalKeyPair(baileysKeys.creds().noiseKey().publicKey().data(), baileysKeys.creds().noiseKey().privateKey().data()))
                .identityKeyPair(new SignalKeyPair(baileysKeys.creds().signedIdentityKey().publicKey().data(), baileysKeys.creds().signedIdentityKey().privateKey().data()))
                .signedKeyPair(new SignalSignedKeyPair(baileysKeys.creds().signedPreKey().keyId(), new SignalKeyPair(baileysKeys.creds().signedPreKey().keyPair().publicKey().data(), baileysKeys.creds().signedPreKey().keyPair().privateKey().data()), baileysKeys.creds().signedPreKey().signature().data()))
                .preKeys(baileysKeys.keys() == null || baileysKeys.keys().preKeys() == null ? new ConcurrentLinkedDeque<>() : baileysKeys.keys().preKeys()
                        .keySet()
                        .stream()
                        .filter(id -> baileysKeys.keys().preKeys().get(id) != null)
                        .map(id -> new SignalPreKeyPair(
                                        id,
                                        baileysKeys.keys().preKeys().get(id).publicKey().data(),
                                        baileysKeys.keys().preKeys().get(id).privateKey().data()
                                )
                        )
                        .collect(Collectors.toCollection(ConcurrentLinkedDeque::new)))
                .companion(baileysKeys.keys() == null || baileysKeys.creds().me() == null ? null : ContactJid.of(baileysKeys.creds().me().id()))
                .companionKey(baileysKeys.creds().advSecretKey())
                .companionIdentity(baileysKeys.creds().account() == null ? null : SignedDeviceIdentity.builder()
                        .deviceSignature(baileysKeys.creds().account().deviceSignature())
                        .accountSignature(baileysKeys.creds().account().accountSignature())
                        .accountSignatureKey(baileysKeys.creds().account().accountSignatureKey())
                        .details(baileysKeys.creds().account().details())
                        .build())
                .appStateKeys(baileysKeys.keys() == null ? new ConcurrentLinkedDeque<>() : baileysKeys.keys().appStateSyncKeys() != null ? baileysKeys.keys().appStateSyncKeys()
                        .keySet()
                        .stream()
                        .map(name -> it.auties.whatsapp.model.sync.AppStateSyncKey.builder()
                                .keyId(new AppStateSyncKeyId(new byte[32]))
                                .keyData(AppStateSyncKeyData.builder()
                                        .keyData(baileysKeys.keys().appStateSyncKeys().get(name).keyData())
                                        .fingerprint(AppStateSyncKeyFingerprint.builder()
                                                .currentIndex(baileysKeys.keys().appStateSyncKeys().get(name).fingerprint().currentIndex())
                                                .deviceIndexes(baileysKeys.keys().appStateSyncKeys().get(name).fingerprint().deviceIndexes())
                                                .rawId(baileysKeys.keys().appStateSyncKeys().get(name).fingerprint().rawId())
                                                .build())
                                        .build())
                                .build())
                        .collect(Collectors.toCollection(ConcurrentLinkedDeque::new)) : new ConcurrentLinkedDeque<>())
                .sessions(baileysKeys.keys() == null ? new ConcurrentHashMap<>() : baileysKeys.keys().sessions()
                        .keySet()
                        .stream()
                        .map(key -> Map.entry(key,
                                new it.auties.whatsapp.model.signal.session.Session(baileysKeys.keys().sessions().get(key)._sessions()
                                        .keySet()
                                        .stream()
                                        .map(sessionKey -> {
                                            var sessionState = baileysKeys.keys().sessions().get(key)._sessions().get(sessionKey);
                                            return SessionState.builder()
                                                    .version(CURRENT_VERSION)
                                                    .registrationId(sessionState.registrationId())
                                                    .rootKey(sessionState.currentRatchet().rootKey())
                                                    .ephemeralKeyPair(new SignalKeyPair(sessionState.currentRatchet().ephemeralKeyPair().pubKey(), sessionState.currentRatchet().ephemeralKeyPair().privKey()))
                                                    .lastRemoteEphemeralKey(sessionState.currentRatchet().lastRemoteEphemeralKey())
                                                    .previousCounter(sessionState.currentRatchet().previousCounter())
                                                    .remoteIdentityKey(sessionState.indexInfo().remoteIdentityKey())
                                                    .chains(sessionState._chains().keySet().stream().map(chainKey -> Map.entry(Bytes.ofBase64(chainKey).toHex(), SessionChain.builder()
                                                                    .counter(sessionState._chains().get(chainKey).chainKey().counter())
                                                                    .key(sessionState._chains().get(chainKey).chainKey().key())
                                                                    .messageKeys(sessionState._chains().get(chainKey)
                                                                            .messageKeys()
                                                                            .keySet()
                                                                            .stream()
                                                                            .map(messageKey -> Map.entry(messageKey, sessionState._chains().get(chainKey).messageKeys().get(messageKey)))
                                                                            .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first, ConcurrentHashMap::new)))
                                                                    .build()))
                                                            .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first, ConcurrentHashMap::new)))
                                                    .pendingPreKey(sessionState.pendingPreKey() != null ? new SessionPreKey(sessionState.pendingPreKey().preKeyId(), sessionState.pendingPreKey().baseKey(), sessionState.pendingPreKey().signedKeyId()) : null)
                                                    .baseKey(sessionState.indexInfo().baseKey())
                                                    .closed(sessionState.indexInfo().closed() > 0)
                                                    .build();
                                        })
                                        .collect(Collectors.toCollection(ConcurrentLinkedDeque::new)))))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .senderKeys(baileysKeys.keys() == null ? new ConcurrentHashMap<>() : baileysKeys.keys().senderKeys() != null ? baileysKeys.keys().senderKeys()
                        .keySet()
                        .stream()
                        .map(keyName -> {
                            var senderKey = baileysKeys.keys().senderKeys().get(keyName);
                            return Map.entry(keyName, new SenderKeyRecord(senderKey.stream()
                                    .map(keyEntry -> SenderKeyState.builder()
                                            .id(keyEntry.senderKeyId())
                                            .chainKey(new it.auties.whatsapp.model.signal.sender.SenderChainKey(keyEntry.senderChainKey().iteration(), keyEntry.senderChainKey().seed()))
                                            .signingKey(new it.auties.whatsapp.model.signal.sender.SenderSigningKey(keyEntry.senderSigningKey().publicKey(), keyEntry.senderSigningKey().privateKey()))
                                            .build())
                                    .collect(Collectors.toCollection(ConcurrentLinkedDeque::new))));
                        })
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)) : new HashMap<>())
                .build();
    }

    record BaileysKeys(Credentials creds, Keys keys) {

    }

    @JsonIgnoreProperties({"signalIdentities", "accountSettings"})
    record Credentials(KeyPair noiseKey, KeyPair signedIdentityKey, SignedKeyPair signedPreKey,
                       int registrationId, byte[] advSecretKey, int nextPreKeyId, int firstUnuploadedPreKeyId, boolean serverHasPreKeys,
                       Account account, Me me, byte[] myAppStateKeyId, long lastAccountSyncTimestamp) {

    }

    record Me(String id, String verifiedName, String name){

    }

    record KeyPair(@JsonProperty("public") KeyPairEntry publicKey,
                   @JsonProperty("private") KeyPairEntry privateKey) {

    }

    record SignedKeyPair(KeyPair keyPair, KeyPairEntry signature, int keyId) {

    }


    record KeyPairEntry(String type, byte[] data){

    }

    record Account(byte[] details, byte[] accountSignatureKey, byte[] accountSignature, byte[] deviceSignature) {

    }

    @JsonIgnoreProperties({"appStateVersions", "senderKeyMemory"})
    record Keys(Map<Integer, KeyPair> preKeys, Map<SessionAddress, SessionWrapper> sessions, Map<String, AppStateSyncKey> appStateSyncKeys, Map<SenderKeyName, List<SenderKey>> senderKeys) {

    }

    record SenderKey(int senderKeyId, SenderChainKey senderChainKey, SenderSigningKey senderSigningKey){

    }

    record SenderChainKey(int iteration, byte[] seed){

    }

    record SenderSigningKey(@JsonProperty("public") byte[] publicKey, @JsonProperty("private") byte[] privateKey){

    }

    record SessionWrapper(Map<String, Session> _sessions, String version){

    }

    record Session(int registrationId, CurrentRatchet currentRatchet, IndexInfo indexInfo, Map<String, Chain> _chains,
                   PendingPreKey pendingPreKey) {

    }

    record PendingPreKey(int signedKeyId, byte[] baseKey, int preKeyId){

    }

    record CurrentRatchet(Curve25519KeyPair ephemeralKeyPair, byte[] lastRemoteEphemeralKey, int previousCounter,
                          byte[] rootKey) {

    }

    record Curve25519KeyPair(byte[] pubKey, byte[] privKey){

    }

    record IndexInfo(byte[] baseKey, int baseKeyType, long closed, long used, long created, byte[] remoteIdentityKey) {

    }

    record Chain(ChainKey chainKey, int chainType, Map<Integer, byte[]> messageKeys){

    }

    record ChainKey(int counter, byte[] key){

    }

    record AppStateSyncKey(byte[] keyData, Fingerprint fingerprint, String timestamp){

    }

    record Fingerprint(int rawId, int currentIndex, List<Integer> deviceIndexes){

    }
}