package it.auties.whatsapp.model.signal.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

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

    @ProtobufProperty(index = 3, type = MESSAGE, implementation = WebPayload.class)
    private WebPayload payload;

    @ProtobufProperty(index = 4, type = MESSAGE, implementation = WebInfo.WebInfoWebSubPlatform.class)
    private WebInfoWebSubPlatform platform;

    public WebInfo(WebInfoWebSubPlatform platform) {
        this.platform = platform;
    }

    @AllArgsConstructor
    @Accessors(fluent = true)
    @ProtobufName("WebSubPlatform")
    public enum WebInfoWebSubPlatform implements ProtobufMessage {
        WEB_BROWSER(0),
        APP_STORE(1),
        WIN_STORE(2),
        DARWIN(3),
        WIN32(4);
        
        @Getter
        private final int index;

        @JsonCreator
        public static WebInfoWebSubPlatform of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(null);
        }
    }
}