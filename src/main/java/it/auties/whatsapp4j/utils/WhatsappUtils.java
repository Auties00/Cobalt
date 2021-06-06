package it.auties.whatsapp4j.utils;

import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.protobuf.contact.Contact;
import it.auties.whatsapp4j.protobuf.model.Node;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This utility class provides helper functionality to easily extract data out of Whatsapp models or raw protobuf messages
 * The use of accessors in those classes is preferred if they are easily available
 */
@UtilityClass
public class WhatsappUtils {
    /**
     * A singleton instance of {@link WhatsappDataManager}
     */
    private final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();

    /**
     * Returns the phone number associated with a jid
     *
     * @param jid the input jid
     * @return a non null String
     */
    public @NotNull String phoneNumberFromJid(@NotNull String jid) {
        return jid.split("@", 2)[0];
    }

    /**
     * Parses c.us jids to standard whatsapp jids
     *
     * @param jid the input jid
     * @return a non null String
     */
    public @NotNull String parseJid(@NotNull String jid) {
        return jid.replaceAll("@c.us", "@s.whatsapp.net");
    }

    /**
     * Returns a random message id
     *
     * @return a non null ten character String
     */
    public @NotNull String randomId() {
        return BinaryArray.random(10).toHex();
    }

    /**
     * Returns a request tag built using {@code configuration}
     *
     * @param configuration the configuration to use to build the message
     * @return a non null String
     */
    public @NotNull String buildRequestTag(@NotNull WhatsappConfiguration configuration) {
        return "%s.--%s".formatted(configuration.requestTag(), MANAGER.tagAndIncrement());
    }

    /**
     * Returns a ZoneDateTime for {@code time}
     *
     * @param time the time in seconds since {@link Instant#EPOCH}
     * @return a non null empty optional if the {@code time} isn't 0
     */
    public @NotNull Optional<ZonedDateTime> parseWhatsappTime(long time) {
        return time == 0 ? Optional.empty() : Optional.of(ZonedDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.systemDefault()));
    }

    /**
     * Returns a boolean that determines whether {@code jid} is a group
     *
     * @param jid the input jid
     * @return true if {@code jid} is a group
     */
    public boolean isGroup(@NotNull String jid) {
        return jid.contains("-");
    }

    /**
     * Returns a List of WhatsappNodes that represent {@code contacts}
     *
     * @param contacts any number of contacts to convert
     * @return a non null List of WhatsappNodes
     * @throws IllegalArgumentException if {@code contacts} is empty
     */
    public @NotNull List<Node> jidsToParticipantNodes(@NotNull Contact... contacts) {
        return jidsToParticipantNodes(Arrays.stream(contacts).map(Contact::jid).toArray(String[]::new));
    }

    /**
     * Returns a List of WhatsappNodes that represent {@code jids}
     *
     * @param jids any number of jids to convert
     * @return a non null List of WhatsappNodes
     * @throws IllegalArgumentException if {@code jids} is empty
     */
    public @NotNull List<Node> jidsToParticipantNodes(@NotNull String... jids) {
        return Arrays.stream(jids).map(jid -> new Node("participant", Map.of("jid", jid), null)).toList();
    }

    /**
     * Returns a binary array containing an encrypted media
     *
     * @param url the url of the encrypted media to download
     * @return a non empty optional if the media is available
     */
    public @NotNull Optional<BinaryArray> readEncryptedMedia(@NotNull String url) {
        try {
            return Optional.of(BinaryArray.forArray(new URL(url).openStream().readAllBytes()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Returns a map of attributes
     *
     * @param entries the attributes
     * @return a non null Map of attributes
     */
    @SafeVarargs
    public @NotNull Map<String, String> attributes(@NotNull Map.Entry<String, String>... entries) {
        return Arrays.stream(entries).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Returns a new attribute from a key and a value
     *
     * @param key   the non null key
     * @param value the non null value
     * @return a non null Entry
     */
    public @NotNull Map.Entry<String, String> attr(@NotNull String key, @NotNull Object value) {
        return Map.entry(key, String.valueOf(value));
    }
}
