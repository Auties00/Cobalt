package it.auties.whatsapp.socket;

import it.auties.bytes.Bytes;
import it.auties.whatsapp.binary.Sync;
import it.auties.whatsapp.crypto.AesCbc;
import it.auties.whatsapp.crypto.Hmac;
import it.auties.whatsapp.crypto.LTHash;
import it.auties.whatsapp.model.action.*;
import it.auties.whatsapp.model.chat.Chat;
import it.auties.whatsapp.model.chat.ChatMute;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.request.Node;
import it.auties.whatsapp.model.setting.UnarchiveChatsSetting;
import it.auties.whatsapp.model.sync.*;
import it.auties.whatsapp.util.*;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.auties.whatsapp.api.ErrorHandler.Location.APP_STATE_SYNC;
import static it.auties.whatsapp.model.request.Node.with;
import static it.auties.whatsapp.model.request.Node.withChildren;
import static java.util.Map.of;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toMap;

class AppStateHandler {
    private static final int PULL_ATTEMPTS = 1;

    private final Socket socket;
    private final Semaphore lock;
    protected AppStateHandler(Socket socket) {
        this.socket = socket;
        this.lock = new Semaphore(1);
    }

    protected CompletableFuture<Void> push(@NonNull PatchRequest patch) {
        return pull(patch.type()).thenComposeAsync(ignored -> pushWithSync(patch));
    }

    private CompletableFuture<Void> pushWithSync(PatchRequest patch) {
        try {
            lock.acquire();

            var oldState = socket.keys().findHashStateByName(patch.type())
                    .orElseThrow(() -> new NoSuchElementException("Missing patch for %s".formatted(patch.type())));
            var newState = oldState.copy();

            var key = socket.keys().appKey();

            var index = patch.index()
                    .getBytes(StandardCharsets.UTF_8);
            var actionData = ActionDataSync.builder()
                    .index(index)
                    .value(patch.sync())
                    .padding(new byte[0])
                    .version(patch.version())
                    .build();
            var encoded = JacksonProvider.PROTOBUF.writeValueAsBytes(actionData);

            var mutationKeys = MutationKeys.of(key.keyData()
                    .keyData());
            var encrypted = AesCbc.encryptAndPrefix(encoded, mutationKeys.encKey());
            var valueMac = generateMac(patch.operation(), encrypted, key.keyId()
                    .keyId(), mutationKeys.macKey());
            var indexMac = Hmac.calculateSha256(index, mutationKeys.indexKey());

            var generator = new LTHash(newState);
            generator.mix(indexMac, valueMac, patch.operation());
            var result = generator.finish();
            newState.hash(result.hash());
            newState.indexValueMap(result.indexValueMap());
            newState.version(newState.version() + 1);

            var syncId = new KeyId(key.keyId()
                    .keyId());
            var record = RecordSync.builder()
                    .index(new IndexSync(indexMac))
                    .value(new ValueSync(Bytes.of(encrypted, valueMac)
                            .toByteArray()))
                    .keyId(syncId)
                    .build();
            var mutation = MutationSync.builder()
                    .operation(patch.operation())
                    .record(record)
                    .build();

            var snapshotMac = generateSnapshotMac(newState.hash(), newState.version(), patch.type(),
                    mutationKeys.snapshotMacKey());
            var patchMac = generatePatchMac(snapshotMac, valueMac, newState.version(), patch.type(),
                    mutationKeys.patchMacKey());
            var sync = PatchSync.builder()
                    .patchMac(patchMac)
                    .snapshotMac(snapshotMac)
                    .keyId(syncId)
                    .mutations(List.of(mutation))
                    .build();
            newState.indexValueMap()
                    .put(Bytes.of(indexMac)
                            .toBase64(), valueMac);

            var body = withChildren("collection",
                    of("name", patch.type(), "version", newState.version() - 1, "return_snapshot", false),
                    with("patch", JacksonProvider.PROTOBUF.writeValueAsBytes(sync)));
            return socket.sendQuery("set", "w:sync:app:state", withChildren("sync", body))
                    .thenAcceptAsync(this::parseSyncRequest)
                    .thenRunAsync(() -> socket.keys().putState(patch.type(), newState))
                    .thenRunAsync(() -> handleSyncRequest(patch.type(), oldState.version(), newState, sync))
                    .thenRunAsync(lock::release)
                    .exceptionallyAsync(this::handleSyncError);
        } catch (Throwable throwable) {
            lock.release();
            throw new RuntimeException("Cannot push patch", throwable);
        }
    }

