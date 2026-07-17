package com.github.auties00.cobalt.wire.linked.setting;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a user-supplied password together with the parameters required
 * to verify it.
 *
 * <p>Passwords are never stored in clear form. Instead WhatsApp applies a key
 * derivation function to the user input and persists the derived bytes along
 * with the encoding of the original string, the name of the derivation
 * function, and the arguments that the derivation function needs (for
 * example the salt and iteration count of PBKDF2). To verify a password
 * attempt the client re-runs the same derivation on the attempt and checks
 * that the output matches {@link #transformedData}.
 *
 * <p>This type is currently used to protect the secret code of the
 * {@link ChatLockSettings chat lock} feature.
 */
@ProtobufMessage(name = "UserPassword")
public final class UserPassword {
    /**
     * Character encoding used to convert the input password into bytes before
     * applying the transformer.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    Encoding encoding;

    /**
     * Key derivation function applied to the encoded password.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    Transformer transformer;

    /**
     * Key-value arguments supplied to the transformer (for example the salt
     * and the iteration count of PBKDF2).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    List<TransformerArg> transformerArg;

    /**
     * Output of the transformer, used as the reference value when verifying
     * later password attempts.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] transformedData;


    /**
     * Constructs a new user password with the given verification parameters.
     *
     * @param encoding        the input encoding, may be {@code null}
     * @param transformer     the key derivation function, may be {@code null}
     * @param transformerArg  the transformer arguments, may be {@code null}
     * @param transformedData the derived verification bytes, may be {@code null}
     */
    UserPassword(Encoding encoding, Transformer transformer, List<TransformerArg> transformerArg, byte[] transformedData) {
        this.encoding = encoding;
        this.transformer = transformer;
        this.transformerArg = transformerArg;
        this.transformedData = transformedData;
    }

    /**
     * Returns the character encoding applied to the input password.
     *
     * @return an {@link Optional} containing the encoding, or empty if not set
     */
    public Optional<Encoding> encoding() {
        return Optional.ofNullable(encoding);
    }

    /**
     * Returns the key derivation function used to compute the verification bytes.
     *
     * @return an {@link Optional} containing the transformer, or empty if not set
     */
    public Optional<Transformer> transformer() {
        return Optional.ofNullable(transformer);
    }

    /**
     * Returns the arguments supplied to the transformer.
     *
     * @return an unmodifiable list of transformer arguments, never {@code null}
     */
    public List<TransformerArg> transformerArg() {
        return transformerArg == null ? List.of() : Collections.unmodifiableList(transformerArg);
    }

    /**
     * Returns the derived verification bytes.
     *
     * @return an {@link Optional} containing the bytes, or empty if not set
     */
    public Optional<byte[]> transformedData() {
        return Optional.ofNullable(transformedData);
    }

    /**
     * Updates the character encoding applied to the input password.
     *
     * @param encoding the new encoding, or {@code null} to unset the field
     */
    public void setEncoding(Encoding encoding) {
        this.encoding = encoding;
    }

    /**
     * Updates the key derivation function.
     *
     * @param transformer the new transformer, or {@code null} to unset the field
     */
    public void setTransformer(Transformer transformer) {
        this.transformer = transformer;
    }

    /**
     * Updates the arguments supplied to the transformer.
     *
     * @param transformerArg the new argument list, or {@code null} to unset the field
     */
    public void setTransformerArg(List<TransformerArg> transformerArg) {
        this.transformerArg = transformerArg;
    }

    /**
     * Updates the derived verification bytes.
     *
     * @param transformedData the new verification bytes, or {@code null} to unset the field
     */
    public void setTransformedData(byte[] transformedData) {
        this.transformedData = transformedData;
    }

    /**
     * Enumerates the character encodings that may be applied to a password
     * before it is fed into the transformer.
     */
    @ProtobufEnum(name = "UserPassword.Encoding")
    public static enum Encoding {
        /**
         * Standard UTF-8 encoding.
         */
        UTF8(0),
        /**
         * Legacy UTF-8 encoding that preserves a historical bug in older
         * clients, kept for backwards compatibility when verifying passwords
         * stored before the bug was fixed.
         */
        UTF8_BROKEN(1);

        /**
         * Constructs an encoding constant with the given protobuf index.
         *
         * @param index the protobuf index associated with this constant
         */
        Encoding(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Protobuf index associated with this constant.
         */
        final int index;

        /**
         * Returns the protobuf index associated with this constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Enumerates the key derivation functions supported for password verification.
     */
    @ProtobufEnum(name = "UserPassword.Transformer")
    public static enum Transformer {
        /**
         * No derivation is applied; the encoded password bytes are used directly.
         */
        NONE(0),
        /**
         * PBKDF2 with HMAC-SHA-512 as the pseudorandom function.
         */
        PBKDF2_HMAC_SHA512(1),
        /**
         * PBKDF2 with HMAC-SHA-384 as the pseudorandom function.
         */
        PBKDF2_HMAC_SHA384(2);

        /**
         * Constructs a transformer constant with the given protobuf index.
         *
         * @param index the protobuf index associated with this constant
         */
        Transformer(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Protobuf index associated with this constant.
         */
        final int index;

        /**
         * Returns the protobuf index associated with this constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Represents a single named argument supplied to the transformer.
     *
     * <p>The argument value is polymorphic: depending on what the transformer
     * expects it may be either a binary blob (for example a salt) or an
     * unsigned integer (for example an iteration count).
     */
    @ProtobufMessage(name = "UserPassword.TransformerArg")
    public static final class TransformerArg {
        /**
         * Name of the argument, used by the transformer to look up the value.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String key;

        /**
         * Value associated with the argument.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        TransformerArg.Value value;


        /**
         * Constructs a new transformer argument with the given key and value.
         *
         * @param key   the argument name, may be {@code null}
         * @param value the argument value, may be {@code null}
         */
        TransformerArg(String key, Value value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Returns the name of the argument.
         *
         * @return an {@link Optional} containing the name, or empty if not set
         */
        public Optional<String> key() {
            return Optional.ofNullable(key);
        }

        /**
         * Returns the value of the argument.
         *
         * @return an {@link Optional} containing the value, or empty if not set
         */
        public Optional<Value> value() {
            return Optional.ofNullable(value);
        }

        /**
         * Updates the name of the argument.
         *
         * @param key the new name, or {@code null} to unset the field
         */
        public void setKey(String key) {
            this.key = key;
    }

        /**
         * Updates the value of the argument.
         *
         * @param value the new value, or {@code null} to unset the field
         */
        public void setValue(Value value) {
            this.value = value;
    }

        /**
         * Sealed view over the possible shapes of a {@link Value}.
         *
         * <p>A transformer argument value is either a binary blob
         * ({@link AsBlob}) or an unsigned integer ({@link AsUnsignedInteger}).
         * This sealed interface lets client code branch exhaustively on the
         * two concrete cases.
         */
        public sealed interface ValueSpec permits ValueSpec.AsBlob, ValueSpec.AsUnsignedInteger {

            /**
             * Variant of {@link ValueSpec} that carries a binary blob.
             */
            final class AsBlob implements ValueSpec {
                /**
                 * The wrapped binary blob.
                 */
                byte[] asBlob;

                /**
                 * Constructs a new blob variant wrapping the given bytes.
                 *
                 * @param asBlob the bytes to wrap
                 */
                AsBlob(byte[] asBlob) {
                    this.asBlob = asBlob;
                }

                /**
                 * Returns the wrapped binary blob.
                 *
                 * @return the wrapped bytes
                 */
                @ProtobufSerializer
                public byte[] asBlob() {
                    return asBlob;
                }

                /**
                 * Factory method used by the protobuf runtime to construct a
                 * blob variant from its wire representation.
                 *
                 * @param asBlob the bytes to wrap
                 * @return a new blob variant
                 */
                @ProtobufDeserializer
                public static AsBlob of(byte[] asBlob) {
                    return new AsBlob(asBlob);
                }
            }

            /**
             * Variant of {@link ValueSpec} that carries an unsigned integer.
             */
            final class AsUnsignedInteger implements ValueSpec {
                /**
                 * The wrapped unsigned integer value.
                 */
                Integer asUnsignedInteger;

                /**
                 * Constructs a new unsigned-integer variant wrapping the given value.
                 *
                 * @param asUnsignedInteger the integer to wrap
                 */
                AsUnsignedInteger(Integer asUnsignedInteger) {
                    this.asUnsignedInteger = asUnsignedInteger;
                }

                /**
                 * Returns the wrapped unsigned integer.
                 *
                 * @return the wrapped integer
                 */
                @ProtobufSerializer
                public Integer asUnsignedInteger() {
                    return asUnsignedInteger;
                }

                /**
                 * Factory method used by the protobuf runtime to construct an
                 * unsigned-integer variant from its wire representation.
                 *
                 * @param asUnsignedInteger the integer to wrap
                 * @return a new unsigned-integer variant
                 */
                @ProtobufDeserializer
                public static AsUnsignedInteger of(Integer asUnsignedInteger) {
                    return new AsUnsignedInteger(asUnsignedInteger);
                }
            }
        }

        /**
         * Raw storage for a transformer argument value.
         *
         * <p>This type models the protobuf oneof used on the wire, holding
         * either a binary blob or an unsigned integer. Client code should
         * normally use {@link #value()} to obtain the active variant as a
         * {@link ValueSpec} rather than inspect the raw fields directly.
         */
        @ProtobufMessage(name = "UserPassword.TransformerArg.Value")
        public static final class Value {
            /**
             * Binary blob variant, or {@code null} when the integer variant is set.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
            byte[] asBlob;

            /**
             * Unsigned integer variant, or {@code null} when the blob variant is set.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
            Integer asUnsignedInteger;


            /**
             * Constructs a new value with the given variant fields.
             *
             * <p>Exactly one of the two arguments is expected to be non-null.
             *
             * @param asBlob            the blob variant, or {@code null}
             * @param asUnsignedInteger the unsigned-integer variant, or {@code null}
             */
            Value(byte[] asBlob, Integer asUnsignedInteger) {
                this.asBlob = asBlob;
                this.asUnsignedInteger = asUnsignedInteger;
            }

            /**
             * Returns the active variant as a {@link ValueSpec}.
             *
             * <p>The blob variant is preferred when both raw fields are set.
             *
             * @return an {@link Optional} containing the active variant, or empty
             *         when neither raw field is set
             */
            public Optional<? extends ValueSpec> value() {
                if (asBlob != null) return Optional.of(ValueSpec.AsBlob.of(asBlob));
                if (asUnsignedInteger != null) return Optional.of(ValueSpec.AsUnsignedInteger.of(asUnsignedInteger));
                return Optional.empty();
            }

            /**
             * Updates the blob variant.
             *
             * @param asBlob the new bytes, or {@code null} to unset the field
             */
            public void setAsBlob(byte[] asBlob) {
                this.asBlob = asBlob;
    }

            /**
             * Updates the unsigned-integer variant.
             *
             * @param asUnsignedInteger the new integer, or {@code null} to unset the field
             */
            public void setAsUnsignedInteger(Integer asUnsignedInteger) {
                this.asUnsignedInteger = asUnsignedInteger;
    }
        }
    }
}
