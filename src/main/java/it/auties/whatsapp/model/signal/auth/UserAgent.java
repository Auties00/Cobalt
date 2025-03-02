package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.*;

@ProtobufMessage(name = "ClientPayload.UserAgent")
public record UserAgent(@ProtobufProperty(index = 1, type = ENUM) PlatformType platform,
                        @ProtobufProperty(index = 2, type = MESSAGE) Version appVersion,
                        @ProtobufProperty(index = 3, type = STRING) String mcc,
                        @ProtobufProperty(index = 4, type = STRING) String mnc,
                        @ProtobufProperty(index = 5, type = STRING) String osVersion,
                        @ProtobufProperty(index = 6, type = STRING) String manufacturer,
                        @ProtobufProperty(index = 7, type = STRING) String device,
                        @ProtobufProperty(index = 8, type = STRING) String osBuildNumber,
                        @ProtobufProperty(index = 9, type = STRING) String phoneId,
                        @ProtobufProperty(index = 10, type = ENUM) ReleaseChannel releaseChannel,
                        @ProtobufProperty(index = 11, type = STRING) String localeLanguageIso6391,
                        @ProtobufProperty(index = 12, type = STRING) String localeCountryIso31661Alpha2,
                        @ProtobufProperty(index = 13, type = STRING) String deviceBoard,
                        @ProtobufProperty(index = 15, type = ENUM) DeviceType deviceType,
                        @ProtobufProperty(index = 16, type = STRING) String deviceModelType) {

    @ProtobufEnum(name = "ClientPayload.UserAgent.Platform")
    public enum PlatformType {
        UNKNOWN("Unknown", 999),
        ANDROID("Android", 0),
        IOS("iOS", 1),
        ANDROID_BUSINESS("Android", 10),
        KAIOS("KaiOS", 11),
        IOS_BUSINESS("iOS", 12),
        WINDOWS("Windows", 13),
        MACOS("MacOS", 24),
        WEB("Web", 14);

        PlatformType(String platformName, @ProtobufEnumIndex int index) {
            this.platformName = platformName;
            this.index = index;
        }

        final String platformName;
        final int index;

        public int index() {
            return this.index;
        }

        public String platformName() {
            return platformName;
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

        public boolean isDesktop() {
            return this == WINDOWS || this == MACOS;
        }

        public boolean isWeb() {
            return this == WEB;
        }

        public boolean isMobile() {
            return isAndroid() || isIOS() || isKaiOs();
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

    @ProtobufEnum(name = "ClientPayload.UserAgent.ReleaseChannel")
    public enum ReleaseChannel {
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

    @ProtobufEnum(name = "ClientPayload.UserAgent.DeviceType")
    public enum DeviceType {
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
