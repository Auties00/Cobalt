package com.github.auties00.cobalt.wire.cloud.content;

import com.github.auties00.cobalt.wire.core.message.ListResponseContent;

import java.util.Optional;

/**
 * The Cloud transport's inbound list-selection body.
 */
public final class CloudListResponseContent implements ListResponseContent {
    /**
     * The selected row title, or {@code null} when unset.
     */
    private final String title;

    /**
     * The selected row description, or {@code null} when unset.
     */
    private final String description;

    /**
     * The selected row id, or {@code null} when unset.
     */
    private final String selectedRowId;

    /**
     * Constructs a Cloud list-selection body.
     *
     * @param title         the selected row title, or {@code null} when unset
     * @param description   the selected row description, or {@code null} when unset
     * @param selectedRowId the selected row id, or {@code null} when unset
     */
    public CloudListResponseContent(String title, String description, String selectedRowId) {
        this.title = title;
        this.description = description;
        this.selectedRowId = selectedRowId;
    }

    @Override
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    @Override
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    @Override
    public Optional<String> selectedRowId() {
        return Optional.ofNullable(selectedRowId);
    }
}
