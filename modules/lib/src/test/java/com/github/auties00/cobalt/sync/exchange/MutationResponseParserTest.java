package com.github.auties00.cobalt.sync.exchange;

import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.wire.linked.media.ExternalBlobReferenceBuilder;
import com.github.auties00.cobalt.wire.linked.media.ExternalBlobReferenceSpec;
import com.github.auties00.cobalt.wire.linked.signal.KeyIdBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdPatchBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdPatchSpec;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdVersionBuilder;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins each observable branch of {@link MutationResponseParser}: the happy paths
 * (snapshot-only, patches-only, version, has_more), collection-level error routing
 * (409 Conflict, 400/404 fatal, anything else retryable), IQ-level error routing
 * (400/404/405/406 fatal, anything else retryable with the {@code backoff} attribute
 * preserved), malformed non-protobuf {@code <patch>}/{@code <snapshot>} content,
 * structural failures (missing {@code <sync>}/{@code <collection>}, unknown collection
 * name), and the multi-collection batched parse.
 *
 * <p>The parser is stateless, so a single {@link #PARSER} instance is shared; every test
 * builds its input {@link Stanza} via the helpers at the bottom of the class.
 */
@DisplayName("MutationResponseParser")
class MutationResponseParserTest {

    private static final MutationResponseParser PARSER = new MutationResponseParser();

    @Nested
    @DisplayName("happy path - patches and snapshot responses")
    class HappyPath {
        @Test
        @DisplayName("collection with patches yields the right type/version/has_more and patch list")
        void patchesOnly() {
            var patchBytes = SyncdPatchSpec.encode(new SyncdPatchBuilder()
                    .version(new SyncdVersionBuilder().version(42L).build())
                    .keyId(new KeyIdBuilder().id(new byte[]{1, 2, 3}).build())
                    .build());

            var iq = iq("result", collection("regular", attrs -> attrs
                    .attribute("version", "42")
                    .attribute("has_more_patches", "true"),
                    new StanzaBuilder().description("patches").content(List.of(
                            new StanzaBuilder().description("patch").content(patchBytes).build()
                    )).build()
            ));

            var response = PARSER.parseSyncResponse(iq);
            assertEquals(SyncPatchType.REGULAR, response.collectionName());
            assertEquals(42L, response.version());
            assertTrue(response.hasMore(), "has_more_patches='true' must map to hasMore=true");
            assertEquals(1, response.patches().size());
            assertFalse(response.isSnapshot());
            assertTrue(response.snapshotReference().isEmpty());
            assertTrue(response.collectionError().isEmpty());
        }

        @Test
        @DisplayName("collection with snapshot yields the parsed external blob reference")
        void snapshotOnly() {
            var blob = new ExternalBlobReferenceBuilder()
                    .mediaKey(new byte[]{(byte) 0xAA})
                    .mediaDirectPath("/path")
                    .build();
            var blobBytes = ExternalBlobReferenceSpec.encode(blob);

            var iq = iq("result", collection("regular_low", attrs -> attrs
                    .attribute("version", "1"),
                    new StanzaBuilder().description("snapshot").content(blobBytes).build()
            ));

            var response = PARSER.parseSyncResponse(iq);
            assertEquals(SyncPatchType.REGULAR_LOW, response.collectionName());
            assertEquals(1L, response.version());
            assertFalse(response.hasMore());
            assertTrue(response.isSnapshot());
            assertNotNull(response.snapshotReference().orElseThrow());
            assertTrue(response.patches().isEmpty());
        }

        @Test
        @DisplayName("empty collection (no snapshot, no patches) is a valid no-op response")
        void emptyCollection() {
            var iq = iq("result", collection("regular_high", attrs -> attrs.attribute("version", "5")));
            var response = PARSER.parseSyncResponse(iq);
            assertEquals(SyncPatchType.REGULAR_HIGH, response.collectionName());
            assertEquals(5L, response.version());
            assertTrue(response.patches().isEmpty());
            assertFalse(response.isSnapshot());
        }

        @Test
        @DisplayName("missing version attribute defaults to 0")
        void missingVersionDefaultsToZero() {
            var iq = iq("result", collection("regular", attrs -> {}));
            assertEquals(0L, PARSER.parseSyncResponse(iq).version());
        }
    }

    @Nested
    @DisplayName("collection-level errors - codes route to specific exception subtypes")
    class CollectionErrors {
        @Test
        @DisplayName("409 surfaces Conflict on collectionError instead of throwing (single-collection mode)")
        void code409Conflict() {
            var iq = iq("result", collection("regular", attrs -> attrs.attribute("type", "error"),
                    new StanzaBuilder().description("error").attribute("code", "409").build()
            ));
            var response = PARSER.parseSyncResponse(iq);
            assertInstanceOf(WhatsAppWebAppStateSyncException.Conflict.class,
                    response.collectionError().orElseThrow());
        }

        @Test
        @DisplayName("409 with has_more_patches sets Conflict.hasMore=true")
        void code409HasMoreSet() {
            var iq = iq("result", collection("regular", attrs -> attrs
                    .attribute("type", "error")
                    .attribute("has_more_patches", "true"),
                    new StanzaBuilder().description("error").attribute("code", "409").build()
            ));
            var conflict = assertInstanceOf(WhatsAppWebAppStateSyncException.Conflict.class,
                    PARSER.parseSyncResponse(iq).collectionError().orElseThrow());
            assertTrue(conflict.hasMorePatches(), "Conflict.hasMore must reflect collection-level has_more_patches");
        }

        @Test
        @DisplayName("409 surfaces the catch-up patches so the caller can apply them and retry")
        void code409CarriesCatchUpPatches() {
            var patchBytes = SyncdPatchSpec.encode(new SyncdPatchBuilder()
                    .version(new SyncdVersionBuilder().version(1L).build())
                    .keyId(new KeyIdBuilder().id(new byte[]{1, 2, 3}).build())
                    .build());
            var iq = iq("result", collection("regular", attrs -> attrs.attribute("type", "error"),
                    new StanzaBuilder().description("error").attribute("code", "409").build(),
                    new StanzaBuilder().description("patches").content(List.of(
                            new StanzaBuilder().description("patch").content(patchBytes).build()
                    )).build()
            ));
            var response = PARSER.parseSyncResponse(iq);
            assertInstanceOf(WhatsAppWebAppStateSyncException.Conflict.class,
                    response.collectionError().orElseThrow());
            assertEquals(1, response.patches().size(), "409 must surface the catch-up patches, not discard them");
        }

        @Test
        @DisplayName("400 throws UnexpectedError (fatal)")
        void code400Throws() {
            var iq = iq("result", collection("regular", attrs -> attrs.attribute("type", "error"),
                    new StanzaBuilder().description("error").attribute("code", "400").build()
            ));
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> PARSER.parseSyncResponse(iq));
        }

        @Test
        @DisplayName("404 throws UnexpectedError (fatal)")
        void code404Throws() {
            var iq = iq("result", collection("regular", attrs -> attrs.attribute("type", "error"),
                    new StanzaBuilder().description("error").attribute("code", "404").build()
            ));
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> PARSER.parseSyncResponse(iq));
        }

        @Test
        @DisplayName("unmapped collection-level code throws RetryableServerError")
        void otherCodeIsRetryable() {
            var iq = iq("result", collection("regular", attrs -> attrs.attribute("type", "error"),
                    new StanzaBuilder().description("error").attribute("code", "503").build()
            ));
            assertThrows(WhatsAppWebAppStateSyncException.RetryableServerError.class,
                    () -> PARSER.parseSyncResponse(iq));
        }
    }

    @Nested
    @DisplayName("IQ-level errors - fatal vs retryable")
    class IqLevelErrors {
        @Test
        @DisplayName("type=error code=400 is fatal (UnexpectedError)")
        void iq400IsFatal() {
            var iq = errorIq(400, null);
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> PARSER.parseSyncResponse(iq));
        }

        @Test
        @DisplayName("type=error code=404 is fatal")
        void iq404IsFatal() {
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> PARSER.parseSyncResponse(errorIq(404, null)));
        }

        @Test
        @DisplayName("type=error code=405 is fatal")
        void iq405IsFatal() {
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> PARSER.parseSyncResponse(errorIq(405, null)));
        }

        @Test
        @DisplayName("type=error code=406 is fatal")
        void iq406IsFatal() {
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> PARSER.parseSyncResponse(errorIq(406, null)));
        }

        @Test
        @DisplayName("type=error code=503 is retryable (RetryableServerError)")
        void iq503IsRetryable() {
            assertThrows(WhatsAppWebAppStateSyncException.RetryableServerError.class,
                    () -> PARSER.parseSyncResponse(errorIq(503, null)));
        }

        @Test
        @DisplayName("type=error with backoff attribute is preserved in RetryableServerError")
        void retryableServerCarriesBackoff() {
            var ex = assertThrows(WhatsAppWebAppStateSyncException.RetryableServerError.class,
                    () -> PARSER.parseSyncResponse(errorIq(429, 1500L)));
            assertNotNull(ex);
        }
    }

    @Nested
    @DisplayName("structural failures - missing required nodes / unknown collection")
    class StructuralFailures {
        @Test
        @DisplayName("response missing <sync> throws UnexpectedError")
        void missingSync() {
            var iq = new StanzaBuilder().description("iq").attribute("type", "result").build();
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> PARSER.parseSyncResponse(iq));
        }

        @Test
        @DisplayName("response missing <collection> throws UnexpectedError")
        void missingCollection() {
            var iq = new StanzaBuilder().description("iq").attribute("type", "result")
                    .content(new StanzaBuilder().description("sync").build()).build();
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> PARSER.parseSyncResponse(iq));
        }

        @Test
        @DisplayName("collection missing 'name' attribute throws UnexpectedError")
        void missingCollectionName() {
            var iq = iq("result", new StanzaBuilder().description("collection")
                    .attribute("version", "1").build());
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> PARSER.parseSyncResponse(iq));
        }

        @Test
        @DisplayName("unknown collection name throws UnexpectedError")
        void unknownCollectionName() {
            var iq = iq("result", collection("bogus_collection", attrs -> attrs.attribute("version", "1")));
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> PARSER.parseSyncResponse(iq));
        }
    }

    @Nested
    @DisplayName("malformed bytes - non-protobuf <patch>/<snapshot> content")
    class MalformedBytes {
        @Test
        @DisplayName("non-protobuf patch bytes throw UnexpectedError")
        void malformedPatch() {
            var iq = iq("result", collection("regular", attrs -> {},
                    new StanzaBuilder().description("patches").content(List.of(
                            new StanzaBuilder().description("patch")
                                    .content(new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF})
                                    .build()
                    )).build()
            ));
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> PARSER.parseSyncResponse(iq));
        }

        @Test
        @DisplayName("patch stanza with no content throws UnexpectedError")
        void patchWithoutContent() {
            var iq = iq("result", collection("regular", attrs -> {},
                    new StanzaBuilder().description("patches").content(List.of(
                            new StanzaBuilder().description("patch").build()
                    )).build()
            ));
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> PARSER.parseSyncResponse(iq));
        }

        @Test
        @DisplayName("snapshot stanza with no content throws UnexpectedError")
        void snapshotWithoutContent() {
            var iq = iq("result", collection("regular", attrs -> {},
                    new StanzaBuilder().description("snapshot").build()
            ));
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> PARSER.parseSyncResponse(iq));
        }
    }

    @Nested
    @DisplayName("parseBatchedSyncResponse - multiple collections, per-collection errors captured")
    class BatchedResponse {
        @Test
        @DisplayName("multiple collections produce one MutationSyncResponse per child")
        void multipleCollections() {
            var iq = new StanzaBuilder().description("iq").attribute("type", "result")
                    .content(new StanzaBuilder().description("sync").content(List.of(
                            collection("regular",    attrs -> attrs.attribute("version", "1")),
                            collection("regular_low", attrs -> attrs.attribute("version", "2")),
                            collection("critical_block", attrs -> attrs.attribute("version", "3"))
                    )).build())
                    .build();

            var responses = PARSER.parseBatchedSyncResponse(iq);
            assertEquals(3, responses.size());
            assertEquals(SyncPatchType.REGULAR,        responses.get(0).collectionName());
            assertEquals(SyncPatchType.REGULAR_LOW,    responses.get(1).collectionName());
            assertEquals(SyncPatchType.CRITICAL_BLOCK, responses.get(2).collectionName());
            assertEquals(1L, responses.get(0).version());
            assertEquals(2L, responses.get(1).version());
            assertEquals(3L, responses.get(2).version());
        }

        @Test
        @DisplayName("collection-level errors are captured on the response, not thrown")
        void errorCapturedNotThrown() {
            var iq = new StanzaBuilder().description("iq").attribute("type", "result")
                    .content(new StanzaBuilder().description("sync").content(List.of(
                            collection("regular",    attrs -> attrs.attribute("version", "1")),
                            collection("regular_low", attrs -> attrs.attribute("type", "error"),
                                    new StanzaBuilder().description("error").attribute("code", "409").build())
                    )).build())
                    .build();

            var responses = PARSER.parseBatchedSyncResponse(iq);
            assertEquals(2, responses.size());
            assertTrue(responses.get(0).collectionError().isEmpty(),
                    "successful collection has no captured error");
            var error = responses.get(1).collectionError().orElseThrow();
            assertInstanceOf(WhatsAppWebAppStateSyncException.Conflict.class, error,
                    "409 must be captured as Conflict on the failing collection");
        }

        @Test
        @DisplayName("empty <sync> returns an empty list")
        void emptySync() {
            var iq = new StanzaBuilder().description("iq").attribute("type", "result")
                    .content(new StanzaBuilder().description("sync").build())
                    .build();
            assertTrue(PARSER.parseBatchedSyncResponse(iq).isEmpty());
        }

        @Test
        @DisplayName("IQ-level error short-circuits the batched parse")
        void iqLevelErrorShortCircuits() {
            assertThrows(WhatsAppWebAppStateSyncException.UnexpectedError.class,
                    () -> PARSER.parseBatchedSyncResponse(errorIq(400, null)));
        }
    }

    private static Stanza iq(String iqType, Stanza collectionStanza) {
        return new StanzaBuilder().description("iq").attribute("type", iqType)
                .content(new StanzaBuilder().description("sync")
                        .content(collectionStanza).build())
                .build();
    }

    private static Stanza errorIq(int code, Long backoffMs) {
        var error = new StanzaBuilder().description("error").attribute("code", String.valueOf(code));
        if (backoffMs != null) error.attribute("backoff", String.valueOf(backoffMs));
        return new StanzaBuilder().description("iq").attribute("type", "error")
                .content(error.build()).build();
    }

    @FunctionalInterface
    private interface AttrConfig {
        void apply(StanzaBuilder builder);
    }

    private static Stanza collection(String name, AttrConfig config, Stanza... children) {
        var builder = new StanzaBuilder().description("collection").attribute("name", name);
        config.apply(builder);
        if (children.length > 0) {
            builder.content(List.of(children));
        }
        return builder.build();
    }
}
