package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.constant.MuteType;
import it.auties.whatsapp4j.constant.UserPresence;
import it.auties.whatsapp4j.utils.Validate;
import lombok.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Map;

@AllArgsConstructor
@Builder
@Data
@Accessors(fluent = true)
@ToString
public class WhatsappChat {
    private @NotNull String jid;
    private @NotNull WhatsappMessages messages;
    private @NotNull String name;
    private @NotNull Map<WhatsappContact, UserPresence> presences;
    private @Nullable MuteType mute;
    private @Nullable String chatPicture;
    private @Nullable WhatsappGroupMetadata metadata;
    private @Nullable String pin;
    private @Nullable String ephemeralMessagesToggleTime;
    private @Nullable String ephemeralMessageTime;
    private int timestamp;
    private int unreadMessages;
    private boolean archived;
    private boolean readOnly;
    private boolean spam;

    public boolean isGroup(){
        return jid.contains("-");
    }

    public boolean isMuted(){
        Validate.isTrue(mute != null, "WhatsappChat: Missing mute field, cannot check if chat is muted");
        return mute.timeInSeconds() != 0;
    }
}
