package com.github.auties00.cobalt.media.transcode.text.link;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the safety properties of {@link Linkify}: the TLD-guard short-circuit,
 * {@link Linkify#hasHttpLink} scheme strictness, {@link Linkify#validateEmail} mailto-only
 * acceptance, and the {@code null}-safety of every public entry.
 */
@DisplayName("Linkify")
class LinkifyTest {

    @Test
    @DisplayName("input lacking any TLD is rejected by the TLD-guard short-circuit")
    void noTldRejected() {
        assertTrue(Linkify.findLink("just some text without a domain", false).isEmpty(),
                "TLD_GUARD must short-circuit before regex matching");
    }

    @Test
    @DisplayName("hasHttpLink rejects bare hosts without an explicit scheme")
    void hasHttpLinkRequiresScheme() {
        assertFalse(Linkify.hasHttpLink("see example.com here"),
                "bare host must NOT count as an http link");
    }

    @Test
    @DisplayName("validateEmail rejects an http URL")
    void validateEmailRejectsHttp() {
        assertTrue(Linkify.validateEmail("https://example.com").isEmpty(),
                "validateEmail must reject non-mailto schemes");
    }

    @Test
    @DisplayName("validateEmail rejects when text contains extra characters around the mailto")
    void validateEmailRejectsPartialCoverage() {
        assertTrue(Linkify.validateEmail("contact mailto:user@example.com").isEmpty(),
                "validateEmail requires the candidate to cover the input entirely");
    }

    @Test
    @DisplayName("null input is a clean empty rather than NPE")
    void nullSafe() {
        assertTrue(Linkify.findLink(null, true).isEmpty());
        assertTrue(Linkify.findLinks(null, true).isEmpty());
        assertFalse(Linkify.hasHttpLink(null));
        assertTrue(Linkify.validateEmail(null).isEmpty());
    }

    @Test
    @DisplayName("unknown TLD is filtered out by the TLD guard")
    void unknownTldRejected() {
        assertTrue(Linkify.findLink("hit example.notatld page", false).isEmpty(),
                "non-TLD extensions are not URL candidates");
    }
}
