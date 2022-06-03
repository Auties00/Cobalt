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

/**
 * A configuration class used to specify the behaviour of {@link Whatsapp}.
 * Each field is immutable, this means that once this class has been initialized, cannot be changed.
 * If reflection is used, it is not guaranteed that the settings will effectively change.
 * This class should be configured using its builder, accessible using {@link WhatsappOptions#newOptions()}.
 * An all arguments' constructor is also available if considered more suitable for the coding style of the project.
 */
@Builder(builderMethodName = "newOptions", buildMethodName = "create")
@With
@Data
@Accessors(fluent = true)
public class WhatsappOptions {
    /**
     * The id of the session
     */
    private final int id;

    /**
     * The version of WhatsappWeb to use
     * If the version is too outdated, the server will refuse to connect
     */
    @Default
    private final Version whatsappVersion = new Version(2, 2212, 7);

    /**
     * The url of the multi device beta socket
     */
    @Default
    @NonNull
    private final String whatsappUrl = "wss://web.whatsapp.com/ws/chat";

    /**
     * The description provided to Whatsapp during the authentication process
     * This should be, for example, the name of your service
     */
    @Default
    @NonNull
    private final String description = "Whatsapp4j";

    /**
     * A flag to specify whether sensible data associated with a session should be serialized.
     * If this flag is set to false, {@link WhatsappOptions#serializationStrategies()} are ignored
     */
    @Default
    private boolean serialization = true;

    /**
     * Describes how much chat history Whatsapp should send when the QR is first scanned
     */
    @Default
    private HistoryLength historyLength = HistoryLength.THREE_MONTHS;

    /**
     * A list of strategies to serialize sensible data associated with a session.
     * By default, all data is serialized synchronously when the socket is closed in a json locally.
     */
    @Default
    @NonNull
    private final Set<SerializationStrategy> serializationStrategies
            = Set.of(SerializationStrategy.onClose());

    /**
     * Constructs a new instance of WhatsappConfiguration with default options
     *
     * @return a new instance of WhatsappConfiguration with the above characteristics
     */
    public static WhatsappOptions defaultOptions() {
        return newOptions().create();
    }

    public static class WhatsappOptionsBuilder {
        public WhatsappOptionsBuilder serializationStrategy(SerializationStrategy strategy){
            if(!serializationStrategies$set){
                this.serializationStrategies$set = true;
                this.serializationStrategies$value = new HashSet<>();
            }

            serializationStrategies$value.add(strategy);
            return this;
        }
    }
}
