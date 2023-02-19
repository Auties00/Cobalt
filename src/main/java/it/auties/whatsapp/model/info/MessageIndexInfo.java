package it.auties.whatsapp.model.info;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.util.JacksonProvider;
import lombok.NonNull;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * An index that contains data about a setting change or an action
 *
 * @param type      the type of the change
 * @param chatJid   the chat where the change happened
 * @param messageId the nullable id of the message regarding the chane
 * @param fromMe    whether the change regards yourself
 */
public record MessageIndexInfo(@NonNull String type, @NonNull Optional<ContactJid> chatJid,
                               @NonNull Optional<String> messageId, boolean fromMe) implements Info, JacksonProvider {
    /**
     * Constructs a new message index info
     *
     * @param type      the type of the change
     * @param chatJid   the chat where the change happened
     * @param messageId the nullable id of the message regarding the chane
     * @param fromMe    whether the change regards yourself
     * @return a non-null message index info
     */
    public static MessageIndexInfo of(@NonNull String type, ContactJid chatJid, String messageId, boolean fromMe) {
        return new MessageIndexInfo(type, Optional.ofNullable(chatJid), Optional.ofNullable(messageId), fromMe);
    }

    /**
     * Constructs a new index info from a json string
     *
     * @param json the non-null json string
     * @return a non-null index info
     */
    public static MessageIndexInfo ofJson(@NonNull String json) {
        try {
            var array = JSON.readValue(json, new TypeReference<List<String>>() {
            });
            var type = getProperty(array, 0).orElseThrow(() -> new NoSuchElementException("Cannot parse MessageSync: missing type"));
            var chatJid = getProperty(array, 1).map(ContactJid::of);
            var messageId = getProperty(array, 2);
            var fromMe = getProperty(array, 3).map(Boolean::parseBoolean).orElse(false);
            return new MessageIndexInfo(type, chatJid, messageId, fromMe);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot parse MessageSync: a json exception occurred while parsing %s", exception);
        }
    }

    private static Optional<String> getProperty(List<String> list, int index) {
        return list.size() > index ? Optional.ofNullable(list.get(index)) : Optional.empty();
    }
}
