package it.auties.whatsapp.socket;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.binary.PatchType;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.crypto.LTHash;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.model.action.*;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatMute;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.MessageIndexInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.request.Attributes;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.setting.EphemeralSetting;
import it.auties.whatsapp.model.setting.LocaleSetting;
import it.auties.whatsapp.model.setting.PushNameSetting;
import it.auties.whatsapp.model.setting.UnarchiveChatsSetting;
import it.auties.whatsapp.model.sync.*;
import it.auties.whatsapp.util.*;
import lombok.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.auties.whatsapp.api.ErrorHandler.Location.*;
import static java.lang.System.Logger.Level.WARNING;

class AppStateHandler {
    private static final int TIMEOUT = 120;
    private static final int PULL_ATTEMPTS = 3;

    private final SocketHandler socketHandler;
    private final Map<PatchType, Integer> attempts;
    private final OrderedAsyncTaskRunner runner;

    protected AppStateHandler(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
        this.attempts = new HashMap<>();
        this.runner = new OrderedAsyncTaskRunner();
    }

    protected CompletableFuture<Void> push(@NonNull PatchType type, @NonNull ContactJid jid, @NonNull List<PatchRequest> patches) {
        if(socketHandler.store().clientType() == ClientType.MOBILE){
            return runner.runAsync(() -> sendPush(jid, createPushRequest(type, jid, patches))
                    .exceptionallyAsync(throwable -> socketHandler.handleFailure(PUSH_APP_STATE, throwable))
                    .orTimeout(TIMEOUT, TimeUnit.SECONDS));
        }
        
        return runner.runAsync(() -> pullUninterruptedly(jid, List.of(type)))
                .thenCompose(ignored -> sendPush(jid, createPushRequest(type, jid, patches)))
                .exceptionallyAsync(throwable -> socketHandler.handleFailure(PUSH_APP_STATE, throwable))
                .orTimeout(TIMEOUT, TimeUnit.SECONDS);
    }

    private PushRequest createPushRequest(PatchType type, ContactJid jid, List<PatchRequest> patches) {
        var oldState = socketHandler.keys()
                .findHashStateByName(jid, type)
                .orElseGet(() -> new LTHashState(type));
        var newState = oldState.copy();
        var key = socketHandler.keys().getLatestAppKey(jid);
        var mutationKeys = MutationKeys.of(key.keyData().keyData());
        var syncId = new KeyId(key.keyId().keyId());
        var mutations = patches.stream()
                .map(patch -> createMutation(patch, mutationKeys, key, newState, syncId))
                .toList();
        var snapshotMac = generateSnapshotMac(newState.hash(), newState.version(), type, mutationKeys.snapshotMacKey());
        var valueMac = mutations.stream()
                .map(entry -> newState.indexValueMap().get(Base64.getEncoder().encodeToString(entry.record().index().blob())))
                .reduce(new byte[0], (first, second) -> Bytes.of(first, second).toByteArray());
        var patchMac = generatePatchMac(snapshotMac, valueMac, newState.version(), type, mutationKeys.patchMacKey());
        var sync = PatchSync.builder()
                .patchMac(patchMac)
                .snapshotMac(snapshotMac)
                .keyId(syncId)
                .mutations(mutations)
                .build();
        return new PushRequest(type, oldState, newState, sync);
    }

    private MutationSync createMutation(PatchRequest patch, MutationKeys mutationKeys, AppStateSyncKey key, LTHashState newState, KeyId syncId) {
        var index = patch.index().getBytes(StandardCharsets.UTF_8);
        var actionData = ActionDataSync.builder()
                .index(index)
                .value(patch.sync())
                .padding(new byte[0])
                .version(patch.version())
                .build();
        var encoded = Protobuf.writeMessage(actionData);
        var encrypted = AesCbc.encryptAndPrefix(encoded, mutationKeys.encKey());
        var valueMac = generateMac(patch.operation(), encrypted, key.keyId().keyId(), mutationKeys.macKey());
        var indexMac = Hmac.calculateSha256(index, mutationKeys.indexKey());
        var generator = new LTHash(newState);
        generator.mix(indexMac, valueMac, patch.operation());
        var result = generator.finish();
        newState.hash(result.hash());
        newState.indexValueMap(result.indexValueMap());
        newState.version(newState.version() + 1);
        var record = RecordSync.builder()
                .index(new IndexSync(indexMac))
                .value(new ValueSync(Bytes.of(encrypted, valueMac).toByteArray()))
                .keyId(syncId)
                .build();
        newState.indexValueMap().put(Bytes.of(indexMac).toBase64(), valueMac);
        return MutationSync.builder()
                .operation(patch.operation())
                .record(record)
                .build();
    }

