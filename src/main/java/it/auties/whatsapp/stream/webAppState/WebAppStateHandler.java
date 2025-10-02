package it.auties.whatsapp.stream.webAppState;

import com.github.auties00.libsignal.key.SignalIdentityPublicKey;
import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappClientType;
import it.auties.whatsapp.stream.webAppState2.WebAppStatePushRequest;
import it.auties.whatsapp.io.node.Node;
import it.auties.whatsapp.io.node.NodeBuilder;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.exception.MalformedJidException;
import it.auties.whatsapp.model.action.*;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatMute;
import it.auties.whatsapp.model.companion.CompanionHashState;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.NewsletterMessageInfo;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.mobile.PhoneCountryLocale;
import it.auties.whatsapp.model.newsletter.Newsletter;
import it.auties.whatsapp.model.setting.LocaleSettings;
import it.auties.whatsapp.model.setting.PushNameSettings;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.model.setting.UnarchiveChatsSettings;
import it.auties.whatsapp.model.sync.*;
import it.auties.whatsapp.stream.webAppState2.WebAppStatePatch;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Medias;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.auties.whatsapp.api.WhatsappErrorHandler.Location.*;

// TODO: Optimize and rewrite me
public final class WebAppStateHandler extends AbstractHandler {
    private static final int PULL_ATTEMPTS = 3;
    private static final Integer MAC_LENGTH = 8;
    
    private final ConcurrentMap<PatchType, Integer> attempts;

    public WebAppStateHandler(Whatsapp whatsapp) {
        super(whatsapp);
        this.attempts = new ConcurrentHashMap<>();
    }

    public synchronized void push(Jid jid, List<WebAppStatePushRequest> patches) {
        var clientType = whatsapp.store().clientType();
        if (clientType == WhatsappClientType.WEB) {
            var patchTypes = patches.stream()
                    .map(WebAppStatePushRequest::type)
                    .collect(Collectors.toUnmodifiableSet());
            pull(jid, patchTypes);
        }
        try {
            var requests = patches.stream()
                    .map(entry -> createPushRequest(jid, entry))
                    .toList();
            var mobile = whatsapp.store().clientType() == WhatsappClientType.MOBILE;
            var body = requests.stream()
                    .map(request -> createPushRequestNode(request, mobile))
                    .toList();
            var sync = new NodeBuilder()
                    .attribute("data_namespace", 3, mobile)
                    .content(body)
                    .build();
            var node = new NodeBuilder()
                    .description("iq")
                    .attribute("method", "set")
                    .attribute("xmlns", "w:sync:app:state")
                    .content(sync)
                    .build();
            var resultNode = whatsapp.sendNode(node);
            parseSyncRequest(resultNode);
            onPush(jid, requests, clientType == WhatsappClientType.WEB);
        } catch (Throwable throwable) {
            whatsapp.handleFailure(PUSH_WEB_APP_STATE, throwable);
        }
    }

    private PushRequest createPushRequest(Jid jid, WebAppStatePushRequest request) {
        var oldState = whatsapp.keys()
                .findWebAppHashStateByName(jid, request.type())
                .orElseGet(() -> new CompanionHashState(request.type()));
        var newState = oldState.copy();
        var key = whatsapp.keys().getLatestAppKey(jid);
        var mutationKeys = MutationKeys.of(key.keyData().keyData());
        var syncId = new KeyId(key.keyId().value());
        var mutations = request.patches()
                .stream()
                .map(patch -> createMutationSync(patch, mutationKeys, key, syncId))
                .toList();
        var newStateGenerator = new LTHash(newState);
        for (var mutation : mutations) {
            newStateGenerator.mix(mutation.indexMac(), mutation.valueMac(), mutation.operation());
        }
        var result = newStateGenerator.finish();
        newState.setHash(result.hash());
        newState.setIndexValueMap(result.indexValueMap());
        newState.setVersion(newState.version() + 1);
        var snapshotMac = generateSnapshotMac(newState.hash(), newState.version(), request.type(), mutationKeys.snapshotMacKey());
        var patchMac = generatePatchMac(snapshotMac, mutations, newState.version(), request.type(), mutationKeys.patchMacKey());
        var syncs = mutations.stream()
                .map(MutationResult::sync)
                .toList();
        var sync = new PatchSyncBuilder()
                .patchMac(patchMac)
                .snapshotMac(snapshotMac)
                .keyId(syncId)
                .mutations(syncs)
                .build();
        return new PushRequest(request.type(), oldState, newState, sync);
    }

