package it.auties.whatsapp.model.contact;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.chat.Chat;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * A model class that represents a Contact.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * This class also offers a builder, accessible using {@link Contact#builder()}.
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ToString
public final class Contact implements ProtobufMessage, ContactJidProvider {
    /**
     * The non-null unique jid used to identify this contact
     */
    private final ContactJid jid;

    /**
     * The nullable name specified by this contact when he created a Whatsapp account.
     * Theoretically, it should not be possible for this field to be null as it's required when registering for Whatsapp.
     * Though it looks that it can be removed later, so it's nullable.
     */
    private String chosenName;

    /**
     * The nullable name associated with this contact on the phone connected with Whatsapp
     */
    private String fullName;

    /**
     * The nullable short name associated with this contact on the phone connected with Whatsapp
     * If a name is available, theoretically, also a short name should be
     */
    private String shortName;

    /**
     * The nullable last known presence of this contact.
     * This field is associated only with the presence of this contact in the corresponding conversation.
     * If, for example, this contact is composing, recording or paused in a group this field will not be affected.
     * Instead, {@link Chat#presences()} should be used.
     * By default, Whatsapp will not send updates about a contact's status unless they send a message or are in the recent contacts.
     * To force Whatsapp to send updates, use {@link Whatsapp#subscribeToPresence(ContactJidProvider)}.
     */
    private ContactStatus lastKnownPresence;

    /**
     * The nullable last endTimeStamp this contact was seen available.
     * Any contact can decide to hide this information in their privacy settings.
     */
    private ZonedDateTime lastSeen;

    /**
     * Constructs a new Contact from a provided jid
     *
     * @param jid the non-null jid
     * @return a non-null Contact
     */
    public static Contact ofJid(@NonNull ContactJid jid) {
        return Contact.builder()
                .jid(jid)
                .build();
    }

    /**
     * Returns the best name available for this contact
     *
     * @return a non-null String
     */
    public String name() {
        return shortName != null ?
                shortName :
                fullName != null ?
                        fullName :
                        chosenName != null ?
                                chosenName :
                                jid().user();
    }

    /**
     * Returns an optional object wrapping this contact's last known presence.
     * If this information isn't available, an empty optional is returned.
     *
     * @return an optional object wrapping this contact's last known presence
     */
    public Optional<ContactStatus> lastKnownPresence() {
        return Optional.ofNullable(lastKnownPresence);
    }

    /**
     * Returns an optional object wrapping the last endTimeStamp this contact was seen.
     * If this information isn't available, an empty optional is returned.
     *
     * @return an optional object wrapping the last endTimeStamp this contact was seen available
     */
    public Optional<ZonedDateTime> lastSeen() {
        return Optional.ofNullable(lastSeen);
    }

    /**
     * Returns this object as a jid
     *
     * @return a non-null jid
     */
    @Override
    public @NonNull ContactJid toJid() {
        return jid();
    }
}
