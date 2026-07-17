package com.github.auties00.cobalt.wire.linked.setting;

import java.util.Objects;
import java.util.Optional;

/**
 * Carries one entry of the parsed OHAI (Oblivious HTTP Authentication
 * for Initiation) key configuration list.
 *
 * <p>OHAI is the HPKE key bundle used to encapsulate Account-Centre-
 * Service requests sent by the OHAI client. The relay rotates the
 * key set periodically and clients are expected to refetch the
 * configuration when their cached value expires.
 *
 * <p>Each entry carries an opaque server-assigned identifier, the
 * three HPKE algorithm-suite ids ({@code aead_id}, {@code kdf_id},
 * {@code kem_id}), a hex-encoded public key, and the issue / expiry
 * timestamps. Every field is optional because the relay surfaces them
 * as scalar GraphQL fields and the WhatsApp Web bundle treats missing
 * values as a parse-failed entry.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it surfaces
 * the parsed reply to caller code and never travels on the wire.
 */
public final class OhaiKeyConfig {
    /**
     * The {@code aead_id} scalar identifying the AEAD cipher suite.
     */
    private final String aeadId;

    /**
     * The {@code expiration_date} scalar (Unix epoch second).
     */
    private final String expirationDate;

    /**
     * The {@code kdf_id} scalar identifying the HPKE KDF.
     */
    private final String kdfId;

    /**
     * The {@code kem_id} scalar identifying the HPKE KEM.
     */
    private final String kemId;

    /**
     * The {@code key_id} scalar carrying the server-assigned
     * identifier.
     */
    private final String keyId;

    /**
     * The {@code last_updated_time} scalar (Unix epoch second).
     */
    private final String lastUpdatedTime;

    /**
     * The {@code public_key} scalar carrying the OHAI public key
     * bytes encoded as a hexadecimal string.
     */
    private final String publicKey;

    /**
     * Constructs a new entry.
     *
     * @param aeadId          the {@code aead_id} scalar; may be
     *                        {@code null}
     * @param expirationDate  the {@code expiration_date} scalar; may
     *                        be {@code null}
     * @param kdfId           the {@code kdf_id} scalar; may be
     *                        {@code null}
     * @param kemId           the {@code kem_id} scalar; may be
     *                        {@code null}
     * @param keyId           the {@code key_id} scalar; may be
     *                        {@code null}
     * @param lastUpdatedTime the {@code last_updated_time} scalar;
     *                        may be {@code null}
     * @param publicKey       the {@code public_key} scalar; may be
     *                        {@code null}
     */
    public OhaiKeyConfig(String aeadId, String expirationDate, String kdfId, String kemId,
                         String keyId, String lastUpdatedTime, String publicKey) {
        this.aeadId = aeadId;
        this.expirationDate = expirationDate;
        this.kdfId = kdfId;
        this.kemId = kemId;
        this.keyId = keyId;
        this.lastUpdatedTime = lastUpdatedTime;
        this.publicKey = publicKey;
    }

    /**
     * Returns the {@code aead_id} field.
     *
     * @return an {@link Optional} carrying the value
     */
    public Optional<String> aeadId() {
        return Optional.ofNullable(aeadId);
    }

    /**
     * Returns the {@code expiration_date} field.
     *
     * @return an {@link Optional} carrying the value
     */
    public Optional<String> expirationDate() {
        return Optional.ofNullable(expirationDate);
    }

    /**
     * Returns the {@code kdf_id} field.
     *
     * @return an {@link Optional} carrying the value
     */
    public Optional<String> kdfId() {
        return Optional.ofNullable(kdfId);
    }

    /**
     * Returns the {@code kem_id} field.
     *
     * @return an {@link Optional} carrying the value
     */
    public Optional<String> kemId() {
        return Optional.ofNullable(kemId);
    }

    /**
     * Returns the {@code key_id} field.
     *
     * @return an {@link Optional} carrying the value
     */
    public Optional<String> keyId() {
        return Optional.ofNullable(keyId);
    }

    /**
     * Returns the {@code last_updated_time} field.
     *
     * @return an {@link Optional} carrying the value
     */
    public Optional<String> lastUpdatedTime() {
        return Optional.ofNullable(lastUpdatedTime);
    }

    /**
     * Returns the {@code public_key} field.
     *
     * @return an {@link Optional} carrying the value
     */
    public Optional<String> publicKey() {
        return Optional.ofNullable(publicKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (OhaiKeyConfig) obj;
        return Objects.equals(this.aeadId, that.aeadId)
                && Objects.equals(this.expirationDate, that.expirationDate)
                && Objects.equals(this.kdfId, that.kdfId)
                && Objects.equals(this.kemId, that.kemId)
                && Objects.equals(this.keyId, that.keyId)
                && Objects.equals(this.lastUpdatedTime, that.lastUpdatedTime)
                && Objects.equals(this.publicKey, that.publicKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aeadId, expirationDate, kdfId, kemId, keyId, lastUpdatedTime, publicKey);
    }

    @Override
    public String toString() {
        return "OhaiKeyConfig[aeadId=" + aeadId
                + ", expirationDate=" + expirationDate
                + ", kdfId=" + kdfId
                + ", kemId=" + kemId
                + ", keyId=" + keyId
                + ", lastUpdatedTime=" + lastUpdatedTime
                + ", publicKey=" + publicKey + ']';
    }
}
