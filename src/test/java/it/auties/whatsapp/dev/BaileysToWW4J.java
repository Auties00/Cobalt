package it.auties.whatsapp.dev;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.auties.whatsapp.controller.WhatsappKeys;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.util.JacksonProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class BaileysToWW4J implements JacksonProvider {
    @Test
    public void toWW4J() throws IOException {
        var path = Path.of("C:\\Users\\alaut\\WebstormProjects\\Baileys\\auth_info_multi.json");
        var keys = new ObjectMapper().readValue(path.toFile(), BaileysKeys.class);
    }

    record BaileysKeys(Credentials creds, Keys keys) {

    }

    @JsonIgnoreProperties("signalIdentities")
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

    @JsonIgnoreProperties("appStateVersions")
    record Keys(Map<Integer, KeyPair> preKeys, Map<ContactJid, SessionWrapper> sessions, Map<String, AppStateSyncKey> appStateSyncKeys) {

    }

    record SessionWrapper(Map<String, Session> _sessions, String version){

    }

    record Session(int registrationId, CurrentRatchet currentRatchet, IndexInfo indexInfo, Map<String, Chain> _chains) {

    }

    record CurrentRatchet(Curve25519KeyPair ephemeralKeyPair, byte[] lastRemoteEphemeralKey, int previousCounter,
                          byte[] rootKey) {

    }

    record Curve25519KeyPair(byte[] pubKey, byte[] privKey){

    }

    record IndexInfo(byte[] baseKey, int baseKeyType, int closed, long used, long created, byte[] remoteIdentityKey) {

    }

    record Chain(ChainKey chainKey, int chainType, Map<Integer, byte[]> messageKeys){

    }

    record ChainKey(int counter, byte[] key){

    }

    record AppStateSyncKey(byte[] keyData, Fingerprint fingerprint, String timestamp){

    }

    record Fingerprint(long rawId, int currentIndex, List<Integer> deviceIndexes){

    }
}
