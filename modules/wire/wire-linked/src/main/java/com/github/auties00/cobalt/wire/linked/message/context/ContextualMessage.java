package com.github.auties00.cobalt.wire.linked.message.context;

import com.github.auties00.cobalt.wire.linked.message.bot.AIRichResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.media.MediaMessage;
import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.linked.message.call.CallOfferMessage;
import com.github.auties00.cobalt.wire.linked.message.commerce.*;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactMessage;
import com.github.auties00.cobalt.wire.linked.message.contact.ContactsArrayMessage;
import com.github.auties00.cobalt.wire.linked.message.event.EventMessage;
import com.github.auties00.cobalt.wire.linked.message.group.GroupInviteMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.TemplateButtonReplyMessage;
import com.github.auties00.cobalt.wire.linked.message.interactive.TemplateMessage;
import com.github.auties00.cobalt.wire.linked.message.list.ListMessage;
import com.github.auties00.cobalt.wire.linked.message.list.ListResponseMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LiveLocationMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LocationMessage;
import com.github.auties00.cobalt.wire.linked.message.media.AlbumMessage;
import com.github.auties00.cobalt.wire.linked.message.media.StickerPackMessage;
import com.github.auties00.cobalt.wire.linked.message.newsletter.NewsletterAdminInviteMessage;
import com.github.auties00.cobalt.wire.linked.message.newsletter.NewsletterFollowerInviteMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollCreationMessage;
import com.github.auties00.cobalt.wire.linked.message.poll.PollResultSnapshotMessage;
import com.github.auties00.cobalt.wire.linked.message.system.RequestPhoneNumberMessage;
import com.github.auties00.cobalt.wire.linked.message.system.history.MessageHistoryBundle;
import com.github.auties00.cobalt.wire.linked.message.system.history.MessageHistoryNotice;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;

import java.util.Optional;

/**
 * Represents a {@link Message} that can carry contextual metadata in the form
 * of a {@link ContextInfo} instance.
 *
 * <p>Context information is the extra data that travels alongside a message
 * body and is not part of the message content itself. It is used to model
 * features such as replies to a previous message, mentions of chat or group
 * members, forwarding history, ad attribution (Click-To-WhatsApp), status
 * reshares, ephemeral (disappearing) message settings and a number of
 * business, bot and newsletter related attributes.
 *
 * <p>Only a specific set of message types is allowed to expose context
 * information. These types are declared in the {@code permits} clause of this
 * sealed interface and include, among others, text, media, interactive,
 * template, list, location, contact, commerce, poll and system messages.
 * Messages that are not part of this set (for example protocol or encryption
 * control messages) intentionally do not expose a {@link ContextInfo} and
 * therefore cannot be replied to, forwarded or tagged with mentions.
 *
 * <p>The context information is optional: a contextual message that has never
 * been tagged with any contextual metadata returns an empty {@code Optional}
 * from {@link #contextInfo()}. When present, callers typically inspect it to
 * determine whether the message is a reply, whether it contains mentions, or
 * whether it was originated from an ad or a forwarded status.
 */
public sealed interface ContextualMessage extends Message permits
        MediaMessage,
    AIRichResponseMessage,
    AlbumMessage,
    ButtonsMessage,
    ButtonsResponseMessage,
    CallOfferMessage,
    ContactMessage,
    ContactsArrayMessage,
    EventMessage,
    ExtendedTextMessage,
    GroupInviteMessage,
    InteractiveMessage,
    InteractiveResponseMessage,
    ListMessage,
    ListResponseMessage,
    LiveLocationMessage,
    LocationMessage,
    MessageHistoryBundle,
    MessageHistoryNotice,
    NewsletterAdminInviteMessage,
    NewsletterFollowerInviteMessage,
    OrderMessage,
    PollCreationMessage,
    PollResultSnapshotMessage,
    ProductMessage,
    RequestPhoneNumberMessage,
    StickerPackMessage,
    TemplateButtonReplyMessage,
    TemplateMessage {

    /**
     * Returns the context information currently attached to this message.
     *
     * <p>The returned {@link ContextInfo}, when present, exposes data such as
     * the quoted message, the list of mentioned JIDs, forwarding flags, ad
     * attribution data, ephemeral settings and many other contextual
     * attributes. An empty result means that no contextual metadata has been
     * set on this message.
     *
     * @return an {@code Optional} containing the {@link ContextInfo} if any
     *         has been attached, or {@code Optional.empty()} otherwise
     */
    Optional<ContextInfo> contextInfo();

    /**
     * Attaches the given context information to this message, replacing any
     * previously set value.
     *
     * <p>Passing {@code null} clears the context information and makes
     * subsequent calls to {@link #contextInfo()} return an empty
     * {@code Optional}.
     *
     * @param contextInfo the {@link ContextInfo} to attach, or {@code null}
     *                    to clear it
     */
    void setContextInfo(ContextInfo contextInfo);
}
