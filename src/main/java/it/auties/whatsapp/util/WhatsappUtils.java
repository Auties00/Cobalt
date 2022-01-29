package it.auties.whatsapp.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.socket.Node;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.time.ZoneId.systemDefault;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.prefs.Preferences.userRoot;

/**
 * This utility class provides helper functionality to easily extract data out of Whatsapp models or raw protobuf messages
 * The use of accessors in those classes is preferred if they are easily available
 */
@UtilityClass
public class WhatsappUtils implements JacksonProvider {
    private final String ID_PATH = WhatsappUtils.class.getName();

    public String createPreferencesPath(String path, Object id) {
        return "%s$%s".formatted(path, id);
    }

    public LinkedList<Integer> knownIds() {
        try {
            var json = userRoot().get(ID_PATH, "[]");
            return JACKSON.readValue(json, new TypeReference<>() {});
        }catch (JsonProcessingException exception){
            throw new UncheckedIOException("Cannot read IDs", exception);
        }
    }

    public int saveId(int id){
        try {
            var knownIds = knownIds();
            if(knownIds.contains(id)){
                return id;
            }

            knownIds.add(id);
            userRoot().put(ID_PATH, JACKSON.writeValueAsString(knownIds));
            return id;
        }catch (JsonProcessingException exception){
            throw new UncheckedIOException("Cannot serialize IDs", exception);
        }
    }

    /**
     * Returns a random message id
     *
     * @return a non-null ten character String
     */
    public String randomId() {
        return BinaryArray.random(10).toHex();
    }

    /**
     * Returns a ZoneDateTime for {@code endTimeStamp}
     *
     * @param input the endTimeStamp in seconds since {@link Instant#EPOCH}
     * @return a non-null empty optional if the {@code endTimeStamp} isn't 0
     */
    public Optional<ZonedDateTime> parseWhatsappTime(long input) {
        return Optional.of(input)
                .filter(time -> time != 0)
                .map(time -> ofInstant(Instant.ofEpochSecond(time), systemDefault()));
    }
}
