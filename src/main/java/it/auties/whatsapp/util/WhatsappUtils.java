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
     * Request counter, decoupled from {@link it.auties.whatsapp.manager.WhatsappStore}
     */
    private final AtomicLong requestCounter = new AtomicLong();

    /**
     * The request tag, used to create messages
     */
    private final String requestTag = BinaryArray.random(12).toHex().toLowerCase(Locale.ROOT);

    /**
     * Returns a random message id
     *
     * @return a non-null ten character String
     */
    public String randomId() {
        return BinaryArray.random(10).toHex();
    }

    /**
     * Returns a request tag built using {@code configuration}
     *
     * @return a non-null String
     */
    public String buildRequestTag() {
        return "%s-%s".formatted(requestTag, requestCounter.getAndIncrement());
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

    /**
     * Reads the nullable id of the provided node
     *
     * @param node the input node
     * @return a nullable string
     */
    public String readNullableId(@NonNull Node node){
        return node.attributes().getString("id", null);
    }
}