    private void handleSyncRequest(Sync sync, long minimumVersion, LTHashState state, PatchSync patch) {
        decodePatches(sync, minimumVersion, List.of(patch.withVersion(new VersionSync(state.version())))).stream()
                .map(MutationsRecord::records)
                .flatMap(Collection::stream)
                .forEach(this::processActions);
    }

    private Map<Sync, Long> getInitialStateVersions(List<Sync> syncs) {
        return syncs.stream()
                .collect(toMap(Function.identity(), this::getInitialStateVersion));
    }

    private long getInitialStateVersion(Sync sync) {
        return socket.keys().findHashStateByName(sync)
                .map(LTHashState::version)
                .orElse(0L);
    }

    protected void pull() {
        try {
            pullWithoutLock(Arrays.asList(Sync.values()))
                    .thenRunAsync(lock::release);
        } catch (Throwable throwable) {
            lock.release();
            throw new RuntimeException("Cannot pull initial patches", throwable);
        }
    }

    protected CompletableFuture<Void> pull(Sync... syncs) {
        try {
            Validate.isTrue(syncs.length != 0, "Cannot pull no patches", IllegalArgumentException.class);

            lock.acquire();
            return pullWithoutLock(Arrays.asList(syncs))
                    .thenRunAsync(lock::release);
        } catch (Throwable throwable) {
            lock.release();
            throw new RuntimeException("Cannot pull patches", throwable);
        }
    }

    private CompletableFuture<Void> pullWithoutLock(List<Sync> syncs) {
        var versions = getInitialStateVersions(syncs);
        var nodes = syncs.stream()
                .map(this::getOrCreateState)
                .map(LTHashState::toNode)
                .toList();
        return socket.sendQuery("set", "w:sync:app:state", withChildren("sync", nodes))
                .thenApplyAsync(this::parseSyncRequest)
                .thenApplyAsync(records -> handleSyncRequest(records, versions))
                .thenComposeAsync(this::checkIncompleteRecords)
                .exceptionallyAsync(this::handleSyncError);
    }

    private LTHashState getOrCreateState(Sync sync) {
        return socket.keys().findHashStateByName(sync)
                .orElseGet(() -> createHashState(sync));
    }

    private LTHashState createHashState(Sync sync) {
        var hashState = new LTHashState(sync);
        socket.keys().putState(sync, hashState);
        return hashState;
    }

    private List<SnapshotSyncRecord> handleSyncRequest(List<SnapshotSyncRecord> records, Map<Sync, Long> versions) {
        parsePatches(records, versions).forEach(this::processActions);
        return records;
    }

    private CompletableFuture<Void> checkIncompleteRecords(List<SnapshotSyncRecord> records) {
        var remaining = records.stream()
                .filter(SnapshotSyncRecord::hasMore)
                .map(SnapshotSyncRecord::name)
                .toList();
        return remaining.isEmpty() ?
                completedFuture(null) :
                pullWithoutLock(remaining);
    }

    private Void handleSyncError(Throwable exception) {
        lock.release();
        return socket.errorHandler().handleFailure(APP_STATE_SYNC, exception);
    }

    private List<ActionDataSync> parsePatches(List<SnapshotSyncRecord> patches, Map<Sync, Long> versions) {
        return patches.stream()
                .map(patch -> parsePatch(patch, versions.get(patch.name()), 0))
                .flatMap(Collection::stream)
                .toList();
    }

    private List<ActionDataSync> parsePatch(SnapshotSyncRecord patch, long minimumVersion, int failures) {
        try {
            var results = new ArrayList<ActionDataSync>();

            LTHashState snapshotState = socket.keys().findHashStateByName(patch.name())
                    .orElse(null);
            if (patch.hasSnapshot()) {
                var decodedSnapshot = decodeSnapshot(patch.name(), minimumVersion, patch.snapshot());
                results.addAll(decodedSnapshot.records());
                socket.keys().putState(patch.name(), decodedSnapshot.state());
                snapshotState = decodedSnapshot.state();
            }

            if (patch.hasPatches()) {
                decodePatches(patch.name(), minimumVersion, patch.patches(),
                        requireNonNull(snapshotState).copy()).stream()
                        .map(MutationsRecord::records)
                        .forEach(results::addAll);
            }

            return results;
        } catch (Throwable throwable) {
            createHashState(patch.name());
            if (failures + 1 >= PULL_ATTEMPTS) {
                throw new RuntimeException("Cannot parse patch(%s tries)".formatted(failures + 1), throwable);
            }

            return parsePatch(patch, minimumVersion, failures + 1);
        }
    }

