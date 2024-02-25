package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.OBJECT;
import static it.auties.protobuf.model.ProtobufType.STRING;

@ProtobufMessageName("ClientPayload.WebInfo")
public record WebInfo(@ProtobufProperty(index = 1, type = STRING) String refToken,
                      @ProtobufProperty(index = 2, type = STRING) String version,
                      @ProtobufProperty(index = 3, type = OBJECT) WebPayload webPayload,
                      @ProtobufProperty(index = 4, type = OBJECT) Platform webSubPlatform) implements ProtobufMessage {

    @ProtobufMessageName("ClientPayload.UserAgent.Platform")
    public enum Platform implements ProtobufEnum {

        WEB_BROWSER(0),
        APP_STORE(1),
        WIN_STORE(2),
        DARWIN(3),
        WIN32(4);

        Platform(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }
    }
}