    private CompletableFuture<Void> sendPush(ContactJid jid, PushRequest request) {
        var mobile = socketHandler.store().clientType() == ClientType.MOBILE;
        var collectionAttributes = Attributes.of()
                .put("name", request.type())
                .put("version", request.newState().version() - 1, !mobile)
                .put("return_snapshot", false, !mobile)
                .put("order", request.type() != PatchType.CRITICAL_UNBLOCK_LOW ? "1" : "0", mobile)
                .toMap();
        var body = Node.ofChildren("collection",
                collectionAttributes,
                Node.of("patch", Protobuf.writeMessage(request.sync())));
        var syncAttributes = Attributes.of()
                .put("data_namespace", 3, mobile)
                .toMap();
        var sync = Node.ofChildren("sync", syncAttributes, body);
        return socketHandler.sendQuery("set", "w:sync:app:state", sync)
                .thenAcceptAsync(this::parseSyncRequest)
                .thenRunAsync(() -> onPush(jid, request));
    }

    private void onPush(ContactJid jid, PushRequest request) {
        socketHandler.keys().putState(jid, request.newState());
        handleSyncRequest(jid, request);
    }

    private void handleSyncRequest(ContactJid jid, PushRequest request) {
        var patches = List.of(request.sync().withVersion(new VersionSync(request.newState().version())));
        var results = decodePatches(jid, request.type(), patches, request.oldState());
        results.records().forEach(entry -> processActions(request.type(), entry));
    }

    protected void pull(PatchType... patchTypes) {
        if (patchTypes == null || patchTypes.length == 0) {
            CompletableFuture.completedFuture(null);
            return;
        }

        runner.runAsync(() -> pullUninterruptedly(socketHandler.store().jid(), Arrays.asList(patchTypes)).thenAcceptAsync(success -> onPull(false, success))
                .exceptionallyAsync(exception -> onPullError(false, exception)));
    }

    protected CompletableFuture<Void> pullInitial() {
        if(socketHandler.store().initialSync()){
            return CompletableFuture.completedFuture(null);
        }

        return pullUninterruptedly(socketHandler.store().jid(), Arrays.asList(PatchType.values()))
                .thenAcceptAsync(success -> onPull(true, success))
                .exceptionallyAsync(exception -> onPullError(true, exception));
    }

    private void onPull(boolean initial, boolean success) {
        if (!socketHandler.store().initialSync()) {
            socketHandler.store().initialSync((initial && success) || isSyncComplete());
        }

        attempts.clear();
    }

    private boolean isSyncComplete() {
        return Arrays.stream(PatchType.values())
                .allMatch(this::isSyncComplete);
    }

    private boolean isSyncComplete(PatchType entry) {
        return socketHandler.keys()
                .findHashStateByName(socketHandler.store().jid(), entry) // FIXME: Is this right?
                .filter(type -> type.version() > 0)
                .isPresent();
    }

    private Void onPullError(boolean initial, Throwable exception) {
        attempts.clear();
        if (initial) {
            return socketHandler.handleFailure(INITIAL_APP_STATE_SYNC, exception);
        }
        return socketHandler.handleFailure(PULL_APP_STATE, exception);
    }

    private CompletableFuture<Boolean> pullUninterruptedly(ContactJid jid, List<PatchType> patchTypes) {
        var tempStates = new HashMap<PatchType, LTHashState>();
        var nodes = getPullNodes(jid, patchTypes, tempStates);
        return socketHandler.sendQuery("set", "w:sync:app:state", Node.ofChildren("sync", nodes))
                .thenApplyAsync(this::parseSyncRequest)
                .thenApplyAsync(records -> decodeSyncs(jid, tempStates, records))
                .thenComposeAsync(remaining -> handlePullResult(jid, remaining))
                .orTimeout(TIMEOUT, TimeUnit.SECONDS);
    }

