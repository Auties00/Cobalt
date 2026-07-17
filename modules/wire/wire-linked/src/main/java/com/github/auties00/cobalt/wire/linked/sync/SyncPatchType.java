package com.github.auties00.cobalt.wire.linked.sync;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Identifies the partition (or "patch type") of an app-state mutation.
 *
 * <p>App-state mutations are split across a small fixed family of
 * collections so that critical changes (block-list updates, unblock
 * follow-ups) can be fetched ahead of bulkier non-critical changes
 * during bootstrap. The five collections also act as the unit of
 * version/integrity tracking: each one keeps its own monotonic version
 * counter and LT-Hash, persisted separately by the sync state machine.
 *
 * <p>The lowercase form of each constant name (for example
 * {@code "critical_block"}) is the wire token used by the relay; the
 * byte encoding of that token is cached at construction so hot paths do
 * not repeatedly allocate.
 */
@ProtobufEnum
public enum SyncPatchType {
    /**
     * Critical collection dedicated to block-list mutations. Applied
     * first during bootstrap so the user's privacy choices are enforced
     * before any other state.
     */
    CRITICAL_BLOCK(0),

    /**
     * Critical collection holding low-priority unblock-follow-up
     * mutations that still need to be applied before the regular
     * collections.
     */
    CRITICAL_UNBLOCK_LOW(1),

    /**
     * Non-critical collection carrying high-priority mutations that
     * should converge quickly across companion devices.
     */
    REGULAR_HIGH(2),

    /**
     * Non-critical collection carrying bulky low-priority mutations
     * that may be deferred without user-visible impact.
     */
    REGULAR_LOW(3),

    /**
     * Non-critical collection carrying the bulk of regular mutations.
     */
    REGULAR(4);

    /**
     * Lookup table mapping the lowercase name string to the matching
     * enum constant, used by {@link #of(String)}.
     */
    private static final Map<String, SyncPatchType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(SyncPatchType::toString, Function.identity()));

    /**
     * Protobuf wire index for this enum constant.
     */
    final int index;

    /**
     * Cached byte encoding of the lowercase wire name, reused to avoid
     * allocating on every serialisation.
     */
    private final byte[] bytes;

    /**
     * Constructs a new enum constant with the given protobuf wire index.
     *
     * @param index the protobuf wire index
     */
    SyncPatchType(@ProtobufEnumIndex int index) {
        this.index = index;
        this.bytes = toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns the protobuf wire index for this enum constant.
     *
     * @return the protobuf wire index
     */
    public int index() {
        return index;
    }

    /**
     * Resolves a collection by its lowercase wire name.
     *
     * @param name the lowercase wire name, or {@code null}
     * @return an optional containing the matching constant, or empty
     *         when {@code name} is {@code null} or does not match a
     *         known collection
     */
    public static Optional<SyncPatchType> of(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(BY_NAME.get(name));
    }

    /**
     * Returns the lowercase wire name of this collection.
     *
     * @return the lowercase collection name
     */
    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the cached byte encoding of the lowercase wire name. The
     * returned array is the cached buffer; callers must not mutate it.
     *
     * @return the cached name bytes
     */
    public byte[] toBytes() {
        return bytes;
    }

    /**
     * Returns whether this collection is classified as a critical
     * collection.
     *
     * <p>During bootstrap only critical collections
     * ({@link #CRITICAL_BLOCK} and {@link #CRITICAL_UNBLOCK_LOW}) are
     * processed from server sync notifications so that privacy and
     * safety related mutations are applied before the rest of the app
     * state.
     *
     * @return {@code true} if this is a critical collection
     */
    public boolean isCritical() {
        return this == CRITICAL_BLOCK || this == CRITICAL_UNBLOCK_LOW;
    }
}
