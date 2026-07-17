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

import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.auties00.cobalt.calls.crypto.LiveCallKeyExchange;

/**
 * Adversarial P8 re-check of the Signal seam from the construction and import angle.
 *
 * <p>{@code CallKeySignalSeamTest} already fails on any textual reference to a private Signal cipher in the
 * calls tree. This suite is the complementary, narrower guard P8 wiring asks for: it fails on a
 * {@code new SignalSessionCipher(}/{@code new SignalGroupCipher(} construction or an {@code import} of
 * either type anywhere under {@code calls}, and positively pins that the call-key path is wired through
 * {@code MessageService} on the service ({@link LiveCallsService} holds a {@code MessageService}) and the
 * crypto facade ({@code LiveCallKeyExchange} calls {@code MessageService.processCall} and
 * {@code MessageEncryption.encryptForDevice}). The two together leave no way for the call-key ratchet to
 * bypass the per-address lock in {@code SignalCryptoLocks}.
 */
@DisplayName("calls P8 Signal seam wiring re-check")
class SignalSeamWiringTest {
    private static final List<String> FORBIDDEN_CONSTRUCTIONS =
            List.of("new SignalSessionCipher(", "new SignalGroupCipher(");
    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "import com.github.auties00.libsignal.SignalSessionCipher;",
            "import com.github.auties00.libsignal.groups.SignalGroupCipher;",
            "SignalSessionCipher;", "SignalGroupCipher;");

    @Test
    @DisplayName("no calls class constructs or imports a raw SignalSessionCipher or SignalGroupCipher")
    void noRawCipherConstructionOrImport() {
        var root = callsSourceRoot();
        var offenders = new ArrayList<String>();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        var raw = read(p);
                        var code = stripComments(raw);
                        for (var ctor : FORBIDDEN_CONSTRUCTIONS) {
                            if (code.contains(ctor)) {
                                offenders.add(root.relativize(p) + " constructs " + ctor);
                            }
                        }
                        for (var imp : FORBIDDEN_IMPORTS) {
                            if (code.contains(imp)) {
                                offenders.add(root.relativize(p) + " imports/names " + imp);
                            }
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertTrue(offenders.isEmpty(),
                "the call-key path must go through MessageEncryption/MessageService, never a private Signal "
                        + "cipher (bypassing SignalCryptoLocks races the ratchet): " + offenders);
    }

    @Test
    @DisplayName("LiveCallsService holds a MessageService and the crypto facade routes through the message pipeline")
    void callKeyPathWiredThroughMessagePipeline() {
        var service = read(callsSourceRoot().resolve("LiveCallsService.java"));
        assertTrue(service.contains("import com.github.auties00.cobalt.message.MessageService;"),
                "LiveCallsService must hold the MessageService that owns offer encryption and key decryption");
        assertTrue(service.contains("private final MessageService messageService;"),
                "LiveCallsService must keep the MessageService as an injected field");

        var crypto = callsSourceRoot().resolve("crypto").resolve("LiveCallKeyExchange.java");
        assertTrue(Files.exists(crypto), "LiveCallKeyExchange.java must exist at " + crypto);
        var cryptoSrc = read(crypto);
        assertTrue(cryptoSrc.contains("messageService.processCall("),
                "the call-key decrypt path must call MessageService.processCall");
        assertTrue(cryptoSrc.contains("encryption.encryptForDevice("),
                "the call-key encrypt path must call MessageEncryption.encryptForDevice");
    }

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
     * Removes block and line comments so the forbidden-identifier scan ignores the explanatory mention of
     * the rule in {@code LiveCallKeyExchange}'s javadoc.
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
