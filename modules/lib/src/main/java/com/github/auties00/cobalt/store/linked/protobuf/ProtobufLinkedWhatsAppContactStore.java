package com.github.auties00.cobalt.store.linked.protobuf;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.business.BusinessVerifiedName;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.contact.Contact;
import com.github.auties00.cobalt.wire.linked.contact.ContactBuilder;
import com.github.auties00.cobalt.wire.linked.contact.ContactTextStatus;
import com.github.auties00.cobalt.wire.linked.contact.OutContact;
import com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilities;
import com.github.auties00.cobalt.wire.linked.device.capabilities.DeviceCapabilitiesEntry;
import com.github.auties00.cobalt.wire.linked.device.info.DeviceList;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.linked.newsletter.Newsletter;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppContactStore;
import com.github.auties00.collections.ConcurrentLinkedHashMap;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNullElseGet;

/**
 * The protobuf-backed {@link LinkedWhatsAppContactStore} holding this session's address-book and per-peer device state.
 *
 * <p>This is a nested {@code MESSAGE} sub-store of {@link ProtobufWhatsAppStore}; it owns the contacts,
 * out-contacts and verified-business-name certificates (persisted), plus the transient contact-status
 * cache, phone-number/LID mapping table, device-list LRU cache, block list, interop
 * hosted-verification cache and per-device capability records.
 *
 * @implNote
 * This implementation seeds the LID mapping table from the contacts passed to its constructor. The
 * self-address case of {@link #findPhoneByLid(Jid)} and {@link #findLidByPhone(Jid)} reads the local
 * account JID/LID through an {@link LinkedWhatsAppAccountStore} reference wired by the owning aggregate via
 * {@link #setAccount(LinkedWhatsAppAccountStore)} after construction (the protobuf deserializer cannot pass a
 * sibling sub-store into this object's constructor).
 */
