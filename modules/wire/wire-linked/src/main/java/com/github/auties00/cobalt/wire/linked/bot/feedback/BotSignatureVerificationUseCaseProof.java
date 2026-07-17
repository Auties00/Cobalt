package com.github.auties00.cobalt.wire.linked.bot.feedback;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Represents a cryptographic proof for a specific use case, used to verify
 * the authenticity of an AI bot message.
 *
 * <p>Each proof includes a {@linkplain #version() protocol version} number,
 * the {@linkplain #useCase() use case} it applies to (for example,
 * {@link BotSignatureUseCase#WA_BOT_MSG WA_BOT_MSG} for standard bot messages),
 * the raw {@linkplain #signature() cryptographic signature} bytes, and the
 * {@linkplain #certificateChain() certificate chain} ordered from leaf to root
 * that the client uses to validate the signature against a trusted root.
 *
 * @see BotSignatureVerificationMetadata
 */
@ProtobufMessage(name = "BotSignatureVerificationUseCaseProof")
public final class BotSignatureVerificationUseCaseProof {
    /**
     * The version of the signature verification protocol.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    Integer version;

    /**
     * The use case this proof applies to.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    BotSignatureUseCase useCase;

    /**
     * The raw cryptographic signature bytes.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] signature;

    /**
     * The certificate chain used to validate the signature, ordered from
     * leaf to root.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    List<byte[]> certificateChain;


    /**
     * Constructs a new {@code BotSignatureVerificationUseCaseProof} with the
     * specified values.
     *
     * @param version          the protocol version, or {@code null}
     * @param useCase          the use case, or {@code null}
     * @param signature        the signature bytes, or {@code null}
     * @param certificateChain the certificate chain, or {@code null}
     */
    BotSignatureVerificationUseCaseProof(Integer version, BotSignatureUseCase useCase, byte[] signature, List<byte[]> certificateChain) {
        this.version = version;
        this.useCase = useCase;
        this.signature = signature;
        this.certificateChain = certificateChain;
    }

    /**
     * Returns the version of the signature verification protocol.
     *
     * @return an {@code OptionalInt} describing the version, or an empty
     *         {@code OptionalInt} if not set
     */
    public OptionalInt version() {
        return version == null ? OptionalInt.empty() : OptionalInt.of(version);
    }

    /**
     * Returns the use case this proof applies to.
     *
     * @return an {@code Optional} describing the use case, or an empty
     *         {@code Optional} if not set
     */
    public Optional<BotSignatureUseCase> useCase() {
        return Optional.ofNullable(useCase);
    }

    /**
     * Returns the raw cryptographic signature bytes.
     *
     * @return an {@code Optional} describing the signature, or an empty
     *         {@code Optional} if not set
     */
    public Optional<byte[]> signature() {
        return Optional.ofNullable(signature);
    }

    /**
     * Returns the certificate chain used to validate the signature.
     *
     * @return an unmodifiable list of certificate byte arrays, never {@code null}
     */
    public List<byte[]> certificateChain() {
        return certificateChain == null ? List.of() : Collections.unmodifiableList(certificateChain);
    }

    /**
     * Sets the version of the signature verification protocol.
     *
     * @param version the new version, or {@code null}
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * Sets the use case this proof applies to.
     *
     * @param useCase the new use case, or {@code null}
     */
    public void setUseCase(BotSignatureUseCase useCase) {
        this.useCase = useCase;
    }

    /**
     * Sets the raw cryptographic signature bytes.
     *
     * @param signature the new signature bytes, or {@code null}
     */
    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    /**
     * Sets the certificate chain used to validate the signature.
     *
     * @param certificateChain the new certificate chain, or {@code null}
     */
    public void setCertificateChain(List<byte[]> certificateChain) {
        this.certificateChain = certificateChain;
    }

    /**
     * Enumerates the use cases that a bot signature verification proof can
     * apply to.
     */
    @ProtobufEnum(name = "BotSignatureVerificationUseCaseProof.BotSignatureUseCase")
    public static enum BotSignatureUseCase {
        /**
         * The use case is unspecified.
         */
        UNSPECIFIED(0),

        /**
         * The signature verifies a WhatsApp bot message.
         */
        WA_BOT_MSG(1);

        /**
         * Constructs a new {@code BotSignatureUseCase} with the specified protobuf index.
         *
         * @param index the protobuf index value
         */
        BotSignatureUseCase(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf index of this enum constant.
         */
        final int index;

        /**
         * Returns the protobuf index of this enum constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }
}
