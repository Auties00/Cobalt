package com.github.auties00.cobalt.wire.linked.business.ctwa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model representing the per-customer click-to-WhatsApp (CTWA) data
 * sharing preference for a single business-account customer.
 *
 * <p>When a user clicks a Click-to-WhatsApp ad on Facebook or Instagram,
 * the conversation that follows is enriched with attribution metadata so
 * the business can link the chat back to the original ad. The user can
 * choose to opt out of sharing this data with the business; the chosen
 * value is stored per business-account customer.
 *
 * <p>Each entry pairs the customer's {@linkplain #accountLid() account LID}
 * with the {@linkplain #enabled() enabled flag} reported by the server.
 * Cobalt persists each preference independently so callers can resolve a
 * single customer's choice without iterating the whole map.
 *
 * <p>This class is a local model only. Modifying its fields does not send any
 * request to the WhatsApp servers; it simply reflects the locally cached
 * state.
 */
@ProtobufMessage
public final class CtwaDataSharingPreference {
    /**
     * The non-{@code null} raw LID string identifying the business-account
     * customer this preference belongs to. Used as the primary key by
     * Cobalt's store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String accountLid;

    /**
     * Whether per-customer CTWA data sharing is currently enabled for the
     * customer identified by {@link #accountLid}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    boolean enabled;

    /**
     * Constructs a new CTWA data-sharing preference with the given
     * account LID and enabled flag.
     *
     * @param accountLid the non-{@code null} account LID raw string
     * @param enabled    the enabled flag
     */
    CtwaDataSharingPreference(String accountLid, boolean enabled) {
        this.accountLid = Objects.requireNonNull(accountLid, "accountLid cannot be null");
        this.enabled = enabled;
    }

    /**
     * Returns the non-{@code null} raw LID string identifying the customer
     * this preference belongs to.
     *
     * @return the account LID raw string
     */
    public String accountLid() {
        return accountLid;
    }

    /**
     * Returns whether per-customer CTWA data sharing is currently enabled
     * for this customer.
     *
     * @return {@code true} if data sharing is enabled
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Sets whether data sharing is enabled for this customer.
     *
     * @param enabled the new enabled flag
     * @return this preference instance for method chaining
     */
    public CtwaDataSharingPreference setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Returns a hash code derived from this preference's
     * {@linkplain #accountLid() account LID}.
     *
     * @return the hash code of the account LID
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(accountLid);
    }

    /**
     * Returns whether this preference is equal to the given object.
     *
     * <p>Two preferences are considered equal when they share the same
     * {@linkplain #accountLid() account LID}, regardless of the enabled
     * flag.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code CtwaDataSharingPreference}
     *         with the same account LID
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof CtwaDataSharingPreference that && Objects.equals(this.accountLid, that.accountLid);
    }
}