@ProtobufMessage
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class ProtobufLinkedWhatsAppContactStore implements LinkedWhatsAppContactStore {
    /**
     * The logger for {@link ProtobufLinkedWhatsAppContactStore}.
     */
    private static final System.Logger LOGGER = Log.get(ProtobufLinkedWhatsAppContactStore.class);

    /**
     * The maximum number of per-user device-list entries kept before the eldest is evicted.
     */
    private static final int MAX_DEVICE_LISTS = 5000;

    /**
     * The map of every contact known to the local account, keyed by JID.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    private final ConcurrentHashMap<Jid, Contact> contactsMap;

    /**
     * The map of out-contacts (invitees who received messages but have no saved address-book entry), keyed by JID.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    private final ConcurrentHashMap<Jid, OutContact> outContactsMap;

    /**
     * The cache of business-verified-name certificates, keyed by the verified business JID.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    private final ConcurrentMap<Jid, BusinessVerifiedName> verifiedBusinessNamesMap;

    /**
     * The cache of contact "About" text statuses, keyed by contact user JID; rebuilt on demand, not persisted.
     */
    private final ConcurrentHashMap<Jid, ContactTextStatus> contactTextStatusesMap;

    /**
     * The per-user device-list cache with LRU eviction; not persisted.
     */
    private final ConcurrentLinkedHashMap<Jid, DeviceList> deviceLists;

    /**
     * The LID-to-phone mapping table; not persisted.
     */
    private final ConcurrentHashMap<Jid, Jid> lidToPhoneMappings;

    /**
     * The phone-to-LID mapping table; not persisted.
     */
    private final ConcurrentHashMap<Jid, Jid> phoneToLidMappings;

    /**
     * The per-LID mapping timestamps used to drop stale mapping events; not persisted.
     */
    private final ConcurrentHashMap<Jid, Instant> lidMappingTimestamps;

    /**
     * The cache of peers verified as hosted WhatsApp interop users; not persisted.
     */
    private final Set<Jid> interopHostedVerificationCache;

    /**
     * The JIDs of contacts the user has blocked; not persisted.
     */
    private final Set<Jid> blockedContacts;

    /**
     * The content hash of the cached block list; not persisted.
     */
    private volatile String blocklistHash;

    /**
     * Whether the block list has been migrated to the current format; not persisted.
     */
    private volatile boolean blocklistMigrated;

    /**
     * Whether a block-list migration arrived before the 1x1 migration; not persisted.
     */
    private volatile boolean receivedBlocklistMigrationBefore1x1Migration;

    /**
     * The capabilities advertised by the primary device; not persisted.
     */
    private DeviceCapabilities primaryDeviceCapabilities;

    /**
     * The per-device capability records keyed by device JID; not persisted.
     */
    private final ConcurrentMap<Jid, DeviceCapabilitiesEntry> deviceCapabilitiesStates;

    /**
     * The account sub-store consulted for the self-address LID resolution case; wired post-construction.
     */
    private LinkedWhatsAppAccountStore account;

    /**
     * Constructs a contact sub-store and seeds the LID mapping table from the supplied contacts.
     *
     * @param contactsMap              the contact map, never {@code null}
     * @param outContactsMap           the out-contact map, or {@code null} for an empty map
     * @param verifiedBusinessNamesMap the verified-business-name map, or {@code null} for an empty map
     */
    ProtobufLinkedWhatsAppContactStore(ConcurrentHashMap<Jid, Contact> contactsMap, ConcurrentHashMap<Jid, OutContact> outContactsMap, ConcurrentMap<Jid, BusinessVerifiedName> verifiedBusinessNamesMap) {
        this.contactsMap = Objects.requireNonNull(contactsMap, "contactsMap cannot be null");
        this.outContactsMap = requireNonNullElseGet(outContactsMap, ConcurrentHashMap::new);
        this.verifiedBusinessNamesMap = requireNonNullElseGet(verifiedBusinessNamesMap, ConcurrentHashMap::new);
        this.contactTextStatusesMap = new ConcurrentHashMap<>();
        this.deviceLists = new ConcurrentLinkedHashMap<>();
        this.lidToPhoneMappings = new ConcurrentHashMap<>();
        this.phoneToLidMappings = new ConcurrentHashMap<>();
        this.lidMappingTimestamps = new ConcurrentHashMap<>();
        this.interopHostedVerificationCache = ConcurrentHashMap.newKeySet();
        this.blockedContacts = ConcurrentHashMap.newKeySet();
        this.deviceCapabilitiesStates = new ConcurrentHashMap<>();
        for (var contact : contactsMap.values()) {
            contact.lid()
                    .ifPresent(entry -> registerLidMapping(contact.jid(), entry));
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "contact store initialized, contacts={0}, outContacts={1}, verifiedNames={2}",
                    contactsMap.size(), this.outContactsMap.size(), this.verifiedBusinessNamesMap.size());
        }
    }

    /**
     * Sets the account sub-store used for the self-address LID resolution case.
     *
     * <p>The account sub-store cannot be a constructor argument because the protobuf deserializer builds
     * this sub-store from its own wire fields alone; it is set once by the {@link ProtobufWhatsAppStore}
     * aggregate constructor, which composes both sub-stores.
     *
     * @param account the account sub-store, never {@code null}
     */
    void setAccount(LinkedWhatsAppAccountStore account) {
        this.account = Objects.requireNonNull(account, "account cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "contact store account sub-store set");
    }

    /**
     * Returns the live contact map backing this store.
     *
     * @return the live contact map
     */
    ConcurrentHashMap<Jid, Contact> contactsMap() {
        return contactsMap;
    }

    /**
     * Returns the live out-contact map backing this store.
     *
     * @return the live out-contact map
     */
    ConcurrentHashMap<Jid, OutContact> outContactsMap() {
        return outContactsMap;
    }

    /**
     * Returns the live verified-business-name map backing this store.
     *
     * @return the live verified-business-name map
     */
    ConcurrentMap<Jid, BusinessVerifiedName> verifiedBusinessNamesMap() {
        return verifiedBusinessNamesMap;
    }

    @Override
    public Optional<Contact> findContactByJid(JidProvider jid) {
        return switch (jid) {
            case Contact contact -> Optional.of(contact);
            case null -> Optional.empty();
            default -> {
                var targetJid = jid.toJid();
                if (targetJid.hasUserServer()) {
                    var jidContact = contactsMap.get(targetJid);
                    if (jidContact != null) {
                        yield Optional.of(jidContact);
                    } else {
                        yield findLidByPhone(targetJid)
                                .map(contactsMap::get);
                    }
                } else if (targetJid.hasLidServer()) {
                    var lidContact = contactsMap.get(targetJid);
                    if (lidContact != null) {
                        yield Optional.of(lidContact);
                    } else {
                        yield findPhoneByLid(targetJid)
                                .map(contactsMap::get);
                    }
                } else {
                    var contact = contactsMap.get(targetJid);
                    yield Optional.ofNullable(contact);
                }
            }
        };
    }

    @Override
    public Collection<Contact> contacts() {
        return List.copyOf(contactsMap.values());
    }

    @Override
    public Collection<ContactTextStatus> contactTextStatuses() {
        return List.copyOf(contactTextStatusesMap.values());
    }

    @Override
    public Contact addNewContact(Jid jid) {
        Objects.requireNonNull(jid, "jid cannot be null");
        var newContact = new ContactBuilder()
                .jid(jid)
                .lid(phoneToLidMappings.get(jid))
                .build();
        return addContact(newContact);
    }

    @Override
    public Contact addContact(Contact contact) {
        Objects.requireNonNull(contact, "contact cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "adding contact {0}", contact.jid());
        contactsMap.put(contact.jid(), contact);
        return contact;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation mirrors the LID/phone fallback chain of {@link #findContactByJid} so a
     * contact stored under one identity is removed when called by the paired identity.
     */
    @Override
    public Optional<Contact> removeContact(JidProvider contactJid) {
        if (contactJid == null) {
            return Optional.empty();
        } else {
            var targetJid = contactJid.toJid();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "removing contact {0}", targetJid);
            if (targetJid.hasUserServer()) {
                var jidContact = contactsMap.remove(targetJid);
                if (jidContact != null) {
                    return Optional.of(jidContact);
                } else {
                    return findLidByPhone(targetJid)
                            .map(contactsMap::remove);
                }
            } else if (targetJid.hasLidServer()) {
                var lidContact = contactsMap.remove(targetJid);
                if (lidContact != null) {
                    return Optional.of(lidContact);
                } else {
                    return findPhoneByLid(targetJid)
                            .map(contactsMap::remove);
                }
            } else {
                var chat = contactsMap.remove(targetJid);
                return Optional.ofNullable(chat);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation normalises the supplied JID through {@link Jid#toUserJid()} and then
     * mirrors the LID/phone fallback chain of {@link #findContactByJid} so a status cached under one
     * identity surfaces when queried by the paired identity.
     */
    @Override
    public Optional<ContactTextStatus> findContactTextStatus(JidProvider jid) {
        if (jid == null) {
            return Optional.empty();
        }

        var targetJid = jid.toJid().toUserJid();
        if (targetJid.hasUserServer()) {
            var direct = contactTextStatusesMap.get(targetJid);
            if (direct != null) {
                return Optional.of(direct);
            }
            return findLidByPhone(targetJid).map(contactTextStatusesMap::get);
        }

        if (targetJid.hasLidServer()) {
            var direct = contactTextStatusesMap.get(targetJid);
            if (direct != null) {
                return Optional.of(direct);
            }
            return findPhoneByLid(targetJid).map(contactTextStatusesMap::get);
        }

        return Optional.ofNullable(contactTextStatusesMap.get(targetJid));
    }

    @Override
    public void addContactTextStatus(Jid contactJid, ContactTextStatus status) {
        Objects.requireNonNull(contactJid, "contactJid cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        contactTextStatusesMap.put(contactJid.toUserJid(), status);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation tries the direct removal first and, if that misses, mirrors the LID/phone
     * fallback chain of {@link #findContactTextStatus} so a status cached under one identity is
     * removed when called by the paired identity.
     */
    @Override
    public Optional<ContactTextStatus> removeContactTextStatus(JidProvider jid) {
        if (jid == null) {
            return Optional.empty();
        }

        var targetJid = jid.toJid().toUserJid();
        var removed = contactTextStatusesMap.remove(targetJid);
        if (removed != null) {
            return Optional.of(removed);
        }

        if (targetJid.hasUserServer()) {
            return findLidByPhone(targetJid).map(contactTextStatusesMap::remove);
        }

        if (targetJid.hasLidServer()) {
            return findPhoneByLid(targetJid).map(contactTextStatusesMap::remove);
        }

        return Optional.empty();
    }

    @Override
    public Collection<OutContact> outContacts() {
        return List.copyOf(outContactsMap.values());
    }

    @Override
    public Optional<OutContact> findOutContact(Jid jid) {
        if (jid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(outContactsMap.get(jid));
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation merges into any existing {@link OutContact} for the same JID rather than
     * overwriting it: only {@code fullName} and {@code firstName} are taken from the incoming entry,
     * preserving any additional state the existing record accumulated.
     */
    @Override
    public LinkedWhatsAppContactStore addOutContact(OutContact outContact) {
        Objects.requireNonNull(outContact, "outContact cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "adding out-contact {0}", outContact.jid());
        outContactsMap.merge(outContact.jid(), outContact, (existing, incoming) -> {
            existing.setFullName(incoming.fullName().orElse(null));
            existing.setFirstName(incoming.firstName().orElse(null));
            return existing;
        });
        return this;
    }

    @Override
    public Optional<OutContact> removeOutContact(Jid jid) {
        if (jid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(outContactsMap.remove(jid));
    }

    @Override
    public LinkedWhatsAppContactStore clearOutContacts() {
        outContactsMap.clear();
        return this;
    }

    @Override
    public void registerLidMapping(Jid phoneJid, Jid lidJid) {
        registerLidMapping(phoneJid, lidJid, null);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation normalises both JIDs through {@link Jid#withoutData()} before indexing so
     * that mappings stay stable across device-suffix changes. When a non-null timestamp is supplied
     * it is compared against the per-LID timestamp sidecar, and a strictly older value is dropped so
     * out-of-order delivery of mapping events cannot rewrite a fresher binding.
     */
    @Override
    public void registerLidMapping(Jid phoneJid, Jid lidJid, Instant timestamp) {
        if (phoneJid == null || lidJid == null) {
            return;
        }
        var normalizedPhone = phoneJid.withoutData();
        var normalizedLid = lidJid.withoutData();
        if (timestamp != null) {
            var existing = lidMappingTimestamps.get(normalizedLid);
            if (existing != null && timestamp.isBefore(existing)) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "dropping stale lid mapping for {0}, event older than recorded timestamp", normalizedLid);
                }
                return;
            }
            lidMappingTimestamps.put(normalizedLid, timestamp);
        }
        lidToPhoneMappings.put(normalizedLid, normalizedPhone);
        phoneToLidMappings.put(normalizedPhone, normalizedLid);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "registered lid mapping {0} <-> {1}", normalizedPhone, normalizedLid);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation short-circuits when the supplied LID belongs to the local account
     * (resolved through the bound {@link LinkedWhatsAppAccountStore}) and otherwise consults the LID-to-phone
     * table first. On a miss it scans the contact map for any entry whose {@link Contact#lid()}
     * matches because partial-state stores may have populated only the contact-side LID without
     * registering an entry in the mapping table.
     */
    @Override
    public Optional<Jid> findPhoneByLid(Jid lidJid) {
        if (lidJid == null) {
            return Optional.empty();
        }
        var localLid = account == null ? null : account.lid().orElse(null);
        if (localLid != null && Objects.equals(lidJid.user(), localLid.user())) {
            return account.jid()
                    .map(pn -> pn.withDevice(lidJid.device()));
        }
        var mapped = lidToPhoneMappings.get(lidJid.withoutData());
        if (mapped != null) {
            return Optional.of(mapped);
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "lid mapping table miss for {0}, scanning contacts", lidJid);
        return contactsMap.values()
                .stream()
                .filter(contact -> contact.lid().filter(lidJid::equals).isPresent())
                .findFirst()
                .map(Contact::jid);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation short-circuits when the supplied JID already targets the LID server or
     * belongs to the local account (resolved through the bound {@link LinkedWhatsAppAccountStore}), then consults
     * the phone-to-LID table. On a miss it reads {@link Contact#lid()} straight from the contact map
     * for the same dual-source reason as {@link #findPhoneByLid}; the lookup deliberately bypasses
     * {@link #findContactByJid} because that method calls back into this one for cross-LID resolution
     * and would recurse forever.
     */
    @Override
    public Optional<Jid> findLidByPhone(Jid phoneJid) {
        if (phoneJid == null) {
            return Optional.empty();
        }
        if (phoneJid.hasLidServer()) {
            return Optional.of(phoneJid);
        }
        var localJid = account == null ? null : account.jid().orElse(null);
        if (localJid != null && Objects.equals(phoneJid.user(), localJid.user())) {
            return account.lid()
                    .map(lid -> lid.withDevice(phoneJid.device()));
        }
        var normalisedPhone = phoneJid.withoutData();
        var mapped = phoneToLidMappings.get(normalisedPhone);
        if (mapped != null) {
            return Optional.of(mapped);
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "phone-to-lid mapping table miss for {0}, checking contact record", normalisedPhone);
        var contact = contactsMap.get(normalisedPhone);
        if (contact != null) {
            return contact.lid();
        }
        return Optional.empty();
    }

    @Override
    public Optional<BusinessVerifiedName> findVerifiedBusinessName(Jid jid) {
        Objects.requireNonNull(jid, "jid cannot be null");
        return Optional.ofNullable(verifiedBusinessNamesMap.get(jid.toUserJid()));
    }

    @Override
    public void addVerifiedBusinessName(Jid jid, BusinessVerifiedName record) {
        Objects.requireNonNull(jid, "jid cannot be null");
        Objects.requireNonNull(record, "record cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "adding verified business name for {0}", jid);
        verifiedBusinessNamesMap.put(jid.toUserJid(), record);
    }

    @Override
    public void removeVerifiedBusinessName(Jid jid) {
        Objects.requireNonNull(jid, "jid cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "removing verified business name for {0}", jid);
        verifiedBusinessNamesMap.remove(jid.toUserJid());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation falls back to the paired phone/LID identity when the direct lookup misses,
     * mirroring the dual-key fallback in {@link #findContactByJid}, because the device-list cache may
     * have been populated under either identity for the same user.
     */
    @Override
    public Optional<DeviceList> findDeviceList(Jid userJid) {
        Objects.requireNonNull(userJid, "userJid cannot be null");

        var deviceList = deviceLists.get(userJid);
        if (deviceList != null) {
            return Optional.of(deviceList);
        }

        Jid alternateJid;
        if (userJid.hasUserServer()) {
            alternateJid = findLidByPhone(userJid).orElse(null);
        } else if (userJid.hasLidServer()) {
            alternateJid = findPhoneByLid(userJid).orElse(null);
        } else {
            alternateJid = null;
        }

        if (alternateJid == null) {
            return Optional.empty();
        }

        if (Log.TRACE) LOGGER.log(Level.TRACE, "device list cache miss for {0}, resolved alternate identity {1}", userJid, alternateJid);
        var alternateList = deviceLists.get(alternateJid);
        return Optional.ofNullable(alternateList);
    }

    @Override
    public SequencedCollection<DeviceList> deviceLists() {
        return List.copyOf(deviceLists.sequencedValues());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation enforces an LRU-style cap of {@link #MAX_DEVICE_LISTS} entries: when the
     * cache is full and the {@code userJid} is not already present, the eldest entry is evicted
     * before the new mapping is inserted.
     */
    @Override
    public void addDeviceList(DeviceList deviceList) {
        Objects.requireNonNull(deviceList, "deviceList cannot be null");

        var userJid = deviceList.userJid();

        if (deviceLists.size() >= MAX_DEVICE_LISTS && !deviceLists.containsKey(userJid)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "device list cache full at {0} entries, evicting eldest entry", MAX_DEVICE_LISTS);
            deviceLists.pollLastEntry();
        }

        deviceLists.put(userJid, deviceList);
    }

    @Override
    public void removeDeviceList(Jid userJid) {
        Objects.requireNonNull(userJid, "userJid cannot be null");
        deviceLists.remove(userJid);
    }

    @Override
    public void clearDeviceLists() {
        deviceLists.clear();
    }

    @Override
    public void addToInteropHostedVerificationCache(Jid userJid) {
        if (userJid != null) {
            interopHostedVerificationCache.add(userJid.toUserJid());
        }
    }

    @Override
    public boolean isInInteropHostedVerificationCache(Jid userJid) {
        if (userJid == null) {
            return false;
        }
        return interopHostedVerificationCache.contains(userJid.toUserJid());
    }

    @Override
    public void clearInteropHostedVerificationCache() {
        interopHostedVerificationCache.clear();
    }

    @Override
    public Set<Jid> blockedContacts() {
        return Set.copyOf(blockedContacts);
    }

    @Override
    public void addBlockedContact(Jid contact) {
        if (contact != null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "blocking contact {0}", contact);
            blockedContacts.add(contact.toUserJid());
        }
    }

    @Override
    public void removeBlockedContact(Jid contact) {
        if (contact != null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "unblocking contact {0}", contact);
            blockedContacts.remove(contact.toUserJid());
        }
    }

    @Override
    public void setBlockedContacts(Collection<Jid> contacts) {
        blockedContacts.clear();
        if (contacts != null) {
            for (var contact : contacts) {
                if (contact != null) {
                    blockedContacts.add(contact.toUserJid());
                }
            }
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "block list replaced, entries={0}", blockedContacts.size());
    }

    @Override
    public Optional<String> blocklistHash() {
        return Optional.ofNullable(blocklistHash);
    }

    @Override
    public LinkedWhatsAppContactStore setBlocklistHash(String blocklistHash) {
        this.blocklistHash = blocklistHash;
        return this;
    }

    @Override
    public boolean blocklistMigrated() {
        return blocklistMigrated;
    }

    @Override
    public LinkedWhatsAppContactStore setBlocklistMigrated(boolean blocklistMigrated) {
        this.blocklistMigrated = blocklistMigrated;
        return this;
    }

    @Override
    public boolean receivedBlocklistMigrationBefore1x1Migration() {
        return receivedBlocklistMigrationBefore1x1Migration;
    }

    @Override
    public LinkedWhatsAppContactStore setReceivedBlocklistMigrationBefore1x1Migration(boolean value) {
        this.receivedBlocklistMigrationBefore1x1Migration = value;
        return this;
    }

    @Override
    public Optional<DeviceCapabilities> primaryDeviceCapabilities() {
        return Optional.ofNullable(primaryDeviceCapabilities);
    }

    @Override
    public LinkedWhatsAppContactStore setPrimaryDeviceCapabilities(DeviceCapabilities capabilities) {
        this.primaryDeviceCapabilities = capabilities;
        return this;
    }

    @Override
    public Collection<DeviceCapabilitiesEntry> deviceCapabilitiesStates() {
        return List.copyOf(deviceCapabilitiesStates.values());
    }

    @Override
    public Optional<DeviceCapabilitiesEntry> findDeviceCapabilitiesEntry(Jid deviceJid) {
        if (deviceJid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(deviceCapabilitiesStates.get(deviceJid));
    }

    @Override
    public LinkedWhatsAppContactStore putDeviceCapabilitiesEntry(DeviceCapabilitiesEntry entry) {
        Objects.requireNonNull(entry, "entry cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "storing device capabilities entry for {0}", entry.deviceJid());
        deviceCapabilitiesStates.put(entry.deviceJid(), entry);
        return this;
    }

    @Override
    public Optional<DeviceCapabilitiesEntry> removeDeviceCapabilitiesEntry(Jid deviceJid) {
        if (deviceJid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(deviceCapabilitiesStates.remove(deviceJid));
    }

    @Override
    public LinkedWhatsAppContactStore clearDeviceCapabilitiesStates() {
        deviceCapabilitiesStates.clear();
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProtobufLinkedWhatsAppContactStore that)) {
            return false;
        }
        return blocklistMigrated == that.blocklistMigrated
               && receivedBlocklistMigrationBefore1x1Migration == that.receivedBlocklistMigrationBefore1x1Migration
               && Objects.equals(contactsMap, that.contactsMap)
               && Objects.equals(outContactsMap, that.outContactsMap)
               && Objects.equals(verifiedBusinessNamesMap, that.verifiedBusinessNamesMap)
               && Objects.equals(contactTextStatusesMap, that.contactTextStatusesMap)
               && Objects.equals(deviceLists, that.deviceLists)
               && Objects.equals(lidToPhoneMappings, that.lidToPhoneMappings)
               && Objects.equals(phoneToLidMappings, that.phoneToLidMappings)
               && Objects.equals(lidMappingTimestamps, that.lidMappingTimestamps)
               && Objects.equals(interopHostedVerificationCache, that.interopHostedVerificationCache)
               && Objects.equals(blockedContacts, that.blockedContacts)
               && Objects.equals(blocklistHash, that.blocklistHash)
               && Objects.equals(primaryDeviceCapabilities, that.primaryDeviceCapabilities)
               && Objects.equals(deviceCapabilitiesStates, that.deviceCapabilitiesStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contactsMap, outContactsMap, verifiedBusinessNamesMap, contactTextStatusesMap,
                deviceLists, lidToPhoneMappings, phoneToLidMappings, lidMappingTimestamps,
                interopHostedVerificationCache, blockedContacts, blocklistHash, blocklistMigrated,
                receivedBlocklistMigrationBefore1x1Migration, primaryDeviceCapabilities, deviceCapabilitiesStates);
    }
}
