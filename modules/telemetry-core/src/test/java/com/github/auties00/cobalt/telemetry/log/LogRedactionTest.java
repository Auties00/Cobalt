package com.github.auties00.cobalt.telemetry.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the privacy redaction layer: the {@link LogRedactor} fingerprint engine, the {@link LogRedactable}
 * wrapper records that classify a sensitive value, and {@link Log}'s {@code enabled} toggle and
 * {@link LogRedactableProvider} dispatch. The tests assert the three properties the design promises -- same
 * input yields the same token, distinct inputs yield distinct tokens, and no token echoes the raw value --
 * plus the length-preserving {@code byte[]} case, the string-based JID redaction, and the on/off toggle. The
 * redaction of a real {@code Jid} lives in the wire-core module, which owns that type.
 *
 * <p>Every test uses a fixed sample phone number ({@code 393331234567}) as the canonical secret so the "raw
 * value never appears" assertions read consistently. The salt is process-random, so the tests assert
 * within-run determinism rather than pinning exact token bytes.
 */
@DisplayName("Log redaction")
class LogRedactionTest {
    private static final String PHONE = "393331234567";

    @AfterEach
    void reEnableRedaction() {
        Log.setEnabled(true);
    }

    @Nested
    @DisplayName("fingerprinting")
    class Fingerprinting {
        @Test
        @DisplayName("is deterministic for equal inputs and distinct for different inputs")
        void deterministicAndDistinct() {
            var first = LogRedactor.fingerprint(PHONE.getBytes());
            var again = LogRedactor.fingerprint(PHONE.getBytes());
            var other = LogRedactor.fingerprint("393339999999".getBytes());
            assertEquals(first, again, "same bytes must fingerprint identically");
            assertNotEquals(first, other, "different bytes must fingerprint differently");
        }

        @Test
        @DisplayName("never echoes the raw value")
        void neverEchoesRaw() {
            assertFalse(LogRedactor.fingerprint(PHONE.getBytes()).contains(PHONE));
        }
    }

    @Nested
    @DisplayName("byte[]")
    class ByteRedaction {
        @Test
        @DisplayName("keeps the length but not the content")
        void lengthOnly() {
            var key = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
            var redacted = new LogRedactable.Bytes(key).toRedactedLog();
            assertTrue(redacted.startsWith("bytes(len=8,#"));
            assertFalse(redacted.contains("1, 2"), "array content must not appear");
        }

        @Test
        @DisplayName("renders through the LogRedactable branch when wrapped")
        void dispatchedWhenWrapped() {
            var key = new byte[]{9, 8, 7};
            assertEquals("bytes(len=3,#" + LogRedactor.fingerprint(key) + ")", Log.redact(new LogRedactable.Bytes(key)));
        }
    }

    @Nested
    @DisplayName("wrappers")
    class Wrappers {
        @Test
        @DisplayName("mask the raw value behind a typed token")
        void maskRawValue() {
            assertFalse(new LogRedactable.Phone(PHONE).toString().contains(PHONE));
            assertTrue(new LogRedactable.Phone(PHONE).toString().startsWith("phone(#"));
            assertTrue(new LogRedactable.Token("abcSECRETxyz").toString().startsWith("token(len=12,#"));
            assertFalse(new LogRedactable.Token("abcSECRETxyz").toString().contains("SECRET"));
            assertTrue(new LogRedactable.Email("a@b.com").toString().startsWith("email(#"));
            assertTrue(new LogRedactable.Code("123456").toString().startsWith("code(#"));
            assertTrue(new LogRedactable.Secret("hunter2").toString().startsWith("secret(#"));
            assertFalse(new LogRedactable.Secret("hunter2").toString().contains("hunter2"));
        }

