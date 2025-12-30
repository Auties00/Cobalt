package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.client.WhatsAppClientErrorHandler;
import com.github.auties00.cobalt.exception.LidMigrationException;
import com.github.auties00.cobalt.model.chat.Chat;
import com.github.auties00.cobalt.model.jid.Jid;
import com.github.auties00.cobalt.model.sync.HistorySync;
import com.github.auties00.cobalt.model.sync.LIDMigrationMapping;
import com.github.auties00.cobalt.model.sync.LIDMigrationMappingSyncPayload;
import com.github.auties00.cobalt.node.mex.json.response.LidChangeNotificationResponse;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for orchestrating the LID (Long ID) migration process.
 * <p>
 * This service manages the transition from phone number-based addressing
 * to LID-based addressing in WhatsApp communications.
 * <p>
 * The migration process follows these steps:
 * <ol>
 *   <li>Wait for AB property to enable migration (WAITING_PROP)</li>
 *   <li>Wait for primary device to send PN-to-LID mappings (WAITING_MAPPINGS)</li>
 *   <li>Process received mappings and prepare for migration (READY)</li>
 *   <li>Execute chat database migration (IN_PROGRESS)</li>
 *   <li>Complete migration and switch to LID addressing (COMPLETE)</li>
 * </ol>
 */
public final class LidMigrationService {
    /**
     * The WhatsApp client instance for error handling and store access.
     */
    private final WhatsAppClient whatsapp;

    /**
     * Current status of the LID migration process.
     * <p>
     * Tracks the progression from waiting for mappings to migration complete.
     * Not serialized - reset on each session.
     */
    private final AtomicReference<LidThreadMigrationStatus> lidMigrationStatus;

    /**
     * Cache of PN to assigned LID mappings received from primary device.
     * <p>
     * Stores the LID that was assigned at the time of migration.
     */
    private final ConcurrentHashMap<Jid, Jid> primaryAssignedLidMappings;

    /**
     * Cache of PN to latest LID mappings received from primary device.
     * <p>
     * Stores the most recent LID if different from assigned (e.g., after LID changes).
     */
    private final ConcurrentHashMap<Jid, Jid> primaryLatestLidMappings;

    /**
     * Timestamp when the chat database migration should occur.
     * <p>
     * Received from the primary device during LID migration sync.
     * Used to coordinate the migration timing across devices.
     */
    private volatile long chatDbMigrationTimestamp;

    /**
     * Creates a new LidMigrationService.
     *
     * @param whatsapp the WhatsAppClient instance
     */
    public LidMigrationService(WhatsAppClient whatsapp) {
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp cannot be null");
        this.lidMigrationStatus = new AtomicReference<>(LidThreadMigrationStatus.WAITING_MAPPINGS);
        this.primaryAssignedLidMappings = new ConcurrentHashMap<>();
        this.primaryLatestLidMappings = new ConcurrentHashMap<>();
    }

    public void handleProtocolMessage(LIDMigrationMappingSyncPayload lidMigrationMapping) {
        Objects.requireNonNull(lidMigrationMapping, "lidMigrationMapping cannot be null");

        this.chatDbMigrationTimestamp = lidMigrationMapping.chatDbMigrationTimestamp();

        var mappings = lidMigrationMapping.pnToLidMappings();
        if (mappings != null) {
            for (var mapping : mappings) {
                processPrimaryMapping(mapping);
            }
        }

        // Learn all mappings in bulk after processing
        learnMappingsInBulk();

        // If ready, do the migration
        if (lidMigrationStatus.compareAndSet(LidThreadMigrationStatus.WAITING_MAPPINGS, LidThreadMigrationStatus.READY)) {
            triggerMigration();
        }
    }

    /**
     * Processes a single mapping from the primary device.
     * Stores both assigned and latest LID separately for proper resolution.
     */
    private void processPrimaryMapping(LIDMigrationMapping mapping) {
        var phoneJid = mapping.phoneNumberJid().orElse(null);
        if (phoneJid == null) {
            return;
        }

        var normalizedPhone = phoneJid.withoutData();

        // Store assigned LID
        mapping.assignedLidJid().ifPresent(assignedLid ->
            primaryAssignedLidMappings.put(normalizedPhone, assignedLid.withoutData())
        );

        // Store latest LID if present
        mapping.latestLidJid().ifPresent(latestLid ->
            primaryLatestLidMappings.put(normalizedPhone, latestLid.withoutData())
        );

        // Register the effective mapping in the store for lookups
        var effectiveLid = mapping.effectiveLidJid().orElse(null);
        if (effectiveLid != null) {
            whatsapp.store()
                    .registerLidMapping(normalizedPhone, effectiveLid);
            whatsapp.store()
                    .findContactByJid(normalizedPhone)
                    .ifPresent(contact -> contact.setLid(effectiveLid));
        }
    }

