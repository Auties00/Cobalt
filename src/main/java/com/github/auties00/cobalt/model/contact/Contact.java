package com.github.auties00.cobalt.model.contact;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidProvider;
import com.github.auties00.cobalt.util.Clock;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a Contact. This class is only a model, this means that changing its
 * values will have no real effect on WhatsappWeb's servers.
 */
@ProtobufMessage
public final class Contact implements JidProvider {
    /**
     * The non-null unique value used to identify this contact
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid jid;

    /**
     * The nullable name specified by this contact when he created a Whatsapp account. Theoretically,
     * it should not be possible for this field to be null as it's required when registering for
     * Whatsapp. Though it looks that it can be removed later, so it's nullable.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String chosenName;

    /**
     * The nullable name associated with this contact on the phone connected with Whatsapp
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String fullName;

    /**
     * The nullable short name associated with this contact on the phone connected with Whatsapp If a
     * name is available, theoretically, also a short name should be
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String shortName;

    /**
     * The nullable last known presence of this contact. This field is associated only with the
     * presence of this contact in the corresponding conversation. If, for example, this contact is
     * composing, recording or paused in a group this field will not be affected. Instead,
     * {@link Chat#getPresence(JidProvider)} should be used. By default, Whatsapp will not send updates about a
     * contact's status unless they send a message or are in the recent contacts. To force Whatsapp to
     * send updates, use {@link WhatsAppClient#subscribeToPresence(JidProvider)}.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    ContactStatus lastKnownPresence;

    /**
     * The nullable last seconds this contact was seen available. Any contact can decide to hide this
     * information in their privacy settings.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.UINT64)
    long lastSeenSeconds;

    /**
     * Whether this contact is blocked
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    boolean blocked;

    /**
     * Whether the status updates of the contact are muted
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    boolean statusMuted;

    /**
     * The LID (Long ID) associated with this contact for the LID migration.
     * This maps the phone number JID to the corresponding LID.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    Jid lid;

    Contact(Jid jid, String chosenName, String fullName, String shortName, ContactStatus lastKnownPresence, long lastSeenSeconds, boolean blocked, boolean statusMuted, Jid lid) {
        this.jid = Objects.requireNonNull(jid, "value cannot be null");
        this.chosenName = chosenName;
        this.fullName = fullName;
        this.shortName = shortName;
        this.lastKnownPresence = Objects.requireNonNullElse(lastKnownPresence, ContactStatus.UNAVAILABLE);
        this.lastSeenSeconds = lastSeenSeconds;
        this.blocked = blocked;
        this.statusMuted = statusMuted;
        this.lid = lid;
    }

    public Jid jid() {
        return this.jid;
    }

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

    public long lastSeenSeconds() {
        return lastSeenSeconds;
    }

    public Optional<ZonedDateTime> lastSeen() {
        return Clock.parseSeconds(lastSeenSeconds);
    }

    public Optional<String> chosenName() {
        return Optional.ofNullable(this.chosenName);
    }

    public Optional<String> fullName() {
        return Optional.ofNullable(this.fullName);
    }

    public Optional<String> shortName() {
        return Optional.ofNullable(this.shortName);
    }

    public ContactStatus lastKnownPresence() {
        return this.lastKnownPresence;
    }

    public boolean blocked() {
        return this.blocked;
    }

    public void setChosenName(String chosenName) {
        this.chosenName = chosenName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public void setLastKnownPresence(ContactStatus lastKnownPresence) {
        this.lastKnownPresence = lastKnownPresence;
    }

    public void setLastSeen(ZonedDateTime lastSeen) {
        this.lastSeenSeconds = lastSeen.toEpochSecond();
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean statusMuted() {
        return statusMuted;
    }

    public void setStatusMuted(boolean statusMuted) {
        this.statusMuted = statusMuted;
    }

    /**
     * Returns the LID (Long ID) associated with this contact
     *
     * @return an optional LID
     */
    public Optional<Jid> lid() {
        return Optional.ofNullable(lid);
    }

    /**
     * Sets the LID (Long ID) for this contact
     *
     * @param lid the LID to set
     */
    public void setLid(Jid lid) {
        this.lid = lid;
    }

    /**
     * Returns whether this contact has a LID mapping
     *
     * @return true if a LID is set
     */
    public boolean hasLid() {
        return lid != null;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.jid());
    }

    public boolean equals(Object other) {
        return other instanceof Contact that && Objects.equals(this.jid(), that.jid());
    }

    @Override
    public Jid toJid() {
        var lid = this.lid;
        if(lid != null) {
            return lid;
        } else {
            return jid;
        }
    }

    public boolean hasName(String name) {
        return name != null
               && (name.equals(fullName) || name.equals(shortName) || name.equals(chosenName));
    }
}
