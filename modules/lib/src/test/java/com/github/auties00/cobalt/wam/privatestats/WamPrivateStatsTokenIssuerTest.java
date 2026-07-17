package com.github.auties00.cobalt.wam.privatestats;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.exception.WhatsAppException;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link WamPrivateStatsTokenIssuer}'s IQ orchestration against the captured live-bundle
 * vectors.
 *
 * <p>The suite feeds a deterministic {@link SecureRandom} into the package-private constructor so
 * the token and blinding-factor draws line up with the captured vector, then pins the outbound
 * {@code <iq xmlns="privatestats" type="get">} stanza shape, the response parsing path, and the
 * resulting {@link WamPrivateStatsToken} bytes against the {@code ed25519-live-bundle-vectors.json}
 * known-answer fixture. Those are the same vectors {@code Ed25519LiveBundleKatTest} pins for the
 * blind and unblind primitives in isolation.
 */
@DisplayName("WamPrivateStatsTokenIssuer behavioural")
class WamPrivateStatsTokenIssuerTest {
    private static final String VECTORS_FIXTURE = "/fixtures/wam/ed25519-live-bundle-vectors.json";

    @Test
    @DisplayName("issue round-trip produces the captured unblinded token")
    void issueRoundTripMatchesLiveKat() {
        var vector = loadFirstVector();
        var msg = HexFormat.of().parseHex(vector.msg);
        var scalar = HexFormat.of().parseHex(vector.scalar);
        var signed = HexFormat.of().parseHex(vector.signed);
        var pk = HexFormat.of().parseHex(vector.pk);
        var expectedUnblinded = HexFormat.of().parseHex(vector.unblinded);
        var expectedBlinded = HexFormat.of().parseHex(vector.blinded);

        var capturedIq = new AtomicReference<Stanza>();
        var client = TestWhatsAppClient.create()
                .withSendNodeHandler(builder -> {
                    var built = builder.build();
                    capturedIq.set(built);
                    return successResponse(signed, pk);
                });
        var issuer = new WamPrivateStatsTokenIssuer(client, scriptedRandom(msg, scalar));

        var token = issuer.issue();

        assertNotNull(token, "issue() must return a non-null token");
        assertArrayEquals(msg, token.token(),
                "token.token() bytes must equal the original randomised token");
        var expectedSharedSecret = sha512Concat(msg, expectedUnblinded);
        assertArrayEquals(expectedSharedSecret, token.sharedSecret(),
                "sharedSecret bytes must equal SHA-512(token || unblinded)");

        var iq = capturedIq.get();
        assertNotNull(iq, "sendNode must have been invoked");
        assertEquals("iq", iq.description());
        assertEquals("privatestats", iq.getAttributeAsString("xmlns", ""));
        assertEquals("get", iq.getAttributeAsString("type", ""));
        assertEquals("s.whatsapp.net", iq.getAttributeAsString("to", ""));

        var signCredential = iq.getChild("sign_credential").orElseThrow();
        assertEquals("1", signCredential.getAttributeAsString("version", ""));
        var blindedCredential = signCredential.getChild("blinded_credential").orElseThrow();
        if (blindedCredential instanceof Stanza.BytesStanza bytesNode) {
            assertArrayEquals(expectedBlinded, bytesNode.content(),
                    "outbound blinded_credential bytes must equal the live-bundle's blind(msg, scalar) output");
        } else {
            throw new AssertionError("blinded_credential is not a BytesStanza: " + blindedCredential);
        }
    }

    @Test
    @DisplayName("server error response throws WhatsAppPrivateStatsTokenIssuerException")
    void serverErrorThrows() {
        var vector = loadFirstVector();
        var msg = HexFormat.of().parseHex(vector.msg);
        var scalar = HexFormat.of().parseHex(vector.scalar);
        var client = TestWhatsAppClient.create()
                .withSendNodeHandler(builder -> new StanzaBuilder()
                        .description("iq")
                        .attribute("type", "error")
                        .build());
        var issuer = new WamPrivateStatsTokenIssuer(client, scriptedRandom(msg, scalar));
        assertThrows(WhatsAppException.class, issuer::issue);
    }

    @Test
    @DisplayName("response missing signed_credential throws")
    void missingSignedCredentialThrows() {
        var vector = loadFirstVector();
        var msg = HexFormat.of().parseHex(vector.msg);
        var scalar = HexFormat.of().parseHex(vector.scalar);
        var pk = HexFormat.of().parseHex(vector.pk);
        var client = TestWhatsAppClient.create()
                .withSendNodeHandler(builder -> new StanzaBuilder()
                        .description("iq")
                        .attribute("type", "result")
                        .content(new StanzaBuilder()
                                .description("sign_credential")
                                .content(new StanzaBuilder()
                                        .description("acs_public_key")
                                        .content(pk)
                                        .build())
                                .build())
                        .build());
        var issuer = new WamPrivateStatsTokenIssuer(client, scriptedRandom(msg, scalar));
        assertThrows(Exception.class, issuer::issue,
                "missing signed_credential must raise an exception, not return a malformed token");
    }