    private MutationResult createMutationSync(WebAppStatePatch patch, MutationKeys mutationKeys, AppStateSyncKey key, KeyId syncId) {
        try {
            var index = patch.index()
                    .getBytes(StandardCharsets.UTF_8);
            var version = patch.sync()
                    .version()
                    .orElseThrow(() -> new IllegalArgumentException("Empty patch sync"));
            var actionData = new ActionDataSyncBuilder()
                    .index(index)
                    .value(patch.sync())
                    .padding(new byte[0])
                    .version(version)
                    .build();
            var encoded = ActionDataSyncSpec.encode(actionData);
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var keySpec = new SecretKeySpec(mutationKeys.encKey(), "AES");
            var iv = Bytes.random(16);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
            var encryptedLength = cipher.getOutputSize(encoded.length);
            var encrypted = new byte[iv.length + encryptedLength];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            cipher.doFinal(encoded, 0, encoded.length, encrypted, iv.length);
            var valueMac = generateMac(patch.operation(), encrypted, encrypted.length, key.keyId().value(), mutationKeys.macKey());
            var hmacSHA256 = Mac.getInstance("HmacSHA256");
            var hmacKey = new SecretKeySpec(mutationKeys.indexKey(), "HmacSHA256");
            hmacSHA256.init(hmacKey);
            var indexMac = hmacSHA256.doFinal(index);
            var record = new RecordSyncBuilder()
                    .index(new IndexSync(indexMac))
                    .value(new ValueSync(Bytes.concat(encrypted, valueMac)))
                    .keyId(syncId)
                    .build();
            var sync = new MutationSyncBuilder()
                    .operation(patch.operation())
                    .record(record)
                    .build();
            return new MutationResult(sync, indexMac, valueMac, patch.operation());
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot encrypt data", exception);
        }
    }

    private Node createPushRequestNode(PushRequest request, boolean mobile) {
        var version = request.oldState().version();
        var collectionAttributes = NodeAttributes.of()
                .put("name", request.type())
                .put("version", version, !mobile)
                .put("return_snapshot", false, !mobile)
                .toMap();
        return Node.of("collection", collectionAttributes,
                Node.of("patch", PatchSyncSpec.encode(request.sync())));
    }

    private void onPush(Jid jid, List<PushRequest> requests, boolean readPatches) {
        requests.forEach(request -> {
            whatsapp.keys().addWebAppHashState(jid, request.newState());
            if (!readPatches) {
                return;
            }

            var patch = new PatchSyncBuilder()
                    .version(new VersionSync(request.newState().version()))
                    .keyId(request.sync().keyId())
                    .deviceIndex(request.sync().deviceIndex())
                    .patchMac(request.sync().patchMac())
                    .exitCode(request.sync().exitCode())
                    .externalMutations(request.sync().externalMutations())
                    .mutations(request.sync().mutations())
                    .snapshotMac(request.sync().snapshotMac())
                    .build();
            var results = decodePatches(jid, request.type(), List.of(patch), request.oldState());
            results.records().forEach(this::processActions);
        });
    }

    public void pull(boolean initial, PatchType... patchTypes) {
        if (patchTypes == null || patchTypes.length == 0) {
            return;
        }

        var jid = whatsapp.store().jid();
        if (jid.isEmpty()) {
            return;
        }

        try {
            var success = pull(jid.get(), Set.of(patchTypes));
            if(success) {
                attempts.clear();
            }
        } catch (Throwable exception) {
            attempts.clear();
            whatsapp.handleFailure(initial ? INITIAL_WEB_APP_STATE_SYNC : PULL_WEB_APP_STATE, exception);
        }
    }

