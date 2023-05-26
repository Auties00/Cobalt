package it.auties.whatsapp.util;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.binary.Tokens;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;

import java.net.URI;
import java.util.Base64;
import java.util.HexFormat;

@SuppressWarnings("unused")
public class Spec {
    public final static class Whatsapp {
        public static final Bytes PROTOCOL = Bytes.of("Noise_XX_25519_AESGCM_SHA256\0\0\0\0");
        public static final String WEB_ORIGIN = "https://web.whatsapp.com";
        public static final String WEB_HOST = "web.whatsapp.com";
        public static final URI WEB_ENDPOINT = URI.create("wss://web.whatsapp.com/ws/chat");
        public static final String APP_ENDPOINT_HOST = "g.whatsapp.net";
        public static final int APP_ENDPOINT_PORT = 443;
        public static final String WEB_UPDATE_URL = "https://web.whatsapp.com/check-update?version=2.2245.9&platform=web";
        public static final String MOBILE_REGISTRATION_ENDPOINT = "https://v.whatsapp.net/v2";
        private static final String WHATSAPP_HEADER = "WA";
        private static final byte[] WEB_VERSION = new byte[]{6, Tokens.DICTIONARY_VERSION};
        public static final byte[] WEB_PROLOGUE = Bytes.of(WHATSAPP_HEADER).append(WEB_VERSION).toByteArray();
        private static final byte[] MOBILE_VERSION = new byte[]{5, Tokens.DICTIONARY_VERSION};
        public static final byte[] APP_PROLOGUE = Bytes.of(WHATSAPP_HEADER).append(MOBILE_VERSION).toByteArray();
        public static final byte[] ACCOUNT_SIGNATURE_HEADER = {6, 0};
        public static final byte[] DEVICE_WEB_SIGNATURE_HEADER = {6, 1};
        public static final byte[] DEVICE_MOBILE_SIGNATURE_HEADER = {6, 2};
        public static final int MAX_COMPANIONS = 5;
        public static final int THUMBNAIL_WIDTH = 480;
        public static final int THUMBNAIL_HEIGHT = 339;
        public static final String MOBILE_DOWNLOAD_URL = "https://www.whatsapp.com/android/current/WhatsApp.apk";
        public static final String MOBILE_BUSINESS_DOWNLOAD_URL = "https://dw.uptodown.com/dwn/tQT7qXhiw-qe1is04R_tv_bm7fXt-GdqUUSX6-8JWOZQghewMHOLyASn-K6r37ALTzBvYUpmtU1wW5esTeKzEYjjrrhRomos_QMsBcPx9Xv2GTv-WYLQKhgF5txOq2rs/OjWc5WIQUGu0woVuns79AVcYHY5gxXKybxAYAh01kDpQ4ZqLPLv0SyJRQZPdSddz2kDP1JqjYfprHUhbrvAYXR6xHyLDnGuKB_sUMApalLWlq3LFnobWX0Ah-MYUboeX/1_-UshHiZze6ylofK68YmzFsvwj_AxgIZBc67k-N6rJzsmx7rCove_W2CxhihL23Y-z1CQH8UsoA1XjULZdbY-uwFBouwzwb4sPeFcYhQOI=";
        public static final byte[] MOBILE_ANDROID_SALT = Base64.getDecoder().decode("PkTwKSZqUfAUyR0rPQ8hYJ0wNsQQ3dW1+3SCnyTXIfEAxxS75FwkDf47wNv/c8pP3p0GXKR6OOQmhyERwx74fw1RYSU10I4r1gyBVDbRJ40pidjM41G1I1oN");
        public static final String DEFAULT_MOBILE_OS_VERSION = "15.3.1";
        public static final String DEFAULT_MOBILE_DEVICE_MODEL = "iPhone_7";
        public static final String DEFAULT_MOBILE_DEVICE_MANUFACTURER = "Apple";
        public static final UserAgentPlatform DEFAULT_MOBILE_OS_TYPE = UserAgentPlatform.IOS;
        public static final byte[] MOBILE_SALT = Base64.getDecoder().decode("PkTwKSZqUfAUyR0rPQ8hYJ0wNsQQ3dW1+3SCnyTXIfEAxxS75FwkDf47wNv/c8pP3p0GXKR6OOQmhyERwx74fw1RYSU10I4r1gyBVDbRJ40pidjM41G1I1oN");
        // public static final String DEFAULT_MOBILE_OS_VERSION = "8.0.0";
        // public static final String DEFAULT_MOBILE_DEVICE_MODEL = "star2lte";
        // public static final String DEFAULT_MOBILE_DEVICE_MANUFACTURER = "samsung";
        // public static final UserAgentPlatform DEFAULT_MOBILE_OS_TYPE = UserAgentPlatform.ANDROID;
        public static final String DEFAULT_WEB_OS_VERSION = "10.0";
        public static final String DEFAULT_WEB_DEVICE_MODEL = "Surface Laptop Studio";
        public static final String DEFAULT_WEB_DEVICE_MANUFACTURER = "Microsoft";
        public static final UserAgentPlatform DEFAULT_WEB_OS_TYPE = UserAgentPlatform.WINDOWS;
        public static final byte[] REGISTRATION_PUBLIC_KEY = HexFormat.of().parseHex("8e8c0f74c3ebc5d7a6865c6c3c843856b06121cce8ea774d22fb6f122512302d");
        public static final String MOBILE_IOS_STATIC = "0a1mLfGUIBVrMKF1RdvLI5lkRBvof6vn0fD2QRSM";
        public static final int COMPANION_PAIRING_TIMEOUT = 10;
    }

    public final static class Signal {
        public static final int CURRENT_VERSION = 3;
        public static final int IV_LENGTH = 16;
        public static final int KEY_LENGTH = 32;
        public static final int MAC_LENGTH = 8;
        public static final int SIGNATURE_LENGTH = 64;
        public static final int KEY_TYPE = 5;
        public static final byte[] KEY_BUNDLE_TYPE = new byte[]{5};
        public static final int MAX_MESSAGES = 2000;
        public static final String SKMSG = "skmsg";
        public static final String PKMSG = "pkmsg";
        public static final String MSG = "msg";
        public static final String UNAVAILABLE = "unavailable";
    }
}
