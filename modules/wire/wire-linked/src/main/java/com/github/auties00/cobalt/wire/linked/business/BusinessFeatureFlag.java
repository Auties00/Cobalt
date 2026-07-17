package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model representing a single per-account WhatsApp Business feature flag.
 *
 * <p>WhatsApp Business uses these flags to gradually roll out features to a
 * specific business account, such as catalogs, carts, payments and broadcast
 * variants. Each flag pairs a server-defined feature {@linkplain #name() name}
 * with an {@linkplain #enabled() enabled} boolean that says whether the
 * feature should be exposed to the user.
 *
 * <p>The flag set is delivered by the server during the privacy-and-features
 * sync notification and refreshed whenever the rollout changes. Cobalt
 * persists each flag as an independent record so callers can query a single
 * feature without materialising the whole map.
 *
 * <p>This class is a local model only. Modifying its fields does not send any
 * request to the WhatsApp servers; it simply reflects the locally cached
 * state.
 */
@ProtobufMessage
public final class BusinessFeatureFlag {
    /**
     * The non-{@code null} server-defined name of the feature this flag
     * gates. Names are opaque identifiers chosen by WhatsApp and used as the
     * primary key by Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    /**
     * Whether the feature identified by {@link #name} is currently enabled
     * for this business account.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    boolean enabled;

    /**
     * Constructs a new business feature flag with the given name and enabled
     * state.
     *
     * @param name    the non-{@code null} feature name
     * @param enabled whether the feature is enabled for this account
     */
    BusinessFeatureFlag(String name, boolean enabled) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.enabled = enabled;
    }

    /**
     * Returns the non-{@code null} server-defined feature name that uniquely
     * identifies this flag.
     *
     * @return the feature name
     */
    public String name() {
        return name;
    }

    /**
     * Returns whether the feature identified by {@link #name()} is currently
     * enabled for this business account.
     *
     * @return {@code true} if the feature is enabled
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Sets whether the feature is enabled for this business account.
     *
     * @param enabled the new enabled flag
     * @return this feature flag instance for method chaining
     */
    public BusinessFeatureFlag setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Returns a hash code derived from this flag's {@linkplain #name() name}.
     *
     * @return the hash code of the feature name
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    /**
     * Returns whether this feature flag is equal to the given object.
     *
     * <p>Two business feature flags are considered equal when they share the
     * same {@linkplain #name() feature name}, regardless of their enabled
     * state.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code BusinessFeatureFlag}
     *         with the same name
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof BusinessFeatureFlag that && Objects.equals(this.name, that.name);
    }
}
