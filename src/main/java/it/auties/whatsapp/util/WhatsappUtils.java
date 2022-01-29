package it.auties.whatsapp.util;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.socket.Node;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static java.time.ZoneId.systemDefault;
import static java.time.ZonedDateTime.ofInstant;

/**
 * This utility class provides helper functionality to easily extract data out of Whatsapp models or raw protobuf messages
 * The use of accessors in those classes is preferred if they are easily available
 */
@UtilityClass
public class WhatsappUtils {
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