    private List<SnapshotSyncRecord> parseSyncRequest(Node node) {
        return Optional.ofNullable(node)
                .flatMap(sync -> sync.findNode("sync"))
                .map(sync -> sync.findNodes("collection"))
                .stream()
                .flatMap(Collection::stream)
                .map(this::parseSync)
                .toList();
    }

    private SnapshotSyncRecord parseSync(Node sync) {
        var name = Sync.forName(sync.attributes()
                .getString("name"));
        var type = sync.attributes()
                .getString("type");
        Validate.isTrue(!Objects.equals(type, "error"), "Received erroneous sync: %s", IllegalStateException.class,
                sync.findNode("error")
                        .orElse(null));
        var more = sync.attributes()
                .getBool("has_more_patches");
        var snapshotSync = sync.findNode("snapshot")
                .map(this::decodeSnapshot)
                .orElse(null);
        var patches = decodePatches(sync);
        return new SnapshotSyncRecord(name, snapshotSync, patches, more);
    }

    @SneakyThrows
    private SnapshotSync decodeSnapshot(Node snapshot) {
        if (snapshot == null) {
            return null;
        }

        var blob = JacksonProvider.PROTOBUF.readMessage(snapshot.bytes(), ExternalBlobReference.class);
        var syncedData = Medias.download(blob, socket.store());
        return JacksonProvider.PROTOBUF.readMessage(syncedData, SnapshotSync.class);
    }

    private List<PatchSync> decodePatches(Node sync) {
        var versionCode = sync.attributes()
                .getInt("version");
        return sync.findNode("patches")
                .orElse(sync)
                .findNodes("patch")
                .stream()
                .map(patch -> decodePatch(patch, versionCode))
                .flatMap(Optional::stream)
                .toList();
    }

    @SneakyThrows
    private Optional<PatchSync> decodePatch(Node patch, long versionCode) {
        if (!patch.hasContent()) {
            return Optional.empty();
        }

        var patchSync = JacksonProvider.PROTOBUF.readMessage(patch.bytes(), PatchSync.class);
        if (!patchSync.hasVersion()) {
            var version = new VersionSync(versionCode + 1);
            patchSync.version(version);
        }

        return Optional.of(patchSync);
    }

    private void processActions(ActionDataSync mutation) {
        var value = mutation.value();
        if (value == null) {
            return;
        }

        var action = value.action();
        if (action != null) {
            var jid = ContactJid.of(mutation.messageIndex()
                    .chatJid());
            var targetContact = socket.store().findContactByJid(jid);
            var targetChat = socket.store().findChatByJid(jid);
            var targetMessage = targetChat.flatMap(chat -> socket.store().findMessageById(chat, mutation.messageIndex()
                    .messageId()));
            switch (action) {
                case ClearChatAction clearChatAction -> clearMessages(targetChat.orElse(null), clearChatAction);
                case ContactAction contactAction -> updateName(targetContact.orElseGet(() -> socket.createContact(jid)),
                        targetChat.orElseGet(() -> socket.createChat(jid)), contactAction);
                case DeleteChatAction ignored -> targetChat.map(Chat::messages)
                        .ifPresent(SortedMessageList::clear);
                case DeleteMessageForMeAction ignored ->
                        targetMessage.ifPresent(message -> targetChat.ifPresent(chat -> deleteMessage(message, chat)));
                case MarkChatAsReadAction markAction -> targetChat.ifPresent(chat -> chat.unreadMessages(
                        markAction.read() ?
                                0 :
                                -1));
                case MuteAction muteAction ->
                        targetChat.ifPresent(chat -> chat.mute(ChatMute.muted(muteAction.muteEndTimestamp())));
                case PinAction pinAction -> targetChat.ifPresent(chat -> chat.pinned(pinAction.pinned() ?
                        mutation.value()
                                .timestamp() :
                        0));
                case StarAction starAction -> targetMessage.ifPresent(message -> message.starred(starAction.starred()));
                case ArchiveChatAction archiveChatAction ->
                        targetChat.ifPresent(chat -> chat.archived(archiveChatAction.archived()));
                default -> {}
            }

            socket.onAction(action);
        }

        var setting = value.setting();
        if (setting != null) {
            if (setting instanceof UnarchiveChatsSetting unarchiveChatsSetting) {
                socket.store().unarchiveChats(unarchiveChatsSetting.unarchiveChats());
            }

            socket.onSetting(setting);
        }

        var features = mutation.value()
                .primaryFeature();
        if (features != null && !features.flags()
                .isEmpty()) {
            socket.onFeatures(features);
        }
    }

