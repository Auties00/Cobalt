package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufEnum;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.OBJECT;
import static it.auties.protobuf.model.ProtobufType.STRING;

@ProtobufMessageName("ClientPayload.UserAgent")
public record UserAgent(@ProtobufProperty(index = 1, type = OBJECT) PlatformType platform,
                        @ProtobufProperty(index = 2, type = OBJECT) Version appVersion,
                        @ProtobufProperty(index = 3, type = STRING) String mcc,
                        @ProtobufProperty(index = 4, type = STRING) String mnc,
                        @ProtobufProperty(index = 5, type = STRING) String osVersion,
                        @ProtobufProperty(index = 6, type = STRING) String manufacturer,
                        @ProtobufProperty(index = 7, type = STRING) String device,
                        @ProtobufProperty(index = 8, type = STRING) String osBuildNumber,
                        @ProtobufProperty(index = 9, type = STRING) String phoneId,
                        @ProtobufProperty(index = 10, type = OBJECT) ReleaseChannel releaseChannel,
                        @ProtobufProperty(index = 11, type = STRING) String localeLanguageIso6391,
                        @ProtobufProperty(index = 12, type = STRING) String localeCountryIso31661Alpha2,
                        @ProtobufProperty(index = 13, type = STRING) String deviceBoard,
                        @ProtobufProperty(index = 15, type = OBJECT) DeviceType deviceType) implements ProtobufMessage {

    @ProtobufMessageName("ClientPayload.UserAgent.Platform")
    public enum PlatformType implements ProtobufEnum {
        UNKNOWN(999),
        ANDROID(0),
        IOS(1),
        ANDROID_BUSINESS(10),
        KAIOS(11),
        IOS_BUSINESS(12),
        WINDOWS(13),
        MACOS(24),
        WEB(14);

        PlatformType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }

        public boolean isWeb() {
            return this == WEB;
        }

        public boolean isAndroid() {
            return this == ANDROID || this == ANDROID_BUSINESS;
        }

        public boolean isIOS() {
            return this == IOS || this == IOS_BUSINESS;
        }

        public boolean isBusiness() {
            return this == ANDROID_BUSINESS || this == IOS_BUSINESS;
        }

        public boolean isKaiOs() {
            return this == KAIOS;
        }

        public PlatformType toPersonal() {
            return switch (this) {
                case ANDROID_BUSINESS -> ANDROID;
                case IOS_BUSINESS -> IOS;
                default -> this;
            };
        }

        public PlatformType toBusiness() {
            return switch (this) {
                case ANDROID -> ANDROID_BUSINESS;
                case IOS -> IOS_BUSINESS;
                default -> this;
            };
        }
    }

    @ProtobufMessageName("ClientPayload.UserAgent.ReleaseChannel")
    public enum ReleaseChannel implements ProtobufEnum {
        RELEASE(0),
        BETA(1),
        ALPHA(2),
        DEBUG(3);

        ReleaseChannel(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }
    }

    @ProtobufMessageName("ClientPayload.UserAgent.DeviceType")
    public enum DeviceType implements ProtobufEnum {
        PHONE(0),
        TABLET(1),
        DESKTOP(2),
        WEARABLE(3),
        VR(4);

        DeviceType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        final int index;

        public int index() {
            return this.index;
        }
    }
}
