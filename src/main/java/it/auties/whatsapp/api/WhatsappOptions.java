package it.auties.whatsapp.api;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

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
    private final int[] whatsappVersion = new int[]{2, 2126, 12};

    /**
     * The url of the multi device beta socket
     */
    @Default
    @NonNull
    private final String whatsappUrlBeta = "wss://web.whatsapp.com/ws/chat";

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
     * Constructs a new instance of WhatsappConfiguration with default options
     *
     * @return a new instance of WhatsappConfiguration with the above characteristics
     */
    public static @NonNull WhatsappOptions defaultOptions() {
        return WhatsappOptions.builder().build();
    }
}
