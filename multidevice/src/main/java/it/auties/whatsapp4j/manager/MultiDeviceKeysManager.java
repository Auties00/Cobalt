package it.auties.whatsapp4j.manager;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.auties.whatsapp4j.utils.IdentityKeyPair;
import it.auties.whatsapp4j.utils.SignedKeyPair;
import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.common.serialization.KeyPairDeserializer;
import it.auties.whatsapp4j.common.serialization.KeyPairSerializer;
import it.auties.whatsapp4j.common.utils.CypherUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.libsignal.ecc.DjbECPrivateKey;
import org.whispersystems.libsignal.ecc.DjbECPublicKey;
import org.whispersystems.libsignal.util.KeyHelper;

import java.util.Base64;

/**
 * This class is a data class used to hold the clientId, serverToken, clientToken, publicKey, privateKey, encryptionKey and macKey.
 * It can be serialized using Jackson and deserialized using the fromPreferences named constructor.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(fluent = true, chain = true)
public class MultiDeviceKeysManager extends WhatsappKeysManager {
    @JsonProperty
    @JsonSerialize(using = KeyPairSerializer.class)
    @JsonDeserialize(using = KeyPairDeserializer.class)
    private IdentityKeyPair ephemeralKeyPair;

    @JsonProperty
    @JsonSerialize(using = KeyPairSerializer.class)
    @JsonDeserialize(using = KeyPairDeserializer.class)
    private IdentityKeyPair signedIdentityKey;

    @JsonProperty
    private SignedKeyPair signedPreKey;

    @JsonProperty
    private int registrationId;

    @JsonProperty
    private @NonNull String advSecretKey;

    @JsonProperty
    private BinaryArray writeKey, readKey;

    public MultiDeviceKeysManager() {
        super(Base64.getEncoder().encodeToString(BinaryArray.random(16).data()), CypherUtils.randomKeyPair(), null, null, null, null);
        this.ephemeralKeyPair = createKeyPair();
        this.signedIdentityKey = createKeyPair();
        this.signedPreKey = generateSignedPreKey();
        this.registrationId = KeyHelper.generateRegistrationId(false);
        this.advSecretKey = Base64.getEncoder().encodeToString(KeyHelper.generateSenderKey());
    }

    private IdentityKeyPair createKeyPair() {
        var pair = KeyHelper.generateSenderSigningKey();
        var publicKey = ((DjbECPublicKey) pair.getPublicKey()).getPublicKey();
        var privateKey = ((DjbECPrivateKey) pair.getPrivateKey()).getPrivateKey();
        return new IdentityKeyPair(publicKey, privateKey);
    }

    private SignedKeyPair generateSignedPreKey() {
        var keyPair = createKeyPair();
        var publicKey = BinaryArray.singleton((byte) 5).append(keyPair.publicKey()).data();
        var signature = Curve25519.getInstance(Curve25519.BEST).calculateSignature(signedIdentityKey().privateKey(), publicKey);
        return new SignedKeyPair(1, keyPair, signature);
    }

    public void initializeKeys(@NonNull BinaryArray writeKey, @NonNull BinaryArray readKey) {
        this.initializeKeys(null, null, writeKey, readKey);
    }

    @Override
    public MultiDeviceKeysManager initializeKeys(String serverToken, String clientToken, @NonNull BinaryArray writeKey, @NonNull BinaryArray readKey) {
        return writeKey(writeKey).readKey(readKey);
    }

    @Override
    public MultiDeviceKeysManager withPreferences() {
        super.withPreferences();
        return this;
    }
}