package com.github.auties00.cobalt.message.send.icdc;

import com.github.auties00.cobalt.wire.linked.device.identity.ADVEncryptionType;
import com.github.auties00.cobalt.device.icdc.TestIcdcResults;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfoBuilder;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Branch-coverage suite for {@link IcdcEnricher#enrich}, exercising each input
 * shape: both ICDC inputs {@code null} (no-op, original container returned),
 * sender-only, recipient-only, both populated, all-empty ICDC values, and a
 * merge over a pre-existing
 * {@link com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfo} carrying
 * a {@code messageSecret}. {@link TestIcdcResults} fixture helpers mint
 * {@link com.github.auties00.cobalt.device.icdc.IcdcResult} instances directly,
 * isolating the enricher from {@link com.github.auties00.cobalt.device.DeviceService}
 * device-list bookkeeping.
 */
@DisplayName("IcdcEnricher")
class IcdcEnricherTest {

    private static final byte[] SENDER_KEY_HASH = new byte[]{1, 2, 3};
    private static final byte[] RECIPIENT_KEY_HASH = new byte[]{4, 5, 6};
    private static final Instant SENDER_TS = Instant.ofEpochSecond(1700000000L);
    private static final Instant RECIPIENT_TS = Instant.ofEpochSecond(1700000100L);

    @Test
    @DisplayName("both ICDC null: container is returned unchanged (no-op)")
    void bothNullReturnsSameContainer() {
        var original = LinkedMessageContainer.of("hello");
        var result = IcdcEnricher.enrich(original, null, null);
        assertSame(original, result,
                "no ICDC inputs must return the container by reference");
    }

    @Test
    @DisplayName("sender-only ICDC: deviceListMetadata.sender* fields populated")
    void senderOnly() {
        var senderIcdc = TestIcdcResults.create(
                SENDER_KEY_HASH, SENDER_TS, List.of(0, 73, 79), ADVEncryptionType.E2EE);
        var enriched = IcdcEnricher.enrich(LinkedMessageContainer.of("hi"), senderIcdc, null);

        var ctx = enriched.messageContextInfo().orElseThrow();
        var meta = ctx.deviceListMetadata().orElseThrow();

        assertArrayEquals(SENDER_KEY_HASH, meta.senderKeyHash().orElseThrow());
        assertEquals(SENDER_TS, meta.senderTimestamp().orElseThrow());
        assertEquals(List.of(0, 73, 79), meta.senderKeyIndexes());
        assertEquals(ADVEncryptionType.E2EE, meta.senderAccountType().orElseThrow());
        assertEquals(2, ctx.deviceListMetadataVersion().orElseThrow(),
                "version=2 must be stamped on every enrich call");

        assertTrue(meta.recipientKeyHash().isEmpty());
        assertTrue(meta.recipientTimestamp().isEmpty());
        assertTrue(meta.recipientKeyIndexes().isEmpty());
    }

    @Test
    @DisplayName("recipient-only ICDC: deviceListMetadata.recipient* fields populated")
    void recipientOnly() {
        var recipientIcdc = TestIcdcResults.create(
                RECIPIENT_KEY_HASH, RECIPIENT_TS, List.of(0), ADVEncryptionType.E2EE);
        var enriched = IcdcEnricher.enrich(LinkedMessageContainer.of("hi"), null, recipientIcdc);

        var meta = enriched.messageContextInfo().orElseThrow().deviceListMetadata().orElseThrow();

        assertArrayEquals(RECIPIENT_KEY_HASH, meta.recipientKeyHash().orElseThrow());
        assertEquals(RECIPIENT_TS, meta.recipientTimestamp().orElseThrow());
        assertEquals(List.of(0), meta.recipientKeyIndexes());
        assertEquals(ADVEncryptionType.E2EE, meta.receiverAccountType().orElseThrow());

        assertTrue(meta.senderKeyHash().isEmpty());
    }

    @Test
    @DisplayName("both sender and recipient ICDC populate the corresponding fields independently")
    void bothPopulated() {
        var senderIcdc = TestIcdcResults.create(SENDER_KEY_HASH, SENDER_TS, List.of(0, 73), null);
        var recipientIcdc = TestIcdcResults.create(RECIPIENT_KEY_HASH, RECIPIENT_TS, List.of(0), null);
        var enriched = IcdcEnricher.enrich(LinkedMessageContainer.of("hi"), senderIcdc, recipientIcdc);

        var meta = enriched.messageContextInfo().orElseThrow().deviceListMetadata().orElseThrow();
        assertArrayEquals(SENDER_KEY_HASH, meta.senderKeyHash().orElseThrow());
        assertArrayEquals(RECIPIENT_KEY_HASH, meta.recipientKeyHash().orElseThrow());
        assertEquals(List.of(0, 73), meta.senderKeyIndexes());
        assertEquals(List.of(0), meta.recipientKeyIndexes());
    }

    // Empty ICDC inputs (every field null) take the non-short-circuit path, unlike a null input:
    // the deviceListMetadataVersion=2 stamp still appears while the field-detail Optionals stay empty.
    @Test
    @DisplayName("empty ICDC (all fields null): meta is built but Optional fields stay empty")
    void emptyIcdcStillPopulatesMeta() {
        var enriched = IcdcEnricher.enrich(LinkedMessageContainer.of("hi"),
                TestIcdcResults.empty(), TestIcdcResults.empty());

        var ctx = enriched.messageContextInfo().orElseThrow();
        var meta = ctx.deviceListMetadata().orElseThrow();
        assertTrue(meta.senderKeyHash().isEmpty());
        assertTrue(meta.recipientKeyHash().isEmpty());
        assertEquals(2, ctx.deviceListMetadataVersion().orElseThrow());
    }

    @Test
    @DisplayName("existing messageContextInfo: messageSecret and other fields are preserved across enrich")
    void existingContextInfoPreserved() {
        var secret = new byte[32];
        var existing = new ChatMessageContextInfoBuilder()
                .messageSecret(secret)
                .build();
        var container = LinkedMessageContainer.of("hi").withMessageContextInfo(existing);

        var enriched = IcdcEnricher.enrich(container,
                TestIcdcResults.create(SENDER_KEY_HASH, SENDER_TS, List.of(0), null),
                null);

        var ctx = enriched.messageContextInfo().orElseThrow();
        assertArrayEquals(secret, ctx.messageSecret().orElseThrow(),
                "messageSecret must be preserved through the ICDC merge");
        assertEquals(2, ctx.deviceListMetadataVersion().orElseThrow());
        var meta = ctx.deviceListMetadata().orElseThrow();
        assertArrayEquals(SENDER_KEY_HASH, meta.senderKeyHash().orElseThrow());
    }

    @Test
    @DisplayName("enriched container content() still returns the original payload (side-channel-only change)")
    void contentUnchanged() {
        var container = LinkedMessageContainer.of("preserved");
        var enriched = IcdcEnricher.enrich(container,
                TestIcdcResults.create(SENDER_KEY_HASH, SENDER_TS, List.of(0), null),
                null);
        var originalContent = container.content();
        var enrichedContent = enriched.content();
        assertEquals(originalContent.getClass(), enrichedContent.getClass(),
                "enrich must not change the payload type");
    }
}
