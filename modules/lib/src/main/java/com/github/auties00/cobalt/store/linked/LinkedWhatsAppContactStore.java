package com.github.auties00.cobalt.store.linked;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.BusinessVerifiedName;
import com.github.auties00.cobalt.wire.linked.contact.Contact;
import com.github.auties00.cobalt.wire.linked.contact.ContactTextStatus;
import com.github.auties00.cobalt.wire.linked.contact.OutContact;
import com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilities;
import com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilitiesEntry;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceList;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;

/**
 * The address-book and per-peer device state of a WhatsApp client session.
 *
 * <p>This is the sub-store that owns the people this session knows about: the
 * contacts, the "out contacts" (invitees who have not joined), the cached
 * contact "About" statuses, the bidirectional phone-number/LID mapping table,
 * the verified-business-name certificates, the per-user device-list cache, the
 * block list, the interop hosted-verification cache and the per-device
 * capability records.
 *
 * <p>Phone-number/LID resolution ({@link #findPhoneByLid(Jid)} and
 * {@link #findLidByPhone(Jid)}) consults the local account identity for the
 * self-address case; that identity is supplied by the owning
 * {@link LinkedWhatsAppStore}.
 *
 * @apiNote
 * Embedders reach this through {@link LinkedWhatsAppStore#contactStore()}.
 *
 * @see LinkedWhatsAppStore
 */
