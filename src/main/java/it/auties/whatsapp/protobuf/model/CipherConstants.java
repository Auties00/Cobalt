package it.auties.whatsapp.protobuf.model;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CipherConstants {
    public final int CURRENT_VERSION = 3;
    public final int WHISPER_TYPE = 2;
    public final int PRE_KEY_TYPE = 3;
    public final int SENDER_KEY_TYPE = 4;
    public final int SENDER_KEY_DISTRIBUTION_TYPE = 5;
    public final int ENCRYPTED_MESSAGE_OVERHEAD = 53;
}
