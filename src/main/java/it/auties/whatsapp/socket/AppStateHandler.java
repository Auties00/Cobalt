package it.auties.whatsapp.socket;

import it.auties.whatsapp.api.ClientType;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.crypto.LTHash;
import it.auties.whatsapp.exception.HmacValidationException;
import it.auties.whatsapp.model.action.*;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatMute;
import it.auties.whatsapp.model.companion.CompanionHashState;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.info.MessageIndexInfo;
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
import it.auties.whatsapp.util.Bytes;
import it.auties.whatsapp.util.Medias;
import it.auties.whatsapp.util.SignalConstants;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.auties.whatsapp.api.ErrorHandler.Location.*;

class AppStateHandler {
    private static final int TIMEOUT = 120;
    private static final int PULL_ATTEMPTS = 3;

    private final SocketHandler socketHandler;
    private final ConcurrentMap<PatchType, Integer> attempts;
    private final Semaphore pullSemaphore;
    private final Semaphore pushSemaphore;

    protected AppStateHandler(SocketHandler socketHandler) {
        this.socketHandler = socketHandler;
        this.attempts = new ConcurrentHashMap<>();
        this.pullSemaphore = new Semaphore(1, true);
        this.pushSemaphore = new Semaphore(1, true);
    }

    protected CompletableFuture<Void> push(Jid jid, List<PatchRequest> patches) {
        var clientType = socketHandler.store().clientType();
        var pullOperation = switch (clientType) {
            case MOBILE -> CompletableFuture.completedFuture(null);
            case WEB -> pull(jid, getPatchesTypes(patches));
        };
        return pullOperation.thenComposeAsync(ignored -> sendPush(jid, patches, clientType != ClientType.MOBILE))
                .orTimeout(TIMEOUT, TimeUnit.SECONDS)
                .exceptionallyAsync(throwable -> socketHandler.handleFailure(PUSH_APP_STATE, throwable));
    }

    private Set<PatchType> getPatchesTypes(List<PatchRequest> patches) {
        return patches.stream()
                .map(PatchRequest::type)
                .collect(Collectors.toUnmodifiableSet());
    }

    private CompletableFuture<Void> sendPush(Jid jid, List<PatchRequest> patches, boolean readPatches) {
        try {
            pushSemaphore.acquire();
            var requests = patches.stream()
                    .map(entry -> createPushRequest(jid, entry))
                    .toList();
            var mobile = socketHandler.store().clientType() == ClientType.MOBILE;
            var body = requests.stream()
                    .map(request -> createPushRequestNode(request, mobile))
                    .toList();
            var syncAttributes = Attributes.of()
                    .put("data_namespace", 3, mobile)
                    .toMap();
            var sync = Node.of("sync", syncAttributes, body);
            return socketHandler.sendQuery("set", "w:sync:app:state", sync)
                    .thenAcceptAsync(this::parseSyncRequest)
                    .thenRunAsync(() -> onPush(jid, requests, readPatches))
                    .thenRun(pushSemaphore::release)
                    .exceptionallyCompose(throwable -> {
                        pushSemaphore.release();
                        return CompletableFuture.failedFuture(throwable);
                    });
        }catch (Throwable throwable) {
            pushSemaphore.release();
            return CompletableFuture.failedFuture(throwable);
        }
    }

