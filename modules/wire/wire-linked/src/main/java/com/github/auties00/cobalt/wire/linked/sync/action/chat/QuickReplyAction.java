package com.github.auties00.cobalt.wire.linked.sync.action.chat;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Sync action that creates, updates or deletes a WhatsApp Business quick
 * reply template.
 *
 * <p>Quick replies are reusable canned responses that a business account
 * can insert in conversations using a shortcut (for example {@code /hello}).
 * Each template carries a shortcut, the actual message text, a list of
 * keywords used by search, a usage counter and a deletion flag.
 *
 * <p>The action is replicated across the business user's linked devices so
 * that the template library stays identical everywhere.
 */
@ProtobufMessage(name = "SyncActionValue.QuickReplyAction")
public final class QuickReplyAction implements SyncAction<QuickReplyActionArgs> {
    /**
     * Canonical action name written as the first element of the sync index.
     */
    public static final String ACTION_NAME = "quick_reply";

    /**
     * Schema version declared by this action.
     */
    public static final int ACTION_VERSION = 2;

    /**
     * Collection this action is stored in when encoded into an app state patch.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the string {@code "quick_reply"}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version of this action type.
     *
     * @return the integer value {@code 2}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Shortcut token that triggers insertion of this quick reply (for
     * example {@code "/hello"}).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String shortcut;

    /**
     * Text that is inserted into the conversation when the shortcut is used.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String message;

    /**
     * Search keywords associated with the quick reply.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    List<String> keywords;

    /**
     * Usage counter incremented every time the quick reply is used.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    Integer count;

    /**
     * Deletion flag; when {@code true} the quick reply is removed from the
     * template library.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    Boolean deleted;


    /**
     * Constructs a new {@code QuickReplyAction} with the given fields.
     *
     * @param shortcut the shortcut token
     * @param message  the quick reply text
     * @param keywords the search keywords
     * @param count    the usage count
     * @param deleted  the deletion flag
     */
    QuickReplyAction(String shortcut, String message, List<String> keywords, Integer count, Boolean deleted) {
        this.shortcut = shortcut;
        this.message = message;
        this.keywords = keywords;
        this.count = count;
        this.deleted = deleted;
    }

    /**
     * Returns the shortcut token that triggers this quick reply.
     *
     * @return an {@link Optional} containing the shortcut, or an empty
     *         {@code Optional} when no shortcut is defined
     */
    public Optional<String> shortcut() {
        return Optional.ofNullable(shortcut);
    }

    /**
     * Returns the text of the quick reply.
     *
     * @return an {@link Optional} containing the message text, or an empty
     *         {@code Optional} when no text is defined
     */
    public Optional<String> message() {
        return Optional.ofNullable(message);
    }

    /**
     * Returns the list of search keywords associated with this quick reply.
     *
     * <p>The returned list is unmodifiable; an empty list is returned when
     * no keywords are defined.
     *
     * @return an unmodifiable {@link List} of keywords
     */
    public List<String> keywords() {
        return keywords == null ? List.of() : Collections.unmodifiableList(keywords);
    }

    /**
     * Returns the usage count of this quick reply.
     *
     * @return an {@link OptionalInt} containing the count, or an empty
     *         {@code OptionalInt} when no counter has been recorded
     */
    public OptionalInt count() {
        return count == null ? OptionalInt.empty() : OptionalInt.of(count);
    }

    /**
     * Returns whether this quick reply has been deleted.
     *
     * <p>If the underlying field is {@code null} this method returns
     * {@code false}.
     *
     * @return {@code true} if the quick reply is marked as deleted
     */
    public boolean deleted() {
        return deleted != null && deleted;
    }

    /**
     * Sets the shortcut token.
     *
     * @param shortcut the new shortcut, or {@code null} to clear it
     */
    public void setShortcut(String shortcut) {
        this.shortcut = shortcut;
    }

    /**
     * Sets the message text of the quick reply.
     *
     * @param message the new text, or {@code null} to clear it
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Sets the search keywords associated with the quick reply.
     *
     * @param keywords the new keyword list, or {@code null} to clear it
     */
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    /**
     * Sets the usage count.
     *
     * @param count the new count, or {@code null} to clear it
     */
    public void setCount(Integer count) {
        this.count = count;
    }

    /**
     * Sets the deletion flag.
     *
     * @param deleted the new deletion flag, or {@code null} to clear it
     */
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }


}
