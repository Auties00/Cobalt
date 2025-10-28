package com.github.auties00.cobalt.socket.state;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.node.Node;
import com.github.auties00.cobalt.node.NodeBuilder;
import com.github.auties00.cobalt.model.contact.ContactStatus;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.jid.JidServer;
import com.github.auties00.cobalt.media.MediaConnection;
import com.github.auties00.cobalt.model.privacy.PrivacySettingEntryBuilder;
import com.github.auties00.cobalt.model.privacy.PrivacySettingType;
import com.github.auties00.cobalt.model.privacy.PrivacySettingValue;
import com.github.auties00.cobalt.model.sync.PatchType;
import com.github.auties00.cobalt.socket.SocketStream;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.auties00.cobalt.client.WhatsAppClientErrorHandler.Location.AUTH;
import static com.github.auties00.cobalt.client.WhatsAppClientErrorHandler.Location.MEDIA_CONNECTION;

// TODO: Orchestrate a platform specific login flow in separate classes that can be auto updated
public final class SuccessStreamNodeHandler extends SocketStream.Handler {
    private static final int PRE_KEYS_UPLOAD_CHUNK = 10;
    private static final int DEFAULT_MEDIA_CONNECTION_TTL = 300;

    public SuccessStreamNodeHandler(WhatsAppClient whatsapp) {
        super(whatsapp, "success");
    }

    @Override
    public void handle(Node node) {
        updateIdentity(node);
        finishLogin();
    }

    private void updateIdentity(Node node) {
        node.getAttributeAsJid("lid")
                .ifPresent(whatsapp.store()::setLid);
    }

    private void finishLogin() {
        switch (whatsapp.store().clientType()) {
            case WEB -> finishWebLogin();
            case MOBILE -> finishMobileLogin();
        }
    }

    private void finishWebLogin() {
        try {
            notifyStore();
            queryGroups();
            pullInitialWebAppStatePatches();
            setActiveConnection();
            sendInitialPreKeys();
            scheduleMediaConnectionUpdate();
            updateSelfPresence();
            queryInitial2fa();
            queryInitialAboutPrivacy();
            queryInitialPrivacySettings();
            queryInitialDisappearingMode();
            queryInitialBlockList();
            onInitialInfo();
            queryNewsletters();
        } catch (Exception throwable) {
            whatsapp.handleFailure(AUTH, throwable);
        }
    }

    private void notifyStore() {
        try {
            whatsapp.store()
                    .serializer()
                    .finishDeserialize(whatsapp.store());
            if(whatsapp.store().syncedChats()) {
                var chats = whatsapp.store().chats();
                for(var listener : whatsapp.store().listeners()) {
                    Thread.startVirtualThread(() -> listener.onChats(whatsapp, chats));
                }
            }
            if(whatsapp.store().syncedContacts()) {
                var contacts = whatsapp.store().contacts();
                for(var listener : whatsapp.store().listeners()) {
                    Thread.startVirtualThread(() -> listener.onContacts(whatsapp, contacts));
                }
            }
            if(whatsapp.store().syncedNewsletters()) {
                var newsletters = whatsapp.store().newsletters();
                for(var listener : whatsapp.store().listeners()) {
                    Thread.startVirtualThread(() -> listener.onNewsletters(whatsapp, newsletters));
                }
            }
            if(whatsapp.store().syncedStatus()) {
                var status = whatsapp.store().status();
                for(var listener : whatsapp.store().listeners()) {
                    Thread.startVirtualThread(() -> listener.onStatus(whatsapp, status));
                }
            }
        } catch (Exception exception) {
            whatsapp.handleFailure(AUTH, exception);
        }
    }

    private void queryGroups() {
        if (whatsapp.store().syncedChats()) {
            return;
        }
        var _ = whatsapp.queryGroups();
    }

    private void pullInitialWebAppStatePatches() {
        if (!whatsapp.store().hasPreKeys() || whatsapp.store().syncedWebAppState()) {
            return;
        }
        whatsapp.pullWebAppState(PatchType.values());
        whatsapp.store()
                .setSyncedWebAppState(true);
    }

    private void setActiveConnection() {
        var active = new NodeBuilder()
                .description("active")
                .build();
        var query = new NodeBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .attribute("xmlns", "passive")
                .content(active);
        whatsapp.sendNode(query);
    }

    private void sendInitialPreKeys() {
        if (whatsapp.store().hasPreKeys()) {
            return;
        }

        whatsapp.sendPreKeys(PRE_KEYS_UPLOAD_CHUNK);
    }

    private void finishMobileLogin() {
        // TODO: Implement mobile login
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void onInitialInfo() {
        if(!whatsapp.store().registered()) {
            whatsapp.store()
                    .setRegistered(true);
            whatsapp.store()
                    .serialize();
        }

        for(var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onLoggedIn(whatsapp));
        }
    }

    private void queryInitial2fa() {
        var queryRequestBody = new NodeBuilder()
                .description("2fa")
                        .build();
        var queryRequest = new NodeBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .attribute("xmlns", "urn:xmpp:whatsapp:account")
                .content(queryRequestBody);
        whatsapp.sendNode(queryRequest);
    }

    private void queryInitialAboutPrivacy() {
        var queryRequestBody = new NodeBuilder()
                .description("privacy")
                .build();
        var queryRequest = new NodeBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .attribute("xmlns", "status")
                        .content(queryRequestBody);
        whatsapp.sendNode(queryRequest);
    }

    private void queryInitialPrivacySettings() {
        var queryRequestBody = new NodeBuilder()
                .description("privacy")
                .build();
        var queryRequest = new NodeBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .attribute("xmlns", "privacy")
                .content(queryRequestBody);
        var result = whatsapp.sendNode(queryRequest);
        result.streamChildren("privacy")
                .flatMap(Node::streamChildren)
                .forEach(this::addPrivacySetting);
    }