@WhatsAppWebModule(moduleName = "WAWebCollections")
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface LinkedWhatsAppContactStore {
    /**
     * Returns all contacts known to the local account.
     *
     * @return an unmodifiable copy of the contacts
     */
    Collection<Contact> contacts();

    /**
     * Looks up a contact by JID, retrying under the paired phone/LID identity on a direct miss.
     *
     * @param jid the contact identifier, or {@code null}
     * @return the matching contact, or empty if none is known
     */
    Optional<Contact> findContactByJid(JidProvider jid);

    /**
     * Creates and stores a new contact for the given JID, seeding its LID from the mapping table.
     *
     * @param jid the contact JID, never {@code null}
     * @return the newly created contact
     */
    Contact addNewContact(Jid jid);

    /**
     * Stores a contact, replacing any existing entry for the same JID.
     *
     * @param contact the contact, never {@code null}
     * @return the stored contact
     */
    Contact addContact(Contact contact);

    /**
     * Removes a contact by JID, retrying under the paired phone/LID identity on a direct miss.
     *
     * @param contactJid the contact identifier, or {@code null}
     * @return the removed contact, or empty if none matched
     */
    Optional<Contact> removeContact(JidProvider contactJid);

    /**
     * Returns the cached contact "About" text statuses.
     *
     * @return an unmodifiable copy of the cached statuses
     */
    Collection<ContactTextStatus> contactTextStatuses();

    /**
     * Looks up a cached contact "About" status, retrying under the paired phone/LID identity.
     *
     * @param jid the contact identifier, or {@code null}
     * @return the cached status, or empty if none is cached
     */
    Optional<ContactTextStatus> findContactTextStatus(JidProvider jid);

    /**
     * Caches a contact "About" status under the contact's user JID.
     *
     * @param contactJid the contact JID, never {@code null}
     * @param status     the status, never {@code null}
     */
    void addContactTextStatus(Jid contactJid, ContactTextStatus status);

    /**
     * Removes a cached contact "About" status, retrying under the paired phone/LID identity.
     *
     * @param jid the contact identifier, or {@code null}
     * @return the removed status, or empty if none was cached
     */
    Optional<ContactTextStatus> removeContactTextStatus(JidProvider jid);

    /**
     * Returns all out-contacts (invitees who have received messages but have no saved address-book entry).
     *
     * @return an unmodifiable copy of the out-contacts
     */
    Collection<OutContact> outContacts();

    /**
     * Looks up an out-contact by JID.
     *
     * @param jid the out-contact JID, or {@code null}
     * @return the out-contact, or empty if none is known
     */
    Optional<OutContact> findOutContact(Jid jid);

    /**
     * Stores an out-contact, merging name fields into any existing entry for the same JID.
     *
     * @param outContact the out-contact, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppContactStore addOutContact(OutContact outContact);

    /**
     * Removes an out-contact by JID.
     *
     * @param jid the out-contact JID, or {@code null}
     * @return the removed out-contact, or empty if none matched
     */
    Optional<OutContact> removeOutContact(Jid jid);

    /**
     * Clears all out-contacts.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppContactStore clearOutContacts();

    /**
     * Registers a bidirectional phone-number/LID mapping.
     *
     * @param phoneJid the phone-number JID
     * @param lidJid   the paired LID
     */
    void registerLidMapping(Jid phoneJid, Jid lidJid);

    /**
     * Registers a bidirectional phone-number/LID mapping, dropping it if a fresher mapping exists.
     *
     * @param phoneJid  the phone-number JID
     * @param lidJid    the paired LID
     * @param timestamp the mapping event timestamp, or {@code null} to skip staleness checking
     */
    void registerLidMapping(Jid phoneJid, Jid lidJid, Instant timestamp);

    /**
     * Resolves the phone-number JID paired with a LID, consulting the local account for the self case.
     *
     * @param lidJid the LID, or {@code null}
     * @return the paired phone-number JID, or empty if unknown
     */
    Optional<Jid> findPhoneByLid(Jid lidJid);

    /**
     * Resolves the LID paired with a phone-number JID, consulting the local account for the self case.
     *
     * @param phoneJid the phone-number JID, or {@code null}
     * @return the paired LID, or empty if unknown
     */
    Optional<Jid> findLidByPhone(Jid phoneJid);

    /**
     * Looks up a cached business-verified-name certificate.
     *
     * @param jid the business JID, never {@code null}
     * @return the certificate, or empty if none is cached
     */
    Optional<BusinessVerifiedName> findVerifiedBusinessName(Jid jid);

    /**
     * Caches a business-verified-name certificate under the business user JID.
     *
     * @param jid    the business JID, never {@code null}
     * @param record the certificate, never {@code null}
     */
    void addVerifiedBusinessName(Jid jid, BusinessVerifiedName record);

    /**
     * Removes a cached business-verified-name certificate.
     *
     * @param jid the business JID, never {@code null}
     */
    void removeVerifiedBusinessName(Jid jid);

    /**
     * Returns the cached device list for a user, falling back to the paired phone/LID identity.
     *
     * @param userJid the user JID, never {@code null}
     * @return the device list, or empty if not cached
     */
    Optional<DeviceList> findDeviceList(Jid userJid);

    /**
     * Returns all cached device lists in insertion order.
     *
     * @return an unmodifiable copy of the cached device lists
     */
    SequencedCollection<DeviceList> deviceLists();

    /**
     * Stores a device list, evicting the eldest entry when the LRU cap is reached.
     *
     * @param deviceList the device list, never {@code null}
     */
    void addDeviceList(DeviceList deviceList);

    /**
     * Removes the cached device list for a user.
     *
     * @param userJid the user JID, never {@code null}
     */
    void removeDeviceList(Jid userJid);

    /**
     * Clears all cached device lists.
     */
    void clearDeviceLists();

    /**
     * Records that a peer has been verified as a hosted (server-side) WhatsApp interop user.
     *
     * @param userJid the user JID, or {@code null} to ignore
     */
    void addToInteropHostedVerificationCache(Jid userJid);

    /**
     * Returns whether a peer is in the interop hosted-verification cache.
     *
     * @param userJid the user JID, or {@code null}
     * @return {@code true} if the peer has been verified as a hosted interop user
     */
    boolean isInInteropHostedVerificationCache(Jid userJid);

    /**
     * Clears the interop hosted-verification cache.
     */
    void clearInteropHostedVerificationCache();

    /**
     * Returns the JIDs of contacts the user has blocked.
     *
     * @return an unmodifiable copy of the blocked-contact JIDs
     */
    Set<Jid> blockedContacts();

    /**
     * Adds a contact to the block list.
     *
     * @param contact the contact JID, or {@code null} to ignore
     */
    void addBlockedContact(Jid contact);

    /**
     * Removes a contact from the block list.
     *
     * @param contact the contact JID, or {@code null} to ignore
     */
    void removeBlockedContact(Jid contact);

    /**
     * Replaces the entire block list.
     *
     * @param contacts the blocked-contact JIDs, or {@code null} to clear
     */
    void setBlockedContacts(Collection<Jid> contacts);

    /**
     * Returns the content hash of the cached block list.
     *
     * @return the block-list hash, or empty if none is set
     */
    Optional<String> blocklistHash();

    /**
     * Sets the block-list content hash.
     *
     * @param blocklistHash the hash, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppContactStore setBlocklistHash(String blocklistHash);

    /**
     * Returns whether the block list has been migrated to the current format.
     *
     * @return {@code true} if the block list has been migrated
     */
    boolean blocklistMigrated();

    /**
     * Sets the block-list migration flag.
     *
     * @param blocklistMigrated whether the block list has been migrated
     * @return this store instance for method chaining
     */
    LinkedWhatsAppContactStore setBlocklistMigrated(boolean blocklistMigrated);

    /**
     * Returns whether a block-list migration arrived before the 1x1 migration.
     *
     * @return {@code true} if the block-list migration preceded the 1x1 migration
     */
    boolean receivedBlocklistMigrationBefore1x1Migration();

    /**
     * Sets the flag tracking whether a block-list migration preceded the 1x1 migration.
     *
     * @param value the flag value
     * @return this store instance for method chaining
     */
    LinkedWhatsAppContactStore setReceivedBlocklistMigrationBefore1x1Migration(boolean value);

    /**
     * Returns the capabilities advertised by the primary device.
     *
     * @return the primary device capabilities, or empty if unknown
     */
    Optional<DeviceCapabilities> primaryDeviceCapabilities();

    /**
     * Sets the primary device capabilities.
     *
     * @param capabilities the capabilities, or {@code null} to clear
     * @return this store instance for method chaining
     */
    LinkedWhatsAppContactStore setPrimaryDeviceCapabilities(DeviceCapabilities capabilities);

    /**
     * Returns the per-device capability records.
     *
     * @return an unmodifiable copy of the capability records
     */
    Collection<DeviceCapabilitiesEntry> deviceCapabilitiesStates();

    /**
     * Looks up the capability record for a device.
     *
     * @param deviceJid the device JID, or {@code null}
     * @return the capability record, or empty if none is stored
     */
    Optional<DeviceCapabilitiesEntry> findDeviceCapabilitiesEntry(Jid deviceJid);

    /**
     * Stores a per-device capability record.
     *
     * @param entry the capability record, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppContactStore putDeviceCapabilitiesEntry(DeviceCapabilitiesEntry entry);

    /**
     * Removes a per-device capability record.
     *
     * @param deviceJid the device JID, or {@code null}
     * @return the removed record, or empty if none was stored
     */
    Optional<DeviceCapabilitiesEntry> removeDeviceCapabilitiesEntry(Jid deviceJid);

    /**
     * Clears all per-device capability records.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppContactStore clearDeviceCapabilitiesStates();
}
