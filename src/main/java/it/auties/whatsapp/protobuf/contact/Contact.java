package it.auties.whatsapp.protobuf.contact;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.chat.Chat;
import it.auties.whatsapp.utils.WhatsappUtils;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;
import java.util.Optional;


/**
 * A model class that represents a Contact.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 * This class also offers a builder, accessible using {@link Contact#builder()}.
 */
@AllArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
@ToString
public class Contact {
    /**
     * The non-null unique jid used to identify this contact
     */
    private final @NonNull String jid;

    /**
     * The nullable name specified by this contact when he created a Whatsapp account.
     * Theoretically, it should not be possible for this field to be null as it's required when registering for Whatsapp.
     * Though it looks that it can be removed later, so it's nullable.
     */
    private final String chosenName;

    /**
     * The nullable name associated with this contact on the phone connected with Whatsapp
     */
    private final String name;

    /**
     * The nullable short name associated with this contact on the phone connected with Whatsapp
     * If a name is available, theoretically, also a short name should be
     */
    private final String shortName;

    /**
     * The nullable last known presence of this contact.
     * This field is associated only with the presence of this contact in the corresponding conversation.
     * If, for example, this contact is composing, recording or paused in a group this field will not be affected.
     * Instead, {@link Chat#presences()} should be used.
     * By default, Whatsapp will not send updates about a contact's status unless they send a message or are in the recent contacts.
     * To force Whatsapp to send updates, use {@link Whatsapp#subscribeToContactPresence(Contact)}.
     */
    private ContactStatus lastKnownPresence;

    /**
     * The nullable last time this contact was seen available.
     * Any contact can decide to hide this information in their privacy settings.
     */
    private ZonedDateTime lastSeen;


    /**
     * Constructs a new Contact from a jid
     *
     * @param jid the jid of the new contact
     */
    public static Contact fromJid(@NonNull String jid){
        return Contact.builder()
                .jid(jid)
                .build();
    }

    /**
     * Returns an optional String representing the first valid(non-null) name for this contact.
     * If no valid name is found, an empty optional is returned.
     * In this case, consider using the phone number of this contact as a name.
     * It can be obtained by passing this contact's jid to {@link WhatsappUtils#phoneNumberFromJid(String)}.
     *
     * @return an optional String
     */
    public @NonNull Optional<String> bestName() {
        return Optional.ofNullable(name != null ? name : chosenName);
    }

    /**
     * Returns a non-null String representing the first valid(non-null) name for this contact.
     * If no valid name is found, {@code orElse} is returned.
     *
     * @param orElse a non-null String returned if no valid name is present for this contact
     * @return a non-null String
     */
    public @NonNull String bestName(@NonNull String orElse) {
        return bestName().orElse(orElse);
    }

    /**
     * Returns an optional object wrapping this contact's last known presence.
     * If this information isn't available, an empty optional is returned.
     *
     * @return an optional object wrapping this contact's last known presence
     */
    public @NonNull Optional<ContactStatus> lastKnownPresence() {
        return Optional.ofNullable(lastKnownPresence);
    }

    /**
     * Returns an optional object wrapping the last time this contact was seen.
     * If this information isn't available, an empty optional is returned.
     *
     * @return an optional object wrapping the last time this contact was seen available
     */
    public @NonNull Optional<ZonedDateTime> lastSeen() {
        return Optional.ofNullable(lastSeen);
    }
}