    @Test
    @DisplayName("wrong-length signed_credential throws")
    void wrongLengthSignedCredentialThrows() {
        var vector = loadFirstVector();
        var msg = HexFormat.of().parseHex(vector.msg);
        var scalar = HexFormat.of().parseHex(vector.scalar);
        var pk = HexFormat.of().parseHex(vector.pk);
        var client = TestWhatsAppClient.create()
                .withSendNodeHandler(builder -> new StanzaBuilder()
                        .description("iq")
                        .attribute("type", "result")
                        .content(new StanzaBuilder()
                                .description("sign_credential")
                                .content(new StanzaBuilder()
                                        .description("signed_credential")
                                        .content(new byte[16])
                                        .build())
                                .content(new StanzaBuilder()
                                        .description("acs_public_key")
                                        .content(pk)
                                        .build())
                                .build())
                        .build());
        var issuer = new WamPrivateStatsTokenIssuer(client, scriptedRandom(msg, scalar));
        assertThrows(Exception.class, issuer::issue);
    }

    // A success IQ shaped like the live server's sign_credential reply.
    private static Stanza successResponse(byte[] signed, byte[] pk) {
        return new StanzaBuilder()
                .description("iq")
                .attribute("type", "result")
                .content(new StanzaBuilder()
                        .description("sign_credential")
                        .content(List.of(
                                new StanzaBuilder()
                                        .description("signed_credential")
                                        .content(signed)
                                        .build(),
                                new StanzaBuilder()
                                        .description("acs_public_key")
                                        .content(pk)
                                        .build()))
                        .build())
                .build();
    }

    // Every test reuses vector 0 so the captured bytes cross-reference with Ed25519LiveBundleKatTest.
    private static Vector loadFirstVector() {
        try (var stream = WamPrivateStatsTokenIssuerTest.class.getResourceAsStream(VECTORS_FIXTURE)) {
            if (stream == null) {
                throw new AssertionError("ed25519 vectors fixture missing on classpath: " + VECTORS_FIXTURE);
            }
            var json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            var root = JSON.parseObject(json);
            var vector = root.getJSONArray("vectors").getJSONObject(0);
            return new Vector(
                    vector.getString("msg"),
                    vector.getString("scalar"),
                    vector.getString("blinded"),
                    vector.getString("pk"),
                    vector.getString("signed"),
                    vector.getString("unblinded"));
        } catch (IOException error) {
            throw new UncheckedIOException("failed to load " + VECTORS_FIXTURE, error);
        }
    }

    // issue() draws from nextBytes exactly twice (token, then blinding factor); failing loud on a
    // third call breaks the suite if the production draw count drifts.
    private static SecureRandom scriptedRandom(byte[] first, byte[] second) {
        return new SecureRandom() {
            private int call;

            @Override
            public void nextBytes(byte[] target) {
                var source = switch (call++) {
                    case 0 -> first;
                    case 1 -> second;
                    default -> throw new AssertionError(
                            "scripted SecureRandom asked for more than two byte sequences");
                };
                if (target.length != source.length) {
                    throw new AssertionError("scripted SecureRandom: target length " + target.length
                            + " != source length " + source.length);
                }
                System.arraycopy(source, 0, target, 0, source.length);
            }
        };
    }

    @Test
    @DisplayName("scripted SecureRandom rejects a third call (issue must use exactly two random sequences)")
    void scriptedRandomFailsLoudOnExtraCall() {
        var random = scriptedRandom(new byte[32], new byte[32]);
        random.nextBytes(new byte[32]);
        random.nextBytes(new byte[32]);
        assertThrows(AssertionError.class, () -> random.nextBytes(new byte[32]));
        assertTrue(true);
    }

    // SHA-512(a || b); the assertion oracle for sharedSecret, mirroring WAACSTokenUtils.getSharedSecret.
    private static byte[] sha512Concat(byte[] a, byte[] b) {
        try {
            var md = MessageDigest.getInstance("SHA-512");
            md.update(a);
            md.update(b);
            return md.digest();
        } catch (NoSuchAlgorithmException error) {
            throw new AssertionError("SHA-512 must be available on every JVM", error);
        }
    }

    // Per-vector hex strings from the KAT fixture.
    private record Vector(String msg, String scalar, String blinded, String pk, String signed, String unblinded) {
    }
}
