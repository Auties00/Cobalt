package it.auties.whatsapp4j.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
@Builder
@Data
@Accessors(fluent = true)
public class WhatsappChat {
    private @NotNull String jid;
    private @NotNull WhatsappMessages messages;
    private @Nullable String name;
    private @Nullable String modifyTag;
    private @Nullable String chatPicture;
    private @Nullable WhatsappGroupMetadata metadata;
    private @Nullable String mute;
    private @Nullable String pin;
    private @Nullable String ephemeralMessagesToggleTime;
    private @Nullable String ephemeralMessageTime;
    private int timestamp;
    private int unreadMessages;
    private boolean archived;
    private boolean cleared;
    private boolean readOnly;
    private boolean spam;

    public boolean isGroup(){
        return jid.contains("-");
    }

    @Override
    public String toString() {
        return "WhatsappChat{" +
                "jid='" + jid + '\'' +
                ", messages=" + messages.size() +
                ", name='" + name + '\'' +
                ", modifyTag='" + modifyTag + '\'' +
                ", chatPicture='" + chatPicture + '\'' +
                ", metadata=" + metadata +
                ", mute='" + mute + '\'' +
                ", pin='" + pin + '\'' +
                ", ephemeralMessagesToggleTime='" + ephemeralMessagesToggleTime + '\'' +
                ", ephemeralMessageTime='" + ephemeralMessageTime + '\'' +
                ", timestamp=" + timestamp +
                ", unreadMessages=" + unreadMessages +
                ", archived=" + archived +
                ", cleared=" + cleared +
                ", readOnly=" + readOnly +
                ", spam=" + spam +
                '}';
    }
}
