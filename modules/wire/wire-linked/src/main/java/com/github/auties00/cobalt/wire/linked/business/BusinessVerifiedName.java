package com.github.auties00.cobalt.wire.linked.business;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Locally-cached verified-business-name record describing one WhatsApp
 * Business contact.
 *
 * <p>Whenever the client receives a {@code verified_name} stanza during
 * USync, a contact-info refresh, or a message acknowledgment, it persists
 * one of these records keyed by the contact's {@link #jid()}. The record
 * captures both the static identity attached to the certificate (display
 * name, certificate serial, verification level, issuer-derived API/SMB
 * flags) and the dynamic privacy-mode triplet that controls how messages
 * with this business are processed and which messaging-privacy badge the
 * client renders in chat surfaces.
 *
 * <p>Two instances are considered equal when they share the same
 * {@link #jid()}, allowing the local store to keep a single record per
 * business contact and simply replace the contents on refresh.
 */
@ProtobufMessage
public final class BusinessVerifiedName {
    /**
     * JID that uniquely identifies the business contact this record
     * describes. For LID-migrated accounts this is a LID-based JID;
     * otherwise it is the user portion of the phone-number-based JID. The
     * field is required and acts as the primary key for the record.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid jid;

    /**
     * Verified business name as approved by WhatsApp. This is the canonical
     * display name extracted from the verified-name certificate and shown
     * in the chat header and contact info for verified business contacts.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String name;

    /**
     * Numeric verification level mirrored from the {@code verified_level}
     * attribute supplied by the server. Common values are {@code 0}
     * (unknown), {@code 1} (low), and {@code 2} (high); a {@code 2} unlocks
     * the verified-business badge in chat surfaces.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    int level;

    /**
     * Serial number of the business's verified-name certificate. The client
     * compares the stored serial with the one carried by an incoming
     * verified-name update to detect certificate refreshes that require
     * re-validation of the business identity.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT64)
    long serial;

    /**
     * Whether this business is registered through the WhatsApp Business
     * API (Cloud API or On-Premises API). Derived from the certificate
     * issuer: {@code true} when the issuer is the enterprise issuer
     * ({@code "ent:wa"}), {@code false} otherwise.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    boolean isApi;

    /**
     * Whether this business is registered through the WhatsApp Business
     * App (the small/medium-business consumer app). Derived from the
     * certificate issuer: {@code true} when the issuer is the small-business
     * issuer ({@code "smb:wa"}), {@code false} otherwise.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    boolean isSmb;

    /**
     * Type of infrastructure that hosts the business's data and message
     * processing. Together with {@link #actualActors} and
     * {@link #privacyModeTimestamp} it forms the privacy-mode triplet that
     * resolves to a reduced privacy mode (E2EE, BSP-mediated, Meta-hosted,
     * or Cloud API). May be {@code null} when no privacy mode has been
     * configured server-side.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.ENUM)
    BusinessHostStorageType hostStorage;

    /**
     * Entity that actually reads and writes messages on behalf of the
     * business. Together with {@link #hostStorage} and
     * {@link #privacyModeTimestamp} it forms the privacy-mode triplet. May
     * be {@code null} when no privacy mode has been configured.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.ENUM)
    ActualActorsType actualActors;

    /**
     * Moment at which the privacy-mode configuration of this business was
     * last changed. Wire encoding is seconds since the Unix epoch, converted
     * to {@link Instant} via {@link InstantSecondsMixin}. Together with
     * {@link #hostStorage} and {@link #actualActors} it forms the
     * privacy-mode triplet; the record only exposes a complete privacy
     * mode when all three fields are present.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant privacyModeTimestamp;

    /**
     * Constructs a new {@code BusinessVerifiedName} record. The
     * {@code jid} parameter is required and identifies the contact this
     * record describes; all other parameters may be {@code null} or default
     * when the corresponding field is absent.
     *
     * @param jid                  the business contact's JID; must not be {@code null}
     * @param name                 the verified business name, or {@code null} if absent
     * @param level                the numeric verification level
     * @param serial               the certificate serial number
     * @param isApi                {@code true} if registered through the Business API
     * @param isSmb                {@code true} if registered through the Business App
     * @param hostStorage          the hosting infrastructure type, or {@code null}
     * @param actualActors         the message-processing entity, or {@code null}
     * @param privacyModeTimestamp the privacy-mode change timestamp, or {@code null}
     * @throws NullPointerException if {@code jid} is {@code null}
     */
    BusinessVerifiedName(Jid jid, String name, int level, long serial, boolean isApi, boolean isSmb,
                         BusinessHostStorageType hostStorage, ActualActorsType actualActors,
                         Instant privacyModeTimestamp) {
        this.jid = Objects.requireNonNull(jid, "jid");
        this.name = name;
        this.level = level;
        this.serial = serial;
        this.isApi = isApi;
        this.isSmb = isSmb;
        this.hostStorage = hostStorage;
        this.actualActors = actualActors;
        this.privacyModeTimestamp = privacyModeTimestamp;
    }

    /**
     * Returns the JID that uniquely identifies this business contact.
     *
     * @return the JID, never {@code null}
     */
    public Jid jid() {
        return jid;
    }

    /**
     * Returns the verified business name approved by WhatsApp.
     *
     * @return an {@code Optional} containing the business name, or empty
     *         when no name has been recorded
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the numeric verification level for this business.
     *
     * @return the verification level (typically {@code 0} unknown,
     *         {@code 1} low, {@code 2} high)
     */
    public int level() {
        return level;
    }

    /**
     * Returns the serial number of the business's verified-name
     * certificate.
     *
     * @return the certificate serial number
     */
    public long serial() {
        return serial;
    }

    /**
     * Returns whether this business is registered through the WhatsApp
     * Business API.
     *
     * @return {@code true} if the business uses the Business API (enterprise
     *         tier), {@code false} otherwise
     */
    public boolean isApi() {
        return isApi;
    }

    /**
     * Returns whether this business is registered through the WhatsApp
     * Business App (small/medium-business tier).
     *
     * @return {@code true} if the business uses the Business App,
     *         {@code false} otherwise
     */
    public boolean isSmb() {
        return isSmb;
    }

    /**
     * Returns the type of infrastructure that hosts the business's data.
     *
     * @return an {@code Optional} containing the {@link BusinessHostStorageType},
     *         or empty when no privacy mode has been configured
     */
    public Optional<BusinessHostStorageType> hostStorage() {
        return Optional.ofNullable(hostStorage);
    }

    /**
     * Returns the entity that actually reads and writes messages on
     * behalf of the business.
     *
     * @return an {@code Optional} containing the {@link ActualActorsType},
     *         or empty when no privacy mode has been configured
     */
    public Optional<ActualActorsType> actualActors() {
        return Optional.ofNullable(actualActors);
    }

    /**
     * Returns the moment at which the privacy-mode configuration was last
     * changed.
     *
     * @return an {@code Optional} containing the privacy-mode timestamp,
     *         or empty when no privacy mode has been configured
     */
    public Optional<Instant> privacyModeTimestamp() {
        return Optional.ofNullable(privacyModeTimestamp);
    }

    /**
     * Returns whether this record has a complete privacy-mode triplet
     * configured (all of {@link #hostStorage()}, {@link #actualActors()},
     * and {@link #privacyModeTimestamp()} are present). When all three are
     * present the record can be resolved to a reduced privacy mode (E2EE,
     * BSP-mediated, Meta-hosted, or Cloud API).
     *
     * @return {@code true} if all three privacy-mode fields are non-null,
     *         {@code false} otherwise
     */
    public boolean hasPrivacyMode() {
        return hostStorage != null && actualActors != null && privacyModeTimestamp != null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BusinessVerifiedName that && Objects.equals(jid, that.jid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jid);
    }

    @Override
    public String toString() {
        return "BusinessVerifiedName[jid=" + jid + ", name=" + name + ", level=" + level +
               ", isApi=" + isApi + ", hostStorage=" + hostStorage +
               ", actualActors=" + actualActors + ", privacyModeTimestamp=" + privacyModeTimestamp + ']';
    }

    /**
     * Identifies the entity that actually reads and writes messages on
     * behalf of a WhatsApp Business account, as cached in a
     * {@link BusinessVerifiedName} record. Combined with
     * {@link BusinessHostStorageType} and the privacy-mode timestamp it determines
     * the reduced privacy mode (E2EE, BSP-mediated, or Cloud API) advertised
     * to chat peers.
     */
    @ProtobufEnum
    public enum ActualActorsType {
        /**
         * The business itself processes messages directly, preserving
         * end-to-end encryption between the customer and the business.
         */
        SELF(1),

        /**
         * A Business Solution Provider (BSP) processes messages on behalf
         * of the business, meaning a third party has access to the message
         * content.
         */
        BSP(2),

        /**
         * Cloud API (CAPI) handles message processing on Meta-managed
         * infrastructure, indicating the business is operated through the
         * Cloud API rather than direct or BSP-mediated paths.
         */
        CAPI(3);

        /**
         * Protobuf wire index for this enum constant.
         */
        final int index;

        /**
         * Constructs an {@code ActualActorsType} enum constant bound to
         * the given protobuf wire index.
         *
         * @param index the protobuf wire index
         */
        ActualActorsType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the protobuf wire index of this enum constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return index;
        }
    }
}