    private boolean pull(Jid jid, Set<PatchType> patchTypes) {
        try {
            pullSemaphore.acquire();
            var tempStates = new HashMap<PatchType, CompanionHashState>();
            var nodes = getPullNodes(jid, patchTypes, tempStates);
            var resultNode = whatsapp.sendQuery("set", "w:sync:app:state", Node.of("sync", nodes));
            var records = parseSyncRequest(resultNode);
            var remaining = decodeSyncs(jid, tempStates, records);
            return handlePullResult(jid, remaining);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        } finally {
            pullSemaphore.release();
        }
    }

    private boolean handlePullResult(Jid jid, Set<PatchType> remaining) {
        return remaining.isEmpty() || pull(jid, remaining);
    }

    private List<Node> getPullNodes(Jid jid, Set<PatchType> patchTypes, Map<PatchType, CompanionHashState> tempStates) {
        return patchTypes.stream()
                .map(name -> createStateWithVersion(jid, name))
                .peek(state -> tempStates.put(state.type(), state))
                .map(CompanionHashState::toNode)
                .toList();
    }

    private CompanionHashState createStateWithVersion(Jid jid, PatchType name) {
        return whatsapp.keys()
                .findWebAppHashStateByName(jid, name)
                .orElseGet(() -> new CompanionHashState(name));
    }

    private Set<PatchType> decodeSyncs(Jid jid, Map<PatchType, CompanionHashState> tempStates, List<SnapshotSyncRecord> records) {
        return records.stream()
                .map(record -> {
                    var chunk = decodeSync(jid, record, tempStates);
                    chunk.records().forEach(this::processActions);
                    return chunk;
                })
                .filter(PatchChunk::hasMore)
                .map(PatchChunk::patchType)
                .collect(Collectors.toUnmodifiableSet());
    }

    private PatchChunk decodeSync(Jid jid, SnapshotSyncRecord record, Map<PatchType, CompanionHashState> tempStates) {
        try {
            var results = new ArrayList<ActionDataSync>();
            if (record.hasSnapshot()) {
                var snapshot = decodeSnapshot(jid, record.patchType(), record.snapshot());
                snapshot.ifPresent(decodedSnapshot -> {
                    results.addAll(decodedSnapshot.records());
                    tempStates.put(record.patchType(), decodedSnapshot.state());
                    whatsapp.keys().addWebAppHashState(jid, decodedSnapshot.state());
                });
            }
            if (record.hasPatches()) {
                var decodedPatches = decodePatches(jid, record.patchType(), record.patches(), tempStates.get(record.patchType()));
                results.addAll(decodedPatches.records());
                whatsapp.keys().addWebAppHashState(jid, decodedPatches.state());
            }
            return new PatchChunk(record.patchType(), results, record.hasMore());
        } catch (Throwable throwable) {
            var hashState = new CompanionHashState(record.patchType());
            whatsapp.keys().addWebAppHashState(jid, hashState);
            attempts.put(record.patchType(), attempts.getOrDefault(record.patchType(), 0) + 1);
            if (attempts.get(record.patchType()) >= PULL_ATTEMPTS) {
                throw new RuntimeException("Cannot parse patch(%s tries)".formatted(PULL_ATTEMPTS), throwable);
            }
            return decodeSync(jid, record, tempStates);
        }
    }

