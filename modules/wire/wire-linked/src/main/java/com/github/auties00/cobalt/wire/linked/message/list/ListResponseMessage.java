package com.github.auties00.cobalt.wire.linked.message.list;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A reply message sent by the recipient of a {@link ListMessage} to report
 * which row they selected from the list sheet.
 *
 * <p>When the recipient opens a list sheet and taps a row, their client
 * emits a {@code ListResponseMessage} that echoes the chosen row title and
 * carries the row identifier in a {@link SingleSelectReply}. The sender can
 * then correlate the selection back to the original {@link ListMessage} to
 * drive business logic such as menu navigation or order placement.
 */
@ProtobufMessage(name = "Message.ListResponseMessage")
public final class ListResponseMessage implements ContextualMessage {
    /**
     * The title of the row that the recipient selected, echoed back from the
     * original list sheet.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String title;

    /**
     * Declares which list variant the original {@link ListMessage} used,
     * indicating how the reply payload should be interpreted.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    ListType listType;

    /**
     * The reply payload carrying the identifier of the selected row when the
     * list type is {@link ListType#SINGLE_SELECT}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    SingleSelectReply singleSelectReply;

    /**
     * Contextual metadata attached to this message such as quoted message
     * information, mentioned users, and forwarding details.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * The description of the row that the recipient selected, echoed back
     * from the original list sheet.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String description;


    /**
     * Constructs a new {@code ListResponseMessage} with the given reply
     * properties. This constructor is package-private; instances are
     * normally built via the generated {@code ListResponseMessageBuilder}.
     *
     * @param title             the title of the selected row
     * @param listType          the list variant of the original message
     * @param singleSelectReply the single-select reply payload
     * @param contextInfo       contextual metadata attached to this message
     * @param description       the description of the selected row
     */
    ListResponseMessage(String title, ListType listType, SingleSelectReply singleSelectReply, ContextInfo contextInfo, String description) {
        this.title = title;
        this.listType = listType;
        this.singleSelectReply = singleSelectReply;
        this.contextInfo = contextInfo;
        this.description = description;
    }

    /**
     * Returns the title of the row that the recipient selected.
     *
     * @return an {@link Optional} containing the selected row title, or
     *         empty if none is set
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the list variant of the original {@link ListMessage}.
     *
     * @return an {@link Optional} containing the list type, or empty if none
     *         is set
     */
    public Optional<ListType> listType() {
        return Optional.ofNullable(listType);
    }

    /**
     * Returns the reply payload carrying the identifier of the selected row.
     *
     * @return an {@link Optional} containing the single-select reply, or
     *         empty if none is set
     */
    public Optional<SingleSelectReply> singleSelectReply() {
        return Optional.ofNullable(singleSelectReply);
    }

    /**
     * Returns the contextual metadata attached to this message.
     *
     * @return an {@link Optional} containing the context info, or empty if
     *         none is set
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the description of the row that the recipient selected.
     *
     * @return an {@link Optional} containing the selected row description,
     *         or empty if none is set
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Sets the title of the row that the recipient selected.
     *
     * @param title the new selected row title, or {@code null} to clear it
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Sets the list variant of the original {@link ListMessage}.
     *
     * @param listType the new list type, or {@code null} to clear it
     */
    public void setListType(ListType listType) {
        this.listType = listType;
    }

    /**
     * Sets the reply payload carrying the identifier of the selected row.
     *
     * @param singleSelectReply the new single-select reply, or {@code null}
     *                          to clear it
     */
    public void setSingleSelectReply(SingleSelectReply singleSelectReply) {
        this.singleSelectReply = singleSelectReply;
    }

    /**
     * Sets the contextual metadata attached to this message.
     *
     * @param contextInfo the new context info, or {@code null} to clear it
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Sets the description of the row that the recipient selected.
     *
     * @param description the new selected row description, or {@code null}
     *                    to clear it
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Enumerates the list variants for which a {@link ListResponseMessage}
     * may be produced.
     *
     * <p>The variant mirrors the corresponding {@link ListMessage.ListType}
     * of the original list message and drives how the reply payload should
     * be interpreted.
     */
    @ProtobufEnum(name = "Message.ListResponseMessage.ListType")
    public static enum ListType {
        /**
         * The list variant is unknown or unspecified.
         */
        UNKNOWN(0),
        /**
         * The original list was a single-select menu and the reply carries a
         * {@link SingleSelectReply} payload.
         */
        SINGLE_SELECT(1);

        /**
         * Constructs a list type with the given protobuf index.
         *
         * @param index the protobuf enum index
         */
        ListType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf enum index associated with this variant.
         */
        final int index;

        /**
         * Returns the protobuf enum index associated with this variant.
         *
         * @return the enum index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * The reply payload produced when the recipient selects a row from a
     * single-select {@link ListMessage}.
     *
     * <p>It carries the stable identifier of the selected {@link
     * ListMessage.Row}, which the sender can match against the original list
     * definition to determine which choice was made.
     */
    @ProtobufMessage(name = "Message.ListResponseMessage.SingleSelectReply")
    public static final class SingleSelectReply {
        /**
         * The identifier of the selected row, matching the {@code rowId} of
         * the corresponding {@link ListMessage.Row}.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String selectedRowId;


        /**
         * Constructs a new {@code SingleSelectReply} carrying the identifier
         * of the row the recipient selected.
         *
         * @param selectedRowId the identifier of the selected row
         */
        SingleSelectReply(String selectedRowId) {
            this.selectedRowId = selectedRowId;
        }

        /**
         * Returns the identifier of the row that the recipient selected.
         *
         * @return an {@link Optional} containing the selected row
         *         identifier, or empty if none is set
         */
        public Optional<String> selectedRowId() {
            return Optional.ofNullable(selectedRowId);
        }

        /**
         * Sets the identifier of the row that the recipient selected.
         *
         * @param selectedRowId the new selected row identifier, or
         *                      {@code null} to clear it
         */
        public void setSelectedRowId(String selectedRowId) {
            this.selectedRowId = selectedRowId;
    }
    }
}
