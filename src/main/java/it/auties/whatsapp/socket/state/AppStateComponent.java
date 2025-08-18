package it.auties.whatsapp.socket.state;

import it.auties.protobuf.stream.ProtobufInputStream;
import it.auties.whatsapp.api.WhatsappClientType;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.crypto.LTHash;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.exception.MalformedJidException;
import it.auties.whatsapp.model.action.*;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatMute;
import it.auties.whatsapp.model.companion.CompanionHashState;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.mobile.CountryLocale;
import it.auties.whatsapp.model.node.Attributes;
import it.auties.whatsapp.model.node.Node;
import it.auties.whatsapp.model.setting.LocaleSettings;
import it.auties.whatsapp.model.setting.PushNameSettings;
import it.auties.whatsapp.model.setting.Setting;
import it.auties.whatsapp.model.setting.UnarchiveChatsSettings;
import it.auties.whatsapp.model.sync.*;
import it.auties.whatsapp.model.sync.PatchRequest.PatchEntry;
import it.auties.whatsapp.socket.SocketConnection;
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Medias;
import it.auties.whatsapp.util.SignalConstants;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.auties.whatsapp.api.WhatsappErrorHandler.Location.*;

// TODO: Optimize and rewrite me
public final class AppStateComponent {
    private static final int PULL_ATTEMPTS = 3;

    private final SocketConnection socketConnection;
    private final ConcurrentMap<PatchType, Integer> attempts;
    private final Semaphore pullSemaphore;
    private final Semaphore pushSemaphore;

    public AppStateComponent(SocketConnection socketConnection) {
        this.socketConnection = socketConnection;
        this.attempts = new ConcurrentHashMap<>();
        this.pullSemaphore = new Semaphore(1, true);
        this.pushSemaphore = new Semaphore(1, true);
    }

    public void push(Jid jid, List<PatchRequest> patches) {
        var clientType = socketConnection.store().clientType();
        if (clientType == WhatsappClientType.WEB) {
            pull(jid, getPatchesTypes(patches));
        }
        sendPush(jid, patches, clientType != WhatsappClientType.MOBILE);
    }

    private Set<PatchType> getPatchesTypes(List<PatchRequest> patches) {
        return patches.stream()
                .map(PatchRequest::type)
                .collect(Collectors.toUnmodifiableSet());
    }

    private void sendPush(Jid jid, List<PatchRequest> patches, boolean readPatches) {
        try {
            pushSemaphore.acquire();
            var requests = patches.stream()
                    .map(entry -> createPushRequest(jid, entry))
                    .toList();
            var mobile = socketConnection.store().clientType() == WhatsappClientType.MOBILE;
            var body = requests.stream()
                    .map(request -> createPushRequestNode(request, mobile))
                    .toList();
            var syncAttributes = Attributes.of()
                    .put("data_namespace", 3, mobile)
                    .toMap();
            var sync = Node.of("sync", syncAttributes, body);
            var resultNode = socketConnection.sendQuery("set", "w:sync:app:state", sync);
            parseSyncRequest(resultNode);
            onPush(jid, requests, readPatches);
        } catch (Throwable throwable) {
            socketConnection.handleFailure(PUSH_APP_STATE, throwable);
        } finally {
            pushSemaphore.release();
        }
    }