    private List<SnapshotSyncRecord> parseSyncRequest(Node node) {
        return Stream.ofNullable(node)
                .map(sync -> sync.listChildren("sync"))
                .flatMap(Collection::stream)
                .map(sync -> sync.listChildren("collection"))
                .flatMap(Collection::stream)
                .map(this::parseSync)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<SnapshotSyncRecord> parseSync(Node sync) {
        var patchName = sync.getOptionalAttribute("name")
                .orElseThrow(() -> new IllegalArgumentException("Missing patch name: " + sync));
        var patchType = PatchType.of(patchName.toString());
        var patchResult = sync.getOptionalAttribute("type");
        if(patchResult.isPresent() && patchResult.get().toString().equals("error")) {

        }
        if (sync.getOptionalAttribute("type" "error")) {
            throw new IllegalArgumentException("App state sync failed");
        }
        var more = sync.attributes().getBoolean("has_more_patches");
        var snapshotSync = sync.findChild("snapshot")
                .flatMap(this::decodeSnapshot)
                .orElse(null);
        var versionCode = sync.attributes().getInt("version");
        var patches = sync.findChild("patches")
                .orElse(sync)
                .listChildren("patch")
                .stream()
                .map(patch -> decodePatch(patch, versionCode))
                .flatMap(Optional::stream)
                .toList();
        var result = new SnapshotSyncRecord(patchType, snapshotSync, patches, more);
        return Optional.of(result);
    }

    private Optional<SnapshotSync> decodeSnapshot(Node snapshot) {
        if (snapshot == null) {
            return Optional.empty();
        }

        var externalBlobPayload = snapshot.content()
                .map(NodeContent::toBuffer)
                .orElse(null);
        if (externalBlobPayload == null) {
            return Optional.empty();
        }

        var blob = ExternalBlobReferenceSpec.decode(ProtobufInputStream.fromBuffer(externalBlobPayload));
        var decodeSnapshot = Medias.download(blob, stream -> {
            try (var protobufStream = ProtobufInputStream.fromStream(stream)) {
                return SnapshotSyncSpec.decode(protobufStream);
            } catch (Throwable throwable) {
                throw new RuntimeException("Cannot decode snapshot", throwable);
            }
        });
        return Optional.of(decodeSnapshot);
    }


    private Optional<PatchSync> decodePatch(Node patch, long versionCode) {
        var result = patch.content()
                .map(content -> PatchSyncSpec.decode(ProtobufInputStream.fromBuffer(content.toBuffer())));
        result.ifPresent(patchSync -> {
            if (!patchSync.hasVersion()) {
                var version = new VersionSync(versionCode + 1);
                patchSync.setVersion(version);
            }
        });
        return result;
    }

    private void processActions(ActionDataSync mutation) {
        var value = mutation.value();
        if (value == null) {
            return;
        }

        value.action()
                .ifPresent(action -> onAction(mutation, action));
        value.setting()
                .ifPresent(this::onSetting);
        mutation.value()
                .primaryFeature()
                .ifPresent(this::onPrimaryFeature);
    }

    private void onPrimaryFeature(PrimaryFeature primaryFeature) {
        for(var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onWebAppPrimaryFeatures(primaryFeature.flags()));
            Thread.startVirtualThread(() -> listener.onWebAppPrimaryFeatures(whatsapp, primaryFeature.flags()));
        }
    }

