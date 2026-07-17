package com.github.auties00.cobalt.wire.linked.contact;

import com.github.auties00.cobalt.wire.linked.business.BusinessHostStorageType;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.device.identity.ADVEncryptionType;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A model representing a WhatsApp contact.
 *
 * <p>Each contact is uniquely identified by a phone-number-based {@link Jid}
 * and may optionally carry a LID (Linked Identifier), which is WhatsApp's
 * privacy-preserving replacement for phone numbers in group chats and
 * communities. Three independent name fields originate from different sources
 * and are all exposed separately:
 * <ul>
 *   <li>the {@linkplain #chosenName() chosen name}, also known as the "push
 *       name", is the display name that the contact has set on their own
 *       WhatsApp profile;</li>
 *   <li>the {@linkplain #fullName() full name} is the name stored for the
 *       contact in the local device's address book;</li>
 *   <li>the {@linkplain #shortName() short name} is a shortened form of the
 *       address-book name, typically the first word.</li>
 * </ul>
 *
 * <p>Presence information such as the {@linkplain #lastKnownPresence() online
 * status} and the {@linkplain #lastSeen() last seen timestamp} is maintained
 * per contact and reflects only the state of the 1:1 conversation with that
 * contact. Group-chat presence is tracked separately, per group. By default the
 * WhatsApp server does not push presence updates for every contact; to
 * receive real-time updates for a specific contact call
 * {@code LinkedWhatsAppClient#subscribeToPresence(JidProvider)}.
 *
 * <p>This class is a local model only. Modifying its fields does not send any
 * request to the WhatsApp servers; it simply reflects the locally cached
 * state. Two contacts are considered equal when they share the same
 * {@linkplain #jid() JID}, regardless of the other fields.
 *
 * @see ContactStatus
 * @see ContactCard
 */
@ProtobufMessage
public final class Contact implements JidProvider {
    /**
     * The non-{@code null} phone-number-based JID that uniquely identifies
     * this contact. The JID encodes the contact's phone number and server
     * information and is the primary identity key of this record.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid jid;

    /**
     * The display name that this contact has chosen for their own WhatsApp
     * profile, commonly referred to as the "push name". This value is set by
     * the contact themselves during registration or through their profile
     * settings. Although a push name is required at registration time, it can
     * be cleared later, so this field is nullable.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String chosenName;

    /**
     * The full name associated with this contact in the local device's
     * address book. This value is {@code null} when the contact is not saved
     * in the address book.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String fullName;

    /**
     * A shortened form of the address-book name for this contact, typically
     * the first alphabetic word of the {@linkplain #fullName() full name}.
     * This value is {@code null} when no full name is available or when the
     * first word does not start with an alphabetic character.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String shortName;

    /**
     * The last known presence state of this contact in the 1:1 conversation.
     * This field tracks whether the contact is online, offline, typing, or
     * recording an audio message, and it is not affected by the contact's
     * activity in group chats. Defaults to {@link ContactStatus#UNAVAILABLE}
     * when no presence data has been received.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    ContactStatus lastKnownPresence;

    /**
     * The epoch-second timestamp of when this contact was last seen online,
     * or {@code null} if the information is unavailable. A contact may hide
     * their last-seen timestamp through their privacy settings, in which case
     * this field remains {@code null} regardless of their actual activity.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.UINT64)
    Long lastSeenSeconds;

    /**
     * Whether this contact has been blocked by the current user. Blocked
     * contacts cannot send messages to the user and do not receive presence
     * updates from the user.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    boolean blocked;

    /**
     * Whether the story-style status updates posted by this contact are muted.
     * When muted, the contact's status updates are hidden from the Status tab
     * but the contact can still send and receive regular messages normally.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    boolean statusMuted;

    /**
     * The LID (Linked Identifier) associated with this contact, or
     * {@code null} if none has been assigned. The LID is an alternative,
     * phone-number-free identifier that WhatsApp uses to address users in
     * group chats and communities without exposing their phone number. Once a
     * LID is known for a contact, it becomes the preferred identifier
     * returned by {@link #toJid()}.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    Jid lid;

    /**
     * The optional username associated with this contact's WhatsApp account.
     * Usernames are user-chosen textual handles that can be used to address a
     * contact without knowing their phone number. When present, the username
     * is typically displayed in the {@code @username} form. This value is
     * {@code null} if the contact has not set a username.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    String username;

    /**
     * The end-to-end encryption scheme used by this contact, or {@code null}
     * if it has not yet been determined. The value indicates whether messages
     * exchanged with the contact travel through the standard Signal protocol
     * end-to-end encryption or through the hosted business-API variant. This
     * information is refreshed during device-list synchronisation.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.ENUM)
    ADVEncryptionType encryptionType;

    /**
     * Whether the local user has opted to share their own phone number with
     * this LID-identified contact. This flag only has meaning for contacts
     * that have a {@linkplain #lid() LID} and controls whether the phone
     * number is visible to the other party in LID-addressed conversations.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.BOOL)
    boolean phoneNumberShared;

    /**
     * Whether this contact record was created through the username-based
     * contact discovery flow rather than through phone number lookup. The
     * flag influences removal semantics: only records added by username have
     * their name fields cleared when the contact is removed from the sync
     * layer.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.BOOL)
    boolean addedByUsername;

    /**
     * The contact's privacy-mode host-storage classification, or {@code null}
     * when it has never been observed.
     *
     * <p>This mirrors the {@code host_storage} attribute carried on the
     * {@code <biz>} envelope of messages and notifications from this contact,
     * decoded against the message-level {@link BusinessHostStorageType}
     * encoding ({@link BusinessHostStorageType#ON_PREMISE} for
     * WhatsApp-hosted, {@link BusinessHostStorageType#FACEBOOK} for
     * Meta-hosted). The value is updated in-place from inbound traffic, with
     * the newer {@link #privacyModeTimestamp timestamp} winning, and a
     * {@link BusinessHostStorageType#FACEBOOK} value (see
     * {@link #hostedOnFacebook()}) is read by the country and Terms-of-Service
     * chat-gating checks.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.ENUM)
    BusinessHostStorageType privacyModeHostStorage;

    /**
     * The timestamp of the most recent privacy-mode update applied to
     * {@link #privacyModeHostStorage}, or {@code null} when no privacy-mode
     * value has ever been observed.
     *
     * <p>This is the {@code privacy_mode_ts} attribute carried alongside the
     * {@code host_storage} attribute on the {@code <biz>} envelope, serialized
     * as an epoch-second value via {@link InstantSecondsMixin}. It is the
     * conflict-resolution key for
     * {@link #setPrivacyMode(BusinessHostStorageType, Instant)}: a later
     * update is ignored when its timestamp is not strictly after the stored
     * one, so out-of-order delivery cannot regress the recorded host storage.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant privacyModeTimestamp;

    /**
     * Constructs a new contact with the given field values.
     *
     * @param jid                the non-{@code null} JID uniquely identifying
     *                           this contact
     * @param chosenName         the push name set by the contact, or
     *                           {@code null}
     * @param fullName           the address-book full name, or {@code null}
     * @param shortName          the address-book short name, or {@code null}
     * @param lastKnownPresence  the last known presence state, or
     *                           {@code null} to default to
     *                           {@link ContactStatus#UNAVAILABLE}
     * @param lastSeenSeconds    the epoch-second timestamp of last seen, or
     *                           {@code null}
     * @param blocked            whether this contact is blocked
     * @param statusMuted        whether this contact's status updates are
     *                           muted
     * @param lid                the LID for this contact, or {@code null}
     * @param username           the username for this contact, or
     *                           {@code null}
     * @param encryptionType     the end-to-end encryption scheme, or
     *                           {@code null}
     * @param phoneNumberShared  whether the local user has shared their phone
     *                           number with this contact
     * @param addedByUsername    whether this contact was added through
     *                           username-based discovery
     * @param privacyModeHostStorage the host-storage classification, or
     *                               {@code null} if never observed
     * @param privacyModeTimestamp   the timestamp of the last privacy-mode
     *                               update, or {@code null} if never observed
     */
    Contact(Jid jid, String chosenName, String fullName, String shortName, ContactStatus lastKnownPresence, Long lastSeenSeconds, boolean blocked, boolean statusMuted, Jid lid, String username, ADVEncryptionType encryptionType, boolean phoneNumberShared, boolean addedByUsername, BusinessHostStorageType privacyModeHostStorage, Instant privacyModeTimestamp) {
        this.jid = Objects.requireNonNull(jid, "value cannot be null");
        this.chosenName = chosenName;
        this.fullName = fullName;
        this.shortName = shortName;
        this.lastKnownPresence = Objects.requireNonNullElse(lastKnownPresence, ContactStatus.UNAVAILABLE);
        this.lastSeenSeconds = lastSeenSeconds;
        this.blocked = blocked;
        this.statusMuted = statusMuted;
        this.lid = lid;
        this.username = username;
        this.encryptionType = encryptionType;
        this.phoneNumberShared = phoneNumberShared;
        this.addedByUsername = addedByUsername;
        this.privacyModeHostStorage = privacyModeHostStorage;
        this.privacyModeTimestamp = privacyModeTimestamp;
    }

    /**
     * Returns the contact's privacy-mode host-storage classification.
     *
     * <p>See {@link #hostedOnFacebook()} for the derived predicate the gating
     * checks actually consume.
     *
     * @return an {@link Optional} carrying the host-storage classification, or
     *         empty if it has never been observed
     */
    public Optional<BusinessHostStorageType> privacyModeHostStorage() {
        return Optional.ofNullable(privacyModeHostStorage);
    }

    /**
     * Returns whether this contact's data is hosted on Facebook (Meta) storage
     * rather than on-premise WhatsApp storage.
     *
     * <p>This is {@code true} only when the observed
     * {@link #privacyModeHostStorage() host storage} is
     * {@link BusinessHostStorageType#FACEBOOK}; it is the cross-Meta
     * interoperability signal consumed by the country and Terms-of-Service
     * inbound-message gating checks.
     *
     * @return {@code true} when the contact is Facebook-hosted, {@code false}
     *         otherwise or when the host storage has never been observed
     */
    public boolean hostedOnFacebook() {
        return privacyModeHostStorage == BusinessHostStorageType.FACEBOOK;
    }

    /**
     * Returns the timestamp of the most recent privacy-mode update applied to
     * this contact.
     *
     * @return an {@link Optional} carrying the last privacy-mode update
     *         instant, or empty if no privacy-mode value has ever been observed
     */
    public Optional<Instant> privacyModeTimestamp() {
        return Optional.ofNullable(privacyModeTimestamp);
    }

    /**
     * Applies an observed privacy-mode update to this contact, keeping the
     * newer of the stored and incoming values.
     *
     * <p>The update is accepted only when no privacy-mode timestamp has been
     * recorded yet, or when {@code timestamp} is strictly after the stored
     * {@link #privacyModeTimestamp}. This mirrors WhatsApp's
     * {@code privacy_mode_ts} conflict resolution so that out-of-order inbound
     * delivery cannot regress the recorded host storage.
     *
     * @param hostStorage the observed host-storage classification
     * @param timestamp   the {@code privacy_mode_ts} instant carried with the
     *                    observation
     * @return {@code true} when the update was applied, {@code false} when it
     *         was discarded as stale
     */
    public boolean setPrivacyMode(BusinessHostStorageType hostStorage, Instant timestamp) {
        if (privacyModeTimestamp != null && !timestamp.isAfter(privacyModeTimestamp)) {
            return false;
        }
        this.privacyModeHostStorage = hostStorage;
        this.privacyModeTimestamp = timestamp;
        return true;
    }

    /**
     * Returns the non-{@code null} phone-number-based JID that uniquely
     * identifies this contact.
     *
     * @return the contact's phone-number-based JID
     */
    public Jid jid() {
        return this.jid;
    }

    /**
     * Returns the best available display name for this contact.
     *
     * <p>The resolution order follows the standard WhatsApp name display
     * logic: the short name from the address book is preferred first,
     * followed by the full address-book name, then the push name chosen by
     * the contact. If none of these are available, the user part of the
     * contact's JID is returned as a last-resort fallback, so the result is
     * always a non-{@code null} string.
     *
     * @return a non-{@code null} display name for this contact
     */
    public String name() {
        if (shortName != null) {
            return shortName;
        }

        if (fullName != null) {
            return fullName;
        }

        if (chosenName != null) {
            return chosenName;
        }

        return jid().user();
    }

    /**
     * Returns the push name that this contact has chosen for their WhatsApp
     * profile.
     *
     * @return an {@code Optional} containing the chosen name, or empty if the
     *         contact has not set one
     */
    public Optional<String> chosenName() {
        return Optional.ofNullable(this.chosenName);
    }

    /**
     * Sets the push name that this contact has chosen for their WhatsApp
     * profile.
     *
     * @param chosenName the chosen name to set, or {@code null} to clear
     */
    public void setChosenName(String chosenName) {
        this.chosenName = chosenName;
    }

    /**
     * Returns the full name associated with this contact in the local address
     * book.
     *
     * @return an {@code Optional} containing the full name, or empty if the
     *         contact is not saved in the address book
     */
    public Optional<String> fullName() {
        return Optional.ofNullable(this.fullName);
    }

    /**
     * Sets the address-book full name for this contact.
     *
     * @param fullName the full name to set, or {@code null} to clear
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Returns the shortened address-book name for this contact.
     *
     * @return an {@code Optional} containing the short name, or empty if not
     *         available
     */
    public Optional<String> shortName() {
        return Optional.ofNullable(this.shortName);
    }

    /**
     * Sets the address-book short name for this contact.
     *
     * @param shortName the short name to set, or {@code null} to clear
     */
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the last known presence state of this contact in the 1:1
     * conversation.
     *
     * @return the last known presence, never {@code null} (defaults to
     *         {@link ContactStatus#UNAVAILABLE})
     */
    public ContactStatus lastKnownPresence() {
        return this.lastKnownPresence;
    }

    /**
     * Sets the last known presence state for this contact.
     *
     * @param lastKnownPresence the presence state to set
     */
    public void setLastKnownPresence(ContactStatus lastKnownPresence) {
        this.lastKnownPresence = lastKnownPresence;
    }

    /**
     * Returns the timestamp of when this contact was last seen online.
     *
     * <p>This value is empty when the contact has never been observed online
     * during the current session, or when the contact's privacy settings hide
     * the last-seen information from the local user.
     *
     * @return an {@code Optional} containing the last-seen instant, or empty
     *         if unavailable
     */
    public Optional<Instant> lastSeen() {
        if (lastSeenSeconds == null || lastSeenSeconds <= 0) {
            return Optional.empty();
        }
        return Optional.of(Instant.ofEpochSecond(lastSeenSeconds));
    }

    /**
     * Sets the last-seen timestamp for this contact.
     *
     * @param lastSeen the instant when the contact was last seen online
     */
    public void setLastSeen(Instant lastSeen) {
        this.lastSeenSeconds = lastSeen.getEpochSecond();
    }

    /**
     * Returns whether this contact has been blocked by the current user.
     *
     * @return {@code true} if the contact is blocked, {@code false} otherwise
     */
    public boolean blocked() {
        return this.blocked;
    }

    /**
     * Sets whether this contact is blocked.
     *
     * @param blocked {@code true} to mark as blocked, {@code false} to
     *                unblock
     */
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    /**
     * Returns whether this contact's story-style status updates are muted.
     *
     * @return {@code true} if status updates are muted, {@code false}
     *         otherwise
     */
    public boolean statusMuted() {
        return statusMuted;
    }

    /**
     * Sets whether this contact's story-style status updates are muted.
     *
     * @param statusMuted {@code true} to mute status updates, {@code false}
     *                    to unmute
     */
    public void setStatusMuted(boolean statusMuted) {
        this.statusMuted = statusMuted;
    }

    /**
     * Returns the LID (Linked Identifier) associated with this contact.
     *
     * @return an {@code Optional} containing the LID, or empty if none has
     *         been assigned
     */
    public Optional<Jid> lid() {
        return Optional.ofNullable(lid);
    }

    /**
     * Sets the LID (Linked Identifier) for this contact.
     *
     * @param lid the LID to set, or {@code null} to clear
     */
    public void setLid(Jid lid) {
        this.lid = lid;
    }

    /**
     * Returns whether this contact has been assigned a LID (Linked
     * Identifier).
     *
     * @return {@code true} if a LID is present, {@code false} otherwise
     */
    public boolean hasLid() {
        return lid != null;
    }

    /**
     * Returns the username associated with this contact's WhatsApp account.
     *
     * @return an {@code Optional} containing the username, or empty if the
     *         contact has not set one
     */
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    /**
     * Sets the username for this contact.
     *
     * @param username the username to set, or {@code null} to clear
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns whether this contact has a username set on their WhatsApp
     * account.
     *
     * @return {@code true} if a username is present, {@code false} otherwise
     */
    public boolean hasUsername() {
        return username != null;
    }

    /**
     * Returns the end-to-end encryption scheme used by this contact.
     *
     * @return an {@code Optional} containing the encryption type, or empty
     *         if the type has not been determined yet
     */
    public Optional<ADVEncryptionType> encryptionType() {
        return Optional.ofNullable(encryptionType);
    }

    /**
     * Sets the end-to-end encryption scheme for this contact.
     *
     * @param encryptionType the encryption type to set, or {@code null} to
     *                       clear
     */
    public void setEncryptionType(ADVEncryptionType encryptionType) {
        this.encryptionType = encryptionType;
    }

    /**
     * Returns whether the local user has shared their own phone number with
     * this contact in LID-addressed conversations.
     *
     * @return {@code true} if the phone number has been shared
     */
    public boolean isPhoneNumberShared() {
        return phoneNumberShared;
    }

    /**
     * Sets whether the local user has shared their own phone number with this
     * contact.
     *
     * @param phoneNumberShared {@code true} if the phone number is shared
     */
    public void setPhoneNumberShared(boolean phoneNumberShared) {
        this.phoneNumberShared = phoneNumberShared;
    }

    /**
     * Returns whether this contact was added through username-based contact
     * discovery.
     *
     * @return {@code true} if the contact was added by username
     */
    public boolean isAddedByUsername() {
        return addedByUsername;
    }

    /**
     * Sets whether this contact was added through username-based contact
     * discovery.
     *
     * @param addedByUsername {@code true} if added by username
     */
    public void setAddedByUsername(boolean addedByUsername) {
        this.addedByUsername = addedByUsername;
    }

    /**
     * Returns whether any of the contact's name fields exactly matches the
     * given string.
     *
     * <p>The full name, short name and chosen name are all compared using
     * {@link String#equals(Object)}. The comparison is case-sensitive.
     *
     * @param name the name to check against, or {@code null}
     * @return {@code true} if the given name matches at least one of the
     *         contact's name fields, {@code false} if {@code name} is
     *         {@code null} or no match is found
     */
    public boolean hasName(String name) {
        return name != null
               && (name.equals(fullName) || name.equals(shortName) || name.equals(chosenName));
    }

    /**
     * Returns the preferred JID for addressing this contact.
     *
     * <p>If a LID has been assigned to the contact, the LID is returned;
     * otherwise the phone-number-based JID is returned. This behaviour
     * supports the progressive migration from phone-number JIDs to LIDs as
     * the primary contact identifier.
     *
     * @return the LID if present, otherwise the phone-number-based JID
     */
    @Override
    public Jid toJid() {
        var lid = this.lid;
        if(lid != null) {
            return lid;
        } else {
            return jid;
        }
    }

    /**
     * Returns a hash code derived from this contact's {@linkplain #jid() JID}.
     *
     * @return the hash code of the JID
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.jid());
    }

    /**
     * Returns whether this contact is equal to the given object.
     *
     * <p>Two contacts are considered equal when they share the same
     * {@linkplain #jid() JID}, regardless of the other fields.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a {@code Contact} with the
     *         same JID
     */
    public boolean equals(Object other) {
        return other instanceof Contact that && Objects.equals(this.jid(), that.jid());
    }
}