    private static void clearMessages(Chat targetChat, ClearChatAction clearChatAction) {
        if (targetChat == null || clearChatAction.messageRange() == null || clearChatAction.messageRange()
                .messages() == null) {
            return;
        }

        clearChatAction.messageRange()
                .messages()
                .stream()
                .map(SyncActionMessage::key)
                .filter(Objects::nonNull)
                .forEach(key -> targetChat.messages()
                        .removeIf(entry -> Objects.equals(entry.id(), key.id())));
    }

    private void updateName(Contact contact, Chat chat, ContactAction contactAction) {
        if (contact != null) {
            contact.fullName(contactAction.fullName());
            contact.shortName(contactAction.firstName());
        }

        if (chat != null) {
            var name = requireNonNullElse(contactAction.firstName(), contactAction.fullName());
            chat.name(name);
        }
    }

    private void deleteMessage(MessageInfo message, Chat chat) {
        chat.messages()
                .remove(message);
        socket.onMessageDeleted(message, false);
    }

    private List<MutationsRecord> decodePatches(Sync name, long minimumVersion, List<PatchSync> patches) {
        var newState = socket.keys().findHashStateByName(name)
                .orElseThrow()
                .copy();
        return decodePatches(name, minimumVersion, patches, newState);
    }

    private List<MutationsRecord> decodePatches(Sync name, long minimumVersion, List<PatchSync> patches,
                                                LTHashState newState) {
        var results = patches.stream()
                .map(patch -> decodePatch(name, minimumVersion, newState, patch))
                .flatMap(Optional::stream)
                .toList();
        socket.keys().putState(name, newState);
        return results;
    }

    @SneakyThrows
    private Optional<MutationsRecord> decodePatch(Sync sync, long minimumVersion, LTHashState newState,
                                                  PatchSync patch) {
        if (patch.hasExternalMutations()) {
            var blob = Medias.download(patch.externalMutations(), socket.store());
            var mutationsSync = JacksonProvider.PROTOBUF.readMessage(blob, MutationsSync.class);
            Validate.isTrue(patch.mutations()
                    .addAll(mutationsSync.mutations()), "Cannot add patches to known patches");
        }

        newState.version(patch.version());
        Validate.isTrue(Arrays.equals(calculateSyncMac(patch, sync), patch.patchMac()), "sync_mac",
                HmacValidationException.class);
        var records = patch.mutations()
                .stream()
                .collect(Collectors.toMap(MutationSync::record, MutationSync::operation));
        var mutations = decodeMutations(records, newState);
        newState.hash(mutations.result()
                .hash());
        newState.indexValueMap(mutations.result()
                .indexValueMap());
        Validate.isTrue(Arrays.equals(generatePatchMac(sync, newState, patch), patch.snapshotMac()), "patch_mac",
                HmacValidationException.class);

        return Optional.of(mutations)
                .filter(ignored -> minimumVersion == 0 || newState.version() > minimumVersion);
    }

    private byte[] generatePatchMac(Sync name, LTHashState newState, PatchSync patch) {
        var mutationKeys = getMutationKeys(patch.keyId());
        return generateSnapshotMac(newState.hash(), newState.version(), name, mutationKeys.snapshotMacKey());
    }

    private byte[] calculateSyncMac(PatchSync patch, Sync sync) {
        var mutationKeys = getMutationKeys(patch.keyId());
        var mutationMacs = patch.mutations()
                .stream()
                .map(mutation -> mutation.record()
                        .value()
                        .blob())
                .map(Bytes::of)
                .map(binary -> binary.slice(-SignalSpecification.KEY_LENGTH))
                .reduce(Bytes.newBuffer(), Bytes::append)
                .toByteArray();
        return generatePatchMac(patch.snapshotMac(), mutationMacs, patch.version(), sync, mutationKeys.patchMacKey());
    }

