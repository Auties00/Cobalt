package it.auties.whatsapp.api;

import it.auties.whatsapp.model.signal.auth.Version;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.Set;

/**
 * A configuration class used to specify the behaviour of {@link Whatsapp}.
 * Each field is immutable, this means that once this class has been initialized, cannot be changed.
 * If reflection is used, it is not guaranteed that the settings will effectively change.
 * This class should be configured using its builder, accessible using {@link WhatsappOptions#builder()}.
 * An all arguments' constructor is also available if considered more suitable for the coding style of the project.
 */
@Builder
@Data
@Accessors(fluent = true)
public class WhatsappOptions {
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
     * The short description provided to Whatsapp during the authentication process
     * This should be, for example, an acronym for your service
     */
    @Default
    @NonNull
    private final String shortDescription = "W4J";

    /**
     * A debug flag to print incoming and out-coming nodes
     */
    @Default
    private boolean debug = true;

    /**
     * A flag to specify whether sensible data associated with a session should be serialized.
     * If this flag is set to false, {@link WhatsappOptions#serializationStrategies()} are ignored
     */
    @Default
    private boolean serialization = true;

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
        return WhatsappOptions.builder().build();
    }
}
