package it.auties.whatsapp.model.signal.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class WebInfo implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = STRING)
    private String refToken;

    @ProtobufProperty(index = 2, type = STRING)
    private String version;

    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = WebPayload.class)
    private WebPayload payload;

    @ProtobufProperty(index = 4, type = MESSAGE, concreteType = WebInfoWebSubPlatform.class)
    private WebInfoWebSubPlatform platform;

    public WebInfo(@NonNull WebInfoWebSubPlatform platform) {
        this.platform = platform;
    }

    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum WebInfoWebSubPlatform implements ProtobufMessage {
        WEB_BROWSER(0),
        APP_STORE(1),
        WIN_STORE(2),
        DARWIN(3),
        WIN32(4);

        @Getter
        private final int index;

        @JsonCreator
        public static WebInfoWebSubPlatform forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }
}
