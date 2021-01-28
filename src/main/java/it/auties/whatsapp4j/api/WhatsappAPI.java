package it.auties.whatsapp4j.api;

import it.auties.whatsapp4j.constant.Flag;
import it.auties.whatsapp4j.constant.Metric;
import it.auties.whatsapp4j.constant.ProtoBuf;
import it.auties.whatsapp4j.constant.UserPresence;
import it.auties.whatsapp4j.model.*;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.manager.WhatsappKeysManager;
import it.auties.whatsapp4j.request.UserPresenceUpdateRequest;
import it.auties.whatsapp4j.socket.WhatsappWebSocket;
import it.auties.whatsapp4j.utils.BytesArray;
import it.auties.whatsapp4j.utils.Validate;
import it.auties.whatsapp4j.utils.WhatsappIdUtils;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;

@Accessors(fluent = true)
public class WhatsappAPI extends WhatsappListener {
    private @NotNull WhatsappWebSocket socket;
    private final @NotNull WhatsappConfiguration configuration;
    private final @Getter @NotNull WhatsappDataManager manager;
    private final @Getter @NotNull WhatsappKeysManager keys;
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
        this.socket = socket.disconnect(null, false, true);
        return this;
    }

    public void sendMessage(@NotNull String remoteJid, @NotNull String text, @NotNull BiConsumer<WhatsappMessage, Integer> callback) {
        var message = ProtoBuf.WebMessageInfo.newBuilder()
                .setMessage(ProtoBuf.Message.newBuilder()
                        .setConversation(text)
                        .build())
                .setKey(ProtoBuf.MessageKey.newBuilder()
                        .setFromMe(true)
                        .setRemoteJid(remoteJid)
                        .setId(WhatsappIdUtils.randomId())
                        .build())
                .setMessageTimestamp(Instant.now().getEpochSecond())
                .setStatus(ProtoBuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS.PENDING)
                .build();

        sendMessage(message, callback);
    }

    public void sendMessage(@NotNull String remoteJid, @NotNull String text, @NotNull WhatsappMessage quotedMessage, @NotNull BiConsumer<WhatsappMessage, Integer> callback) {
        var chat = manager.findChatByJid(remoteJid);
        Validate.isTrue(chat.isPresent(), "WhatsappAPI: Cannot send a quoted message to %s as the chat doesn't exist", remoteJid);

        var message = ProtoBuf.WebMessageInfo.newBuilder()
                .setMessage(ProtoBuf.Message.newBuilder()
                        .setExtendedTextMessage(ProtoBuf.ExtendedTextMessage.newBuilder()
                                .setText(text)
                                .setContextInfo(ProtoBuf.ContextInfo.newBuilder()
                                        .setQuotedMessage(quotedMessage.info().getMessage())
                                        .setParticipant(quotedMessage.senderJid().orElse(manager.phoneNumber()))
                                        .setStanzaId(quotedMessage.info().getKey().getId())
                                        .setRemoteJid(quotedMessage.info().getKey().getRemoteJid())
                                        .build())
                                .build())
                        .build())
                .setKey(ProtoBuf.MessageKey.newBuilder()
                        .setFromMe(true)
                        .setRemoteJid(remoteJid)
                        .setId(WhatsappIdUtils.randomId())
                        .build())
                .setMessageTimestamp(Instant.now().getEpochSecond())
                .setStatus(ProtoBuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS.PENDING)
                .build();

        sendMessage(message, callback);
    }

    public void sendMessage(@NotNull ProtoBuf.WebMessageInfo message, @NotNull BiConsumer<WhatsappMessage, Integer> callback){
        var node = WhatsappNodeBuilder.builder()
                .description("action")
                .attrs(Map.of("type", "relay", "epoch", String.valueOf(numberOfMessagesSent++)))
                .content(List.of(new WhatsappNode("message", Map.of(), message)))
                .build();

        manager.addPendingMessage(new WhatsappPendingMessage(new WhatsappMessage(message), callback));
        socket.sendBinaryMessage(node, Metric.MESSAGE, Flag.IGNORE);
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
