package it.auties.whatsapp4j.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@Builder
@Data
@Accessors(fluent = true)
@ToString
public class WhatsappChat {
    private @NotNull String jid;
    private @NotNull WhatsappMessages messages;
    private @NotNull String name;
    private @NotNull Map<WhatsappContact, WhatsappContactStatus> presences;
    private @NotNull WhatsappMute mute;
    private @Nullable String newJid;
    private long timestamp;
    private int unreadMessages;
    private boolean archived;
    private boolean readOnly;
    private boolean spam;
    private int pinned;
    private long ephemeralMessageDuration;
    private long ephemeralMessagesToggleTime;
    public static @NotNull WhatsappChat fromAttributes(@NotNull Map<String, String> attrs){
        var jid = attrs.get("jid");
        return WhatsappChat.builder()
                .timestamp(Long.parseLong(attrs.get("t")))
                .jid(jid)
                .newJid(attrs.get("new_jid"))
                .unreadMessages(Integer.parseInt(attrs.get("count")))
                .mute(new WhatsappMute(Integer.parseInt(attrs.get("mute"))))
                .spam(Boolean.parseBoolean(attrs.get("spam")))
                .archived(Boolean.parseBoolean(attrs.get("archive")))
                .name(attrs.getOrDefault("name", WhatsappUtils.phoneNumberFromJid(jid)))
                .readOnly(Boolean.parseBoolean(attrs.get("read_only")))
                .pinned(Integer.parseInt(attrs.getOrDefault("pin", "0")))
                .messages(new WhatsappMessages())
                .presences(new HashMap<>())
                .build();
    }

    public boolean isGroup(){
        return WhatsappUtils.isGroup(jid);
    }

    public boolean hasNewJid(){
        return newJid != null;
    }

    public @NotNull Optional<String> newJid(){
        return Optional.ofNullable(newJid);
    }

    public boolean isPinned(){
        return pinned != 0;
    }

    public @NotNull Optional<ZonedDateTime> pinned(){
        return WhatsappUtils.parseWhatsappTime(pinned);
    }

    public boolean isEphemeralChat(){
        return ephemeralMessageDuration != 0 && ephemeralMessagesToggleTime != 0;
    }

    public @NotNull Optional<ZonedDateTime> ephemeralMessageDuration(){
        return WhatsappUtils.parseWhatsappTime(ephemeralMessageDuration);
    }

    public @NotNull Optional<ZonedDateTime> ephemeralMessagesToggleTime(){
        return WhatsappUtils.parseWhatsappTime(ephemeralMessagesToggleTime);
    }

    public @NotNull Optional<WhatsappMessage> lastMessage(){
        return !messages.isEmpty() ? Optional.of(messages.get(messages.size() - 1)) : Optional.empty();
    }
}