    /**
     * Learns all primary-provided mappings in bulk.
     * This registers both old (assigned) and new (latest) mappings where they differ
     * from what we already have locally.
     */
    private void learnMappingsInBulk() {
        for (var entry : primaryAssignedLidMappings.entrySet()) {
            var phoneJid = entry.getKey();
            var assignedLid = entry.getValue();
            var latestLid = primaryLatestLidMappings.get(phoneJid);

            // Get what we currently have locally
            var currentLid = whatsapp.store()
                    .findLidByPhone(phoneJid)
                    .orElse(null);

            // If current doesn't match assigned, we need to learn the mapping
            if (currentLid == null || !currentLid.equals(assignedLid)) {
                // Register the assigned LID
                whatsapp.store()
                        .registerLidMapping(phoneJid, assignedLid);

                // If there's also a latest LID that's different, register that too
                if (latestLid != null && !latestLid.equals(assignedLid)) {
                    whatsapp.store()
                            .registerLidMapping(phoneJid, latestLid);
                }
            }
        }
    }

    /**
     * Gets the LID provided by the primary device for a phone number.
     * Returns the effective LID (latest if available, otherwise assigned).
     */
    private Jid getPrimaryProvidedLid(Jid phoneJid) {
        var normalized = phoneJid.withoutData();
        var latestLid = primaryLatestLidMappings.get(normalized);
        if (latestLid != null) {
            return latestLid;
        }
        return primaryAssignedLidMappings.get(normalized);
    }

    public void handleHistorySync(HistorySync history) {
        Objects.requireNonNull(history, "history cannot be null");

        var mappings = history.phoneNumberToLidMappings();
        if (mappings == null || mappings.isEmpty()) {
            return;
        }

        for (var mapping : mappings) {
            var phoneJid = mapping.pnJid().orElse(null);
            var lidJid = mapping.lidJid().orElse(null);
            if (phoneJid == null || lidJid == null) {
                continue;
            }

            // Register the mapping in the store
            whatsapp.store()
                    .registerLidMapping(phoneJid, lidJid);

            // Update the contact if it exists
            whatsapp.store()
                    .findContactByJid(phoneJid)
                    .ifPresent(contact -> contact.setLid(lidJid));
        }

        // If ready, do the migration
        if (lidMigrationStatus.compareAndSet(LidThreadMigrationStatus.WAITING_MAPPINGS, LidThreadMigrationStatus.READY)) {
            triggerMigration();
        }
    }

    public void handleNotification(LidChangeNotificationResponse lidChange) {
        Objects.requireNonNull(lidChange, "lidChange cannot be null");

        if (lidChange.isSelf()) {
            // Update the user's LID in the store
            var newLid = lidChange.newLid();
            whatsapp.store().setLid(newLid);

            // Invalidate old LID mapping if present
            lidChange.oldLid()
                    .flatMap(whatsapp.store()::findPhoneByLid)
                    .ifPresent(currentPhone -> whatsapp.store().registerLidMapping(currentPhone, newLid));

            // Register new mapping with user's phone JID
            whatsapp.store()
                    .jid()
                    .ifPresent(selfJid -> whatsapp.store().registerLidMapping(selfJid.withoutData(), newLid));
        } else {
            var contactJid = lidChange.jid();
            if (contactJid == null) {
                return;
            }

            // Register the new LID mapping
            var newLid = lidChange.newLid();
            whatsapp.store()
                    .registerLidMapping(contactJid, newLid);

            // Update the contact if it exists
            whatsapp.store()
                    .findContactByJid(contactJid)
                    .ifPresent(contact -> contact.setLid(newLid));

            // Update any 1:1 chat thread references
            whatsapp.store()
                    .findChatByJid(contactJid)
                    .ifPresent(chat -> chat.setLid(newLid));
        }

        // Change state machine to ready if the notification says the migration is complete
        if (lidChange.migrationComplete()) {
            var newState = lidMigrationStatus.updateAndGet(current -> {
                if (current == LidThreadMigrationStatus.IN_PROGRESS || current == LidThreadMigrationStatus.COMPLETE) {
                    return current;
                } else {
                    return LidThreadMigrationStatus.READY;
                }
            });
            if (newState == LidThreadMigrationStatus.READY) {
                triggerMigration();
            }
        }
    }

