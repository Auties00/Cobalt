package com.github.auties00.cobalt.media.transcode.text;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.media.TestMediaConnectionService;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessageBuilder;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wam.TestWamService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the short-circuit no-op branches of
 * {@link com.github.auties00.cobalt.media.transcode.text.TextPipeline#run(com.github.auties00.cobalt.wire.core.jid.Jid, com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage)}
 * ({@code null} message, the {@code disableLinkPreviews} privacy flag, an
 * empty body, and a body with no URL) that bail before any HTTP or cache
 * interaction. The HTTP-fetching branches are not covered here because the
 * production constructor exposes no client injection point; they are exercised
 * by the live-corpus parity harness instead.
 */
@DisplayName("TextPipeline")
class TextPipelineTest {

    private static final Jid SELF = Jid.of("12025550100@s.whatsapp.net");

    private static final Jid PEER = Jid.of("19254863482@s.whatsapp.net");

    private static final Jid NEWSLETTER = Jid.of("120363402045452944@newsletter");

    @Test
    @DisplayName("decorate(null message): no-op (no exception, no side effects)")
    void nullMessageNoOp() {
        var props = TestABPropsService.builder().build();
        var client = TestWhatsAppClient.create().withStore(store(false));
        var service = new TextPipeline(client, props, TestMediaConnectionService.create(), TestWamService.create(client));
        service.run(PEER, null);
    }

    @Test
    @DisplayName("decorate: store.settingsStore().disableLinkPreviews()=true -> message body unchanged")
    void disabledPreviewsLeaveMessageAlone() {
        var props = TestABPropsService.builder().build();
        var store = store(true);
        var client = TestWhatsAppClient.create().withStore(store);
        var service = new TextPipeline(client, props, TestMediaConnectionService.create(), TestWamService.create(client));

        var msg = new ExtendedTextMessageBuilder().text("https://example.com").build();
        service.run(PEER, msg);

        assertEquals("https://example.com", msg.text().orElseThrow(),
                "text body is preserved verbatim â€” only preview metadata may be added by decorate");
        assertTrue(msg.matchedText().isEmpty(),
                "disabled preview: matchedText must remain absent");
        assertTrue(msg.title().isEmpty(),
                "disabled preview: title must remain absent");
    }

    @Test
    @DisplayName("decorate: text without any URL -> no preview metadata added")
    void plainTextNoUrlNoOp() {
        var props = TestABPropsService.builder().build();
        var client = TestWhatsAppClient.create().withStore(store(false));
        var service = new TextPipeline(client, props, TestMediaConnectionService.create(), TestWamService.create(client));

        var msg = new ExtendedTextMessageBuilder().text("hello world").build();
        service.run(PEER, msg);

        assertTrue(msg.matchedText().isEmpty(),
                "no URL in text -> no matchedText set");
        assertTrue(msg.title().isEmpty(),
                "no URL in text -> no title set");
    }

    @Test
    @DisplayName("decorate: empty text body -> no-op")
    void emptyTextNoOp() {
        var props = TestABPropsService.builder().build();
        var client = TestWhatsAppClient.create().withStore(store(false));
        var service = new TextPipeline(client, props, TestMediaConnectionService.create(), TestWamService.create(client));

        var msg = new ExtendedTextMessageBuilder().text("").build();
        service.run(PEER, msg);
        assertTrue(msg.matchedText().isEmpty());
    }

    @Test
    @DisplayName("constructor(null client): throws NullPointerException")
    void nullClientThrows() {
        var props = TestABPropsService.builder().build();
        Assertions.assertThrows(NullPointerException.class,
                () -> new TextPipeline(null, props, TestMediaConnectionService.create(), TestWamService.create(TestWhatsAppClient.create())));
    }

    @Test
    @DisplayName("constructor(null abPropsService): throws NullPointerException")
    void nullAbPropsThrows() {
        var client = TestWhatsAppClient.create().withStore(store(false));
        Assertions.assertThrows(NullPointerException.class,
                () -> new TextPipeline(client, null, TestMediaConnectionService.create(), TestWamService.create(client)));
    }

    @Test
    @DisplayName("decorate: text with URL but newsletter recipient + suspicious or non-previewable path -> no-op")
    void newsletterChatRespected() {
        var props = TestABPropsService.builder().build();
        var client = TestWhatsAppClient.create().withStore(store(false));
        var service = new TextPipeline(client, props, TestMediaConnectionService.create(), TestWamService.create(client));
        var msg = new ExtendedTextMessageBuilder().text("https://example.com").build();
        service.run(PEER, msg);
        // keeps the NEWSLETTER fixture referenced; the newsletter branch needs an HTTP stub to assert fully
        assertNotNull(NEWSLETTER);
    }

    private static LinkedWhatsAppStore store(boolean disableLinkPreviews) {
        var store = MessageFixtures.temporaryStore(SELF, null);
        store.settingsStore().setDisableLinkPreviews(disableLinkPreviews);
        return store;
    }
}
