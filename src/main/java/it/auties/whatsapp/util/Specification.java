package it.auties.whatsapp.util;

import it.auties.bytes.Bytes;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
public class Specification {
  public final static class Whatsapp {
    private static final byte[] WHATSAPP_HEADER = "WA".getBytes(StandardCharsets.UTF_8);
    private static final byte[] WEB_VERSION = new byte[]{5, 2};
    private static final byte[] MOBILE_VERSION = new byte[]{4, 0};

    public static final Bytes WEB_PROTOCOL = Bytes.of("Noise_XX_25519_AESGCM_SHA256\0\0\0\0");
    public static final byte[] WEB_PROLOGUE = Bytes.of(WHATSAPP_HEADER).append(WEB_VERSION).toByteArray();
    public static final byte[] MOBILE_PROLOGUE = Bytes.of(WHATSAPP_HEADER).append(MOBILE_VERSION).toByteArray();

    public static final String WEB_ORIGIN = "https://web.whatsapp.com";
    public static final String WEB_HOST = "web.whatsapp.com";
    public static final String MOBILE_ORIGIN = "<todo>";

    public static final String WEB_ENDPOINT = "wss://web.whatsapp.com/ws/chat";
    public static final String MOBILE_ENDPOINT = "g.whatsapp.net";

    public static final String WEB_UPDATE_URL = "https://web.whatsapp.com/check-update?version=%s&platform=web";
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
  }
}