    private MutationsRecord decodeSnapshot(Sync name, long minimumVersion, SnapshotSync snapshot) {
        var newState = new LTHashState(name, snapshot.version()
                .version());
        var records = snapshot.records()
                .stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> RecordSync.Operation.SET));
        var mutations = decodeMutations(records, newState);
        newState.hash(mutations.result()
                .hash());
        newState.indexValueMap(mutations.result()
                .indexValueMap());
        var mutationKeys = getMutationKeys(snapshot.keyId());
        if (!Arrays.equals(snapshot.mac(),
                generateSnapshotMac(newState.hash(), newState.version(), name, mutationKeys.snapshotMacKey()))) {
            throw new HmacValidationException("decode_snapshot");
        }

        if (minimumVersion == 0 || newState.version() > minimumVersion) {
            mutations.records()
                    .clear();
        }

        return mutations;
    }

    private MutationKeys getMutationKeys(KeyId snapshot) {
        var encryptedKey = socket.keys().findAppKeyById(snapshot.id())
                .orElseThrow(() -> new NoSuchElementException("No keys available for mutation"));
        return MutationKeys.of(encryptedKey.keyData()
                .keyData());
    }

    private MutationsRecord decodeMutations(Map<RecordSync, RecordSync.Operation> syncs, LTHashState state) {
        var generator = new LTHash(state);
        var mutations = syncs.entrySet()
                .stream()
                .map(mutation -> decodeMutation(mutation.getValue(), mutation.getKey(), generator))
                .collect(Collectors.toList());
        return new MutationsRecord(state, generator.finish(), mutations);
    }

    @SneakyThrows
    private ActionDataSync decodeMutation(RecordSync.Operation operation, RecordSync sync, LTHash generator) {
        var mutationKeys = getMutationKeys(sync.keyId());

        var blob = Bytes.of(sync.value()
                .blob());
        var encryptedBlob = blob.cut(-SignalSpecification.KEY_LENGTH)
                .toByteArray();
        var encryptedMac = blob.slice(-SignalSpecification.KEY_LENGTH)
                .toByteArray();
        Validate.isTrue(Arrays.equals(encryptedMac, generateMac(operation, encryptedBlob, sync.keyId()
                .id(), mutationKeys.macKey())), "decode_mutation", HmacValidationException.class);

        var result = AesCbc.decrypt(encryptedBlob, mutationKeys.encKey());
        var actionSync = JacksonProvider.PROTOBUF.readMessage(result, ActionDataSync.class);
        Validate.isTrue(Arrays.equals(sync.index()
                        .blob(), Hmac.calculateSha256(actionSync.index(), mutationKeys.indexKey())), "decode_mutation",
                HmacValidationException.class);
        generator.mix(sync.index()
                .blob(), encryptedMac, operation);
        return actionSync;
    }

    private byte[] generateMac(RecordSync.Operation operation, byte[] data, byte[] keyId, byte[] key) {
        var keyData = Bytes.of(operation.content())
                .append(keyId)
                .toByteArray();

        var last = Bytes.newBuffer(SignalSpecification.MAC_LENGTH - 1)
                .append(keyData.length)
                .toByteArray();

        var total = Bytes.of(keyData, data, last)
                .toByteArray();
        return Bytes.of(Hmac.calculateSha512(total, key))
                .cut(SignalSpecification.KEY_LENGTH)
                .toByteArray();
    }

    private byte[] generateSnapshotMac(byte[] ltHash, long version, Sync sync, byte[] key) {
        var total = Bytes.of(ltHash)
                .append(BytesHelper.longToBytes(version))
                .append(sync.toString()
                        .getBytes(StandardCharsets.UTF_8))
                .toByteArray();
        return Hmac.calculateSha256(total, key);
    }

    private byte[] generatePatchMac(byte[] snapshotMac, byte[] valueMac, long version, Sync sync, byte[] key) {
        var total = Bytes.of(snapshotMac)
                .append(valueMac)
                .append(BytesHelper.longToBytes(version))
                .append(sync.toString()
                        .getBytes(StandardCharsets.UTF_8))
                .toByteArray();
        return Hmac.calculateSha256(total, key);
    }
}
