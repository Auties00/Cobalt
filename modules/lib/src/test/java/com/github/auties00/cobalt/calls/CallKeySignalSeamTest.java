package com.github.auties00.cobalt.calls;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.crypto.LiveCallKeyExchange;

/**
 * Static source-level guard enforcing the calls Signal seam: no calls class may construct a
 * {@code SignalSessionCipher} or {@code SignalGroupCipher} directly.
 *
 * <p>SPEC 8 and {@code int-signal-crypto.json} require the call-key path to route every encrypt and
 * decrypt through {@code MessageEncryption} / {@code MessageService.processCall}, which acquire the
 * per-address lock in {@code SignalCryptoLocks}. Instantiating a private Signal cipher inside calls
 * would bypass that lock and race the non-atomic Signal ratchet against concurrent message-plane
 * traffic on the same device session. This test scans the calls main source tree and fails if either
 * cipher type appears as a type reference outside a comment; the explanatory mention of the rule in
 * {@code LiveCallKeyExchange}'s javadoc is tolerated because comments are stripped before scanning.
 */
public class CallKeySignalSeamTest {
    private static final List<String> FORBIDDEN_CIPHERS = List.of("SignalSessionCipher", "SignalGroupCipher");

    @Test
    @DisplayName("no calls class references SignalSessionCipher or SignalGroupCipher outside a comment")
    public void noPrivateSignalCipher() {
        var root = callsSourceRoot();
        var offenders = new ArrayList<String>();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        var code = stripComments(read(p));
                        for (var cipher : FORBIDDEN_CIPHERS) {
                            if (code.contains(cipher)) {
                                offenders.add(root.relativize(p) + " references " + cipher);
                            }
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertTrue(offenders.isEmpty(),
                "calls must go through MessageEncryption/MessageService, not a private Signal cipher: " + offenders);
    }

    @Test
    @DisplayName("LiveCallKeyExchange is reachable and routes through the message pipeline imports")
    public void callKeyCryptographyUsesMessagePipeline() {
        var crypto = callsSourceRoot().resolve("crypto").resolve("LiveCallKeyExchange.java");
        assertTrue(Files.exists(crypto), "LiveCallKeyExchange.java must exist at " + crypto);
        var src = read(crypto);
        assertTrue(src.contains("import com.github.auties00.cobalt.message.send.crypto.MessageEncryption;"),
                "must import MessageEncryption");
        assertTrue(src.contains("import com.github.auties00.cobalt.message.MessageService;"),
                "must import MessageService");
        // The decrypt path must be MessageService.processCall, and the encrypt path encryptForDevice.
        assertTrue(src.contains("messageService.processCall("), "decrypt must call MessageService.processCall");
        assertTrue(src.contains("encryption.encryptForDevice("), "encrypt must call MessageEncryption.encryptForDevice");
        // And it must NOT name a private Signal cipher anywhere in real code.
        var code = stripComments(src);
        for (var cipher : FORBIDDEN_CIPHERS) {
            assertFalse(code.contains(cipher), "LiveCallKeyExchange must not reference " + cipher);
        }
    }

    /**
     * Resolves the {@code calls} main-source directory, probing the module-local and repository-root
     * layouts and walking ancestors of {@code user.dir} so the scan works whether the suite runs per
     * module or from the aggregator.
     */
    private static Path callsSourceRoot() {
        var suffix = Path.of("src", "main", "java", "com", "github", "auties00", "cobalt", "calls");
        var moduleSuffix = Path.of("modules", "lib").resolve(suffix);
        var start = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (var dir = start; dir != null; dir = dir.getParent()) {
            var local = dir.resolve(suffix);
            if (Files.isDirectory(local)) {
                return local;
            }
            var fromRepoRoot = dir.resolve(moduleSuffix);
            if (Files.isDirectory(fromRepoRoot)) {
                return fromRepoRoot;
            }
        }
        throw new IllegalStateException("could not locate the calls source tree from user.dir=" + start);
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + path, e);
        }
    }

    /**
     * Removes block and line comments so a forbidden identifier mentioned only in javadoc or an inline
     * comment does not trip the scan, while leaving string and char literals untouched (no Signal cipher
     * identifier is expected to appear in a literal).
     */
    private static String stripComments(String source) {
        var out = new StringBuilder(source.length());
        var inBlock = false;
        var inLine = false;
        for (var i = 0; i < source.length(); i++) {
            var c = source.charAt(i);
            var next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (inLine) {
                if (c == '\n') {
                    inLine = false;
                    out.append(c);
                }
                continue;
            }
            if (inBlock) {
                if (c == '*' && next == '/') {
                    inBlock = false;
                    i++;
                }
                continue;
            }
            if (c == '/' && next == '/') {
                inLine = true;
                i++;
                continue;
            }
            if (c == '/' && next == '*') {
                inBlock = true;
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }
}
