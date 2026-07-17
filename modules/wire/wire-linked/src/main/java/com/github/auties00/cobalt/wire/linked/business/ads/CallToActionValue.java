package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Destination payload of a Click-to-WhatsApp ad's call-to-action button.
 *
 * <p>Depending on the button type, tapping the call-to-action opens a link, a WhatsApp chat, a
 * Messenger thread, an app, an event, or a lead form. This model carries the union of destination
 * fields WhatsApp Web populates: the {@link #link() link}, the {@link #appLink() app link} and
 * {@link #appDestination() app destination}, the {@link #eventId() event}, {@link #groupId() group} and
 * {@link #leadGenFormId() lead-form} identifiers, the {@link #page() page}, and the
 * {@link #whatsappNumber() WhatsApp number}. Every field is optional; only the ones relevant to the
 * button type are set.
 */
@ProtobufMessage(name = "CallToActionValue")
public final class CallToActionValue {
    /**
     * Destination link the button opens. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String link;

    /**
     * Deep link into an app the button opens. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String appLink;

    /**
     * App-destination token selecting the messaging app the button opens (for example
     * {@code "WHATSAPP"}, {@code "MESSENGER"}). Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String appDestination;

    /**
     * Identifier of the event the button links to. Empty when unset.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String eventId;

    /**
     * Identifier of the group the button links to. Empty when unset.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String groupId;

    /**
     * Identifier of the lead-generation form the button opens. Empty when unset.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String leadGenFormId;

    /**
     * Identifier of the page the button links to. Empty when unset.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String page;

    /**
     * WhatsApp phone number the button opens a chat with. Empty when unset.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String whatsappNumber;

    /**
     * Constructs a new {@code CallToActionValue}. Every argument may be {@code null} to leave the
     * corresponding field unset.
     *
     * @param link           the destination link, or {@code null}
     * @param appLink        the app deep link, or {@code null}
     * @param appDestination the app-destination token, or {@code null}
     * @param eventId        the event identifier, or {@code null}
     * @param groupId        the group identifier, or {@code null}
     * @param leadGenFormId  the lead-form identifier, or {@code null}
     * @param page           the page identifier, or {@code null}
     * @param whatsappNumber the WhatsApp number, or {@code null}
     */
    CallToActionValue(String link, String appLink, String appDestination, String eventId, String groupId,
                      String leadGenFormId, String page, String whatsappNumber) {
        this.link = link;
        this.appLink = appLink;
        this.appDestination = appDestination;
        this.eventId = eventId;
        this.groupId = groupId;
        this.leadGenFormId = leadGenFormId;
        this.page = page;
        this.whatsappNumber = whatsappNumber;
    }

    /**
     * Returns the destination link the button opens.
     *
     * @return an {@link Optional} carrying the link, or empty when unset
     */
    public Optional<String> link() {
        return Optional.ofNullable(link);
    }

    /**
     * Returns the deep link into an app the button opens.
     *
     * @return an {@link Optional} carrying the app link, or empty when unset
     */
    public Optional<String> appLink() {
        return Optional.ofNullable(appLink);
    }

    /**
     * Returns the app-destination token selecting the messaging app the button opens.
     *
     * @return an {@link Optional} carrying the app destination, or empty when unset
     */
    public Optional<String> appDestination() {
        return Optional.ofNullable(appDestination);
    }

    /**
     * Returns the identifier of the event the button links to.
     *
     * @return an {@link Optional} carrying the event identifier, or empty when unset
     */
    public Optional<String> eventId() {
        return Optional.ofNullable(eventId);
    }

    /**
     * Returns the identifier of the group the button links to.
     *
     * @return an {@link Optional} carrying the group identifier, or empty when unset
     */
    public Optional<String> groupId() {
        return Optional.ofNullable(groupId);
    }

    /**
     * Returns the identifier of the lead-generation form the button opens.
     *
     * @return an {@link Optional} carrying the lead-form identifier, or empty when unset
     */
    public Optional<String> leadGenFormId() {
        return Optional.ofNullable(leadGenFormId);
    }

    /**
     * Returns the identifier of the page the button links to.
     *
     * @return an {@link Optional} carrying the page identifier, or empty when unset
     */
    public Optional<String> page() {
        return Optional.ofNullable(page);
    }

    /**
     * Returns the WhatsApp phone number the button opens a chat with.
     *
     * @return an {@link Optional} carrying the WhatsApp number, or empty when unset
     */
    public Optional<String> whatsappNumber() {
        return Optional.ofNullable(whatsappNumber);
    }
}
