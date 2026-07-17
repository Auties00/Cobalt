package com.github.auties00.cobalt.wire.linked.bot.response;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Metadata for a table fragment within a WhatsApp AI bot rich response.
 *
 * <p>The table is represented as an ordered list of
 * {@link AIRichResponseTableRow} entries. Rows flagged as
 * {@linkplain AIRichResponseTableRow#isHeading() headings} are
 * rendered with distinct styling (for example, bold text or background
 * colour) and typically appear at the top of the table. An optional
 * {@linkplain #title() title} may appear above the table.
 *
 * <p>This type implements {@link AIRichResponseSubMessageContent} and
 * appears as the {@link AIRichResponseSubMessageType#TABLE TABLE}
 * variant within an {@link AIRichResponseSubMessage}.
 *
 * @see AIRichResponseSubMessage#content()
 */
@ProtobufMessage(name = "AIRichResponseTableMetadata")
public final class AIRichResponseTableMetadata implements AIRichResponseSubMessageContent {
    /**
     * The ordered list of rows that compose this table.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<AIRichResponseTableRow> rows;

    /**
     * An optional title displayed above the table.
     *
     * <p>Example: {@code "Comparison of programming languages"}
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String title;


    /**
     * Constructs a new table metadata instance.
     *
     * @param rows  the ordered list of table rows, or {@code null}
     * @param title the table title, or {@code null}
     */
    AIRichResponseTableMetadata(List<AIRichResponseTableRow> rows, String title) {
        this.rows = rows;
        this.title = title;
    }

    /**
     * Returns the ordered list of rows that compose this table.
     *
     * @return an unmodifiable list of table rows, never {@code null}
     */
    public List<AIRichResponseTableRow> rows() {
        return rows == null ? List.of() : Collections.unmodifiableList(rows);
    }

    /**
     * Returns the optional title displayed above the table.
     *
     * @return an {@link Optional} containing the title, or empty if
     *         not set
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Sets the ordered list of rows that compose this table.
     *
     * @param rows the table rows to set
     */
    public void setRows(List<AIRichResponseTableRow> rows) {
        this.rows = rows;
    }

    /**
     * Sets the title displayed above the table.
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * A single row within an AI rich response table.
     *
     * <p>Each row contains a list of cell values as strings.
     * Rows marked as {@linkplain #isHeading() headings} are rendered
     * with header styling (e.g. bold text) and typically represent
     * column headers.
     */
    @ProtobufMessage(name = "AIRichResponseTableMetadata.AIRichResponseTableRow")
    public static final class AIRichResponseTableRow {
        /**
         * The cell values for this row, one string per column.
         *
         * <p>Example: {@code ["Language", "Year", "Paradigm"]}
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        List<String> items;

        /**
         * Whether this row is a heading row rendered with header
         * styling.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
        Boolean isHeading;


        /**
         * Constructs a new table row.
         *
         * @param items     the cell values for this row, or {@code null}
         * @param isHeading whether this row is a heading row, or {@code null}
         */
        AIRichResponseTableRow(List<String> items, Boolean isHeading) {
            this.items = items;
            this.isHeading = isHeading;
        }

        /**
         * Returns the cell values for this row.
         *
         * @return an unmodifiable list of cell strings, never
         *         {@code null}
         */
        public List<String> items() {
            return items == null ? List.of() : Collections.unmodifiableList(items);
        }

        /**
         * Returns whether this row is a heading row.
         *
         * @return {@code true} if this row should be rendered as a
         *         heading, {@code false} otherwise
         */
        public boolean isHeading() {
            return isHeading != null && isHeading;
        }

        /**
         * Sets the cell values for this row.
         *
         * @param items the cell strings to set
         */
        public void setItems(List<String> items) {
            this.items = items;
    }

        /**
         * Sets whether this row is a heading row.
         *
         * @param isHeading {@code true} to mark this row as a heading
         */
        public void setHeading(Boolean isHeading) {
            this.isHeading = isHeading;
    }
    }
}