    private PushRequest createPushRequest(Jid jid, PatchRequest request) {
        var oldState = socketConnection.keys()
                .findHashStateByName(jid, request.type())
                .orElseGet(() -> new CompanionHashState(request.type()));
        var newState = oldState.copy();
        var key = socketConnection.keys().getLatestAppKey(jid);
        var mutationKeys = MutationKeys.of(key.keyData().keyData());
        var syncId = new KeyId(key.keyId().keyId());
        var mutations = request.entries()
                .stream()
                .map(patch -> createMutationSync(patch, mutationKeys, key, syncId))
                .toList();
        var newStateGenerator = new LTHash(newState);
        mutations.forEach(mutation -> newStateGenerator.mix(mutation.indexMac(), mutation.valueMac(), mutation.operation()));
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

    private MutationResult createMutationSync(PatchEntry patch, MutationKeys mutationKeys, AppStateSyncKey key, KeyId syncId) {
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
            var valueMac = generateMac(patch.operation(), encrypted, encrypted.length, key.keyId().keyId(), mutationKeys.macKey());
            var indexMac = Hmac.calculateSha256(index, mutationKeys.indexKey());
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
        var collectionAttributes = Attributes.of()
                .put("name", request.type())
                .put("version", version, !mobile)
                .put("return_snapshot", false, !mobile)
                .toMap();
        return Node.of("collection", collectionAttributes,
                Node.of("patch", PatchSyncSpec.encode(request.sync())));
    }

    private void onPush(Jid jid, List<PushRequest> requests, boolean readPatches) {
        requests.forEach(request -> {
            socketConnection.keys().addState(jid, request.newState());
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

    public void pull(PatchType... patchTypes) {
        if (patchTypes == null || patchTypes.length == 0) {
            return;
        }

        var jid = socketConnection.store().jid();
        if (jid.isEmpty()) {
            return;
        }

        try {
            var success = pull(jid.get(), Set.of(patchTypes));
            onPull(false, success);
        } catch (Throwable exception) {
            onPullError(false, exception);
        }
    }

    public void pullInitial() {
        if (socketConnection.keys().initialAppSync()) {
            return;
        }

        var jid = socketConnection.store().jid();
        if (jid.isPresent()) {
            try {
                var success = pull(jid.get(), Set.of(PatchType.values()));
                onPull(true, success);
            } catch (Throwable exception) {
                onPullError(true, exception);
            }
        }
    }

    private void onPull(boolean initial, boolean success) {
        if (!socketConnection.keys().initialAppSync()) {
            var result = (initial && success) || isSyncComplete();
            if(result) {
                socketConnection.keys().setInitialAppSync(true);
                socketConnection.queryNewsletters();
            }
        }

        attempts.clear();
    }

    private boolean isSyncComplete() {
        return Arrays.stream(PatchType.values())
                .allMatch(this::isSyncComplete);
    }

    private boolean isSyncComplete(PatchType entry) {
        var jid = socketConnection.store().jid();
        return jid.isPresent() && socketConnection.keys()
                .findHashStateByName(jid.get(), entry)
                .filter(type -> type.version() > 0)
                .isPresent();
    }

    private void onPullError(boolean initial, Throwable exception) {
        attempts.clear();
        if (initial) {
            socketConnection.handleFailure(INITIAL_APP_STATE_SYNC, exception);
            return;
        }
        socketConnection.handleFailure(PULL_APP_STATE, exception);
    }

    private boolean pull(Jid jid, Set<PatchType> patchTypes) {
        try {
            pullSemaphore.acquire();
            var tempStates = new HashMap<PatchType, CompanionHashState>();
            var nodes = getPullNodes(jid, patchTypes, tempStates);
            var resultNode = socketConnection.sendQuery("set", "w:sync:app:state", Node.of("sync", nodes));
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
        return socketConnection.keys()
                .findHashStateByName(jid, name)
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
                    socketConnection.keys().addState(jid, decodedSnapshot.state());
                });
            }
            if (record.hasPatches()) {
                var decodedPatches = decodePatches(jid, record.patchType(), record.patches(), tempStates.get(record.patchType()));
                results.addAll(decodedPatches.records());
                socketConnection.keys().addState(jid, decodedPatches.state());
            }
            return new PatchChunk(record.patchType(), results, record.hasMore());
        } catch (Throwable throwable) {
            var hashState = new CompanionHashState(record.patchType());
            socketConnection.keys().addState(jid, hashState);
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
        var name = PatchType.of(sync.attributes().getString("name"));
        if (sync.attributes().hasValue("type", "error")) {
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
        return Optional.of(new SnapshotSyncRecord(name, snapshotSync, patches, more));
    }

    private Optional<SnapshotSync> decodeSnapshot(Node snapshot) {
        if (snapshot == null) {
            return Optional.empty();
        }

        var externalBlobPayload = snapshot.contentAsBytes()
                .orElse(null);
        if (externalBlobPayload == null) {
            return Optional.empty();
        }

        var blob = ExternalBlobReferenceSpec.decode(externalBlobPayload);
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
        if (!patch.hasContent()) {
            return Optional.empty();
        }
        var patchSync = PatchSyncSpec.decode(patch.contentAsBytes().orElseThrow());
        if (!patchSync.hasVersion()) {
            var version = new VersionSync(versionCode + 1);
            patchSync.setVersion(version);
        }
        return Optional.of(patchSync);
    }

    private void processActions(ActionDataSync mutation) {
        var value = mutation.value();
        if (value == null) {
            return;
        }

        value.action().ifPresent(action -> onAction(mutation, action));
        value.setting().ifPresent(this::onSetting);
        mutation.value().primaryFeature().ifPresent(socketConnection::onFeatures);
    }

    private void onSetting(Setting setting) {
        switch (setting) {
            case LocaleSettings localeSettings -> {
                var oldLocale = socketConnection.store().locale();
                CountryLocale.of(localeSettings.locale())
                        .ifPresent(newLocale -> socketConnection.updateLocale(newLocale, oldLocale.orElse(null)));
            }
            case PushNameSettings pushNameSettings -> {
                var oldName = socketConnection.store().name();
                socketConnection.onUserChanged(pushNameSettings.name(), oldName);
            }
            case UnarchiveChatsSettings unarchiveChatsSettings -> {
                var settingValue = unarchiveChatsSettings.unarchiveChats();
                socketConnection.store().setUnarchiveChats(settingValue);
            }
            default -> {}
        }
        socketConnection.onSetting(setting);
    }

    private void onAction(ActionDataSync mutation, Action action) {
        var messageIndex = mutation.messageIndex();
        var targetJid = messageIndex.targetId()
                .flatMap(this::tryParseJid);
        var targetContact = targetJid.flatMap(socketConnection.store()::findContactByJid);
        var targetChat = targetJid.flatMap(socketConnection.store()::findChatByJid);
        var targetNewsletter = targetJid.flatMap(socketConnection.store()::findNewsletterByJid);
        var targetChatMessage = targetChat.flatMap(chat -> {
            var messageId = mutation.messageIndex().messageId().orElse(null);
            return socketConnection.store()
                    .findMessageById(chat, messageId);
        });
        var targetNewsletterMessage = targetNewsletter.flatMap(newsletter -> {
            var messageId = mutation.messageIndex().messageId().orElse(null);
            return socketConnection.store()
                    .findMessageById(newsletter, messageId);
        });
        switch (action) {
            case ClearChatAction clearChatAction -> {
                var chat = targetChat.orElse(null);
                clearMessages(chat, clearChatAction);
            }
            case ContactAction contactAction -> {
                var contact = targetContact.orElseGet(() -> {
                    var newContact = socketConnection.store()
                            .addContact(targetJid.orElseThrow());
                    socketConnection.onNewContact(newContact);
                    return newContact;
                });
                var chat = targetChat.orElseGet(() -> socketConnection.store()
                        .addNewChat(targetJid.orElseThrow()));
                updateName(contact, chat, contactAction);
            }
            case DeleteMessageForMeAction ignored -> {
                targetChatMessage.ifPresent(message -> {
                    targetChat.ifPresent(chat -> chat.removeMessage(message));
                    socketConnection.onMessageDeleted(message, false);
                });
                targetNewsletterMessage.ifPresent(message -> {
                    targetNewsletter.ifPresent(newsletter -> newsletter.removeMessage(message));
                    socketConnection.onMessageDeleted(message, false);
                });
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
                socketConnection.store().setTwentyFourHourFormat(format);
            }
            case DeleteChatAction ignored -> targetChat.ifPresent(Chat::removeMessages);
            default -> {}
        }
        socketConnection.onAction(action, messageIndex);
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
        if(socketConnection.store().checkPatchMacs()) {
            var patchMac = generatePatchMac(jid, patch, patchType);
            if(patchMac.isPresent() && !Arrays.equals(patchMac.get(), patch.patchMac())) {
                throw new HmacValidationException("sync_mac");
            }
        }

        var mutations = decodeMutations(jid, patch.mutations(), newState);
        newState.setHash(mutations.result().hash());
        newState.setIndexValueMap(mutations.result().indexValueMap());
        if(socketConnection.store().checkPatchMacs()) {
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
        if(socketConnection.store().checkPatchMacs()) {
            var snapshotMac = generateSnapshotMac(newState.hash(), newState.version(), name, mutationKeys.get().snapshotMacKey());
            if(!Arrays.equals(snapshotMac, snapshot.mac())) {
                throw new HmacValidationException("decode_snapshot");
            }
        }
        return Optional.of(new SyncRecord(newState, mutations.records()));
    }

    private Optional<MutationKeys> getMutationKeys(Jid jid, KeyId snapshot) {
        return socketConnection.keys()
                .findAppKeyById(jid, snapshot.id())
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
            if(socketConnection.store().checkPatchMacs()) {
                var expectedMac = generateMac(operation, blob, blob.length - SignalConstants.KEY_LENGTH, sync.keyId().id(), mutationKeys.get().macKey());
                if(!Arrays.equals(
                        blob, blob.length - SignalConstants.KEY_LENGTH, blob.length,
                        expectedMac, 0, SignalConstants.KEY_LENGTH
                )) {
                    throw new HmacValidationException("decode_mutation");
                }
            }
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var keySpec = new SecretKeySpec(mutationKeys.get().encKey(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(blob, 0, 16));
            var result = cipher.doFinal(blob, 16, blob.length - SignalConstants.KEY_LENGTH - 16);
            var actionSync = ActionDataSyncSpec.decode(result);
            if(socketConnection.store().checkPatchMacs()) {
                var expectedMac = Hmac.calculateSha256(actionSync.index(), mutationKeys.get().indexKey());
                if(!Arrays.equals(sync.index().blob(), expectedMac)) {
                    throw new HmacValidationException("decode_mutation");
                }
            }
            generator.mix(sync.index().blob(), Arrays.copyOfRange(blob, blob.length - SignalConstants.KEY_LENGTH, blob.length), operation);
            return Optional.of(actionSync);
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException("Cannot decrypt data", exception);
        }
    }

    private byte[] generateMac(RecordSync.Operation operation, byte[] data, int dataLength, byte[] keyId, byte[] key) {
        var total = new byte[1 + keyId.length + dataLength + SignalConstants.MAC_LENGTH];
        total[0] = operation.content();
        System.arraycopy(keyId, 0, total, 1, keyId.length);
        System.arraycopy(data, 0, total, 1 + keyId.length, dataLength);
        total[total.length - 1] = (byte) (keyId.length + 1);
        var sha512 = Hmac.calculateSha512(total, key);
        return Arrays.copyOfRange(sha512, 0, SignalConstants.KEY_LENGTH);
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
                    localMac.update(blob, blob.length - SignalConstants.KEY_LENGTH, SignalConstants.KEY_LENGTH);
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

    private record SyncRecord(CompanionHashState state, List<ActionDataSync> records) {

    }

    private record SnapshotSyncRecord(PatchType patchType, SnapshotSync snapshot, List<PatchSync> patches,
                                      boolean hasMore) {
        public boolean hasSnapshot() {
            return snapshot != null;
        }

        public boolean hasPatches() {
            return patches != null && !patches.isEmpty();
        }
    }

    private record MutationsRecord(LTHash.Result result, List<ActionDataSync> records) {

    }

    private record PatchChunk(PatchType patchType, List<ActionDataSync> records, boolean hasMore) {

    }

    private record PushRequest(PatchType type, CompanionHashState oldState, CompanionHashState newState,
                               PatchSync sync) {

    }

    private record MutationResult(MutationSync sync, byte[] indexMac, byte[] valueMac, RecordSync.Operation operation) {

    }
}