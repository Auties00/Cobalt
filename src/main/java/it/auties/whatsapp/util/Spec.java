package it.auties.whatsapp.util;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.binary.Tokens;
import it.auties.whatsapp.model.signal.auth.UserAgent.UserAgentPlatform;

import java.net.URI;

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
        public static final String MOBILE_DOWNLOAD_URL = "https://www.whatsapp.com/download";
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

        // TODO: Fetch this dynamically if possible
        public static final String MOBILE_TOKEN = "0a1mLfGUIBVrMKF1RdvLI5lkRBvof6vn0fD2QRSM4174c0243f5277a5d7720ce842cc4ae6";
        public static final String MOBILE_OS_VERSION = "15.3.1";
        public static final String MOBILE_DEVICE = "iPhone_7";
        public static final String MOBILE_DEVICE_MANUFACTURER = "Apple";
        public static final UserAgentPlatform MOBILE_OS_TYPE = UserAgentPlatform.IOS;
        public static final String MOBILE_UA = "WhatsApp/%s iOS/15.3.1 Device/Apple-iPhone_7";
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