        @Test
        @DisplayName("fingerprint the E.164 and bare-digit phone forms identically")
        void phoneNormalisation() {
            var plus = new LogRedactable.Phone("+" + PHONE).toString();
            var bare = new LogRedactable.Phone(PHONE).toString();
            var numeric = new LogRedactable.Phone(393331234567L).toString();
            assertEquals(plus, bare);
            assertEquals(bare, numeric);
        }

        @Test
        @DisplayName("redact a JID carried as a String, hiding the user but keeping the server and device")
        void userWrapper() {
            var redacted = new LogRedactable.User(PHONE + ":5@s.whatsapp.net").toString();
            assertFalse(redacted.contains(PHONE));
            assertTrue(redacted.startsWith("#"));
            assertTrue(redacted.contains(":5"), "device is preserved");
            assertTrue(redacted.contains("@s.whatsapp.net"), "server is preserved");
        }
    }

    @Nested
    @DisplayName("server")
    class ServerAddress {
        @Test
        @DisplayName("renders an address with no user component verbatim")
        void verbatim() {
            assertEquals("s.whatsapp.net", new LogRedactable.Server("s.whatsapp.net").toRedactedLog());
            assertEquals("s.whatsapp.net", new LogRedactable.Server("s.whatsapp.net").toString());
        }
    }

    @Nested
    @DisplayName("collections")
    class Collections {
        @Test
        @DisplayName("redact each element instead of leaking the collection's own toString")
        void perElement() {
            var users = List.of(new LogRedactable.User(PHONE + "@lid"), new LogRedactable.User("393339999999@lid"));
            var redacted = String.valueOf(Log.redact(users));
            assertFalse(redacted.contains(PHONE), "an element must not leak through the list");
            assertFalse(redacted.contains("393339999999"));
            assertTrue(redacted.contains("@lid"), "element structure survives");
        }

        @Test
        @DisplayName("redact each element of an object array")
        void arrays() {
            var users = new Object[]{new LogRedactable.User(PHONE + "@lid")};
            assertFalse(String.valueOf(Log.redact(users)).contains(PHONE));
        }

        @Test
        @DisplayName("leave a byte[] alone so key material stays an identity hash, not element digits")
        void byteArrayUntouched() {
            var key = new byte[]{1, 2, 3};
            assertSame(key, Log.redact(key), "a byte[] is not an Object[] and must not be walked");
        }

        @Test
        @DisplayName("pass a collection of non-sensitive values through untouched")
        void plainElements() {
            assertEquals("[1, 2]", String.valueOf(Log.redact(List.of(1, 2))));
        }
    }

    @Nested
    @DisplayName("toggle")
    class Toggle {
        @Test
        @DisplayName("passes values through raw when disabled")
        void disabledPassesThrough() {
            Log.setEnabled(false);
            var key = new byte[]{1, 2, 3};
            assertSame(key, Log.redact(key), "redact returns the value itself when disabled");
            assertTrue(new LogRedactable.Phone(PHONE).toString().contains(PHONE), "wrapper shows raw value when disabled");
        }
    }

    @Nested
    @DisplayName("formatMessage")
    class FormatMessage {
        @Test
        @DisplayName("redacts wrapper parameters in the rendered line")
        void redactsParameters() {
            var formatter = new Log.CobaltFormatter();
            var record = new LogRecord(Level.INFO, "sent to {0} with code {1}");
            record.setParameters(new Object[]{new LogRedactable.User(PHONE + "@s.whatsapp.net"), new LogRedactable.Code("654321")});
            var message = formatter.formatMessage(record);
            assertFalse(message.contains(PHONE), "JID phone must be masked");
            assertFalse(message.contains("654321"), "code must be masked");
            assertTrue(message.contains("code(#"));
        }

        @Test
        @DisplayName("leaves a non-sensitive parameter untouched")
        void keepsSafeParameters() {
            var formatter = new Log.CobaltFormatter();
            var record = new LogRecord(Level.INFO, "retry count {0}");
            record.setParameters(new Object[]{7});
            assertEquals("retry count 7", formatter.formatMessage(record));
        }
    }
}
