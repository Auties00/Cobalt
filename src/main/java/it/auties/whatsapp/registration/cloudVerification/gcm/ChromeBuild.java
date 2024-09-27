package it.auties.whatsapp.registration.cloudVerification.gcm;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage
record ChromeBuild(
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        Platform platform,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String chromeVersion,
        @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
        Channel channel
) {
    @ProtobufEnum
    enum Platform {
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

    @ProtobufEnum
    enum Channel {
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
