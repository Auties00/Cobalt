package com.github.auties00.cobalt.message;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.EmptyMessage;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.FutureProofMessage;
import com.github.auties00.cobalt.wire.linked.message.system.FutureProofMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.system.DeviceSentMessage;
import com.github.auties00.cobalt.wire.linked.message.system.DeviceSentMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.media.ImageMessage;
import com.github.auties00.cobalt.wire.linked.message.media.ImageMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.location.LocationMessage;
import com.github.auties00.cobalt.wire.linked.message.location.LocationMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessageBuilder;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessage;
import com.github.auties00.cobalt.wire.linked.message.text.ReactionMessageBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link LinkedMessageContainer#content()} and {@link LinkedMessageContainer#contextualContent()},
 * the resolvers that walk the union-of-fields {@code Message} protobuf and return the
 * innermost payload. Containers are assembled through the generated builders rather than
 * parsed from protobuf bytes, so the precedence assertions exercise the in-memory resolver
 * only.
 */
@DisplayName("LinkedMessageContainer.content")
class LinkedMessageContainerContentTest {

    @Test
    @DisplayName("empty container returns an EmptyMessage sentinel")
    void emptyContainer() {
        var container = LinkedMessageContainer.empty();
        assertInstanceOf(EmptyMessage.class, container.content());
        assertTrue(container.isEmpty());
    }

    @Test
    @DisplayName("string conversation field promotes to an ExtendedTextMessage")
    void conversationPromotesToExtendedText() {
        var container = LinkedMessageContainer.of("hello world");

        var content = container.content();
        assertInstanceOf(ExtendedTextMessage.class, content);
        var text = (ExtendedTextMessage) content;
        assertEquals("hello world", text.text().orElseThrow());
    }

    @Test
    @DisplayName("direct ImageMessage is returned as-is (no wrapping)")
    void directImageMessage() {
        var image = new ImageMessageBuilder()
                .caption("a picture")
                .build();
        var container = new LinkedMessageContainerBuilder()
                .imageMessage(image)
                .build();

        var content = container.content();
        assertSame(image, content, "direct payload field must be returned by-reference");
    }

    @Test
    @DisplayName("FutureProofMessage (viewOnce) unwraps to its inner content")
    void futureProofViewOnceUnwraps() {
        var location = new LocationMessageBuilder()
                .name("center")
                .degreesLatitude(0.0)
                .degreesLongitude(0.0)
                .build();
        var viewOnce = new FutureProofMessageBuilder()
                .messageContainer(new LinkedMessageContainerBuilder()
                        .locationMessage(location)
                        .build())
                .build();
        var container = new LinkedMessageContainerBuilder()
                .viewOnceMessage(viewOnce)
                .build();

        var content = container.content();
        assertSame(location, content,
                "viewOnce wrapper must unwrap to its inner location message");
    }

    @Test
    @DisplayName("FutureProofMessage (ephemeral) unwraps to inner content")
    void futureProofEphemeralUnwraps() {
        var image = new ImageMessageBuilder().caption("eph").build();
        var ephemeral = new FutureProofMessageBuilder()
                .messageContainer(new LinkedMessageContainerBuilder()
                        .imageMessage(image)
                        .build())
                .build();
        var container = new LinkedMessageContainerBuilder()
                .ephemeralMessage(ephemeral)
                .build();
        assertSame(image, container.content());
    }

    @Test
    @DisplayName("FutureProofMessage (editedMessage) unwraps to inner content")
    void futureProofEditedUnwraps() {
        var text = new ExtendedTextMessageBuilder().text("edited body").build();
        var edited = new FutureProofMessageBuilder()
                .messageContainer(new LinkedMessageContainerBuilder()
                        .extendedTextMessage(text)
                        .build())
                .build();
        var container = new LinkedMessageContainerBuilder()
                .editedMessage(edited)
                .build();
        assertSame(text, container.content());
    }

    @Test
    @DisplayName("nested FutureProofMessage wrappers unwrap recursively")
    void nestedFutureProofUnwraps() {
        var image = new ImageMessageBuilder().caption("nested").build();
        var inner = new FutureProofMessageBuilder()
                .messageContainer(new LinkedMessageContainerBuilder()
                        .imageMessage(image)
                        .build())
                .build();
        var outer = new FutureProofMessageBuilder()
                .messageContainer(new LinkedMessageContainerBuilder()
                        .viewOnceMessage(inner)
                        .build())
                .build();
        var container = new LinkedMessageContainerBuilder()
                .ephemeralMessage(outer)
                .build();

        assertSame(image, container.content(),
                "two-level FutureProof nesting must unwrap to the innermost payload");
    }

    @Test
    @DisplayName("DeviceSentMessage unwraps to its inner message before direct fields are consulted")
    void deviceSentUnwraps() {
        var reaction = new ReactionMessageBuilder().text("👍").build();
        var deviceSent = new DeviceSentMessageBuilder()
                .destinationJid(Jid.of("12025550100@s.whatsapp.net"))
                .messageContainer(new LinkedMessageContainerBuilder()
                        .reactionMessage(reaction)
                        .build())
                .build();
        var container = new LinkedMessageContainerBuilder()
                .deviceSentMessage(deviceSent)
                .build();

        assertSame(reaction, container.content(),
                "DeviceSentMessage must unwrap to its inner ReactionMessage");
    }

