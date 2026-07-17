package com.github.auties00.cobalt.sync.crypto;

import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionData;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionDataSpec;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValue;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEntry;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import it.auties.protobuf.stream.ProtobufInputStream;

import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

/**
 * A single mutation that has come off the wire and been authenticated and decrypted.
 *
 * <p>The two variants capture the two stages of the incoming-patch pipeline:
 * <ul>
 *   <li>{@link Untrusted} is the freshly decrypted form produced by
 *       {@link Untrusted#of}; it still carries the raw {@code indexMac},
 *       {@code valueMac}, and {@code keyId} that the LT-Hash bookkeeping and
 *       the snapshot-MAC chaining consume downstream.</li>
 *   <li>{@link Trusted} is the post-validation form attached to the local
 *       store: the MAC metadata has been consumed and only the
 *       application-level fields remain.</li>
 * </ul>
 *
 * <p>The receive pipeline produces {@link Untrusted} from
 * {@link SyncActionValue}-bearing wire records, verifies the patch and snapshot
 * MACs against the resulting value MACs, and only then promotes the survivors
 * to {@link Trusted}.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdDecryptMutations")
@WhatsAppWebModule(moduleName = "WAWebSyncdDecryptMutationsWrapper")
public sealed interface DecryptedMutation {
    /**
     * Returns the UTF-8 decoded mutation index string.
     *
     * <p>The index is the JSON-array key under which the sync handlers locate
     * the mutation target, for example {@code ["archive","1234@s.whatsapp.net"]}.
     *
     * @return the index string
     */
    String index();

    /**
     * Returns whether this mutation sets or removes its index.
     *
     * @return the sync operation
     */
    SyncdOperation operation();

    /**
     * Returns the timestamp the originating device stamped on the action, or
     * {@code null} for a {@link SyncdOperation#REMOVE} tombstone that carries
     * no value.
     *
     * <p>Handlers that order conflicting actions by recency (chat archive,
     * mute, pin) read this value; it is not a server-supplied wall clock. Only
     * a {@link SyncdOperation#SET} is guaranteed to carry a timestamp; a
     * {@link SyncdOperation#REMOVE} is keyed solely by its index and may have
     * none, so callers that compare timestamps must first confirm the
     * operation is a SET.
     *
     * @return the action timestamp, or {@code null} for a valueless REMOVE
     */
    Instant timestamp();

    /**
     * A decrypted mutation that still carries the MAC and key-id metadata
     * needed to chain LT-Hash and snapshot-MAC verification.
     *
     * <p>Produced exclusively by {@link Untrusted#of}. Downstream code consumes
     * {@link #indexMac()} and {@link #valueMac()} when feeding the
     * {@link MutationIntegrityVerifier} patch path, and {@link #keyId()} when
     * recording the {@link SyncActionEntry}
     * that LT-Hash recomputation reads back during consistency checks.
     */
    final class Untrusted implements DecryptedMutation {
        /**
         * The logger for {@link Untrusted}.
         */
        private static final System.Logger LOGGER = Log.get(Untrusted.class);

        /**
         * The UTF-8 decoded mutation index string.
         */
        private final String index;

        /**
         * The wire index MAC, also recomputed from the index for verification.
         */
        private final byte[] indexMac;

        /**
         * The trailing 32 bytes of the wire encrypted value.
         */
        private final byte[] valueMac;

        /**
         * The decoded action payload, or {@code null} for a
         * {@link SyncdOperation#REMOVE} tombstone that carries no value.
         */
        private final SyncActionValue value;

        /**
         * The sync operation this mutation declares.
         */
        private final SyncdOperation operation;

        /**
         * The action timestamp, or {@code null} when {@link #value} is
         * {@code null}.
         */
        private final Instant timestamp;

        /**
         * The sync key id used to decrypt this mutation.
         */
        private final byte[] keyId;

        /**
         * The action protobuf version from the payload.
         */
        private final int actionVersion;

        /**
         * Constructs an untrusted mutation from its already-decoded fields.
         *
         * <p>Only {@link #of} constructs an {@link Untrusted}; the receive
         * pipeline never builds one directly because the MAC chain must be
         * verified first.
         *
         * @param index         the decoded index string
         * @param indexMac      the wire index MAC
         * @param valueMac      the trailing value MAC
         * @param value         the decoded payload, or {@code null} for a REMOVE
         * @param operation     the sync operation
         * @param timestamp     the action timestamp, or {@code null} for a REMOVE
         * @param keyId         the sync key id used for decryption
         * @param actionVersion the action protobuf version
         */
        private Untrusted(
                String index,
                byte[] indexMac,
                byte[] valueMac,
                SyncActionValue value,
                SyncdOperation operation,
                Instant timestamp,
                byte[] keyId,
                int actionVersion
        ) {
            this.index = index;
            this.indexMac = indexMac;
            this.valueMac = valueMac;
            this.value = value;
            this.operation = operation;
            this.timestamp = timestamp;
            this.keyId = keyId;
            this.actionVersion = actionVersion;
        }

        /**
         * Decrypts and authenticates a single wire mutation.
         *
         * <p>The expected wire format is
         * {@snippet :
         *     // encryptedValue = IV (16 bytes)
         *     //                || AES-CBC(SyncActionData protobuf)
         *     //                || HMAC-SHA512(...)[0..32]   // trailing valueMac
         *     // indexMac        = HMAC-SHA256(indexKey, SyncActionData.index)
         * }
         * Verification proceeds value MAC first (so a tampered ciphertext fails
         * before the AES-CBC step), then the protobuf decode, then the index MAC;
         * the thrown exception subtype names the failed step.
         *
         * @implNote
         * This implementation decodes the {@link SyncActionValue} as nullable,
         * matching WA Web's {@code WAWebSyncdDecryptMutations.syncdDecryptMutation}
         * ({@code value: D ? ... : null}). The value and its timestamp are
         * required only for a {@link SyncdOperation#SET}, mirroring
         * {@code WAWebSyncdValidateMutations.validateAndTypeSetMutations}, which
         * runs only on the SET-filtered partition of a decoded patch. A
         * {@link SyncdOperation#REMOVE} is a valueless tombstone keyed solely by
         * its index, so {@code value} and {@code timestamp} are left
         * {@code null} for it; the downstream handlers that apply a REMOVE read
         * the index, never the value.
         *
         * @param encryptedValue the wire {@code IV || ciphertext || valueMac} blob
         * @param indexMac       the wire index MAC to verify against
         * @param keys           the derived sync keys for the relevant key id
         * @param operation      the operation the wire frame declares
         * @param keyId          the sync key id, mixed into the AAD
         * @return a freshly decoded {@link Untrusted} instance
         * @throws IllegalArgumentException                          if {@code encryptedValue} is shorter than {@code IV_LENGTH + MAC_LENGTH}
         * @throws GeneralSecurityException                          if the JCE primitives fail
         * @throws WhatsAppWebAppStateSyncException.ValueMacMismatch if the value MAC does not validate
         * @throws WhatsAppWebAppStateSyncException.IndexMacMismatch if the index MAC does not validate
         * @throws WhatsAppWebAppStateSyncException.DecryptionFailed if the AES-CBC output is not a valid {@link SyncActionData} protobuf
         * @throws WhatsAppWebAppStateSyncException.UnexpectedError  if the decoded protobuf is missing the index or version field, or is missing the value for a {@link SyncdOperation#SET}
         * @throws WhatsAppWebAppStateSyncException.MissingActionTimestamp if a {@link SyncdOperation#SET} action value carries no timestamp
         */
        @WhatsAppWebExport(moduleName = "WAWebSyncdDecryptMutations", exports = "syncdDecryptMutation", adaptation = WhatsAppAdaptation.DIRECT)
        @WhatsAppWebExport(moduleName = "WAWebSyncdDecryptMutationsWrapper", exports = {"tryDecryptSnapshot", "tryDecryptPatch"}, adaptation = WhatsAppAdaptation.ADAPTED)
        public static Untrusted of(
                byte[] encryptedValue,
                byte[] indexMac,
                MutationKeys keys,
                SyncdOperation operation,
                byte[] keyId
        ) throws GeneralSecurityException {
            if (encryptedValue.length < MutationKeys.IV_LENGTH + MutationKeys.MAC_LENGTH) {
                throw new IllegalArgumentException("Encrypted value too short");
            }

            var valueMac = MutationKeys.valueMacFromIndexAndValueCipherText(encryptedValue);

            var associatedData = MutationKeys.generateAssociatedData(operation, keyId);

            var ivAndCipherText = Arrays.copyOfRange(
                    encryptedValue,
                    0,
                    encryptedValue.length - MutationKeys.MAC_LENGTH
            );

            var expectedMac = keys.generateMac(associatedData, ivAndCipherText);
            if (!MessageDigest.isEqual(valueMac, expectedMac)) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "sync mutation value mac mismatch for operation {0}", operation);
                }
                throw new WhatsAppWebAppStateSyncException.ValueMacMismatch();
            }

            var iv = Arrays.copyOfRange(encryptedValue, 0, MutationKeys.IV_LENGTH);
            var cipherText = Arrays.copyOfRange(
                    encryptedValue,
                    MutationKeys.IV_LENGTH,
                    encryptedValue.length - MutationKeys.MAC_LENGTH
            );
            var plaintext = keys.decryptCipherText(iv, cipherText);

            SyncActionData actionData;
            try {
                actionData = SyncActionDataSpec.decode(ProtobufInputStream.fromBytes(plaintext));
            } catch (Exception e) {
                throw new WhatsAppWebAppStateSyncException.DecryptionFailed(
                        "syncd: data protobuf deserialization failed: " + e.getMessage(), e);
            }

            var actionIndex = actionData.index()
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError("missing action index", null));
            var actionVersion = actionData.version()
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                            "missing action version", null));

            var actionValue = actionData.value().orElse(null);
            if (operation == SyncdOperation.SET && actionValue == null) {
                throw new WhatsAppWebAppStateSyncException.UnexpectedError("Missing value from action data", null);
            }
            var actionTimestamp = actionValue == null ? null : actionValue.timestamp().orElse(null);
            if (operation == SyncdOperation.SET && actionTimestamp == null) {
                throw new WhatsAppWebAppStateSyncException.MissingActionTimestamp();
            }

            var expectedIndexMac = keys.generateIndexMac(actionIndex);
            if (!MessageDigest.isEqual(indexMac, expectedIndexMac)) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "sync mutation index mac mismatch for operation {0}", operation);
                }
                throw new WhatsAppWebAppStateSyncException.IndexMacMismatch();
            }
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "sync mutation decrypted and authenticated: operation={0} version={1}",
                        operation, actionVersion);
            }
            return new Untrusted(
                    new String(actionIndex, StandardCharsets.UTF_8),
                    indexMac,
                    valueMac,
                    actionValue,
                    operation,
                    actionTimestamp,
                    keyId,
                    actionVersion
            );
        }

        @Override
        public String index() {
            return index;
        }

        /**
         * Returns the wire index MAC carried by this mutation.
         *
         * @return the index MAC
         */
        public byte[] indexMac() {
            return indexMac;
        }

        /**
         * Returns the value MAC, the trailing 32 bytes of the wire encrypted
         * value.
         *
         * @return the value MAC
         */
        public byte[] valueMac() {
            return valueMac;
        }

        /**
         * Returns the decoded action payload, empty for a
         * {@link SyncdOperation#REMOVE} tombstone that carries no value.
         *
         * @return the action payload, or an empty {@link Optional} for a
         *         valueless REMOVE
         */
        public Optional<SyncActionValue> value() {
            return Optional.ofNullable(value);
        }

        @Override
        public SyncdOperation operation() {
            return operation;
        }

        @Override
        public Instant timestamp() {
            return timestamp;
        }

        /**
         * Returns the sync key id used to decrypt this mutation.
         *
         * @return the sync key id
         */
        public byte[] keyId() {
            return keyId;
        }

        /**
         * Returns the action protobuf version from the payload.
         *
         * @return the action version
         */
        public int actionVersion() {
            return actionVersion;
        }
    }

    /**
     * A validated mutation that the sync handlers can apply to the local
     * store without further crypto checks.
     *
     * <p>Produced either by promoting an {@link Untrusted} after MAC chaining
     * succeeds, or by the outgoing path that originates mutations locally
     * (no decryption involved). Carries no MAC metadata; the sync handlers
     * see only the application-level fields.
     */
    final class Trusted implements DecryptedMutation {
        /**
         * The index string identifying the mutation target.
         */
        private final String index;

        /**
         * The action payload, or {@code null} for a
         * {@link SyncdOperation#REMOVE} tombstone that carries no value.
         */
        private final SyncActionValue value;

        /**
         * The sync operation this mutation declares.
         */
        private final SyncdOperation operation;

        /**
         * The action timestamp, or {@code null} when {@link #value} is
         * {@code null}.
         */
        private final Instant timestamp;

        /**
         * The action protobuf version.
         */
        private final int actionVersion;

        /**
         * Constructs a trusted mutation from its application-level fields.
         *
         * @param index         the index string identifying the mutation target
         * @param value         the action payload, or {@code null} for a
         *                      {@link SyncdOperation#REMOVE} tombstone
         * @param operation     the sync operation
         * @param timestamp     the action timestamp, or {@code null} when
         *                      {@code value} is {@code null}
         * @param actionVersion the action protobuf version
         */
        public Trusted(
                String index,
                SyncActionValue value,
                SyncdOperation operation,
                Instant timestamp,
                int actionVersion
        ) {
            this.index = index;
            this.value = value;
            this.operation = operation;
            this.timestamp = timestamp;
            this.actionVersion = actionVersion;
        }

        @Override
        public String index() {
            return index;
        }

        /**
         * Returns the action payload, empty for a {@link SyncdOperation#REMOVE}
         * tombstone that carries no value.
         *
         * @return the action payload, or an empty {@link Optional} for a
         *         valueless REMOVE
         */
        public Optional<SyncActionValue> value() {
            return Optional.ofNullable(value);
        }

        @Override
        public SyncdOperation operation() {
            return operation;
        }

        @Override
        public Instant timestamp() {
            return timestamp;
        }

        /**
         * Returns the action protobuf version.
         *
         * @return the action version
         */
        public int actionVersion() {
            return actionVersion;
        }
    }
}
