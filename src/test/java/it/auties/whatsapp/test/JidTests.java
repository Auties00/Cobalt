package it.auties.whatsapp.test;

import it.auties.protobuf.model.ProtobufString;
import it.auties.whatsapp.exception.MalformedJidException;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.jid.JidServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JidTests {
    @Test
    public void testOfficialSurveysAccount() {
        var j = Jid.officialSurveysAccount();
        assertNotNull(j);
        assertRoundTrip(j);
    }

    @Test
    public void testOfficialBusinessAccount() {
        var j = Jid.officialBusinessAccount();
        assertNotNull(j);
        assertRoundTrip(j);
    }

    @Test
    public void testStatusBroadcast() {
        var j = Jid.statusBroadcastAccount();
        assertNotNull(j);
        assertRoundTrip(j);
    }

    @Test
    public void testAnnouncements() {
        var j = Jid.announcementsAccount();
        assertNotNull(j);
        assertRoundTrip(j);
    }

    @Test
    public void testMalformed() {
        assertThrows(MalformedJidException.class, () -> Jid.of(-1));
        assertThrows(MalformedJidException.class, () -> Jid.of("", JidServer.user(), -1, 0));
        assertThrows(MalformedJidException.class, () -> Jid.of("", JidServer.user(), 256, 0));
        assertThrows(MalformedJidException.class, () -> Jid.of("", JidServer.user(), 0, -1));
        assertThrows(MalformedJidException.class, () -> Jid.of("", JidServer.user(), 0, 256));
        assertThrows(MalformedJidException.class, () -> Jid.of("user:1:1", JidServer.user()));
        assertThrows(MalformedJidException.class, () -> Jid.of(ProtobufString.lazy("user:1:1".getBytes())));
        assertThrows(MalformedJidException.class, () -> Jid.of("user_1_1", JidServer.user()));
        assertThrows(MalformedJidException.class, () -> Jid.of(ProtobufString.lazy("user_1_1".getBytes())));
        assertThrows(MalformedJidException.class, () -> Jid.of("user::1", JidServer.user()));
        assertThrows(MalformedJidException.class, () -> Jid.of(ProtobufString.lazy("user::1".getBytes())));
        assertThrows(MalformedJidException.class, () -> Jid.of("user__1", JidServer.user()));
        assertThrows(MalformedJidException.class, () -> Jid.of(ProtobufString.lazy("user__1".getBytes())));
        assertThrows(MalformedJidException.class, () -> Jid.of("user:1_1:1", JidServer.user()));
        assertThrows(MalformedJidException.class, () -> Jid.of(ProtobufString.lazy("user:1_1:1".getBytes())));
        assertThrows(MalformedJidException.class, () -> Jid.of("user_1:1_1", JidServer.user()));
        assertThrows(MalformedJidException.class, () -> Jid.of(ProtobufString.lazy("user_1:1_1".getBytes())));
    }

    @Test
    public void testWellFormed() {
        assertDoesNotThrow(() -> {
            var j = Jid.of(0);
            assertRoundTrip(j);
            assertEquals("0@s.whatsapp.net", j.toString());
        });

        assertDoesNotThrow(() -> {
            var j = Jid.of("1234567890");
            assertRoundTrip(j);
            assertEquals("1234567890@s.whatsapp.net", j.toString());
        });
        assertDoesNotThrow(() -> {
            var j = Jid.of("1234567890@g.us");
            assertRoundTrip(j);
            assertEquals("1234567890@g.us", j.toString());
        });
        assertDoesNotThrow(() -> {
            var j = Jid.of(ProtobufString.lazy("1234567890@g.us".getBytes()));
            assertRoundTrip(j);
            assertEquals("1234567890@g.us", j.toString());
        });

        assertDoesNotThrow(() -> {
            var j = Jid.of("1234567890:1");
            assertRoundTrip(j);
            assertEquals("1234567890:1@s.whatsapp.net", j.toString());
        });
        assertDoesNotThrow(() -> {
            var j = Jid.of("1234567890:1@g.us");
            assertRoundTrip(j);
            assertEquals("1234567890:1@g.us", j.toString());
        });
        assertDoesNotThrow(() -> {
            var j = Jid.of(ProtobufString.lazy("1234567890:1@g.us".getBytes()));
            assertRoundTrip(j);
            assertEquals("1234567890:1@g.us", j.toString());
        });

        assertDoesNotThrow(() -> {
            var j = Jid.of("1234567890_1");
            assertRoundTrip(j);
            assertEquals("1234567890_1@s.whatsapp.net", j.toString());
        });
        assertDoesNotThrow(() -> {
            var j = Jid.of("1234567890_1@g.us");
            assertRoundTrip(j);
            assertEquals("1234567890_1@g.us", j.toString());
        });
        assertDoesNotThrow(() -> {
            var j = Jid.of(ProtobufString.lazy("1234567890_1@g.us".getBytes()));
            assertRoundTrip(j);
            assertEquals("1234567890_1@g.us", j.toString());
        });

        assertDoesNotThrow(() -> {
            var j = Jid.of("1234567890_1:1");
            assertRoundTrip(j);
            assertEquals("1234567890_1:1@s.whatsapp.net", j.toString());
        });
        assertDoesNotThrow(() -> {
            var j = Jid.of("1234567890_1:1@g.us");
            assertRoundTrip(j);
            assertEquals("1234567890_1:1@g.us", j.toString());
        });
        assertDoesNotThrow(() -> {
            var j = Jid.of(ProtobufString.lazy("1234567890_1:1@g.us".getBytes()));
            assertRoundTrip(j);
            assertEquals("1234567890_1:1@g.us", j.toString());
        });
    }

    @Test
    public void testServerJid() {
        assertTrue(Jid.of(JidServer.user()).isServerJid(JidServer.user()));
        assertRoundTrip(Jid.of(JidServer.user()));
        assertRoundTrip(Jid.of(JidServer.legacyUser()));
        assertRoundTrip(Jid.of(JidServer.broadcast()));
        assertRoundTrip(Jid.of(JidServer.call()));
        assertRoundTrip(Jid.of(JidServer.groupOrCommunity()));
        assertRoundTrip(Jid.of(JidServer.lid()));
        assertRoundTrip(Jid.of(JidServer.newsletter()));
    }

    private static void assertRoundTrip(Jid original) {
        var canonical = original.toString();
        var reparsed = Jid.of(canonical);
        assertNotNull(reparsed, "Reparsed Jid should not be null");
        assertEquals(canonical, reparsed.toString(), "Round-trip via toString should preserve canonical form");
    }
}