    private PushRequest createPushRequest(Jid jid, PatchRequest request) {
        var oldState = socketHandler.keys()
                .findHashStateByName(jid, request.type())
                .orElseGet(() -> new CompanionHashState(request.type()));
        var newState = oldState.copy();
        var key = socketHandler.keys().getLatestAppKey(jid);
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
        var concatValueMac = mutations.stream()
                .map(MutationResult::valueMac)
                .toArray(byte[][]::new);
        var patchMac = generatePatchMac(snapshotMac, concatValueMac, newState.version(), request.type(), mutationKeys.patchMacKey());
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
        var index = patch.index().getBytes(StandardCharsets.UTF_8);
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
        var encrypted = AesCbc.encryptAndPrefix(encoded, mutationKeys.encKey());
        var valueMac = generateMac(patch.operation(), encrypted, key.keyId().keyId(), mutationKeys.macKey());
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
            socketHandler.keys().putState(jid, request.newState());
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

    protected void pull(PatchType... patchTypes) {
        if (patchTypes == null || patchTypes.length == 0) {
            return;
        }

        var jid = socketHandler.store().jid();
        if (jid.isEmpty()) {
            return;
        }

        pull(jid.get(), Set.of(patchTypes))
                .thenAcceptAsync(success -> onPull(false, success))
                .exceptionallyAsync(exception -> onPullError(false, exception));
    }

    protected CompletableFuture<Void> pullInitial() {
        if (socketHandler.keys().initialAppSync()) {
            return CompletableFuture.completedFuture(null);
        }

        var jid = socketHandler.store().jid();
        return jid.map(value -> pull(value, Set.of(PatchType.values()))
                        .thenAcceptAsync(success -> onPull(true, success))
                        .exceptionallyAsync(exception -> onPullError(true, exception)))
                .orElseGet(() -> CompletableFuture.completedFuture(null));
    }

    private void onPull(boolean initial, boolean success) {
        if (!socketHandler.keys().initialAppSync()) {
            var result = (initial && success) || isSyncComplete();
            if(result) {
                socketHandler.keys().setInitialAppSync(true);
                socketHandler.queryNewsletters();
            }
        }

        attempts.clear();
    }

    private boolean isSyncComplete() {
        return Arrays.stream(PatchType.values())
                .allMatch(this::isSyncComplete);
    }

    private boolean isSyncComplete(PatchType entry) {
        var jid = socketHandler.store().jid();
        return jid.isPresent() && socketHandler.keys()
                .findHashStateByName(jid.get(), entry)
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

    private CompletableFuture<Boolean> pull(Jid jid, Set<PatchType> patchTypes) {
        try {
            pullSemaphore.acquire();
            var tempStates = new HashMap<PatchType, CompanionHashState>();
            var nodes = getPullNodes(jid, patchTypes, tempStates);
            return socketHandler.sendQuery("set", "w:sync:app:state", Node.of("sync", nodes))
                    .thenApplyAsync(this::parseSyncRequest)
                    .thenApplyAsync(records -> decodeSyncs(jid, tempStates, records))
                    .thenComposeAsync(remaining -> handlePullResult(jid, remaining))
                    .orTimeout(TIMEOUT, TimeUnit.SECONDS)
                    .thenApply(result -> {
                        pullSemaphore.release();
                        return result;
                    })
                    .exceptionallyCompose(throwable -> {
                        pullSemaphore.release();
                        return CompletableFuture.failedFuture(throwable);
                    });
        }catch (Throwable throwable) {
            pullSemaphore.release();
            return CompletableFuture.failedFuture(throwable);
        }
    }

    private CompletableFuture<Boolean> handlePullResult(Jid jid, Set<PatchType> remaining) {
        return remaining.isEmpty() ? CompletableFuture.completedFuture(true) : pull(jid, remaining);
    }

    private List<Node> getPullNodes(Jid jid, Set<PatchType> patchTypes, Map<PatchType, CompanionHashState> tempStates) {
        return patchTypes.stream()
                .map(name -> createStateWithVersion(jid, name))
                .peek(state -> tempStates.put(state.type(), state))
                .map(CompanionHashState::toNode)
                .toList();
    }

    private CompanionHashState createStateWithVersion(Jid jid, PatchType name) {
        return socketHandler.keys()
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
            var hashState = new CompanionHashState(record.patchType());
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
        var proxy = switch (socketHandler.store().mediaProxySetting()) {
            case NONE, UPLOADS -> null;
            case DOWNLOADS, ALL -> socketHandler.store().proxy().orElse(null);
        };
        return snapshot == null ? Optional.empty() : snapshot.contentAsBytes()
                .map(ExternalBlobReferenceSpec::decode)
                .map(blob -> Medias.downloadAsync(blob, proxy))
                .flatMap(CompletableFuture::join)
                .map(SnapshotSyncSpec::decode);
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
        mutation.value().primaryFeature().ifPresent(socketHandler::onFeatures);
    }

    private void onSetting(Setting setting) {
        switch (setting) {
            case LocaleSettings localeSettings -> {
                var oldLocale = socketHandler.store().locale();
                CountryLocale.of(localeSettings.locale())
                        .ifPresent(newLocale -> socketHandler.updateLocale(newLocale, oldLocale.orElse(null)));
            }
            case PushNameSettings pushNameSettings -> {
                var oldName = socketHandler.store().name();
                socketHandler.onUserChanged(pushNameSettings.name(), oldName);
            }
            case UnarchiveChatsSettings unarchiveChatsSettings -> {
                var settingValue = unarchiveChatsSettings.unarchiveChats();
                socketHandler.store().setUnarchiveChats(settingValue);
            }
            default -> {}
        }
        socketHandler.onSetting(setting);
    }

    private void onAction(ActionDataSync mutation, Action action) {
        var messageIndex = mutation.messageIndex();
        var targetContact = messageIndex.chatJid()
                .flatMap(socketHandler.store()::findContactByJid);
        var targetChat = messageIndex.chatJid()
                .flatMap(socketHandler.store()::findChatByJid);
        var targetNewsletter = messageIndex.chatJid()
                .flatMap(socketHandler.store()::findNewsletterByJid);
        var targetChatMessage = targetChat.flatMap(chat -> {
            var messageId = mutation.messageIndex().messageId().orElse(null);
            return socketHandler.store()
                    .findMessageById(chat, messageId);
        });
        var targetNewsletterMessage = targetNewsletter.flatMap(newsletter -> {
            var messageId = mutation.messageIndex().messageId().orElse(null);
            return socketHandler.store()
                    .findMessageById(newsletter, messageId);
        });
        switch (action) {
            case ClearChatAction clearChatAction -> {
                var chat = targetChat.orElse(null);
                clearMessages(chat, clearChatAction);
            }
            case ContactAction contactAction -> {
                var contact = targetContact.orElseGet(() -> createContact(messageIndex));
                var chat = targetChat.orElseGet(() -> createChat(messageIndex));
                updateName(contact, chat, contactAction);
            }
            case DeleteMessageForMeAction ignored -> {
                targetChatMessage.ifPresent(message -> {
                    targetChat.ifPresent(chat -> chat.removeMessage(message));
                    socketHandler.onMessageDeleted(message, false);
                });
                targetNewsletterMessage.ifPresent(message -> {
                    targetNewsletter.ifPresent(newsletter -> newsletter.removeMessage(message));
                    socketHandler.onMessageDeleted(message, false);
                });
            }
            case MarkChatAsReadAction markAction -> targetChat.ifPresent(chat -> {
                var read = markAction.read() ? 0 : -1;
                chat.setUnreadMessagesCount(read);
            });
            case MuteAction muteAction -> targetChat.ifPresent(chat -> {
                var timestamp = muteAction.muteEndTimestampSeconds().orElse(0L);
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
                socketHandler.store().setTwentyFourHourFormat(format);
            }
            case DeleteChatAction ignored -> targetChat.ifPresent(Chat::removeMessages);
            default -> {}
        }
        socketHandler.onAction(action, messageIndex);
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
            var proxy = switch (socketHandler.store().mediaProxySetting()) {
                case NONE, UPLOADS -> null;
                case DOWNLOADS, ALL -> socketHandler.store().proxy().orElse(null);
            };
            Medias.downloadAsync(patch.externalMutations(), proxy)
                    .join()
                    .ifPresent(blob -> handleExternalMutation(patch, blob));
        }

        newState.setVersion(patch.encodedVersion());
        if(socketHandler.store().checkPatchMacs()) {
            var patchMac = calculatePatchMac(jid, patch, patchType);
            if(patchMac.isPresent() && !Arrays.equals(patchMac.get(), patch.patchMac())) {
                throw new HmacValidationException("sync_mac");
            }
        }

        var mutations = decodeMutations(jid, patch.mutations(), newState);
        newState.setHash(mutations.result().hash());
        newState.setIndexValueMap(mutations.result().indexValueMap());
        if(socketHandler.store().checkPatchMacs()) {
            var snapshotMac = calculateSnapshotMac(jid, patchType, newState, patch);
            if(snapshotMac.isPresent() && !Arrays.equals(snapshotMac.get(), patch.snapshotMac())) {
                throw new HmacValidationException("snapshot_mac");
            }
        }

        return mutations;
    }

    private void handleExternalMutation(PatchSync patch, byte[] blob) {
        var mutationsSync = MutationsSyncSpec.decode(blob);
        patch.mutations().addAll(mutationsSync.mutations());
    }

    private Optional<byte[]> calculateSnapshotMac(Jid jid, PatchType name, CompanionHashState newState, PatchSync patch) {
        return getMutationKeys(jid, patch.keyId())
                .map(mutationKeys -> generateSnapshotMac(newState.hash(), newState.version(), name, mutationKeys.snapshotMacKey()));
    }

    private Optional<byte[]> calculatePatchMac(Jid jid, PatchSync patch, PatchType patchType) {
        return getMutationKeys(jid, patch.keyId())
                .map(mutationKeys -> generatePatchMac(patch.snapshotMac(), getSyncMutationMac(patch), patch.encodedVersion(), patchType, mutationKeys.patchMacKey()));
    }

    private byte[][] getSyncMutationMac(PatchSync patch) {
        return patch.mutations()
                .stream()
                .map(mutation -> mutation.record().value().blob())
                .map(entry -> Arrays.copyOfRange(entry, entry.length - SignalConstants.KEY_LENGTH, entry.length))
                .toArray(byte[][]::new);
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
        if(socketHandler.store().checkPatchMacs()) {
            var snapshotMac = generateSnapshotMac(newState.hash(), newState.version(), name, mutationKeys.get().snapshotMacKey());
            if(!Arrays.equals(snapshotMac, snapshot.mac())) {
                throw new HmacValidationException("decode_snapshot");
            }
        }
        return Optional.of(new SyncRecord(newState, mutations.records()));
    }

    private Optional<MutationKeys> getMutationKeys(Jid jid, KeyId snapshot) {
        return socketHandler.keys()
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
        var mutationKeys = getMutationKeys(jid, sync.keyId());
        if (mutationKeys.isEmpty()) {
            return Optional.empty();
        }
        var blob = sync.value().blob();
        var encryptedBlob = Arrays.copyOfRange(blob, 0, blob.length - SignalConstants.KEY_LENGTH);
        var encryptedMac = Arrays.copyOfRange(blob, blob.length - SignalConstants.KEY_LENGTH, blob.length);
        if(socketHandler.store().checkPatchMacs()) {
            var expectedMac = generateMac(operation, encryptedBlob, sync.keyId().id(), mutationKeys.get().macKey());
            if(!Arrays.equals(encryptedMac, expectedMac)) {
                throw new HmacValidationException("decode_mutation");
            }
        }
        var result = AesCbc.decrypt(encryptedBlob, mutationKeys.get().encKey());
        var actionSync = ActionDataSyncSpec.decode(result);
        if(socketHandler.store().checkPatchMacs()) {
            var expectedMac = Hmac.calculateSha256(actionSync.index(), mutationKeys.get().indexKey());
            if(!Arrays.equals(sync.index().blob(), expectedMac)) {
                throw new HmacValidationException("decode_mutation");
            }
        }
        generator.mix(sync.index().blob(), encryptedMac, operation);
        return Optional.of(actionSync);
    }

    private byte[] generateMac(RecordSync.Operation operation, byte[] data, byte[] keyId, byte[] key) {
        var keyData = Bytes.concat(operation.content(), keyId);
        var last = new byte[SignalConstants.MAC_LENGTH];
        last[last.length - 1] = (byte) keyData.length;
        var total = Bytes.concat(keyData, data, last);
        var sha512 = Hmac.calculateSha512(total, key);
        return Arrays.copyOfRange(sha512, 0, SignalConstants.KEY_LENGTH);
    }

    private byte[] generateSnapshotMac(byte[] ltHash, long version, PatchType patchType, byte[] key) {
        var total = Bytes.concat(
                ltHash,
                Bytes.longToBytes(version),
                patchType.toString().getBytes(StandardCharsets.UTF_8)
        );
        return Hmac.calculateSha256(total, key);
    }

    private byte[] generatePatchMac(byte[] snapshotMac, byte[][] valueMac, long version, PatchType patchType, byte[] key) {
        var total = Bytes.concat(
                snapshotMac,
                Bytes.concat(valueMac),
                Bytes.longToBytes(version),
                patchType.toString().getBytes(StandardCharsets.UTF_8)
        );
        return Hmac.calculateSha256(total, key);
    }

    protected void dispose() {
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
