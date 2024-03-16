package it.auties.whatsapp.registration.gcm;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

record ChromeBuild(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        Platform platform,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String chromeVersion,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        Channel channel
) implements ProtobufMessage {
    enum Platform implements ProtobufEnum {
        PLATFORM_WIN(1),
        PLATFORM_MAC(2),
        PLATFORM_LINUX(3),
        PLATFORM_CROS(4),
        PLATFORM_IOS(5),
        PLATFORM_ANDROID(6);

        final int index;
        Platform(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }

    enum Channel implements ProtobufEnum {
        CHANNEL_STABLE(1),
        CHANNEL_BETA(2),
        CHANNEL_DEV(3),
        CHANNEL_CANARY(4),
        CHANNEL_UNKNOWN(5);

        final int index;
        Channel(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}
