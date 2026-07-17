package com.github.auties00.cobalt.calls;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adversarial P10 guard that the legacy {@code com.github.auties00.cobalt.call} engine package is fully
 * unbound. The finishing migration repointed eleven seams off {@code call.*} and the Delete phase removed
 * the package; this suite is the standing proof that no production class in the affected areas
 * (calls, client, message, stream, ack, exception, plus the FFmpeg media bindings the call-codec seam used)
 * references that package any more, and a regression fence against reintroducing it.
 *
 * <p>The scan walks the named main-source subtrees, strips comments and string literals (so a forbidden
 * package named only in javadoc or a message does not trip it), and fails on any surviving reference to the
 * legacy package: an {@code import com.github.auties00.cobalt.call.}, a fully-qualified
 * {@code com.github.auties00.cobalt.call.} type use, or the bare {@code call.stream.}/{@code call.signaling.}
 * /{@code call.audio.}/{@code call.video.} segment a relocated seam would carry. The replacement package
 * {@code com.github.auties00.cobalt.calls.} is explicitly NOT matched, since the boundary is a literal
 * package-name dot: {@code calls.} never starts with {@code call.}.
 */
@DisplayName("calls finishing migration: no production reference to the legacy call.* engine package")
class NoLegacyCallRefsTest {
    /**
     * The package prefix that must not appear; the trailing dot makes {@code calls.} a non-match because the
     * char after {@code call} there is {@code s}, not {@code .}.
     */
    private static final String LEGACY_PREFIX = "com.github.auties00.cobalt.call.";

    /**
     * The bare relocated-seam segments a stray legacy reference would carry even without the full package
     * path; each is anchored on the {@code call.} package dot so {@code calls.media} cannot match.
     */
    private static final List<String> LEGACY_SEAM_SEGMENTS = List.of(
            "call.stream.", "call.signaling.", "call.audio.", "call.video.");

    /**
     * The production main-source subtrees the eleven migrated seams lived in, relative to the cobalt package
     * root. The whole calls tree is scanned (it must be self-contained); the other areas are the seam
     * owners named by the migration brief.
     */
    private static final List<String> SCANNED_AREAS = List.of(
            "calls", "client", "message", "stream", "ack", "exception", "media/ffmpeg");

    @Test
    @DisplayName("no scanned production class imports or names the legacy call.* package")
    void noLegacyCallReference() {
        var root = cobaltSourceRoot();
        var offenders = new ArrayList<String>();
        for (var area : SCANNED_AREAS) {
            var areaRoot = root.resolve(area);
            if (!Files.isDirectory(areaRoot)) {
                continue;
            }
            scan(areaRoot, root, offenders);
        }
        assertTrue(offenders.isEmpty(),
                "the legacy call.* engine package must be fully unbound after the finishing migration; "
                        + "offenders: " + offenders);
    }

    @Test
    @DisplayName("the legacy call package directory no longer exists on disk")
    void legacyPackageDeleted() {
        var legacyDir = cobaltSourceRoot().resolve("call");
        assertFalse(Files.isDirectory(legacyDir),
                "the legacy call/ package must have been removed by the Delete phase, found at " + legacyDir);
    }

    @Test
    @DisplayName("the module exports calls.stream and no longer exports call.stream")
    void moduleExportsStreamFromCalls2() {
        var moduleInfo = libSrcMainJava().resolve("module-info.java");
        var src = read(moduleInfo);
        var code = stripCommentsAndStrings(src);
        assertTrue(code.contains("exports com.github.auties00.cobalt.calls.stream"),
                "module-info must export the calls stream package so the public call API types are visible");
        assertFalse(Pattern.compile("exports\\s+com\\.github\\.auties00\\.cobalt\\.call\\.stream")
                        .matcher(code).find(),
                "module-info must no longer export the legacy call.stream package");
    }

    private void scan(Path areaRoot, Path cobaltRoot, List<String> offenders) {
        try (Stream<Path> files = Files.walk(areaRoot)) {
            files.filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        var code = stripCommentsAndStrings(read(p));
                        var rel = cobaltRoot.relativize(p).toString();
                        if (code.contains(LEGACY_PREFIX)) {
                            offenders.add(rel + " references " + LEGACY_PREFIX);
                        }
                        for (var seam : LEGACY_SEAM_SEGMENTS) {
                            if (code.contains(seam)) {
                                offenders.add(rel + " names the legacy seam segment " + seam);
                            }
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException("cannot scan " + areaRoot, e);
        }
    }

    /**
     * Resolves the {@code lib} module's {@code src/main/java} directory, probing both the module-local and
     * repository-root layouts and walking ancestors of {@code user.dir}, mirroring the resolution the calls
     * Signal-seam tests use so the scan works whether the suite runs per module or from the aggregator.
     */
    private static Path libSrcMainJava() {
        var suffix = Path.of("src", "main", "java", "com", "github", "auties00", "cobalt", "calls");
        var moduleSuffix = Path.of("modules", "lib").resolve(suffix);
        var start = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (var dir = start; dir != null; dir = dir.getParent()) {
            var local = dir.resolve(suffix);
            if (Files.isDirectory(local)) {
                return upToSrcMainJava(local);
            }
            var fromRepoRoot = dir.resolve(moduleSuffix);
            if (Files.isDirectory(fromRepoRoot)) {
                return upToSrcMainJava(fromRepoRoot);
            }
        }
        throw new IllegalStateException("could not locate the lib source tree from user.dir=" + start);
    }

    /**
     * Walks a {@code .../src/main/java/com/github/auties00/cobalt/calls} path back up to its
     * {@code src/main/java} root by stripping the five package segments
     * {@code com/github/auties00/cobalt/calls}.
     */
    private static Path upToSrcMainJava(Path callsDir) {
        var dir = callsDir;
        for (var i = 0; i < 5; i++) {
            dir = dir.getParent();
        }
        return dir;
    }

    private static Path cobaltSourceRoot() {
        return libSrcMainJava().resolve(Path.of("com", "github", "auties00", "cobalt"));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + path, e);
        }
    }

    /**
     * Removes block and line comments and the contents of string and char literals, so a legacy package name
     * appearing only in javadoc, an inline comment, or a log/exception message is not treated as a real type
     * reference. Matches the comment-stripping the calls Signal-seam tests use, extended to also blank out
     * literal contents because an exception message may legitimately quote the old package name.
     */
    private static String stripCommentsAndStrings(String source) {
        var out = new StringBuilder(source.length());
        var inBlock = false;
        var inLine = false;
        var inString = false;
        var inChar = false;
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
            if (inString) {
                if (c == '\\') {
                    i++;
                    continue;
                }
                if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (inChar) {
                if (c == '\\') {
                    i++;
                    continue;
                }
                if (c == '\'') {
                    inChar = false;
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
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '\'') {
                inChar = true;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }
}
