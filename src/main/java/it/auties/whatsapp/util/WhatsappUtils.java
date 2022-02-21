package it.auties.whatsapp.util;

import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.manager.WhatsappPreferences;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static java.time.ZoneId.systemDefault;
import static java.time.ZonedDateTime.ofInstant;

/**
 * This utility class provides helper functionality to easily extract data out of Whatsapp models or raw protobuf messages
 * The use of accessors in those classes is preferred if they are easily available
 */
@UtilityClass
public class WhatsappUtils implements JacksonProvider {
    private final WhatsappPreferences SESSIONS = WhatsappPreferences.of("sessions.json");

    @SafeVarargs
    public <T> List<T> combine(List<T>... input){
        return Stream.of(input)
                .flatMap(Collection::stream)
                .toList();
    }

    public LinkedList<Integer> knownIds() {
        return Objects.requireNonNullElseGet(SESSIONS.readJson(new TypeReference<>() {}),
                LinkedList::new);
    }

    public int saveId(int id){
        var knownIds = knownIds();
        if(knownIds.contains(id)){
            return id;
        }

        knownIds.add(id);
        SESSIONS.writeJsonAsync(knownIds);
        return id;
    }

    /**
     * Returns a random message jid
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
