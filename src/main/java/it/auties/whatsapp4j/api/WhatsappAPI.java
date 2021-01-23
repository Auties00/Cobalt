package it.auties.whatsapp4j.api;

import it.auties.whatsapp4j.constant.Flag;
import it.auties.whatsapp4j.constant.Metric;
import it.auties.whatsapp4j.constant.UserPresence;
import it.auties.whatsapp4j.model.WhatsappListener;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.model.WhatsappNodeBuilder;
import it.auties.whatsapp4j.request.UserPresenceUpdateRequest;
import it.auties.whatsapp4j.socket.WhatsappWebSocket;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Accessors(fluent = true)
public class WhatsappAPI extends WhatsappListener {
    private final @NotNull WhatsappConfiguration configuration;
    private final @Getter @NotNull WhatsappDataManager manager;
    private final @Getter @NotNull WhatsappKeysManager keys;
    private final @NotNull WhatsappWebSocket socket;
    private final @NotNull List<WhatsappListener> listeners;
    private int numberOfMessagesSent;
    public WhatsappAPI(){
        this(WhatsappConfiguration.defaultOptions());
    }

    public WhatsappAPI(@NotNull WhatsappConfiguration configuration){
        this.configuration = configuration;
        this.listeners = new ArrayList<>();
        this.manager = WhatsappDataManager.singletonInstance();
        this.keys = WhatsappKeysManager.fromPreferences();
        this.socket = new WhatsappWebSocket(listeners, configuration, keys);
        this.numberOfMessagesSent = 0;
    }

    public WhatsappAPI connect(){
        socket.connect();
        return this;
    }

    public WhatsappAPI disconnect(){
        socket.disconnect(null, false, false);
        return this;
    }

    public WhatsappAPI logout(){
        socket.disconnect(null, true, false);
        return this;
    }

    public WhatsappAPI reconnect(){
        socket.disconnect(null, false, true);
        return this;
    }

    public void loadConversation(@NotNull String remoteJid, int messageCount) {
        var node = WhatsappNodeBuilder.builder()
                .description("query")
                .attrs(Map.of("type", "message", "epoch", String.valueOf(numberOfMessagesSent++), "jid", remoteJid, "kind", "before", "count", String.valueOf(messageCount)))
                .build();

        socket.sendBinaryMessage(node, Metric.QUERY_MESSAGES, Flag.IGNORE);
    }

    public void loadConversation(@NotNull String remoteJid, int messageCount, @NotNull String lastMessageId, boolean lastOwner) {
        var node = WhatsappNodeBuilder.builder()
                .description("query")
                .attrs(Map.of("type", "message", "epoch", String.valueOf(numberOfMessagesSent++), "jid", remoteJid, "kind", "before", "count", String.valueOf(messageCount), "index", lastMessageId, "owner", Boolean.toString(lastOwner)))
                .build();

        socket.sendBinaryMessage(node, Metric.QUERY_MESSAGES, Flag.IGNORE);
    }

    public void changePresence(@NotNull UserPresence presence){
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("type", "set", "epoch", String.valueOf(1)))
                .content(List.of(List.of(new WhatsappNode("presence", Map.of("type", presence.content()), null))))
                .build();

        socket.sendBinaryMessage(node, Metric.PRESENCE, presence.data());
    }

    public void changePresence(@NotNull UserPresence presence, @NotNull String targetJid){
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("type", "set", "epoch", String.valueOf(1)))
                .content(List.of(List.of(new WhatsappNode("presence", Map.of("type", presence.content(), "to", targetJid), null))))
                .build();

        socket.sendBinaryMessage(node, Metric.PRESENCE, presence.data());
    }

    public void updatePresence(@NotNull String jid){
        socket.sendJsonMessage(new UserPresenceUpdateRequest(keys, configuration, jid));
    }

    public WhatsappAPI registerListener(WhatsappListener listener){
        listeners.add(listener);
        return this;
    }

    public WhatsappAPI removeListener(WhatsappListener listener){
        listeners.remove(listener);
        return this;
    }
}
