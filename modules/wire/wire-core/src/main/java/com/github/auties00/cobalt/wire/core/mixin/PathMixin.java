package com.github.auties00.cobalt.wire.core.mixin;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufMixin;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.nio.file.Path;

/**
 * Protobuf mixin that bridges the gap between {@link Path} and the {@code String}
 * representation transmitted on the wire.
 *
 * <p>Protobuf has no native filesystem path type, so fields that reference a local file
 * (for example a cached media location) are encoded as a plain string. This mixin lets
 * these fields be declared as {@link Path} in Cobalt's model classes while the serializer
 * continues to read and write the underlying string.
 *
 * <p>The string form is produced by {@link Path#toString()} and parsed back by
 * {@link Path#of(String, String...)}, so the conversion preserves the path exactly as it
 * was originally stored, including its separator style.
 */
@ProtobufMixin
public final class PathMixin {
    /**
     * Serializes a {@link Path} into its string representation for transmission on the
     * wire.
     *
     * <p>The resulting string is the value returned by {@link Path#toString()}, which uses
     * the platform's default separator. A {@code null} path yields a {@code null} string so
     * that optional path fields can remain unset.
     *
     * @param path the path to serialize, or {@code null} if the field is unset
     * @return the path's string form, or {@code null} when {@code path} is {@code null}
     */
    @ProtobufSerializer
    public static String toStringValue(Path path) {
        return path == null ? null : path.toString();
    }

    /**
     * Deserializes a {@code String} read from the wire into a {@link Path}.
     *
     * <p>The input is parsed through {@link Path#of(String, String...)}, which uses the
     * default filesystem to interpret the string. A {@code null} input yields a
     * {@code null} path so that optional protobuf fields can remain absent after decoding.
     *
     * @param value the path string read from the wire, or {@code null} when the field was
     *              absent
     * @return the reconstructed {@link Path}, or {@code null} when {@code value} is
     *         {@code null}
     */
    @ProtobufDeserializer
    public static Path fromString(String value) {
        return value == null ? null : Path.of(value);
    }
}
