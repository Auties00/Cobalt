package com.github.auties00.cobalt.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for {@link NativeLibLoader} and the FFM toolchain: classifier resolution, an
 * {@code abs(int)} linker round-trip against the running platform's C runtime, fail-fast on an
 * unmanifested library, parallel-extraction recovery against an OS-locked target, and manifest
 * conflict detection. The download-verify-cache-load success path is covered by the per-binding
 * smoke tests instead.
 */
public class NativeLibLoaderTest {

    // Mirrors NativeLibLoader.KNOWN_CLASSIFIERS minus the Windows-on-ARM64 entry, which is not a
    // published natives target.
    private static final Set<String> SUPPORTED_CLASSIFIERS = Set.of(
            "linux-x86_64", "linux-aarch64",
            "darwin-x86_64", "darwin-aarch64",
            "windows-x86_64");

    @Test
    public void classifierMatchesSupportedSet() {
        var classifier = NativeLibLoader.classifier();
        assertNotNull(classifier);
        assertTrue(SUPPORTED_CLASSIFIERS.contains(classifier),
                "classifier '" + classifier + "' is not in the supported set " + SUPPORTED_CLASSIFIERS);
    }

    @Test
    public void canBindAbsFromDefaultLookup() throws Throwable {
        var linker = Linker.nativeLinker();
        var defaultLookup = linker.defaultLookup();
        var absSymbol = defaultLookup.find("abs")
                .orElseThrow(() -> new AssertionError("abs not in default lookup"));
        var abs = linker.downcallHandle(
                absSymbol,
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
        assertEquals(42, (int) abs.invokeExact(-42));
        assertEquals(0, (int) abs.invokeExact(0));
        assertEquals(123, (int) abs.invokeExact(123));
    }

    @Test
    public void unmanifestedLibraryFailsFastWithoutDownload() {
        try {
            NativeLibLoader.clearCache();
            var error = assertThrows(UnsatisfiedLinkError.class,
                    () -> NativeLibLoader.load("nonexistent-test-lib", Arena.global()));
            assertTrue(error.getMessage().contains("nonexistent-test-lib"),
                    "error must mention the requested library: " + error.getMessage());
        } finally {
            NativeLibLoader.clearCache();
        }
    }

    // Regression: a second JVM that finds the combined library already locked by an earlier
    // System.load (Windows LoadLibrary opens it without FILE_SHARE_DELETE) must reuse the cached
    // binary rather than crash with AccessDeniedException trying to rewrite the locked file.
    // Reproduced in one JVM: load cobalt-native, then clearCache() drops the SymbolLookup cache
    // without unloading the library so the OS lock persists, then load again re-enters extraction
    // against the locked target like a parallel JVM.
    @Test
    public void parallelExtractionDoesNotFailWhenTargetIsLocked() {
        try {
            NativeLibLoader.load("cobalt-native", Arena.global());
            NativeLibLoader.clearCache();
            assertDoesNotThrow(() -> NativeLibLoader.load("cobalt-native", Arena.global()),
                    "second load on a target locked by an earlier System.load "
                            + "must reuse the cached binary, not re-extract over it");
        } finally {
            NativeLibLoader.clearCache();
        }
    }

    @Test
    public void disjointManifestsMergeWithoutConflict() {
        var libManifest = parseManifest(
                "lib.jar!/META-INF/native-checksums.json",
                readFixture("fixtures/native-lib/manifest-lib-opus.json"));
        var toolkitManifest = parseManifest(
                "toolkit.jar!/META-INF/native-checksums.json",
                readFixture("fixtures/native-lib/manifest-toolkit-ffmpeg-avformat.json"));
        NativeLibLoader.verifyNoConflicts(List.of(libManifest, toolkitManifest));
    }

    @Test
    public void conflictingManifestsThrow() {
        var first = parseManifest(
                "first.jar!/META-INF/native-checksums.json",
                readFixture("fixtures/native-lib/manifest-lib-opus.json"));
        var second = parseManifest(
                "second.jar!/META-INF/native-checksums.json",
                readFixture("fixtures/native-lib/manifest-lib-opus-conflicting-sha.json"));
        var thrown = assertThrows(IllegalStateException.class,
                () -> NativeLibLoader.verifyNoConflicts(List.of(first, second)));
        assertTrue(thrown.getMessage().contains("first.jar")
                        && thrown.getMessage().contains("second.jar"),
                "conflict message must name both manifests: " + thrown.getMessage());
    }

    @Test
    public void identicalEntriesAcrossManifestsAreAllowed() {
        var manifestJson = readFixture("fixtures/native-lib/manifest-lib-opus-identical.json");
        var first = parseManifest(
                "first.jar!/META-INF/native-checksums.json", manifestJson);
        var second = parseManifest(
                "second.jar!/META-INF/native-checksums.json", manifestJson);
        NativeLibLoader.verifyNoConflicts(List.of(first, second));
    }

    private static NativeLibLoader.ModuleManifest parseManifest(String source, String json) {
        return NativeLibLoader.parseManifest(source,
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    private static String readFixture(String resourcePath) {
        try (var in = NativeLibLoaderTest.class.getResourceAsStream("/" + resourcePath)) {
            if (in == null) {
                throw new IOException("fixture not found on classpath: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read fixture: " + resourcePath, e);
        }
    }
}