    private CompletableFuture<Boolean> handlePullResult(ContactJid jid, List<PatchType> remaining) {
        return remaining.isEmpty() ? CompletableFuture.completedFuture(true) : pullUninterruptedly(jid, remaining);
    }

    private List<Node> getPullNodes(ContactJid jid, List<PatchType> patchTypes, Map<PatchType, LTHashState> tempStates) {
        return patchTypes.stream()
                .map(name -> createStateWithVersion(jid, name))
                .peek(state -> tempStates.put(state.name(), state))
                .map(LTHashState::toNode)
                .toList();
    }

    private LTHashState createStateWithVersion(ContactJid jid, PatchType name) {
        return socketHandler.keys()
                .findHashStateByName(jid, name)
                .orElseGet(() -> new LTHashState(name));
    }

    private List<PatchType> decodeSyncs(ContactJid jid, Map<PatchType, LTHashState> tempStates, List<SnapshotSyncRecord> records) {
        return records.stream()
                .map(record -> {
                    var chunk = decodeSync(jid, record, tempStates);
                    chunk.records().forEach(entry -> processActions(record.patchType(), entry));
                    return chunk;
                })
                .filter(PatchChunk::hasMore)
                .map(PatchChunk::patchType)
                .toList();
    }

    private PatchChunk decodeSync(ContactJid jid, SnapshotSyncRecord record, Map<PatchType, LTHashState> tempStates) {
        try {
            var results = new ArrayList<ActionDataSync>();
            if (record.hasSnapshot()) {
                var snapshot = decodeSnapshot(jid, record.patchType(), record.snapshot());
                snapshot.ifPresent(decodedSnapshot -> {
                    results.addAll(decodedSnapshot.records());
                    tempStates.put(record.patchType(), decodedSnapshot.state());
                    socketHandler.keys().putState(jid, decodedSnapshot.state());
                });
            }
            if (record.hasPatches()) {
                var decodedPatches = decodePatches(jid, record.patchType(), record.patches(), tempStates.get(record.patchType()));
                results.addAll(decodedPatches.records());
                socketHandler.keys().putState(jid, decodedPatches.state());
            }
            return new PatchChunk(record.patchType(), results, record.hasMore());
        } catch (Throwable throwable) {
            var hashState = new LTHashState(record.patchType());
            socketHandler.keys().putState(jid, hashState);
            attempts.put(record.patchType(), attempts.getOrDefault(record.patchType(), 0) + 1);
            if (attempts.get(record.patchType()) >= PULL_ATTEMPTS) {
                throw new RuntimeException("Cannot parse patch(%s tries)".formatted(PULL_ATTEMPTS), throwable);
            }
            return decodeSync(jid, record, tempStates);
        }
    }

