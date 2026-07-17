package com.github.auties00.cobalt.sync;
import com.github.auties00.cobalt.migration.LiveLidMigrationService;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationApplicationResult;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import com.github.auties00.cobalt.sync.handler.WebAppStateActionHandler;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the structural contract of {@link WebAppStateHandlerRegistry}: every default handler being
 * registered, lookup-by-action-name in both the present and missing branches, every handler
 * declaring a non-null collection and a non-negative version,
 * {@link WebAppStateHandlerRegistry#maxSupportedVersion()} being a true maximum, and a later
 * {@link WebAppStateHandlerRegistry#registerHandler(WebAppStateActionHandler)} call overriding an
 * earlier one for the same action name. The registry is built with the same dependency wiring as
 * {@link WebAppStateService} so the assertions exercise the production handler set; the trivial
 * {@link RecordingHandler} stands in for the override and new-registration cases.
 */
@DisplayName("WebAppStateHandlerRegistry")
class WebAppStateHandlerRegistryTest {
    private static final Jid SELF_PN = Jid.of("19250000001@s.whatsapp.net");

    private static final Jid SELF_LID = Jid.of("83116928594000@lid");

    private WebAppStateHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        var props = TestABPropsService.builder().build();
        var store = DeviceFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create().withStore(store);
        var wam = new LiveWamService(client, props);
        var lidMigration = new LiveLidMigrationService(client, props, wam);
        registry = new WebAppStateHandlerRegistry(props, lidMigration, wam);
    }

    @Nested
    @DisplayName("default registration -- every WAWeb*Sync handler is wired")
    class DefaultRegistration {
        @Test
        @DisplayName("a known action name resolves to its handler")
        void knownActionResolves() {
            var handler = registry.findHandler("archive").orElseThrow(
                    () -> new AssertionError("archive handler must be registered by default"));
            assertNotNull(handler);
        }

        @Test
        @DisplayName("a representative selection of action names all resolve")
        void representativeSelectionResolves() {
            for (var actionName : new String[]{
                    "archive", "pin_v1", "mute", "markChatAsRead", "deleteChat",
                    "star", "deleteMessageForMe",
                    "contact", "lid_contact",
                    "label_edit", "label_jid",
                    "quick_reply", "deviceAgent", "agentChatAssignment",
                    "favoriteSticker", "removeRecentSticker", "avatar_updated_action",
                    "payment_info", "payment_tos", "custom_payment_methods",
                    "userStatusMute", "time_format", "favorites",
                    "nux", "primary_feature", "settings_sync", "setting_pushName",
            }) {
                assertTrue(registry.findHandler(actionName).isPresent(),
                        "handler must be registered: " + actionName);
            }
        }
    }

    @Nested
    @DisplayName("lookup -- unknown action")
    class UnknownAction {
        @Test
        @DisplayName("findHandler on an unknown name returns Optional.empty()")
        void unknownReturnsEmpty() {
            assertTrue(registry.findHandler("not_a_real_action_name").isEmpty());
        }

        @Test
        @DisplayName("findHandler on an empty string returns Optional.empty()")
        void emptyStringReturnsEmpty() {
            assertTrue(registry.findHandler("").isEmpty());
        }
    }

    @Nested
    @DisplayName("collection consistency -- every handler points at a valid SyncPatchType")
    class CollectionConsistency {
        @Test
        @DisplayName("every registered handler resolves to a non-null SyncPatchType")
        void allCollectionsNonNull() {
            for (var actionName : sampleRegisteredActionNames()) {
                var handler = registry.findHandler(actionName).orElseThrow();
                assertNotNull(handler.collectionName(),
                        "handler " + actionName + " must declare a collection");
            }
        }

        @Test
        @DisplayName("every handler's version is non-negative")
        void allVersionsNonNegative() {
            for (var actionName : sampleRegisteredActionNames()) {
                var handler = registry.findHandler(actionName).orElseThrow();
                assertTrue(handler.version() >= 0,
                        "handler " + actionName + " has negative version: " + handler.version());
            }
        }
    }

    @Nested
    @DisplayName("maxSupportedVersion")
    class MaxSupportedVersion {
        @Test
        @DisplayName("maxSupportedVersion equals the maximum of all registered handlers' versions")
        void matchesMaxOverAll() {
            var max = registry.maxSupportedVersion();
            assertTrue(max >= 0, "version is non-negative");

            for (var actionName : sampleRegisteredActionNames()) {
                var version = registry.findHandler(actionName).orElseThrow().version();
                assertTrue(version <= max,
                        "handler " + actionName + " (v=" + version
                                + ") exceeds reported max=" + max);
            }
        }
    }

    @Nested
    @DisplayName("registerHandler -- override on duplicate actionName")
    class RegisterHandler {
        @Test
        @DisplayName("a second handler with the same actionName replaces the first")
        void duplicateNameOverrides() {
            var existing = registry.findHandler("archive").orElseThrow();
            var replacement = new RecordingHandler("archive",
                    existing.collectionName(), existing.version());

            registry.registerHandler(replacement);
            assertEquals(replacement, registry.findHandler("archive").orElseThrow(),
                    "second register call overrides the first per WAWebSyncdGetActionHandler.setActionHandlers");
        }

        @Test
        @DisplayName("registering a new handler under a new actionName makes it lookable")
        void newRegistrationLookable() {
            assertFalse(registry.findHandler("__test_action__").isPresent(),
                    "precondition: no handler registered for the test action name");

            var custom = new RecordingHandler("__test_action__", SyncPatchType.REGULAR, 1);
            registry.registerHandler(custom);
            assertEquals(custom, registry.findHandler("__test_action__").orElseThrow());
        }
    }

    /**
     * Returns the sample of action names the iteration tests walk.
     *
     * <p>Kept in sync by hand with the registry's default handlers; a
     * missing action here only weakens coverage, not correctness.
     *
     * @return a set of action names guaranteed to be registered by the
     *         default constructor
     */
    private static Set<String> sampleRegisteredActionNames() {
        var set = new HashSet<String>();
        set.add("archive");
        set.add("pin_v1");
        set.add("mute");
        set.add("markChatAsRead");
        set.add("deleteChat");
        set.add("star");
        set.add("deleteMessageForMe");
        set.add("contact");
        set.add("lid_contact");
        set.add("payment_info");
        set.add("payment_tos");
        set.add("nux");
        set.add("primary_feature");
        set.add("settings_sync");
        return set;
    }

    /**
     * Minimal {@link WebAppStateActionHandler} carrying explicit identity fields and a no-op
     * {@code applyMutation}, used by the override and new-registration tests to substitute a known
     * instance under an existing or fresh action name without touching a real handler. The test
     * suite only inspects identity, never invokes {@code applyMutation}.
     *
     * @param actionName     the action name returned by
     *                       {@link WebAppStateActionHandler#actionName()}
     * @param collectionName the collection returned by
     *                       {@link WebAppStateActionHandler#collectionName()}
     * @param version        the version returned by
     *                       {@link WebAppStateActionHandler#version()}
     */
    private record RecordingHandler(
            String actionName,
            SyncPatchType collectionName,
            int version
    ) implements WebAppStateActionHandler {
        @Override
        public MutationApplicationResult applyMutation(
                LinkedWhatsAppClient client,
                DecryptedMutation.Trusted mutation) {
            return MutationApplicationResult.success();
        }
    }
}
