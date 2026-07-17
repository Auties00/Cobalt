package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A Facebook page a merchant may run WhatsApp Business advertisements for.
 *
 * <p>Creating a Click-to-WhatsApp ad requires choosing the Facebook page the
 * ad runs under. The ad-creation flow shows the pages linked to the merchant's
 * account that the merchant is permitted to advertise on, each with its
 * {@linkplain #name() name}, {@linkplain #id() identifier}, optional
 * {@linkplain #profilePictureUri() profile picture}, and the
 * {@linkplain #permittedTasks() tasks} the merchant may perform on it.
 *
 * <p>This model is one such promotable page as the server reports it. The
 * permitted-task names are server-defined markers exposed as raw strings.
 */
@ProtobufMessage(name = "FacebookPage")
public final class FacebookPage {
    /**
     * Display name of the page. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    /**
     * Server-issued identifier of the page. {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String id;

    /**
     * URI of the page's profile picture, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String profilePictureUri;

    /**
     * Tasks the merchant is permitted to perform on the page, as server-defined
     * markers. Never {@code null}, possibly empty when the server reported
     * none.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final List<String> permittedTasks;

    /**
     * Constructs a new {@code FacebookPage}. A {@code null}
     * {@code permittedTasks} is coerced to an empty list, and the other
     * reference arguments may be {@code null} when the server omitted them.
     *
     * @param name              the page display name, or {@code null}
     * @param id                the page identifier, or {@code null}
     * @param profilePictureUri the profile-picture URI, or {@code null}
     * @param permittedTasks    the permitted-task markers; {@code null} treated as empty
     */
    FacebookPage(String name, String id, String profilePictureUri, List<String> permittedTasks) {
        this.name = name;
        this.id = id;
        this.profilePictureUri = profilePictureUri;
        this.permittedTasks = permittedTasks == null ? List.of() : permittedTasks;
    }

    /**
     * Returns the display name of the page.
     *
     * @return the page name, or empty when the server omitted it
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the server-issued identifier of the page.
     *
     * @return the page id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the URI of the page's profile picture.
     *
     * @return the profile-picture URI, or empty when the server omitted it
     */
    public Optional<String> profilePictureUri() {
        return Optional.ofNullable(profilePictureUri);
    }

    /**
     * Returns the tasks the merchant is permitted to perform on the page.
     *
     * @return an unmodifiable view of the permitted-task markers; never
     *         {@code null}, possibly empty
     */
    public List<String> permittedTasks() {
        return Collections.unmodifiableList(permittedTasks);
    }
}