    private List<SnapshotSyncRecord> parseSyncRequest(Node node) {
        return Stream.ofNullable(node)
                .map(sync -> sync.findNodes("sync"))
                .flatMap(Collection::stream)
                .map(sync -> sync.findNodes("collection"))
                .flatMap(Collection::stream)
                .map(this::parseSync)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<SnapshotSyncRecord> parseSync(Node sync) {
        var name = PatchType.of(sync.attributes().getString("name"));
        var type = sync.attributes().getString("type");
        if (Objects.equals(type, "error")) {
            return Optional.empty();
        }
        var more = sync.attributes().getBoolean("has_more_patches");
        var snapshotSync = sync.findNode("snapshot").flatMap(this::decodeSnapshot).orElse(null);
        var versionCode = sync.attributes().getInt("version");
        var patches = sync.findNode("patches")
                .orElse(sync)
                .findNodes("patch")
                .stream()
                .map(patch -> decodePatch(patch, versionCode))
                .flatMap(Optional::stream)
                .toList();
        return Optional.of(new SnapshotSyncRecord(name, snapshotSync, patches, more));
    }

    private Optional<SnapshotSync> decodeSnapshot(Node snapshot) {
        return snapshot == null ? Optional.empty() : snapshot.contentAsBytes()
                .map(bytes -> Protobuf.readMessage(bytes, ExternalBlobReference.class))
                .map(Medias::download)
                .flatMap(CompletableFuture::join)
                .map(value -> Protobuf.readMessage(value, SnapshotSync.class));
    }

    private Optional<PatchSync> decodePatch(Node patch, long versionCode) {
        if (!patch.hasContent()) {
            return Optional.empty();
        }
        var patchSync = Protobuf.readMessage(patch.contentAsBytes().orElseThrow(), PatchSync.class);
        if (!patchSync.hasVersion()) {
            var version = new VersionSync(versionCode + 1);
            patchSync.version(version);
        }
        return Optional.of(patchSync);
    }

    private void processActions(PatchType type, ActionDataSync mutation) {
        var value = mutation.value();
        if (value == null) {
            return;
        }
        var action = value.action();
        if (action != null) {
            var messageIndex = mutation.messageIndex();
            var targetContact = messageIndex.chatJid().flatMap(socketHandler.store()::findContactByJid);
            var targetChat = messageIndex.chatJid().flatMap(socketHandler.store()::findChatByJid);
            var targetMessage = targetChat.flatMap(chat -> socketHandler.store()
                    .findMessageById(chat, mutation.messageIndex().messageId().orElse(null)));
            if (action instanceof ClearChatAction clearChatAction) {
                clearMessages(targetChat.orElse(null), clearChatAction);
            } else if (action instanceof ContactAction contactAction) {
                updateName(targetContact.orElseGet(() -> createContact(messageIndex)), targetChat.orElseGet(() -> createChat(messageIndex)), contactAction);
            } else if (action instanceof DeleteChatAction) {
                targetChat.ifPresent(Chat::removeMessages);
            } else if (action instanceof DeleteMessageForMeAction) {
                targetMessage.ifPresent(message -> targetChat.ifPresent(chat -> deleteMessage(message, chat)));
            } else if (action instanceof MarkChatAsReadAction markAction) {
                targetChat.ifPresent(chat -> chat.unreadMessagesCount(markAction.read() ? 0 : -1));
            } else if (action instanceof MuteAction muteAction) {
                targetChat.ifPresent(chat -> chat.mute(ChatMute.muted(muteAction.muteEndTimestampSeconds())));
            } else if (action instanceof PinAction pinAction) {
                targetChat.ifPresent(chat -> chat.pinnedTimestampSeconds(pinAction.pinned() ? (int) mutation.value()
                        .timestamp() : 0));
            } else if (action instanceof StarAction starAction) {
                targetMessage.ifPresent(message -> message.starred(starAction.starred()));
            } else if (action instanceof ArchiveChatAction archiveChatAction) {
                targetChat.ifPresent(chat -> chat.archived(archiveChatAction.archived()));
            } else if (action instanceof TimeFormatAction timeFormatAction) {
                socketHandler.store().twentyFourHourFormat(timeFormatAction.twentyFourHourFormatEnabled());
            }
            System.out.println("Action type: " + type);
            socketHandler.onAction(action, messageIndex);
        }
        var setting = value.setting();
        if (setting != null) {
            if (setting instanceof EphemeralSetting ephemeralSetting) {
                showEphemeralMessageWarning(ephemeralSetting);
            } else if (setting instanceof LocaleSetting localeSetting) {
                socketHandler.updateLocale(localeSetting.locale(), socketHandler.store().locale());
            } else if (setting instanceof PushNameSetting pushNameSetting) {
                socketHandler.updateUserName(pushNameSetting.name(), socketHandler.store().name());
            } else if (setting instanceof UnarchiveChatsSetting unarchiveChatsSetting) {
                socketHandler.store().unarchiveChats(unarchiveChatsSetting.unarchiveChats());
            }
            System.out.println("Setting type: " + type);
            socketHandler.onSetting(setting);
        }
        var features = mutation.value().primaryFeature();
        if (features.isPresent() && !features.get().flags().isEmpty()) {
            socketHandler.onFeatures(features.get());
        }
    }

    private Chat createChat(MessageIndexInfo messageIndex) {
        var chat = messageIndex.chatJid().orElseThrow();
        return socketHandler.store().addNewChat(chat);
    }

    private Contact createContact(MessageIndexInfo messageIndex) {
        var chatJid = messageIndex.chatJid().orElseThrow();
        var contact = socketHandler.store().addContact(chatJid);
        socketHandler.onNewContact(contact);
        return contact;
    }

    private void showEphemeralMessageWarning(EphemeralSetting ephemeralSetting) {
        var logger = System.getLogger("AppStateHandler");
        logger.log(WARNING, "An ephemeral status update was received as a setting. " + "Data: %s".formatted(ephemeralSetting) + "This should not be possible." + " Open an issue on Github please");
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
        contactAction.fullName().ifPresent(contact::fullName);
        contactAction.firstName().ifPresent(contact::shortName);
        chat.name(contactAction.name());
    }

    private void deleteMessage(MessageInfo message, Chat chat) {
        chat.removeMessage(message);
        socketHandler.onMessageDeleted(message, false);
    }

    private SyncRecord decodePatches(ContactJid jid, PatchType name, List<PatchSync> patches, LTHashState state) {
        var newState = state.copy();
        var results = patches.stream()
                .map(patch -> decodePatch(jid, name, newState, patch))
                .map(MutationsRecord::records)
                .flatMap(Collection::stream)
                .toList();
        return new SyncRecord(newState, results);
    }

    private MutationsRecord decodePatch(ContactJid jid, PatchType patchType, LTHashState newState, PatchSync patch) {
        if (patch.hasExternalMutations()) {
            Medias.download(patch.externalMutations())
                    .join()
                    .ifPresent(blob -> handleExternalMutation(patch, blob));
        }

        newState.version(patch.encodedVersion());
        var syncMac = calculateSyncMac(jid, patch, patchType);
        Validate.isTrue(syncMac.isEmpty() || Arrays.equals(syncMac.get(), patch.patchMac()), "sync_mac", HmacValidationException.class);
        var mutations = decodeMutations(jid, patch.mutations(), newState);
        newState.hash(mutations.result().hash());
        newState.indexValueMap(mutations.result().indexValueMap());
        var snapshotMac = generatePatchMac(jid, patchType, newState, patch);
        Validate.isTrue(snapshotMac.isEmpty() || Arrays.equals(snapshotMac.get(), patch.snapshotMac()), "patch_mac", HmacValidationException.class);
        return mutations;
    }

    private void handleExternalMutation(PatchSync patch, byte[] blob) {
        var mutationsSync = Protobuf.readMessage(blob, MutationsSync.class);
        patch.mutations().addAll(mutationsSync.mutations());
    }

    private Optional<byte[]> generatePatchMac(ContactJid jid, PatchType name, LTHashState newState, PatchSync patch) {
        return getMutationKeys(jid, patch.keyId())
                .map(mutationKeys -> generateSnapshotMac(newState.hash(), newState.version(), name, mutationKeys.snapshotMacKey()));
    }

    private Optional<byte[]> calculateSyncMac(ContactJid jid, PatchSync patch, PatchType patchType) {
        return getMutationKeys(jid, patch.keyId())
                .map(mutationKeys -> generatePatchMac(patch.snapshotMac(), getSyncMutationMac(patch), patch.encodedVersion(), patchType, mutationKeys.patchMacKey()));
    }

    private byte[] getSyncMutationMac(PatchSync patch) {
        return patch.mutations()
                .stream()
                .map(mutation -> mutation.record().value().blob())
                .map(Bytes::of)
                .map(binary -> binary.slice(-Spec.Signal.KEY_LENGTH))
                .reduce(Bytes.newBuffer(), Bytes::append)
                .toByteArray();
    }

    private Optional<SyncRecord> decodeSnapshot(ContactJid jid, PatchType name, SnapshotSync snapshot) {
        var mutationKeys = getMutationKeys(jid, snapshot.keyId());
        if (mutationKeys.isEmpty()) {
            return Optional.empty();
        }
        var newState = new LTHashState(name, snapshot.version().version());
        var mutations = decodeMutations(jid, snapshot.records(), newState);
        newState.hash(mutations.result().hash());
        newState.indexValueMap(mutations.result().indexValueMap());
        Validate.isTrue(Arrays.equals(snapshot.mac(), generateSnapshotMac(newState.hash(), newState.version(), name, mutationKeys.get()
                .snapshotMacKey())), "decode_snapshot", HmacValidationException.class);
        return Optional.of(new SyncRecord(newState, mutations.records()));
    }

    private Optional<MutationKeys> getMutationKeys(ContactJid jid, KeyId snapshot) {
        return socketHandler.keys()
                .findAppKeyById(jid, snapshot.id())
                .map(AppStateSyncKey::keyData)
                .map(AppStateSyncKeyData::keyData)
                .map(MutationKeys::of);
    }

    private MutationsRecord decodeMutations(ContactJid jid, List<? extends Syncable> syncs, LTHashState state) {
        var generator = new LTHash(state);
        var mutations = syncs.stream()
                .map(mutation -> decodeMutation(jid, mutation.operation(), mutation.record(), generator))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
        return new MutationsRecord(generator.finish(), mutations);
    }

    private Optional<ActionDataSync> decodeMutation(ContactJid jid, RecordSync.Operation operation, RecordSync sync, LTHash generator) {
        var mutationKeys = getMutationKeys(jid, sync.keyId());
        if (mutationKeys.isEmpty()) {
            return Optional.empty();
        }
        var blob = Bytes.of(sync.value().blob());
        var encryptedBlob = blob.cut(-Spec.Signal.KEY_LENGTH).toByteArray();
        var encryptedMac = blob.slice(-Spec.Signal.KEY_LENGTH).toByteArray();
        Validate.isTrue(Arrays.equals(encryptedMac, generateMac(operation, encryptedBlob, sync.keyId()
                .id(), mutationKeys.get().macKey())), "decode_mutation", HmacValidationException.class);
        var result = AesCbc.decrypt(encryptedBlob, mutationKeys.get().encKey());
        var actionSync = Protobuf.readMessage(result, ActionDataSync.class);
        Validate.isTrue(Arrays.equals(sync.index().blob(), Hmac.calculateSha256(actionSync.index(), mutationKeys.get()
                .indexKey())), "decode_mutation", HmacValidationException.class);
        generator.mix(sync.index().blob(), encryptedMac, operation);
        return Optional.of(actionSync);
    }

    private byte[] generateMac(RecordSync.Operation operation, byte[] data, byte[] keyId, byte[] key) {
        var keyData = Bytes.of(operation.content()).append(keyId).toByteArray();
        var last = Bytes.newBuffer(Spec.Signal.MAC_LENGTH - 1).append(keyData.length).toByteArray();
        var total = Bytes.of(keyData, data, last).toByteArray();
        return Bytes.of(Hmac.calculateSha512(total, key)).cut(Spec.Signal.KEY_LENGTH).toByteArray();
    }

    private byte[] generateSnapshotMac(byte[] ltHash, long version, PatchType patchType, byte[] key) {
        var total = Bytes.of(ltHash)
                .append(BytesHelper.longToBytes(version))
                .append(patchType.toString().getBytes(StandardCharsets.UTF_8))
                .toByteArray();
        return Hmac.calculateSha256(total, key);
    }

    private byte[] generatePatchMac(byte[] snapshotMac, byte[] valueMac, long version, PatchType patchType, byte[] key) {
        var total = Bytes.of(snapshotMac)
                .append(valueMac)
                .append(BytesHelper.longToBytes(version))
                .append(patchType.toString().getBytes(StandardCharsets.UTF_8))
                .toByteArray();
        return Hmac.calculateSha256(total, key);
    }

    protected void dispose() {
        attempts.clear();
        runner.cancel();
    }

    private record SyncRecord(LTHashState state, List<ActionDataSync> records) {
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

    private record PushRequest(PatchType type, LTHashState oldState, LTHashState newState, PatchSync sync) {
    }
}
