package com.github.auties00.cobalt.wire.linked.sync.action.media;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A sync action that propagates the user's status-update privacy settings
 * across linked devices.
 *
 * <p>The action describes who is allowed to view the user's status updates,
 * whether updates should be cross-posted to Facebook or Instagram, and any
 * user-defined custom audiences that can be targeted individually. Updating
 * the action replaces the full privacy configuration on every linked device.
 */
@ProtobufMessage(name = "SyncActionValue.StatusPrivacyAction")
public final class StatusPrivacyAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The app-state action name that identifies this action type on the wire.
     */
    public static final String ACTION_NAME = "status_privacy";

    /**
     * The app-state action version that identifies this action revision on the
     * wire.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * The app-state collection that stores this action type.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_HIGH;

    /**
     * Returns the action name used to route this action through the app-state
     * sync pipeline.
     *
     * @return the canonical action name
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version used to route this action through the
     * app-state sync pipeline.
     *
     * @return the canonical action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The mode selecting which group of recipients can view the user's
     * status updates.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    StatusDistributionMode mode;

    /**
     * The list of contact JIDs applied by {@link StatusDistributionMode#ALLOW_LIST}
     * or {@link StatusDistributionMode#DENY_LIST}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    List<Jid> userJid;

    /**
     * Whether the user has opted in to cross-posting their status updates to
     * Facebook.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    Boolean shareToFB;

    /**
     * Whether the user has opted in to cross-posting their status updates to
     * Instagram.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean shareToIG;

    /**
     * The set of named custom audiences the user has defined for status
     * sharing. Each custom list captures an explicit selection of contacts
     * that can be targeted without falling back to the global allow/deny
     * lists.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    List<CustomList> customLists;


    /**
     * Constructs a new {@code StatusPrivacyAction} carrying the supplied
     * privacy configuration.
     *
     * @param mode         the distribution mode
     * @param userJid      the allow/deny contact list
     * @param shareToFB    the Facebook cross-posting flag
     * @param shareToIG    the Instagram cross-posting flag
     * @param customLists  the user-defined custom audiences
     */
    StatusPrivacyAction(StatusDistributionMode mode, List<Jid> userJid, Boolean shareToFB, Boolean shareToIG, List<CustomList> customLists) {
        this.mode = mode;
        this.userJid = userJid;
        this.shareToFB = shareToFB;
        this.shareToIG = shareToIG;
        this.customLists = customLists;
    }

    /**
     * Returns the mode selecting which group of recipients can view the
     * user's status updates.
     *
     * @return the distribution mode, or {@link Optional#empty()} if unset
     */
    public Optional<StatusDistributionMode> mode() {
        return Optional.ofNullable(mode);
    }

    /**
     * Returns the list of contact JIDs applied by the allow or deny mode.
     *
     * @return an unmodifiable view of the contact JIDs, never {@code null}
     */
    public List<Jid> userJid() {
        return userJid == null ? List.of() : Collections.unmodifiableList(userJid);
    }

    /**
     * Returns whether status updates should be cross-posted to Facebook.
     *
     * @return the {@code shareToFB} flag, or {@link Optional#empty()} if unset
     */
    public Optional<Boolean> shareToFB() {
        return Optional.ofNullable(shareToFB);
    }

    /**
     * Returns whether status updates should be cross-posted to Instagram.
     *
     * @return the {@code shareToIG} flag, or {@link Optional#empty()} if unset
     */
    public Optional<Boolean> shareToIG() {
        return Optional.ofNullable(shareToIG);
    }

    /**
     * Returns the user-defined custom audiences for status sharing.
     *
     * @return an unmodifiable view of the custom lists, never {@code null}
     */
    public List<CustomList> customLists() {
        return customLists == null ? List.of() : Collections.unmodifiableList(customLists);
    }

    /**
     * Sets the mode selecting which group of recipients can view the user's
     * status updates.
     *
     * @param mode the new distribution mode, or {@code null} to clear it
     */
    public void setMode(StatusDistributionMode mode) {
        this.mode = mode;
    }

    /**
     * Sets the list of contact JIDs applied by the allow or deny mode.
     *
     * @param userJid the new contact list, or {@code null} to clear it
     */
    public void setUserJid(List<Jid> userJid) {
        this.userJid = userJid;
    }

    /**
     * Sets whether status updates should be cross-posted to Facebook.
     *
     * @param shareToFB the new {@code shareToFB} value, or {@code null} to clear it
     */
    public void setShareToFB(Boolean shareToFB) {
        this.shareToFB = shareToFB;
    }

    /**
     * Sets whether status updates should be cross-posted to Instagram.
     *
     * @param shareToIG the new {@code shareToIG} value, or {@code null} to clear it
     */
    public void setShareToIG(Boolean shareToIG) {
        this.shareToIG = shareToIG;
    }

    /**
     * Sets the custom audience lists for status sharing.
     *
     * @param customLists the custom lists to store, or {@code null} to clear them
     */
    public void setCustomLists(List<CustomList> customLists) {
        this.customLists = customLists;
    }

    /**
     * The distribution mode that selects which group of recipients can view a
     * status update.
     */
    @ProtobufEnum(name = "SyncActionValue.StatusPrivacyAction.StatusDistributionMode")
    public enum StatusDistributionMode {
        /**
         * Restricts visibility to the contacts contained in the
         * {@link StatusPrivacyAction#userJid()} allow list.
         */
        ALLOW_LIST(0),
        /**
         * Hides the status from the contacts contained in the
         * {@link StatusPrivacyAction#userJid()} deny list while showing it to
         * all other contacts.
         */
        DENY_LIST(1),
        /**
         * Shares the status with every contact in the address book.
         */
        CONTACTS(2),
        /**
         * Shares the status with the user's curated close-friends list.
         */
        CLOSE_FRIENDS(3),
        /**
         * Shares the status with the recipients enumerated by a named
         * {@link CustomList} entry.
         */
        CUSTOM_LIST(4);

        /**
         * Constructs a new {@code StatusDistributionMode} constant with the
         * supplied wire index.
         *
         * @param index the protobuf wire index
         */
        StatusDistributionMode(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index for this constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index for this constant.
         *
         * @return the wire index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * A user-defined named audience for status sharing.
     *
     * <p>Each custom list captures a curated selection of contacts the user
     * can target without falling back to the global allow or deny lists.
     * Custom lists are addressed by an opaque identifier and carry a display
     * name plus an optional emoji glyph used to decorate pickers.
     */
    @ProtobufMessage(name = "SyncActionValue.StatusPrivacyAction.CustomList")
    public static final class CustomList {
        /**
         * The opaque identifier of this custom list.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String listId;

        /**
         * The display name shown for this custom list.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String name;

        /**
         * The optional emoji glyph associated with this custom list.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String emoji;

        /**
         * Whether this custom list is the currently selected audience.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
        Boolean isSelected;

        /**
         * Constructs a new custom list with the supplied identifier, display
         * name, optional emoji and selection flag.
         *
         * @param listId     the opaque identifier of the list
         * @param name       the display name of the list
         * @param emoji      the optional emoji glyph for the list
         * @param isSelected whether the list is currently selected
         */
        CustomList(String listId, String name, String emoji, Boolean isSelected) {
            this.listId = listId;
            this.name = name;
            this.emoji = emoji;
            this.isSelected = isSelected;
        }

        /**
         * Returns the opaque identifier of this custom list.
         *
         * @return the list identifier, or {@link Optional#empty()} if unset
         */
        public Optional<String> listId() {
            return Optional.ofNullable(listId);
        }

        /**
         * Returns the display name shown for this custom list.
         *
         * @return the display name, or {@link Optional#empty()} if unset
         */
        public Optional<String> name() {
            return Optional.ofNullable(name);
        }

        /**
         * Returns the optional emoji glyph for this custom list.
         *
         * @return the emoji glyph, or {@link Optional#empty()} if unset
         */
        public Optional<String> emoji() {
            return Optional.ofNullable(emoji);
        }

        /**
         * Returns whether this custom list is currently selected.
         *
         * @return the selection flag, or {@link Optional#empty()} if unset
         */
        public Optional<Boolean> isSelected() {
            return Optional.ofNullable(isSelected);
        }

        /**
         * Sets the opaque identifier of this custom list.
         *
         * @param listId the new identifier, or {@code null} to clear it
         */
        public void setListId(String listId) {
            this.listId = listId;
        }

        /**
         * Sets the display name shown for this custom list.
         *
         * @param name the new display name, or {@code null} to clear it
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Sets the optional emoji glyph for this custom list.
         *
         * @param emoji the new emoji glyph, or {@code null} to clear it
         */
        public void setEmoji(String emoji) {
            this.emoji = emoji;
        }

        /**
         * Sets whether this custom list is currently selected.
         *
         * @param isSelected the new selection flag, or {@code null} to clear it
         */
        public void setIsSelected(Boolean isSelected) {
            this.isSelected = isSelected;
        }
    }
}
