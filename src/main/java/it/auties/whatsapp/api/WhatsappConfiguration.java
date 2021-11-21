package it.auties.whatsapp.api;

import it.auties.whatsapp.binary.BinaryArray;
import it.auties.whatsapp.utils.WhatsappUtils;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.function.Function;

/**
 * A configuration class used to specify the behaviour of {@link Whatsapp}.
 * Each field is immutable, this means that once this class has been initialized, cannot be changed.
 * If reflection is used, it is not guaranteed that the settings will effectively change.
 * This class should be configured using its builder, accessible using {@link WhatsappConfiguration#builder()}.
 * An all arguments' constructor is also available if considered more suitable for the coding style of the project.
 */
@Builder
@Data
@Accessors(fluent = true)
public class WhatsappConfiguration {
    /**
     * The version of WhatsappWeb to use
     * If the version is too outdated, the server will refuse to connect
     */
    @Default
    private final @NonNull String whatsappVersion = "2.2126.14";
    
    /**
     * The url of WhatsappWeb's WebSocket
     * This may change based on the region this API is being used in
     */
    @Default
    private final @NonNull String whatsappUrl = "wss://web.whatsapp.com/ws";

    /**
     * The url of the multi device beta socket
     * This may change based on the region this API is being used in
     */
    @Default
    private final @NonNull String whatsappUrlBeta = "wss://web.whatsapp.com/ws/chat";

    /**
     * The tag used to send messages to WhatsappWeb's WebSocket
     * The tag used to send binary requests to WhatsappWeb's WebSocket after the authentication process has succeeded is built using {@link WhatsappUtils#buildRequestTag(WhatsappConfiguration)}
     * It is important to use a pseudo random string as using the same tag two times in a binary request, even in different sessions, will make the request fail
     */
    @Default
    private final @NonNull String requestTag = BinaryArray.random(12).toHex();

    /**
     * The description provided to Whatsapp during the authentication process
     * This should be, for example, the name of your service
     */
    @Default
    private final @NonNull String description = "Whatsapp4j";

    /**
     * The short description provided to Whatsapp during the authentication process
     * This should be, for example, an acronym for your service
     */
    @Default
    private final @NonNull String shortDescription = "W4J";

    /**
     * This property determines whether the requests sent to WhatsappWeb's WebSocket should be sent asynchronously or not
     * It is recommended to set this field to true as it helps with performance while not using necessarily more resources
     */
    @Default
    private final boolean async = true;

    /**
     * Constructs a new instance of WhatsappConfiguration with default options
     *
     * @return a new instance of WhatsappConfiguration with the above characteristics
     */
    public static @NonNull WhatsappConfiguration defaultOptions() {
        return WhatsappConfiguration.builder().build();
    }
}
