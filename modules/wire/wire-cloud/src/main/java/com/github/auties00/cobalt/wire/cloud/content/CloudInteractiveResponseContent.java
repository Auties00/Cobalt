package com.github.auties00.cobalt.wire.cloud.content;

import com.github.auties00.cobalt.wire.core.message.InteractiveResponseContent;

import java.util.Optional;

/**
 * The Cloud transport's inbound native-flow reply body.
 */
public final class CloudInteractiveResponseContent implements InteractiveResponseContent {
    /**
     * The native-flow response name, or {@code null} when unset.
     */
    private final String name;

    /**
     * The raw response JSON, or {@code null} when unset.
     */
    private final String responseJson;

    /**
     * The optional body text, or {@code null} when none.
     */
    private final String body;

    /**
     * Constructs a Cloud native-flow reply body.
     *
     * @param name         the native-flow response name, or {@code null} when unset
     * @param responseJson the raw response JSON, or {@code null} when unset
     * @param body         the optional body text, or {@code null} when none
     */
    public CloudInteractiveResponseContent(String name, String responseJson, String body) {
        this.name = name;
        this.responseJson = responseJson;
        this.body = body;
    }

    @Override
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    @Override
    public Optional<String> responseJson() {
        return Optional.ofNullable(responseJson);
    }

    @Override
    public Optional<String> body() {
        return Optional.ofNullable(body);
    }
}
