package it.auties.whatsapp4j.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class WhatsappIdUtils {
    public @NotNull String parseJid(@NotNull String jid){
        return jid.replaceAll("@c.us", "@s.whatsapp.net");
    }

    public @NotNull String randomId(){
        return BytesArray.random(10).toHex();
    }
}
