package com.github.auties00.cobalt.wire.linked.device.capabilities;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * Declaration of which optional features the primary device running the
 * account supports, shared with companion devices through the app state
 * synchronisation protocol.
 *
 * <p>WhatsApp rolls out features incrementally and different clients (iOS,
 * Android, Web, Business) gain support at different times. When a new
 * companion device links to an account it needs to know which features the
 * account's primary device is able to handle so that it can enable or
 * disable the corresponding UI and avoid sending data that the primary would
 * not understand. This sync action transports that information.
 *
 * <p>The payload is synchronised through the {@code regular_low} app state
 * collection and has a current version of {@code 7}, meaning that a new
 * full synchronisation is triggered when the client sees a payload with a
 * higher version number. Every field is optional: absence means the
 * capability is either unsupported or unknown.
 *
 * @see SyncAction
 * @see SyncPatchType#REGULAR_LOW
 */
@ProtobufMessage(name = "DeviceCapabilities")
public final class DeviceCapabilities implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The app state action name that identifies this payload inside a sync
     * mutation. Matches the server side constant {@code device_capabilities}.
     */
    public static final String ACTION_NAME = "device_capabilities";
    /**
     * Current schema version of the action. Companion devices compare the
     * incoming version against this constant and trigger a full resync when
     * the server advertises a higher one.
     */
    public static final int ACTION_VERSION = 7;
    /**
     * App state collection that carries this action.
     *
     * <p>The {@link SyncPatchType#REGULAR_LOW} collection is used for low
     * priority settings that do not need to converge in real time.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the app state action name of this sync action.
     *
     * @return {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the current schema version of this sync action.
     *
     * @return {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * How well the primary device implements the "locked chats" feature (a
     * lock that hides chats behind a biometric or passcode prompt). Optional.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    ChatLockSupportLevel chatLockSupportLevel;

    /**
     * Status of the primary's migration from phone number based Signal
     * addressing to LID (local identifier) addressing for 1 on 1 chats.
     * Optional.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    LIDMigration lidMigration;

    /**
     * Set of business broadcast features supported by the primary (only
     * meaningful for WhatsApp Business accounts). Optional.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    BusinessBroadcast businessBroadcast;

    /**
     * Whether the account currently has a profile picture uploaded, cached
     * here so companion devices can render the self avatar without another
     * server round trip. Optional.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    UserHasAvatar userHasAvatar;

    /**
     * Whether the primary supports the group member name tag feature (a
     * small inline badge that attributes a message to the group member that
     * sent it), and in which direction (sender, receiver). Optional.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    MemberNameTagPrimarySupport memberNameTagPrimarySupport;

    /**
     * Support level for Meta AI threads (chats with Meta AI). Optional.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    AiThread aiThread;


    /**
     * Creates a new device capability payload. This constructor is package
     * private and is reserved for the protobuf deserialiser and the
     * generated builder.
     *
     * @param chatLockSupportLevel         chat lock capability level, or {@code null}
     * @param lidMigration                 LID migration state, or {@code null}
     * @param businessBroadcast            business broadcast capabilities, or {@code null}
     * @param userHasAvatar                avatar presence indicator, or {@code null}
     * @param memberNameTagPrimarySupport  group member name tag support, or {@code null}
     * @param aiThread                     Meta AI thread support, or {@code null}
     */
    DeviceCapabilities(ChatLockSupportLevel chatLockSupportLevel, LIDMigration lidMigration, BusinessBroadcast businessBroadcast, UserHasAvatar userHasAvatar, MemberNameTagPrimarySupport memberNameTagPrimarySupport, AiThread aiThread) {
        this.chatLockSupportLevel = chatLockSupportLevel;
        this.lidMigration = lidMigration;
        this.businessBroadcast = businessBroadcast;
        this.userHasAvatar = userHasAvatar;
        this.memberNameTagPrimarySupport = memberNameTagPrimarySupport;
        this.aiThread = aiThread;
    }

    /**
     * Returns the chat lock support level advertised by the primary device.
     *
     * @return the support level, or {@code Optional.empty()} if unknown
     */
    public Optional<ChatLockSupportLevel> chatLockSupportLevel() {
        return Optional.ofNullable(chatLockSupportLevel);
    }

    /**
     * Returns the LID migration state for 1 on 1 chats.
     *
     * @return the migration descriptor, or {@code Optional.empty()} if the
     *         account has not started the migration or the state is unknown
     */
    public Optional<LIDMigration> lidMigration() {
        return Optional.ofNullable(lidMigration);
    }

    /**
     * Returns the business broadcast capability descriptor.
     *
     * @return the business broadcast descriptor, or {@code Optional.empty()}
     *         if the account does not advertise business broadcast support
     */
    public Optional<BusinessBroadcast> businessBroadcast() {
        return Optional.ofNullable(businessBroadcast);
    }

    /**
     * Returns the cached avatar presence indicator.
     *
     * @return the avatar descriptor, or {@code Optional.empty()} if the
     *         indicator has not been published by the primary
     */
    public Optional<UserHasAvatar> userHasAvatar() {
        return Optional.ofNullable(userHasAvatar);
    }

    /**
     * Returns how the primary device handles the group member name tag
     * feature.
     *
     * @return the name tag support level, or {@code Optional.empty()} if
     *         unknown
     */
    public Optional<MemberNameTagPrimarySupport> memberNameTagPrimarySupport() {
        return Optional.ofNullable(memberNameTagPrimarySupport);
    }

    /**
     * Returns the Meta AI thread support descriptor.
     *
     * @return the AI thread descriptor, or {@code Optional.empty()} if the
     *         primary has not advertised AI thread support
     */
    public Optional<AiThread> aiThread() {
        return Optional.ofNullable(aiThread);
    }

    /**
     * Overrides the chat lock support level.
     *
     * @param chatLockSupportLevel the new level, or {@code null} to clear it
     */
    public void setChatLockSupportLevel(ChatLockSupportLevel chatLockSupportLevel) {
        this.chatLockSupportLevel = chatLockSupportLevel;
    }

    /**
     * Overrides the LID migration descriptor.
     *
     * @param lidMigration the new migration descriptor, or {@code null} to
     *                     clear it
     */
    public void setLidMigration(LIDMigration lidMigration) {
        this.lidMigration = lidMigration;
    }

    /**
     * Overrides the business broadcast capability descriptor.
     *
     * @param businessBroadcast the new descriptor, or {@code null} to clear it
     */
    public void setBusinessBroadcast(BusinessBroadcast businessBroadcast) {
        this.businessBroadcast = businessBroadcast;
    }

    /**
     * Overrides the cached avatar presence indicator.
     *
     * @param userHasAvatar the new indicator, or {@code null} to clear it
     */
    public void setUserHasAvatar(UserHasAvatar userHasAvatar) {
        this.userHasAvatar = userHasAvatar;
    }

    /**
     * Overrides the group member name tag support level.
     *
     * @param memberNameTagPrimarySupport the new level, or {@code null} to
     *                                    clear it
     */
    public void setMemberNameTagPrimarySupport(MemberNameTagPrimarySupport memberNameTagPrimarySupport) {
        this.memberNameTagPrimarySupport = memberNameTagPrimarySupport;
    }

    /**
     * Overrides the Meta AI thread support descriptor.
     *
     * @param aiThread the new descriptor, or {@code null} to clear it
     */
    public void setAiThread(AiThread aiThread) {
        this.aiThread = aiThread;
    }

    /**
     * Level of support for the chat lock feature on the primary device.
     *
     * <p>Chat lock allows a user to hide selected chats behind a biometric
     * or passcode prompt. Because older clients do not understand the lock
     * metadata, the primary advertises how faithfully it can honour it.
     */
    @ProtobufEnum(name = "DeviceCapabilities.ChatLockSupportLevel")
    public static enum ChatLockSupportLevel {
        /**
         * Chat lock is not supported. Locked chats sent to this device are
         * shown as regular unlocked chats.
         */
        NONE(0),
        /**
         * The primary understands the lock state of incoming chats but does
         * not implement the full UI flow (for example it can hide a chat but
         * does not support the dedicated locked chats folder).
         */
        MINIMAL(1),
        /**
         * Full chat lock support, including the hidden folder, biometric
         * prompt and lock state synchronisation across devices.
         */
        FULL(2);

        /**
         * Creates a new support level constant.
         *
         * @param index the protobuf wire index bound to this constant
         */
        ChatLockSupportLevel(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Protobuf wire index of this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Level of support for the group member name tag feature on the primary
     * device.
     *
     * <p>The name tag is a small inline badge that accompanies a message in
     * a group to attribute it to a specific participant. Support is split
     * by direction because some clients can display tags attached to
     * incoming messages without being able to attach tags to outgoing ones.
     */
    @ProtobufEnum(name = "DeviceCapabilities.MemberNameTagPrimarySupport")
    public static enum MemberNameTagPrimarySupport {
        /**
         * Member name tags are disabled. The primary cannot display nor
         * produce them.
         */
        DISABLED(0),
        /**
         * The primary can display an incoming name tag but cannot attach
         * one to outgoing messages.
         */
        RECEIVER_ENABLED(1),
        /**
         * The primary can attach a name tag to outgoing messages (and by
         * implication display incoming ones as well).
         */
        SENDER_ENABLED(2);

        /**
         * Creates a new support level constant.
         *
         * @param index the protobuf wire index bound to this constant
         */
        MemberNameTagPrimarySupport(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Protobuf wire index of this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index of this constant.
         *
         * @return the protobuf index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * Level of support advertised by the primary for Meta AI threads.
     *
     * <p>A Meta AI thread is a conversation with the Meta AI assistant.
     * Depending on the platform, a client may have full access to such
     * threads, only the underlying infrastructure (enough to relay the
     * conversation but not render it as a first class chat) or no support
     * at all.
     */
    @ProtobufMessage(name = "DeviceCapabilities.AiThread")
    public static final class AiThread {
        /**
         * Level at which the primary device supports Meta AI threads.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        AiThread.SupportLevel supportLevel;


        /**
         * Creates a new AI thread descriptor. Package private; used by the
         * protobuf deserialiser and generated builder.
         *
         * @param supportLevel the advertised support level, or {@code null}
         */
        AiThread(SupportLevel supportLevel) {
            this.supportLevel = supportLevel;
        }

        /**
         * Returns the support level advertised by the primary device.
         *
         * @return the support level, or {@code Optional.empty()} if the
         *         field was not set
         */
        public Optional<SupportLevel> supportLevel() {
            return Optional.ofNullable(supportLevel);
        }

        /**
         * Overrides the AI thread support level.
         *
         * @param supportLevel the new support level, or {@code null} to
         *                     clear it
         */
        public void setSupportLevel(SupportLevel supportLevel) {
            this.supportLevel = supportLevel;
    }

        /**
         * Discrete levels at which a client can support Meta AI threads.
         */
        @ProtobufEnum(name = "DeviceCapabilities.AiThread.SupportLevel")
        public static enum SupportLevel {
            /**
             * No AI thread support. Messages in AI threads are ignored.
             */
            NONE(0),
            /**
             * Infrastructure level support. The client can persist and
             * forward AI thread messages without providing a dedicated UI.
             */
            INFRA(1),
            /**
             * Full AI thread support including a dedicated chat experience.
             */
            FULL(2);

            /**
             * Creates a new support level constant.
             *
             * @param index the protobuf wire index bound to this constant
             */
            SupportLevel(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * Protobuf wire index of this constant.
             */
            final int index;

            /**
             * Returns the protobuf wire index of this constant.
             *
             * @return the protobuf index
             */
            public int index() {
                return this.index;
            }
        }
    }

    /**
     * Business broadcast feature flags published by the primary device.
     *
     * <p>Business broadcasts are bulk messaging campaigns that a WhatsApp
     * Business account can send to many opted in customers. Only the
     * {@code importListEnabled} sub capability is exposed here: WhatsApp
     * Web's schema declares additional fields (companion support, campaign
     * sync, insights sync, recipient limit) that Cobalt does not currently
     * parse.
     */
    @ProtobufMessage(name = "DeviceCapabilities.BusinessBroadcast")
    public static final class BusinessBroadcast {
        /**
         * Whether the primary supports importing an existing contact list
         * as a business broadcast audience.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        Boolean importListEnabled;


        /**
         * Creates a new business broadcast descriptor. Package private; used
         * by the protobuf deserialiser and generated builder.
         *
         * @param importListEnabled whether list import is supported, or
         *                          {@code null}
         */
        BusinessBroadcast(Boolean importListEnabled) {
            this.importListEnabled = importListEnabled;
        }

        /**
         * Returns whether the primary device supports importing a contact
         * list as a broadcast audience.
         *
         * @return {@code true} if list import is supported, {@code false}
         *         otherwise or if the flag is absent
         */
        public boolean importListEnabled() {
            return importListEnabled != null && importListEnabled;
        }

        /**
         * Overrides the list import support flag.
         *
         * @param importListEnabled the new flag, or {@code null} to clear it
         */
        public void setImportListEnabled(Boolean importListEnabled) {
            this.importListEnabled = importListEnabled;
    }
    }

    /**
     * Descriptor that tracks the primary's migration from phone number based
     * Signal addressing to LID (local identifier) addressing for 1 on 1
     * chats.
     *
     * <p>WhatsApp is progressively replacing phone number JIDs with LIDs so
     * that users can start chats without revealing their phone number. When
     * the primary performs the database migration that maps existing
     * 1 on 1 chats from phone number keys to LID keys, it records the
     * completion timestamp here so companion devices can align their own
     * migration state.
     */
    @ProtobufMessage(name = "DeviceCapabilities.LIDMigration")
    public static final class LIDMigration {
        /**
         * Moment at which the primary device completed the chat database
         * migration to LID addressing.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
        Instant chatDbMigrationTimestamp;


        /**
         * Creates a new LID migration descriptor. Package private; used by
         * the protobuf deserialiser and generated builder.
         *
         * @param chatDbMigrationTimestamp when the migration completed, or
         *                                 {@code null}
         */
        LIDMigration(Instant chatDbMigrationTimestamp) {
            this.chatDbMigrationTimestamp = chatDbMigrationTimestamp;
        }

        /**
         * Returns the moment at which the primary completed the chat
         * database migration to LID addressing.
         *
         * @return the migration timestamp, or {@code Optional.empty()} if
         *         the primary has not reported a completed migration
         */
        public Optional<Instant> chatDbMigrationTimestamp() {
            return Optional.ofNullable(chatDbMigrationTimestamp);
        }

        /**
         * Overrides the chat database migration timestamp.
         *
         * @param chatDbMigrationTimestamp the new timestamp, or {@code null}
         *                                 to clear it
         */
        public void setChatDbMigrationTimestamp(Instant chatDbMigrationTimestamp) {
            this.chatDbMigrationTimestamp = chatDbMigrationTimestamp;
    }
    }

    /**
     * Cached indicator of whether the account currently has a profile
     * picture (avatar) uploaded.
     *
     * <p>Companion devices use this flag to decide whether to display the
     * account's own avatar without querying the server first. A fresh sync
     * will always update this value when the primary uploads or removes an
     * avatar.
     */
    @ProtobufMessage(name = "DeviceCapabilities.UserHasAvatar")
    public static final class UserHasAvatar {
        /**
         * {@code true} when the account has a profile picture uploaded.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
        Boolean userHasAvatar;


        /**
         * Creates a new avatar presence descriptor. Package private; used by
         * the protobuf deserialiser and generated builder.
         *
         * @param userHasAvatar the new flag value, or {@code null}
         */
        UserHasAvatar(Boolean userHasAvatar) {
            this.userHasAvatar = userHasAvatar;
        }

        /**
         * Returns whether the account currently has a profile picture
         * uploaded.
         *
         * @return {@code true} if an avatar is available, {@code false}
         *         otherwise or if the flag is absent
         */
        public boolean userHasAvatar() {
            return userHasAvatar != null && userHasAvatar;
        }

        /**
         * Overrides the avatar presence flag.
         *
         * @param userHasAvatar the new flag value, or {@code null} to clear it
         */
        public void setUserHasAvatar(Boolean userHasAvatar) {
            this.userHasAvatar = userHasAvatar;
    }
    }
}
