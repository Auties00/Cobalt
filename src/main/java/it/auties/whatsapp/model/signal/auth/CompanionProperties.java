package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.whatsapp.model.sync.HistorySyncConfig;

import static it.auties.protobuf.model.ProtobufType.*;


@ProtobufMessageName("DeviceProps")
public record CompanionProperties(@ProtobufProperty(index = 1, type = STRING) String os,
                                  @ProtobufProperty(index = 2, type = OBJECT) Version version,
                                  @ProtobufProperty(index = 3, type = OBJECT) PlatformType platformType,
                                  @ProtobufProperty(index = 4, type = BOOL) boolean requireFullSync,
                                  @ProtobufProperty(index = 5, type = OBJECT) HistorySyncConfig historySyncConfig) implements ProtobufMessage {

    public enum PlatformType implements ProtobufEnum {

        UNKNOWN(0),
        CHROME(1),
        FIREFOX(2),
        IE(3),
        OPERA(4),
        SAFARI(5),
        EDGE(6),
        DESKTOP(7),
        IPAD(8),
        ANDROID_TABLET(9),
        OHANA(10),
        ALOHA(11),
        CATALINA(12);

        PlatformType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return index;
        }
    }
}