    private void addPrivacySetting(Node entry) {
        var privacyType = entry.getAttributeAsString("name")
                .flatMap(PrivacySettingType::of);
        if(privacyType.isEmpty()) {
            return;
        }

        var privacyValue = entry.getAttributeAsString("value")
                .flatMap(PrivacySettingValue::of);
        if(privacyValue.isEmpty()) {
            return;
        }

        var privacySetting = whatsapp.store()
                .findPrivacySetting(privacyType.get());
        var excluded = queryPrivacyExcludedContacts(privacyType.get(), privacyValue.get());
        var newEntry = new PrivacySettingEntryBuilder()
                .type(privacyType.get())
                .value(privacyValue.get())
                .excluded(excluded)
                .build();
        whatsapp.store().addPrivacySetting(newEntry);
    }

    private List<Jid> queryPrivacyExcludedContacts(PrivacySettingType type, PrivacySettingValue value) {
        if (value != PrivacySettingValue.CONTACTS_EXCEPT) {
            return List.of();
        }

        var queryRequestBodyContent = new NodeBuilder()
                .description("list")
                .attribute("name", type.data())
                .attribute("value", value.data())
                .build();
        var queryRequestBody = new NodeBuilder()
                .description("privacy")
                .content(queryRequestBodyContent)
                .build();
        var queryRequest = new NodeBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .attribute("xmlns", "privacy")
                .content(queryRequestBody);
        return whatsapp.sendNode(queryRequest)
                .streamChild("privacy")
                .flatMap(node -> node.streamChild("list"))
                .flatMap(node -> node.streamChildren("user"))
                .flatMap(user -> user.streamAttributeAsJid("jid"))
                .toList();
    }

    private void queryInitialDisappearingMode() {
        var queryRequest = new NodeBuilder()
                .description("iq")
                .attribute("to", JidServer.user())
                .attribute("type", "get")
                .attribute("xmlns", "disappearing_mode");
        whatsapp.sendNode(queryRequest);
    }

    private void queryInitialBlockList() {
        for (var jid : whatsapp.queryBlockList()) {
            markBlocked(jid);
        }
    }

    private void updateSelfPresence() {
        if (!whatsapp.store().automaticPresenceUpdates()) {
            if(!whatsapp.store().online()) {  // Just to be sure
                var presence = new NodeBuilder()
                        .description("presence")
                        .attribute("name", whatsapp.store().name())
                        .attribute("type", "unavailable")
                        .build();
                whatsapp.sendNodeWithNoResponse(presence);
            }
        }else {
            var presence = new NodeBuilder()
                    .description("presence")
                    .attribute("name", whatsapp.store().name())
                    .attribute("type", "available")
                    .build();
            whatsapp.sendNodeWithNoResponse(presence);
            onPresenceUpdated();
        }
    }

    private void onPresenceUpdated() {
        whatsapp.store().setOnline(true);
        whatsapp.store()
                .jid()
                .flatMap(whatsapp.store()::findContactByJid)
                .ifPresent(entry -> {
                    entry.setLastKnownPresence(ContactStatus.AVAILABLE);
                    entry.setLastSeen(ZonedDateTime.now());
                });
    }

    private void markBlocked(Jid entry) {
        whatsapp.store()
                .findContactByJid(entry)
                .orElseGet(() -> {
                    var newContact = whatsapp.store().addNewContact(entry);
                    for(var listener : whatsapp.store().listeners()) {
                        Thread.startVirtualThread(() -> listener.onNewContact(whatsapp, newContact));
                    }
                    return newContact;
                })
                .setBlocked(true);
    }

    private void scheduleMediaConnectionUpdate() {
        try {
            var mediaConn = new NodeBuilder()
                    .description("media_conn")
                    .build();
            var queryRequest = new NodeBuilder()
                    .description("iq")
                    .attribute("to", JidServer.user())
                    .attribute("type", "set")
                    .attribute("xmlns", "w:m")
                    .content(mediaConn);
            var queryResponse = whatsapp.sendNode(queryRequest);
            onMediaConnection(queryResponse);
        } catch (Exception throwable) {
            whatsapp.store().setMediaConnection(null);
            whatsapp.handleFailure(MEDIA_CONNECTION, throwable);
        }finally {
            var mediaConnectionTtl = whatsapp.store()
                    .mediaConnection()
                    .map(MediaConnection::ttl)
                    .orElse(DEFAULT_MEDIA_CONNECTION_TTL);
            var executor = CompletableFuture.delayedExecutor(mediaConnectionTtl, TimeUnit.SECONDS);
            executor.execute(this::scheduleMediaConnectionUpdate);
        }
    }

    private void onMediaConnection(Node node) {
        var mediaConnection = node.getChild("media_conn")
                .orElse(node);
        var auth = mediaConnection.getRequiredAttributeAsString("auth");
        var ttl = Math.toIntExact(mediaConnection.getRequiredAttributeAsLong("ttl"));
        var maxBuckets = Math.toIntExact(mediaConnection.getRequiredAttributeAsLong("max_buckets"));
        var timestamp = System.currentTimeMillis();
        var hosts = mediaConnection.streamChildren("host")
                .map(attributes -> attributes.getRequiredAttributeAsString("hostname"))
                .toList();
        var result = new MediaConnection(auth, ttl, maxBuckets, timestamp, hosts);
        whatsapp.store()
                .setMediaConnection(result);
    }

    private void queryNewsletters() {
        if (whatsapp.store().syncedNewsletters()) {
            return;
        }
        var newsletters = whatsapp.queryNewsletters();
        for(var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onNewsletters(whatsapp, newsletters));
        }
    }
}