package it.auties.whatsapp.api;

import it.auties.whatsapp.model.signal.auth.Version;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;
import lombok.With;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * A configuration class used to specify the behaviour of {@link Whatsapp}
 */
@Builder(builderMethodName = "newOptions", buildMethodName = "create")
@With
@Data
@Accessors(fluent = true)
public class WhatsappOptions {
    /**
     * Last known version of Whatsapp
     */
    private static final Version WHATSAPP_VERSION = new Version(2, 2212, 7);

    /**
     * The id of the session.
     * This id needs to be unique.
     */
    private final int id;

    /**
     * The version of WhatsappWeb to use.
     * If the version is too outdated, the server will refuse to connect.
     */
    @Default
    private final Version version = Version.latest(WHATSAPP_VERSION);

    /**
     * The url of the socket
     */
    @Default
    @NonNull
    private final String url = "wss://web.whatsapp.com/ws/chat";

    /**
     * The description provided to Whatsapp during the authentication process.
     * This should be, for example, the name of your service.
     * By default, it's WhatsappWeb4j.
     */
    @Default
    @NonNull
    private final String description = "WhatsappWeb4j";
    /**
     * A list of strategies to serialize sensible data associated with a session.
     * By default, all data is serialized synchronously when the socket is closed in a json locally.
     */
    @Default
    @NonNull
    private final Set<SerializationStrategy> serializationStrategies = Set.of(SerializationStrategy.onClose());
    /**
     * A flag to specify whether sensible data associated with a session should be serialized.
     * If this flag is set to false, {@link WhatsappOptions#serializationStrategies()} are ignored.
     * By default, serialization is enabled.
     */
    @Default
    private boolean serialization = true;
    /**
     * Describes how much chat history Whatsapp should send when the QR is first scanned.
     * By default, three months are chosen.
     */
    @Default
    private HistoryLength historyLength = HistoryLength.THREE_MONTHS;

    /**
     * Handles failures in the WebSocket.
     * Returns true if the current connection should be killed and a new one created.
     * Otherwise, the connection will not be killed, but more failures may be caused by the latter.
     * By default, the reason is always disregarded and a new connection is created.
     */
    @Default
    private Function<String, Boolean> failureHandler = (reason) -> true;

    /**
     * Constructs a new instance of WhatsappConfiguration with default options
     *
     * @return a non-null options configuration
     */
    public static WhatsappOptions defaultOptions() {
        return newOptions().create();
    }

    public static class WhatsappOptionsBuilder {
        public WhatsappOptionsBuilder serializationStrategy(SerializationStrategy strategy) {
            if (!serializationStrategies$set) {
                this.serializationStrategies$set = true;
                this.serializationStrategies$value = new HashSet<>();
            }

            serializationStrategies$value.add(strategy);
            return this;
        }
    }
}