    /**
     * Migrates all 1-to-1 chats from phone number addressing to LID addressing.
     * Uses proper resolution logic to handle conflicts and edge cases.
     */
    public void triggerMigration() {
        // Check if we have the chats
        if(!whatsapp.store().syncedChats()) {
            return;
        }

        // Change state machine to in progress
        if (!lidMigrationStatus.compareAndSet(LidThreadMigrationStatus.READY, LidThreadMigrationStatus.IN_PROGRESS)) {
            return;
        }

        // Build a set of existing LID chats for split detection
        var existingLidChats = new HashSet<Jid>();
        for (var chat : whatsapp.store().chats()) {
            if (chat.jid().hasLidServer()) {
                existingLidChats.add(chat.jid().withoutData());
            }
        }

        // Process each chat
        for (var chat : whatsapp.store().chats()) {
            var chatJid = chat.jid();

            // Only process user chats (not groups, broadcasts, etc.)
            if (!chatJid.hasUserServer() && !chatJid.hasLidServer()) {
                continue;
            }

            // Resolve how to handle this chat
            var resolution = resolveThreadAccountLid(chat, existingLidChats);

            // Handle the resolution
            switch (resolution) {
                case LidMigrationResolution.Migrate(var threadLid) -> chat.setLid(threadLid);

                case LidMigrationResolution.Delete _ -> whatsapp.store().removeChat(chatJid);

                case LidMigrationResolution.Error(var exception) -> {
                    // Critical error - trigger logout
                    whatsapp.handleFailure(WhatsAppClientErrorHandler.Location.LID_MIGRATION, exception);

                    // Reset state since we failed
                    lidMigrationStatus.compareAndSet(LidThreadMigrationStatus.IN_PROGRESS, LidThreadMigrationStatus.READY);

                    // Exit
                    return;
                }
            }
        }

        // Change state machine to complete
        lidMigrationStatus.compareAndSet(LidThreadMigrationStatus.IN_PROGRESS, LidThreadMigrationStatus.COMPLETE);
    }

    /**
     * Resolves how to handle a chat during LID migration.
     *
     * @param chat             the chat to resolve
     * @param existingLidChats set of existing LID chat JIDs for split detection
     * @return the resolution for this chat
     */
    private LidMigrationResolution resolveThreadAccountLid(Chat chat, Set<Jid> existingLidChats) {
        var chatJid = chat.jid();

        // Already a LID chat - keep as is
        if (chatJid.hasLidServer()) {
            return new LidMigrationResolution.Migrate(chatJid);
        }

        // Get the various LID sources
        var primaryProvidedLid = getPrimaryProvidedLid(chatJid);
        var localLid = whatsapp.store()
                .findLidByPhone(chatJid)
                .orElse(null);
        var chatTimestamp = chat.timestampSeconds();

        // Check if a chat already exists with the target LID
        var targetLid = primaryProvidedLid != null ? primaryProvidedLid : localLid;
        var lidChatExists = targetLid != null && existingLidChats.contains(targetLid.withoutData());

        // Case 1: No primary LID provided
        if (primaryProvidedLid == null) {
            if (localLid == null) {
                // The WhatsApp Web client checks if a chat can be deleted in the WAWebLid1X1ThreadAccountMigrations module
                // The logic is kind of complicated, but it either deletes the chats or throws an error
                // I see no reason to port it, so we just delete the chat for simplicity
                return new LidMigrationResolution.Delete();
            } else {
                // Has local LID but primary didn't provide - check for split thread
                if (lidChatExists) {
                    return new LidMigrationResolution.Error(new LidMigrationException.SplitThreadMismatch());
                } else {
                    return new LidMigrationResolution.Migrate(localLid);
                }
            }
        }

        // Case 2: Primary provided LID
        if (localLid == null || localLid.equals(primaryProvidedLid)) {
            return new LidMigrationResolution.Migrate(primaryProvidedLid);
        }

        // Mismatch between local and primary LID - check timestamps
        if (chatDbMigrationTimestamp > 0 && chatTimestamp >= chatDbMigrationTimestamp) {
            return new LidMigrationResolution.Error(new LidMigrationException.PrimaryMappingsObsolete());
        }

        return new LidMigrationResolution.Migrate(primaryProvidedLid);
    }


    public void reset() {
        lidMigrationStatus.set(LidThreadMigrationStatus.WAITING_MAPPINGS);
        chatDbMigrationTimestamp = 0;
        primaryAssignedLidMappings.clear();
        primaryLatestLidMappings.clear();
    }
}
