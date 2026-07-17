package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural tests for {@link GroupSkmsgFanoutStanza}, pinning the
 * steady-state SKMSG shape (bare {@code <enc type="skmsg">} sibling, no
 * {@code <participants>}), the bot-feedback variant (null ciphertext
 * suppresses the {@code <enc>} entirely), and the verbatim attribute
 * propagation for {@code phash}, {@code decrypt-fail}, and {@code edit}.
 * The tests feed the 16-arg builder directly with a null-children tail,
 * since per-child composition is exercised by the dedicated tests on
 * {@link BizStanza}, {@link BotStanza}, and {@link MetaStanza}; the
 * first-send distribution shape (per-device PKMSG under
 * {@code <participants>}) is covered indirectly by the CAG live oracle.
 */
@DisplayName("GroupSkmsgFanoutStanza")
class GroupSkmsgFanoutStanzaTest {

    private static final Jid GROUP = Jid.of("120363023250764418@g.us");

    private static final String ID = "3EB0CAFEBABE";

    private static final byte[] CIPHERTEXT = new byte[]{1, 2, 3, 4};

    @Test
    @DisplayName("steady-state SKMSG: <message> carries a bare <enc type=\"skmsg\">")
    void steadyStateBareSkmsg() {
        var stanza = GroupSkmsgFanoutStanza.build(
                ID, GROUP, "text", "2:hash", CIPHERTEXT,
                null, null, null, "lid",
                null, null, null, null, null, null, null
        ).build();

        assertEquals("message", stanza.description());
        assertEquals(ID, stanza.getAttributeAsString("id").orElseThrow());
        assertEquals("text", stanza.getAttributeAsString("type").orElseThrow());
        assertEquals("lid", stanza.getAttributeAsString("addressing_mode").orElseThrow());
        assertEquals("2:hash", stanza.getAttributeAsString("phash").orElseThrow());

        var enc = stanza.getChild("enc").orElseThrow();
        assertEquals("skmsg", enc.getAttributeAsString("type").orElseThrow());
        assertEquals("2", enc.getAttributeAsString("v").orElseThrow());
        assertFalse(stanza.getChild("participants").isPresent(),
                "steady-state SKMSG does not emit <participants>");
    }

    @Test
    @DisplayName("absent phash: phash attribute is absent on the wire (not empty string)")
    void absentPhashIsAbsent() {
        var stanza = GroupSkmsgFanoutStanza.build(
                ID, GROUP, "text", null, CIPHERTEXT,
                null, null, null, "lid",
                null, null, null, null, null, null, null
        ).build();
        assertTrue(stanza.getAttribute("phash").isEmpty(),
                "null phash must be absent - not set to an empty string");
    }

    @Test
    @DisplayName("bot-feedback path: null ciphertext suppresses the <enc> child entirely")
    void noCiphertextOmitsEnc() {
        var stanza = GroupSkmsgFanoutStanza.build(
                ID, GROUP, "text", null, null,
                null, null, null, "lid",
                null, null, null, null, null, null, null
        ).build();
        assertFalse(stanza.getChild("enc").isPresent(),
                "null ciphertext must suppress <enc> (bot-feedback path)");
    }

    @Test
    @DisplayName("decrypt-fail attribute propagates to <enc> when set")
    void decryptFailAttribute() {
        var stanza = GroupSkmsgFanoutStanza.build(
                ID, GROUP, "reaction", null, CIPHERTEXT,
                null, "hide", null, "lid",
                null, null, null, null, null, null, null
        ).build();
        var enc = stanza.getChild("enc").orElseThrow();
        assertEquals("hide", enc.getAttributeAsString("decrypt-fail").orElseThrow(),
                "decrypt-fail=hide must propagate on the <enc> for encrypted-reaction CAG sends");
    }

    @Test
    @DisplayName("edit attribute propagates to <message>")
    void editAttribute() {
        var stanza = GroupSkmsgFanoutStanza.build(
                ID, GROUP, "text", null, CIPHERTEXT,
                null, null, "1", "lid",
                null, null, null, null, null, null, null
        ).build();
        assertEquals("1", stanza.getAttributeAsString("edit").orElseThrow(),
                "edit=1 marker propagates to outer <message edit=...>");
    }

    @Test
    @DisplayName("null messageId / groupJid / type / addressingMode throw NullPointerException")
    void requiredArgsNullThrow() {
        assertThrows(NullPointerException.class, () -> GroupSkmsgFanoutStanza.build(
                null, GROUP, "text", null, CIPHERTEXT,
                null, null, null, "lid",
                null, null, null, null, null, null, null));
        assertThrows(NullPointerException.class, () -> GroupSkmsgFanoutStanza.build(
                ID, null, "text", null, CIPHERTEXT,
                null, null, null, "lid",
                null, null, null, null, null, null, null));
        assertThrows(NullPointerException.class, () -> GroupSkmsgFanoutStanza.build(
                ID, GROUP, null, null, CIPHERTEXT,
                null, null, null, "lid",
                null, null, null, null, null, null, null));
        assertThrows(NullPointerException.class, () -> GroupSkmsgFanoutStanza.build(
                ID, GROUP, "text", null, CIPHERTEXT,
                null, null, null, null,
                null, null, null, null, null, null, null));
    }
}