    private void onSetting(Setting setting) {
        switch (setting) {
            case LocaleSettings localeSettings -> {
                var oldLocale = whatsapp.store().locale();
                PhoneCountryLocale.of(localeSettings.locale())
                        .ifPresent(newLocale -> whatsapp.updateLocale(newLocale, oldLocale.orElse(null)));
            }
            case PushNameSettings pushNameSettings -> {
                var oldName = whatsapp.store().name();
                whatsapp.onUserChanged(pushNameSettings.name(), oldName);
            }
            case UnarchiveChatsSettings unarchiveChatsSettings -> {
                var settingValue = unarchiveChatsSettings.unarchiveChats();
                whatsapp.store().setUnarchiveChats(settingValue);
            }
            default -> {}
        }
        for(var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onWebAppStateSetting(setting));
            Thread.startVirtualThread(() -> listener.onWebAppStateSetting(whatsapp, setting));
        }
    }

    private void onAction(ActionDataSync mutation, Action action) {
        var messageIndex = mutation.messageIndex();
        var targetJid = messageIndex.targetId()
                .flatMap(this::tryParseJid);
        var targetContact = targetJid.flatMap(whatsapp.store()::findContactByJid);
        var targetChat = targetJid.flatMap(whatsapp.store()::findChatByJid);
        var targetNewsletter = targetJid.flatMap(whatsapp.store()::findNewsletterByJid);
        var targetChatMessage = targetChat.flatMap(chat -> {
            var messageId = mutation.messageIndex().messageId().orElse(null);
            return whatsapp.store()
                    .findMessageById(chat, messageId);
        });
        var targetNewsletterMessage = targetNewsletter.flatMap(newsletter -> {
            var messageId = mutation.messageIndex().messageId().orElse(null);
            return whatsapp.store()
                    .findMessageById(newsletter, messageId);
        });
        switch (action) {
            case ClearChatAction clearChatAction -> {
                var chat = targetChat.orElse(null);
                clearMessages(chat, clearChatAction);
            }
            case ContactAction contactAction -> {
                var contact = targetContact.orElseGet(() -> {
                    var newContact = whatsapp.store()
                            .addContact(targetJid.orElseThrow());
                    for (var listener : whatsapp.store().listeners()) {
                        Thread.startVirtualThread(() -> listener.onNewContact(newContact));
                        Thread.startVirtualThread(() -> listener.onNewContact(whatsapp, newContact));
                    }
                    return newContact;
                });
                var chat = targetChat.orElseGet(() -> whatsapp.store()
                        .addNewChat(targetJid.orElseThrow()));
                updateName(contact, chat, contactAction);
            }
            case DeleteMessageForMeAction ignored -> {
                targetChatMessage.ifPresent(message -> deleteMessageForMe(message, targetChat.orElse(null)));
                targetNewsletterMessage.ifPresent(message -> deleteMessageForMe(message, targetNewsletter.orElse(null)));
            }
            case MarkChatAsReadAction markAction -> targetChat.ifPresent(chat -> {
                var read = markAction.read() ? 0 : -1;
                chat.setUnreadMessagesCount(read);
            });
            case MuteAction muteAction -> targetChat.ifPresent(chat -> {
                var timestamp = muteAction.muteEndTimestampSeconds();
                chat.setMute(ChatMute.muted(timestamp));
            });
            case PinAction pinAction -> targetChat.ifPresent(chat -> {
                var timestamp = pinAction.pinned() ? (int) mutation.value().timestamp() : 0;
                chat.setPinnedTimestampSeconds(timestamp);
            });
            case StarAction starAction -> targetChatMessage.ifPresent(message -> {
                var starred = starAction.starred();
                message.setStarred(starred);
            });
            case ArchiveChatAction archiveChatAction -> targetChat.ifPresent(chat -> {
                var archived = archiveChatAction.archived();
                chat.setArchived(archived);
            });
            case TimeFormatAction timeFormatAction -> {
                var format = timeFormatAction.twentyFourHourFormatEnabled();
                whatsapp.store().setTwentyFourHourFormat(format);
            }
            case DeleteChatAction ignored -> targetChat.ifPresent(Chat::removeMessages);
            default -> {}
        }
        for(var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onWebAppStateAction(action, messageIndex));
            Thread.startVirtualThread(() -> listener.onWebAppStateAction(whatsapp, action, messageIndex));
        }
    }

    private void deleteMessageForMe(ChatMessageInfo message, Chat chat) {
        if(chat != null) {
            chat.removeMessage(message);
        }
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onMessageDeleted(message, false));
            Thread.startVirtualThread(() -> listener.onMessageDeleted(whatsapp, message, false));
        }
    }

    private void deleteMessageForMe(NewsletterMessageInfo message, Newsletter newsletter) {
        if(newsletter != null) {
            newsletter.removeMessage(message);
        }
        for (var listener : whatsapp.store().listeners()) {
            Thread.startVirtualThread(() -> listener.onMessageDeleted(message, false));
            Thread.startVirtualThread(() -> listener.onMessageDeleted(whatsapp, message, false));
        }
    }

    private Optional<Jid> tryParseJid(String id) {
        try {
            return Optional.of(Jid.of(id));
        }catch(MalformedJidException exception) {
            return Optional.empty();
        }
    }

    private void clearMessages(Chat targetChat, ClearChatAction clearChatAction) {
        if (targetChat == null) {
            return;
        }

        if (clearChatAction.messageRange().isEmpty()) {
            targetChat.removeMessages();
            return;
        }

        clearChatAction.messageRange()
                .stream()
                .map(ActionMessageRangeSync::messages)
                .flatMap(Collection::stream)
                .map(SyncActionMessage::key)
                .filter(Objects::nonNull)
                .forEach(key -> targetChat.removeMessage(entry -> Objects.equals(entry.id(), key.id())));
    }

    private void updateName(Contact contact, Chat chat, ContactAction contactAction) {
        contactAction.fullName().ifPresent(contact::setFullName);
        contactAction.firstName().ifPresent(contact::setShortName);
        contactAction.name().ifPresent(chat::setName);
    }

    private SyncRecord decodePatches(Jid jid, PatchType name, List<PatchSync> patches, CompanionHashState state) {
        var newState = state.copy();
        var results = patches.stream()
                .map(patch -> decodePatch(jid, name, newState, patch))
                .map(MutationsRecord::records)
                .flatMap(Collection::stream)
                .toList();
        return new SyncRecord(newState, results);
    }

    private MutationsRecord decodePatch(Jid jid, PatchType patchType, CompanionHashState newState, PatchSync patch) {
        if (patch.hasExternalMutations()) {
            var mutationsSync = Medias.download(patch.externalMutations(), stream -> {
                try(var protobufStream = ProtobufInputStream.fromStream(stream)) {
                    return MutationsSyncSpec.decode(protobufStream);
                }catch (Exception exception) {
                    throw new RuntimeException("Cannot decode mutations", exception);
                }
            });
            patch.mutations().addAll(mutationsSync.mutations());
        }

        newState.setVersion(patch.encodedVersion());
        if(whatsapp.store().checkPatchMacs()) {
            var patchMac = generatePatchMac(jid, patch, patchType);
            if(patchMac.isPresent() && !Arrays.equals(patchMac.get(), patch.patchMac())) {
                throw new HmacValidationException("sync_mac");
            }
        }

        var mutations = decodeMutations(jid, patch.mutations(), newState);
        newState.setHash(mutations.result().hash());
        newState.setIndexValueMap(mutations.result().indexValueMap());
        if(whatsapp.store().checkPatchMacs()) {
            var snapshotMac = getMutationKeys(jid, patch.keyId())
                    .map(mutationKeys -> generateSnapshotMac(newState.hash(), newState.version(), patchType, mutationKeys.snapshotMacKey()));
            if(snapshotMac.isPresent() && !Arrays.equals(snapshotMac.get(), patch.snapshotMac())) {
                throw new HmacValidationException("snapshot_mac");
            }
        }

        return mutations;
    }

    private Optional<SyncRecord> decodeSnapshot(Jid jid, PatchType name, SnapshotSync snapshot) {
        var mutationKeys = getMutationKeys(jid, snapshot.keyId());
        if (mutationKeys.isEmpty()) {
            return Optional.empty();
        }
        var newState = new CompanionHashState(name, snapshot.version().version());
        var mutations = decodeMutations(jid, snapshot.records(), newState);
        newState.setHash(mutations.result().hash());
        newState.setIndexValueMap(mutations.result().indexValueMap());
        if(whatsapp.store().checkPatchMacs()) {
            var snapshotMac = generateSnapshotMac(newState.hash(), newState.version(), name, mutationKeys.get().snapshotMacKey());
            if(!Arrays.equals(snapshotMac, snapshot.mac())) {
                throw new HmacValidationException("decode_snapshot");
            }
        }
        return Optional.of(new SyncRecord(newState, mutations.records()));
    }

    private Optional<MutationKeys> getMutationKeys(Jid jid, KeyId snapshot) {
        return whatsapp.keys()
                .findWebAppStateKeyById(jid, snapshot.id())
                .map(AppStateSyncKey::keyData)
                .map(AppStateSyncKeyData::keyData)
                .map(MutationKeys::of);
    }

    private MutationsRecord decodeMutations(Jid jid, List<? extends Syncable> syncs, CompanionHashState state) {
        var generator = new LTHash(state);
        var mutations = syncs.stream()
                .map(mutation -> decodeMutation(jid, mutation.operation(), mutation.record(), generator))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
        return new MutationsRecord(generator.finish(), mutations);
    }

    private Optional<ActionDataSync> decodeMutation(Jid jid, RecordSync.Operation operation, RecordSync sync, LTHash generator) {
        try {
            var mutationKeys = getMutationKeys(jid, sync.keyId());
            if (mutationKeys.isEmpty()) {
                return Optional.empty();
            }

            var blob = sync.value().blob();
            if(whatsapp.store().checkPatchMacs()) {
                var expectedMac = generateMac(operation, blob, blob.length - SignalIdentityPublicKey.length(), sync.keyId().id(), mutationKeys.get().macKey());
                if(!Arrays.equals(
                        blob, blob.length - SignalIdentityPublicKey.length(), blob.length,
                        expectedMac, 0, SignalIdentityPublicKey.length()
                )) {
                    throw new HmacValidationException("decode_mutation");
                }
            }
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var keySpec = new SecretKeySpec(mutationKeys.get().encKey(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(blob, 0, 16));
            var result = cipher.doFinal(blob, 16, blob.length - SignalIdentityPublicKey.length() - 16);
            var actionSync = ActionDataSyncSpec.decode(result);
            if(whatsapp.store().checkPatchMacs()) {
                var hmacSHA256 = Mac.getInstance("HmacSHA256");
                var hmacKey = new SecretKeySpec(mutationKeys.get().indexKey(), "HmacSHA256");
                hmacSHA256.init(hmacKey);
                var expectedMac = hmacSHA256.doFinal(actionSync.index());
                if(!Arrays.equals(sync.index().blob(), expectedMac)) {
                    throw new HmacValidationException("decode_mutation");
                }
            }
            generator.mix(sync.index().blob(), Arrays.copyOfRange(blob, blob.length - SignalIdentityPublicKey.length(), blob.length), operation);
            return Optional.of(actionSync);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot decrypt data", exception);
        }
    }

    private byte[] generateMac(RecordSync.Operation operation, byte[] data, int dataLength, byte[] keyId, byte[] key) {
        try {
            var total = new byte[1 + keyId.length + dataLength + MAC_LENGTH];
            total[0] = operation.content();
            System.arraycopy(keyId, 0, total, 1, keyId.length);
            System.arraycopy(data, 0, total, 1 + keyId.length, dataLength);
            total[total.length - 1] = (byte) (keyId.length + 1);
            var localMac = Mac.getInstance("HmacSHA512");
            localMac.init(new SecretKeySpec(key, "HmacSHA512"));
            var sha512 = localMac.doFinal(total);
            return Arrays.copyOfRange(sha512, 0, SignalIdentityPublicKey.length());
        }catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot calculate hmac", exception);
        }
    }

    private byte[] generateSnapshotMac(byte[] ltHash, long version, PatchType patchType, byte[] key) {
        try {
            var localMac = Mac.getInstance("HmacSHA256");
            localMac.init(new SecretKeySpec(key, "HmacSHA256"));
            localMac.update(ltHash);
            digestLong(localMac, version);
            localMac.update(patchType.toString().getBytes());
            return localMac.doFinal();
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot calculate hmac", exception);
        }
    }

    private byte[] generatePatchMac(byte[] snapshotMac, List<MutationResult> mutations, long version, PatchType patchType, byte[] key) {
        try {
            var localMac = Mac.getInstance("HmacSHA256");
            localMac.init(new SecretKeySpec(key, "HmacSHA256"));
            localMac.update(snapshotMac);
            for(var mutation : mutations) {
                localMac.update(mutation.valueMac());
            }
            digestLong(localMac, version);
            localMac.update(patchType.toString().getBytes());
            return localMac.doFinal();
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot calculate hmac", exception);
        }
    }

    private Optional<byte[]> generatePatchMac(Jid jid, PatchSync patch, PatchType patchType) {
        return getMutationKeys(jid, patch.keyId()).map(mutationKeys -> {
            try {
                var localMac = Mac.getInstance("HmacSHA256");
                localMac.init(new SecretKeySpec(mutationKeys.patchMacKey(), "HmacSHA256"));
                localMac.update(patch.snapshotMac());
                for(var mutation : patch.mutations()) {
                    var blob = mutation.record().value().blob();
                    localMac.update(blob, blob.length - SignalIdentityPublicKey.length(), SignalIdentityPublicKey.length());
                }
                digestLong(localMac, patch.encodedVersion());
                localMac.update(patchType.toString().getBytes());
                return localMac.doFinal();
            } catch (GeneralSecurityException exception) {
                throw new IllegalArgumentException("Cannot calculate hmac", exception);
            }
        });
    }

    private void digestLong(Mac mac, long n) {
        mac.update((byte) (n >> 56));
        mac.update((byte) (n >> 48));
        mac.update((byte) (n >> 40));
        mac.update((byte) (n >> 32));
        mac.update((byte) (n >> 24));
        mac.update((byte) (n >> 16));
        mac.update((byte) (n >> 8));
        mac.update((byte) n);
    }


    public void dispose() {
        attempts.clear();
    }
}