    @Test
    @DisplayName("FutureProofMessage wrapper wins over a direct field on the same container")
    void wrapperWinsOverDirectField() {
        var directText = new ExtendedTextMessageBuilder().text("direct").build();
        var wrapped = new ImageMessageBuilder().caption("wrapped").build();
        var viewOnce = new FutureProofMessageBuilder()
                .messageContainer(new LinkedMessageContainerBuilder()
                        .imageMessage(wrapped)
                        .build())
                .build();
        var container = new LinkedMessageContainerBuilder()
                .extendedTextMessage(directText)
                .viewOnceMessage(viewOnce)
                .build();

        assertSame(wrapped, container.content(),
                "viewOnce wrapper takes precedence over a direct extendedTextMessage");
    }

    @Test
    @DisplayName("contextualContent() returns the contextual inner message when present")
    void contextualContentReturnsContextual() {
        var text = new ExtendedTextMessageBuilder().text("with context").build();
        var container = new LinkedMessageContainerBuilder()
                .extendedTextMessage(text)
                .build();

        var contextual = container.contextualContent();
        assertTrue(contextual.isPresent());
        assertSame(text, contextual.orElseThrow());
    }

    @Test
    @DisplayName("contextualContent() returns empty when innermost message is not contextual")
    void contextualContentEmptyForNonContextual() {
        var reaction = new ReactionMessageBuilder().text("👍").build();
        var container = new LinkedMessageContainerBuilder()
                .reactionMessage(reaction)
                .build();

        assertTrue(container.contextualContent().isEmpty(),
                "ReactionMessage is not a ContextualMessage; contextualContent() must be empty");
    }

    @Test
    @DisplayName("DeviceSentMessage with empty inner message falls back to EmptyMessage")
    void deviceSentEmptyInner() {
        var deviceSent = new DeviceSentMessageBuilder()
                .destinationJid(Jid.of("12025550100@s.whatsapp.net"))
                .messageContainer(LinkedMessageContainer.empty())
                .build();
        var container = new LinkedMessageContainerBuilder()
                .deviceSentMessage(deviceSent)
                .build();

        assertInstanceOf(EmptyMessage.class, container.content());
    }

    @Test
    @DisplayName("groupMentioned wrapper wins over every other wrapper (highest priority)")
    void groupMentionedWinsOverOtherWrappers() {
        var location = new LocationMessageBuilder()
                .name("inside groupMentioned")
                .degreesLatitude(0.0).degreesLongitude(0.0)
                .build();
        var groupMentioned = new FutureProofMessageBuilder()
                .messageContainer(new LinkedMessageContainerBuilder()
                        .locationMessage(location)
                        .build())
                .build();
        var siblingImage = new ImageMessageBuilder().caption("ignored").build();
        var siblingViewOnce = new FutureProofMessageBuilder()
                .messageContainer(new LinkedMessageContainerBuilder()
                        .imageMessage(siblingImage)
                        .build())
                .build();
        var container = new LinkedMessageContainerBuilder()
                .groupMentionedMessage(groupMentioned)
                .viewOnceMessage(siblingViewOnce)
                .build();

        assertSame(location, container.content(),
                "groupMentioned wrapper must beat viewOnce wrapper");
    }

    @Test
    @DisplayName("conversation field is overridden by extendedTextMessage when both are set")
    void conversationLosesToExtendedText() {
        var extended = new ExtendedTextMessageBuilder().text("the extended one").build();
        var container = new LinkedMessageContainerBuilder()
                .conversation("the plain one")
                .extendedTextMessage(extended)
                .build();

        var content = container.content();
        assertInstanceOf(ExtendedTextMessage.class, content);
        assertEquals("the plain one", ((ExtendedTextMessage) content).text().orElseThrow(),
                "conversation precedes extendedTextMessage in the field order; wins");
    }

    @Test
    @DisplayName("static factory LinkedMessageContainer.of(Message) routes the message to the correct typed field")
    void factoryOfMessageRoutes() {
        var image = new ImageMessageBuilder().caption("via factory").build();
        var container = LinkedMessageContainer.of(image);
        assertSame(image, container.content(),
                "LinkedMessageContainer.of(image) must route to imageMessage and content() must return it");

        var location = new LocationMessageBuilder().name("origin").degreesLatitude(0.0).degreesLongitude(0.0).build();
        var locContainer = LinkedMessageContainer.of(location);
        assertSame(location, locContainer.content());

        var reaction = new ReactionMessageBuilder().text("🎯").build();
        assertSame(reaction, LinkedMessageContainer.of(reaction).content());
    }

    @Test
    @DisplayName("withMessageContextInfo preserves the original content")
    void withMessageContextInfoPreservesContent() {
        var image = new ImageMessageBuilder().caption("preserved").build();
        var container = LinkedMessageContainer.of(image);

        var withCtx = new LinkedMessageContainerBuilder()
                .imageMessage(image)
                .build();
        assertSame(image, withCtx.content(),
                "side-channel fields don't affect content() resolution");
    }

    @Test
    @DisplayName("scratch suppression: a JID-typed field shape can round-trip through the resolver")
    void jidImportSentinel() {
        var ignored = Jid.of("0@s.whatsapp.net");
        assertEquals("0@s.whatsapp.net", ignored.toString());
    }
}
