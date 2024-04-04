package it.auties.whatsapp.util;

import it.auties.whatsapp.binary.BinaryTokens;
import it.auties.whatsapp.model.signal.auth.Version;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class Specification {
    public final static class Whatsapp {
        public static final String DEFAULT_NAME = "Cobalt";
        public static final byte[] NOISE_PROTOCOL = "Noise_XX_25519_AESGCM_SHA256\0\0\0\0".getBytes(StandardCharsets.UTF_8);
        public static final String WEB_ORIGIN = "https://web.whatsapp.com";
        public static final URI WEB_SOCKET_ENDPOINT = URI.create("wss://web.whatsapp.com/ws/chat");
        public static final URI SOCKET_ENDPOINT = URI.create("http://g.whatsapp.net:443");
        public static final String WEB_UPDATE_URL = "https://web.whatsapp.com/check-update?version=2.2245.9&platform=web";
        public static final String MOBILE_IOS_URL = "https://itunes.apple.com/lookup?bundleId=net.whatsapp.WhatsApp";
        public static final String MOBILE_ANDROID_USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.57 Mobile Safari/537.36";
        public static final String MOBILE_BUSINESS_IOS_URL = "https://itunes.apple.com/lookup?bundleId=net.whatsapp.WhatsAppSMB";
        public static final String MOBILE_REGISTRATION_ENDPOINT = "https://v.whatsapp.net/v2";
        public static final String MOBILE_IOS_OFFLINE_AB = "%7B%22%65%78%70%6F%73%75%72%65%22%3A%5B%22%68%69%64%65%5F%6C%69%6E%6B%5F%64%65%76%69%63%65%5F%62%75%74%74%6F%6E%5F%72%65%6C%65%61%73%65%5F%72%6F%6C%6C%6F%75%74%5F%75%6E%69%76%65%72%73%65%7C%68%69%64%65%5F%6C%69%6E%6B%5F%64%65%76%69%63%65%5F%62%75%74%74%6F%6E%5F%72%65%6C%65%61%73%65%5F%72%6F%6C%6C%6F%75%74%5F%65%78%70%65%72%69%6D%65%6E%74%7C%63%6F%6E%74%72%6F%6C%22%2C%22%69%6F%73%5F%63%6F%6E%66%6C%75%65%6E%63%65%5F%74%6F%73%5F%70%70%5F%6C%69%6E%6B%5F%75%70%64%61%74%65%5F%75%6E%69%76%65%72%73%65%7C%69%70%68%6F%6E%65%5F%63%6F%6E%66%6C%75%65%6E%63%65%5F%74%6F%73%5F%70%70%5F%6C%69%6E%6B%5F%75%70%64%61%74%65%5F%65%78%70%7C%74%65%73%74%22%2C%22%64%75%6D%6D%79%5F%61%61%5F%70%72%6F%64%5F%75%6E%69%76%65%72%73%65%5F%69%6F%73%7C%64%75%6D%6D%79%5F%61%61%5F%70%72%6F%64%5F%65%78%70%65%72%69%6D%65%6E%74%5F%69%6F%73%7C%63%6F%6E%74%72%6F%6C%22%5D%2C%22%6D%65%74%72%69%63%73%22%3A%7B%22%65%78%70%69%64%5F%6D%64%22%3A%31%37%30%38%32%35%36%33%31%30%2C%22%72%63%5F%6F%6C%64%22%3A%74%72%75%65%2C%22%65%78%70%69%64%5F%63%64%22%3A%31%37%30%38%32%35%36%33%31%30%7D%7D";
        public static final String MOBILE_BUSINESS_IOS_OFFLINE_AB = "%7B%22%65%78%70%6F%73%75%72%65%22%3A%5B%22%68%69%64%65%5F%6C%69%6E%6B%5F%64%65%76%69%63%65%5F%62%75%74%74%6F%6E%5F%72%65%6C%65%61%73%65%5F%72%6F%6C%6C%6F%75%74%5F%75%6E%69%76%65%72%73%65%7C%68%69%64%65%5F%6C%69%6E%6B%5F%64%65%76%69%63%65%5F%62%75%74%74%6F%6E%5F%72%65%6C%65%61%73%65%5F%72%6F%6C%6C%6F%75%74%5F%65%78%70%65%72%69%6D%65%6E%74%7C%63%6F%6E%74%72%6F%6C%22%2C%22%69%6F%73%5F%63%6F%6E%66%6C%75%65%6E%63%65%5F%74%6F%73%5F%70%70%5F%6C%69%6E%6B%5F%75%70%64%61%74%65%5F%75%6E%69%76%65%72%73%65%7C%69%70%68%6F%6E%65%5F%63%6F%6E%66%6C%75%65%6E%63%65%5F%74%6F%73%5F%70%70%5F%6C%69%6E%6B%5F%75%70%64%61%74%65%5F%65%78%70%7C%74%65%73%74%22%5D%2C%22%6D%65%74%72%69%63%73%22%3A%7B%22%65%78%70%69%64%5F%63%22%3A%74%72%75%65%2C%22%66%64%69%64%5F%63%22%3A%74%72%75%65%2C%22%72%63%5F%63%22%3A%74%72%75%65%2C%22%65%78%70%69%64%5F%6D%64%22%3A%31%37%31%31%32%30%39%33%34%39%2C%22%65%78%70%69%64%5F%63%64%22%3A%31%37%31%31%32%30%39%33%34%39%7D%7D";
        public static final String MOBILE_ANDROID_OFFLINE_AB = "%7B%22exposure%22%3A%5B%22android_confluence_tos_pp_link_update_universe%7Candroid_confluence_tos_pp_link_update_exp%7Ccontrol%22%2C%22reg_phone_number_update_colors_prod_universe%7Creg_phone_number_update_colors_prod_experiment%7Ccontrol%22%2C%22android_rollout_quebec_tos_reg_universe%7Candroid_rollout_ca_tos_reg_experiment%7Ccontrol%22%2C%22dummy_aa_prod_universe%7Cdummy_aa_prod_experiment%7Ccontrol%22%5D%2C%22metrics%22%3A%7B%22backup_token_source%22%3A%22block_store%22%7D%7D";
        public static final String MOBILE_KAIOS_REGISTRATION_ENDPOINT = "https://v-k.whatsapp.net/v2";
        public static final String MOBILE_KAIOS_URL = "https://api.kai.jiophone.net/v2.0/apps?cu=F90M-FBJIINA";
        public static final String MOBILE_KAIOS_USER_AGENT = "Mozilla/5.0 (Mobile; LYF/F90M/LYF-F90M-000-03-31-121219; Android; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5";
        public static final String APNS_WHATSAPP_BUSINESS_NAME = "net.whatsapp.WhatsAppSMB";
        public static final String APNS_WHATSAPP_NAME = "net.whatsapp.WhatsApp";
        public static final Version MOBILE_DEFAULT_PERSONAL_IOS_VERSION = Version.of("2.24.6.77");
        public static final Version MOBILE_DEFAULT_BUSINESS_IOS_VERSION = Version.of("2.24.4.78");
        public static final String[] DEFAULT_APNS_FILTERS = new String[]{
                "net.whatsapp.WhatsApp",
                "net.whatsapp.WhatsApp.voip",
                "net.whatsapp.WhatsAppSMB",
                "net.whatsapp.WhatsAppSMB.voip"
        };
        public static final String MOBILE_ANDROID_CLIENT_METRICS = "%7B%22attempts%22%3A1%2C%22app_campaign_download_source%22%3A%22google-play%7Cunknown%22%7D";
        private static final byte[] WHATSAPP_VERSION_HEADER = "WA".getBytes(StandardCharsets.UTF_8);
        private static final byte[] WEB_VERSION = new byte[]{6, BinaryTokens.DICTIONARY_VERSION};
        public static final byte[] WEB_PROLOGUE = Bytes.concat(WHATSAPP_VERSION_HEADER, WEB_VERSION);
        private static final byte[] MOBILE_VERSION = new byte[]{5, BinaryTokens.DICTIONARY_VERSION};
        public static final byte[] MOBILE_PROLOGUE = Bytes.concat(WHATSAPP_VERSION_HEADER, MOBILE_VERSION);
        public static final byte[] ACCOUNT_SIGNATURE_HEADER = {6, 0};
        public static final byte[] DEVICE_WEB_SIGNATURE_HEADER = {6, 1};
        public static final byte[] DEVICE_MOBILE_SIGNATURE_HEADER = {6, 2};
        public static final int MAX_COMPANIONS = 5;
        public static final int THUMBNAIL_WIDTH = 480;
        public static final int THUMBNAIL_HEIGHT = 339;
        public static final byte[] REGISTRATION_PUBLIC_KEY = HexFormat.of().parseHex("8e8c0f74c3ebc5d7a6865c6c3c843856b06121cce8ea774d22fb6f122512302d");
        public static final String MOBILE_IOS_STATIC = "0a1mLfGUIBVrMKF1RdvLI5lkRBvof6vn0fD2QRSM";
        public static final String MOBILE_BUSINESS_IOS_STATIC = "USUDuDYDeQhY4RF2fCSp5m3F6kJ1M2J8wS7bbNA2";
        public static final String MOBILE_KAIOS_STATIC = "aa8243c465a743c488beb4645dda63edc2ca9a58";
        public static final int COMPANION_PAIRING_TIMEOUT = 10;
        public static final int DEFAULT_HISTORY_SIZE = 59206;
        public static final byte[][] CALL_RELAY = new byte[][]{
                new byte[]{-105, 99, -47, -29, 13, -106},
                new byte[]{-99, -16, -53, 62, 13, -106},
                new byte[]{-99, -16, -25, 62, 13, -106},
                new byte[]{-99, -16, -5, 62, 13, -106},
                new byte[]{-71, 60, -37, 62, 13, -106}
        };
        public static final String BUSINESS_NAME_VCARD_PROPERTY = "X-WA-BIZ-NAME";
        public static final String PHONE_NUMBER_VCARD_PROPERTY = "WAID";
        public static final String DEFAULT_NUMBER_VCARD_TYPE = "CELL";
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
