package com.github.auties00.cobalt.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.telemetry.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the platform-native shared libraries used by Cobalt's call layer
 * (Opus, libvpx, OpenH264, SCTP, FFmpeg, etc.).
 *
 * <p>Resolution proceeds in a fixed order until a binary is loaded:
 *
 * <ol>
 *   <li>Classpath bundle: looks up the binary at the path declared in the
 *       manifest entry (for example
 *       {@code modules/lib/natives/bin/linux-x86_64/libcobalt-native.so}),
 *       populated by the per-classifier {@code cobalt-VERSION-natives-<classifier>.jar}
 *       artifacts a consumer can opt into for offline / Maven-Central-only
 *       deployments.</li>
 *   <li>On-disk cache: checks
 *       {@code ${user.home}/.cobalt/natives/<cobalt-version>/<classifier>/<libname>},
 *       populated by a previous successful download.</li>
 *   <li>GitHub download: fetches the binary from the Cobalt source repository at
 *       the same path declared in the manifest, streams it through a
 *       {@link DigestInputStream} into a {@code .part} file under the cache,
 *       verifies the SHA-256 against the manifest, then atomically renames into
 *       place.</li>
 *   <li>System library: last-resort fallback via
 *       {@link System#loadLibrary(String)} for development on hosts where the
 *       library is package-managed (apt, Homebrew, similar).</li>
 * </ol>
 *
 * <p>The default {@code cobalt} JAR ships no binaries; it relies on the download
 * path. Bundled {@code cobalt:natives-<classifier>} JARs short-circuit the
 * download but only for their declared classifier; if the consumer somehow
 * picked the wrong classifier (for example shipped a {@code linux-x86_64}
 * fat-jar to an {@code aarch64} host), the loader detects the mismatch and
 * throws an explicit error rather than silently falling through to the slow
 * download path.
 *
 * <p>The {@code cobalt.natives.cache} system property overrides the on-disk
 * cache root (default {@code ${user.home}/.cobalt/natives}). Useful when
 * {@code $HOME} is read-only (some CI runners) or when sharing the cache between
 * several JVMs.
 */
public final class NativeLibLoader {
    /**
     * The logger for {@link NativeLibLoader}.
     */
    private static final System.Logger LOGGER = Log.get(NativeLibLoader.class);

    /**
     * Holds the resource path (inside the regular {@code cobalt} JAR) of the
     * SHA-256 + path manifest the download leg cross-checks every fetched binary
     * against.
     *
     * <p>{@link #loadManifests()} enumerates every module's manifest on the
     * classpath through this resource name. Without the manifest a tampered
     * upstream could silently substitute attacker-supplied natives.
     */
    private static final String MANIFEST_RESOURCE = "META-INF/native-checksums.json";

    /**
     * Holds the GitHub raw-content prefix the download leg fetches binaries
     * from.
     *
     * <p>Concatenated with the immutable commit SHA recorded in the manifest,
     * producing the URL
     * {@code https://raw.githubusercontent.com/Auties00/Cobalt/<sha>/<path>}.
     */
    private static final String REPO_BASE =
            "https://raw.githubusercontent.com/Auties00/Cobalt/";

    /**
     * Holds the system-property name that overrides the on-disk cache root.
     *
     * <p>Setting {@code -Dcobalt.natives.cache=<path>} on the JVM command line
     * relocates the cache; useful when {@code $HOME} is read-only or when sharing
     * the cache between several JVMs.
     */
    private static final String SYS_CACHE = "cobalt.natives.cache";

    /**
     * Holds the default cache directory under the user's home.
     *
     * <p>Used when the {@link #SYS_CACHE} system property is unset or blank.
     */
    private static final Path DEFAULT_CACHE_ROOT =
            Paths.get(System.getProperty("user.home", "."), ".cobalt", "cache", "natives");

    /**
     * Holds the classifiers Cobalt publishes natives for.
     *
     * <p>{@link #verifyNoForeignBundle(String, Entry)} reads this set to detect
     * wrong-classifier bundle mismatches at load time, and the build keeps the
     * per-classifier JAR coordinates in lock-step with it.
     */
    private static final String[] KNOWN_CLASSIFIERS = {
            "linux-x86_64",
            "linux-aarch64",
            "darwin-x86_64",
            "darwin-aarch64",
            "windows-x86_64",
            "windows-aarch64",
    };

    /**
     * Holds the connect timeout for the GitHub download leg.
     *
     * <p>Applied to the shared {@link HttpClient} built by {@link #httpClient()}.
     */
    private static final Duration HTTP_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Holds the per-request timeout for the GitHub download leg.
     *
     * <p>Applied to each {@link HttpRequest} issued by
     * {@link #downloadToCache(String, String, ManifestLookup, Path)}.
     */
    private static final Duration HTTP_REQUEST_TIMEOUT = Duration.ofSeconds(60);

    /**
     * Holds the length, in hex chars, of a SHA-256 digest.
     *
     * <p>{@link Entry#Entry(String, long, String)} validates against this length
     * so tampered manifests fail at parse time, not at download time.
     */
    private static final int SHA256_HEX_LENGTH = 64;

    /**
     * Holds the per-process cache of {@link SymbolLookup}s, keyed by the
     * platform-agnostic name passed to {@link #load(String, Arena)}.
     *
     * <p>Repeat calls for the same library name short-circuit through this cache
     * to avoid re-extracting and re-loading the binary.
     */
    private static final ConcurrentHashMap<String, SymbolLookup> CACHE = new ConcurrentHashMap<>();

    /**
     * Holds the Maven-style classifier of the running platform; one of
     * {@link #KNOWN_CLASSIFIERS}.
     *
     * <p>Computed once per JVM via {@link #computeClassifier()} from
     * {@code os.name} and {@code os.arch}.
     */
    private static final String CLASSIFIER = computeClassifier();

    /**
     * Holds the lazily-loaded list of every {@link #MANIFEST_RESOURCE} on the
     * classpath, one element per module's manifest.
     *
     * <p>{@link #lookupEntry(String, String)} scans the list to find the
     * manifest declaring a given {@code <lib>/<classifier>} key. The
     * {@code cobalt} module ships one manifest; the {@code cobalt-call-toolkit}
     * companion ships another; both coexist on the classpath at runtime.
     */
    private static volatile List<ModuleManifest> manifests;

    /**
     * Holds the lazily-resolved on-disk cache root.
     *
     * <p>Resolved on first call to {@link #cacheRoot()} so the {@link #SYS_CACHE}
     * system property can be set after class initialisation.
     */
    private static volatile Path cacheRoot;

    /**
     * Holds the lazily-built HTTP client used to fetch binaries from GitHub.
     *
     * <p>Constructed on first call to {@link #httpClient()}; held statically
     * because {@link HttpClient} is heavyweight and cheap to reuse across many
     * downloads.
     */
    private static volatile HttpClient httpClient;

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private NativeLibLoader() {
        throw new AssertionError("NativeLibLoader is not instantiable");
    }

    /**
     * Returns the running platform's Maven-style classifier (for example
     * {@code "linux-x86_64"}).
     *
     * <p>The value identifies the per-platform native bundle artifact coordinate
     * when wiring the loader into a build, and labels diagnostics that report why
     * a library failed to load.
     *
     * @return the classifier string
     */
    public static String classifier() {
        return CLASSIFIER;
    }

    /**
     * Loads the named native library and returns a {@link SymbolLookup} bound to
     * {@code arena}.
     *
     * <p>FFM bindings call this to obtain the {@link SymbolLookup} they pass to
     * {@link java.lang.foreign.Linker#downcallHandle(java.lang.foreign.MemorySegment, java.lang.foreign.FunctionDescriptor, java.lang.foreign.Linker.Option...)}.
     * {@link Arena#global()} is passed for the combined {@code cobalt-native}
     * library, which lives for the JVM lifetime. Repeat loads of the same
     * {@code libraryName} short-circuit through the per-process cache and may
     * pass any compatible arena.
     * {@snippet :
     *     var lookup = NativeLibLoader.load("cobalt-native", Arena.global());
     *     var encoderCreate = Linker.nativeLinker().downcallHandle(
     *         lookup.find("opus_encoder_create").orElseThrow(),
     *         FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS));
     * }
     *
     * @implNote
     * This implementation walks the resolution order
     * (classpath -> on-disk cache -> GitHub download -> system library), guarded
     * by a class-monitor critical section so concurrent callers do not race on
     * the cache and download paths. Library naming follows
     * {@link System#mapLibraryName(String)} on the system-library fallback.
     *
     * @param libraryName the platform-agnostic library name (for example
     *                    {@code "cobalt-native"})
     * @param arena       the arena bounding the lookup's lifetime; pass
     *                    {@link Arena#global()} for JVM-lifetime libraries
     * @return a {@link SymbolLookup} for the library's exports
     * @throws NullPointerException if any argument is {@code null}
     * @throws UnsatisfiedLinkError if the library cannot be located via
     *                              classpath, cache, download, or system fallback
     */
    public static SymbolLookup load(String libraryName, Arena arena) {
        Objects.requireNonNull(libraryName, "libraryName cannot be null");
        Objects.requireNonNull(arena, "arena cannot be null");
        var cached = CACHE.get(libraryName);
        if (cached != null) {
            return cached;
        }
        synchronized (NativeLibLoader.class) {
            cached = CACHE.get(libraryName);
            if (cached != null) {
                return cached;
            }
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "resolving native library {0} for classifier {1}", libraryName, CLASSIFIER);
            var path = resolve(libraryName);
            if (path != null) {
                System.load(path.toAbsolutePath().toString());
                var lookup = SymbolLookup.libraryLookup(path, arena);
                CACHE.put(libraryName, lookup);
                if (Log.INFO) LOGGER.log(Level.INFO, "loaded native library {0} from {1}", libraryName, path);
                return lookup;
            }
            try {
                System.loadLibrary(libraryName);
                var lookup = SymbolLookup.libraryLookup(System.mapLibraryName(libraryName), arena);
                CACHE.put(libraryName, lookup);
                if (Log.INFO) LOGGER.log(Level.INFO, "loaded native library {0} via system library fallback", libraryName);
                return lookup;
            } catch (UnsatisfiedLinkError e) {
                if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to load native library " + libraryName, e);
                throw new UnsatisfiedLinkError(
                        "could not load native library '" + libraryName + "' for classifier '"
                                + CLASSIFIER + "': " + describeFailure(libraryName)
                                + "; System.loadLibrary fallback also failed (" + e.getMessage() + ")");
            }
        }
    }

    /**
     * Resolves the binary for {@code libraryName} on disk, walking classpath,
     * cache, then GitHub download.
     *
     * <p>Backs {@link #load(String, Arena)}. Returns {@code null} when no
     * manifest entry is published for this library / classifier combination, so
     * the caller falls back to {@link System#loadLibrary(String)} (the
     * developer-on-Linux-with-libopus-installed scenario).
     *
     * @param libraryName the library name
     * @return the local path, or {@code null} when no manifest entry exists
     */
    private static Path resolve(String libraryName) {
        var lookup = lookupEntry(libraryName, CLASSIFIER).orElse(null);
        if (lookup == null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no manifest entry for {0}/{1}", libraryName, CLASSIFIER);
            return null;
        }
        var entry = lookup.entry();

        var fileName = basenameOf(entry.path());

        var classpathPath = extractFromClasspath(entry, fileName);
        if (classpathPath != null) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "resolved {0} from classpath bundle", libraryName);
            return classpathPath;
        }
        verifyNoForeignBundle(libraryName, entry);

        var cachePath = resolveCachePath(fileName);
        if (cachePath == null) {
            return null;
        }
        if (acceptIfMatchesEntry(cachePath, entry)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "resolved {0} from on-disk cache", libraryName);
            return cachePath;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cache miss for {0}, downloading", libraryName);
        return downloadToCache(libraryName, fileName, lookup, cachePath);
    }

    /**
     * Returns the trailing path component of a forward-slash separated manifest
     * path.
     *
     * <p>Recovers the canonical binary filename ({@code libavformat.so.61}) so
     * the cache and classpath extraction do not re-derive it from
     * {@link System#mapLibraryName(String)}, which strips the soname version
     * suffix.
     *
     * @param path the manifest path
     * @return the trailing component (basename)
     */
    private static String basenameOf(String path) {
        var idx = path.lastIndexOf('/');
        return idx < 0 ? path : path.substring(idx + 1);
    }

    /**
     * Tries to extract the binary from the classpath at the path declared in the
     * manifest entry.
     *
     * <p>Probes the same
     * {@code modules/lib/natives/bin/<classifier>/<filename>} path the
     * per-classifier JARs ship the binary under, matching the source-repo layout.
     * Returns the extracted path on success, or {@code null} when no resource
     * exists.
     *
     * @implNote
     * This implementation trusts an existing cache slot only when its bytes hash
     * to the manifest {@link Entry#sha256()} via
     * {@link #acceptIfMatchesEntry(Path, Entry)}; a same-size but stale binary
     * (for example one left by an earlier build of the same Cobalt version) fails
     * the check and is re-extracted. The freshly extracted bytes are digested in
     * the same copy pass and rejected if they do not match the manifest, so a
     * bundle whose binary and manifest disagree fails loudly here rather than
     * surfacing later as an ABI mismatch at the first native call. Extraction
     * writes to a unique temp sibling and atomically renames into place; if the
     * rename loses to a concurrent JVM that already left a matching binary (which
     * on Windows {@link System#load(String)} may hold locked without
     * {@code FILE_SHARE_DELETE}, throwing
     * {@link java.nio.file.AccessDeniedException}), that winner is accepted
     * rather than clobbering the locked file.
     *
     * @param entry    the manifest entry whose path to probe
     * @param fileName the platform-mapped filename
     * @return the extracted path, or {@code null} when no resource exists
     */
    private static Path extractFromClasspath(Entry entry, String fileName) {
        try {
            var loader = NativeLibLoader.class.getClassLoader();
            if (loader.getResource(entry.path()) == null) {
                return null;
            }
            var dir = ensureCacheDir();
            var out = dir.resolve(fileName);
            if (acceptIfMatchesEntry(out, entry)) {
                return out;
            }
            var tmp = Files.createTempFile(dir, fileName + ".part-", "");
            try {
                var digest = newSha256();
                try (var in = loader.getResourceAsStream(entry.path())) {
                    if (in == null) {
                        return null;
                    }
                    try (var digesting = new DigestInputStream(in, digest)) {
                        Files.copy(digesting, tmp, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                var actualSha = HexFormat.of().formatHex(digest.digest());
                if (!actualSha.equalsIgnoreCase(entry.sha256())) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "bundled native library checksum mismatch for {0}", entry.path());
                    throw new UnsatisfiedLinkError(
                            "bundled native library '" + entry.path()
                                    + "' SHA-256 mismatch: expected " + entry.sha256()
                                    + ", got " + actualSha);
                }
                try {
                    Files.move(tmp, out, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    return out;
                } catch (IOException moveFail) {
                    if (acceptIfMatchesEntry(out, entry)) {
                        return out;
                    }
                    throw moveFail;
                }
            } finally {
                deleteQuietly(tmp);
            }
        } catch (IOException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to extract bundled native library " + entry.path(), e);
            throw new UncheckedIOException(
                    "failed to extract bundled native library: " + entry.path(), e);
        }
    }

    /**
     * Returns a fresh SHA-256 {@link MessageDigest}.
     *
     * <p>Centralises the {@link NoSuchAlgorithmException} handling shared by
     * {@link #fileSha256(Path)} and the streaming verification in
     * {@link #extractFromClasspath(Entry, String)}.
     *
     * @return a new SHA-256 message digest
     * @throws IllegalStateException if the SHA-256 algorithm is not available in
     *                               this JVM
     */
    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Computes the lower-case hex SHA-256 of the file at {@code path}.
     *
     * <p>{@link #acceptIfMatchesEntry(Path, Entry)} uses the digest to verify an
     * already-on-disk binary against the manifest entry.
     *
     * @implNote
     * This implementation streams the file through a {@link DigestInputStream}
     * with a 64 KB buffer, so the multi-MB body is never held in memory.
     *
     * @param path the file to digest
     * @return the lower-case hex SHA-256 of the file's bytes
     * @throws IOException if the file cannot be read
     */
    private static String fileSha256(Path path) throws IOException {
        var digest = newSha256();
        try (var in = Files.newInputStream(path);
             var digesting = new DigestInputStream(in, digest)) {
            var buffer = new byte[1 << 16];
            while (digesting.read(buffer) != -1) {
                // read() feeds the digest as a side effect; the bytes are discarded
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * Throws when the classpath contains a binary for one of the other published
     * classifiers but not for the running one.
     *
     * <p>Pointing at the misconfiguration is cheaper than waiting for a multi-MB
     * pull on every cold start; this guard fires when the consumer picked the
     * wrong native bundle for their target platform.
     *
     * @implNote
     * This implementation derives the
     * {@code dependencies/<lib-dir>/bin/<x>/<file>} pattern from the manifest
     * entry's own path so it does not need a separate lib-name to lib-dir
     * mapping; the extension substitution covers the {@code .so} / {@code .dylib}
     * / {@code .dll} suffix swap when probing foreign bundles.
     *
     * @param libraryName the library name (for the diagnostic)
     * @param entry       the manifest entry for the running classifier
     */
    private static void verifyNoForeignBundle(String libraryName, Entry entry) {
        var ourPath = entry.path();
        var marker = "/bin/" + CLASSIFIER + "/";
        var idx = ourPath.indexOf(marker);
        if (idx < 0) {
            return;
        }
        var prefix = ourPath.substring(0, idx + "/bin/".length());
        var suffix = ourPath.substring(idx + marker.length());
        for (var other : KNOWN_CLASSIFIERS) {
            if (other.equals(CLASSIFIER)) {
                continue;
            }
            var probe = prefix + other + "/"
                    + suffix.replace(extensionFor(CLASSIFIER), extensionFor(other));
            if (NativeLibLoader.class.getClassLoader().getResource(probe) != null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "wrong native bundle classifier for {0}: found {1}, running classifier {2}", libraryName, probe, CLASSIFIER);
                throw new UnsatisfiedLinkError(
                        "wrong native bundle on classpath for library '" + libraryName
                                + "': found " + probe
                                + " but the running platform is '" + CLASSIFIER + "'."
                                + " Use the default `cobalt` artifact (downloads natives at runtime),"
                                + " or replace the natives JAR with `cobalt:natives-" + CLASSIFIER + "`.");
            }
        }
    }

    /**
     * Returns the on-disk cache path for {@code fileName} under
     * {@code <cache-root>/<cobalt-version>/<classifier>/<filename>}.
     *
     * <p>Used by {@link #resolve(String)} and
     * {@link #downloadToCache(String, String, ManifestLookup, Path)}. The version
     * segment ensures stale binaries from a prior install are not reused after an
     * upgrade.
     *
     * @param fileName the platform-mapped filename
     * @return the cache path, or {@code null} when the manifest is absent (no
     *         version to scope the cache by)
     */
    private static Path resolveCachePath(String fileName) {
        var version = anyManifestVersion();
        if (version == null) {
            return null;
        }
        try {
            var dir = cacheRoot().resolve(version).resolve(CLASSIFIER);
            Files.createDirectories(dir);
            return dir.resolve(fileName);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "failed to create cache directory for " + fileName, e);
        }
    }

    /**
     * Downloads the binary for {@code libraryName} from GitHub, streams it to a
     * {@code .part} file under the cache while digesting and size-counting in the
     * same pass, then atomically renames into place once the SHA-256 and size
     * match the manifest.
     *
     * <p>This is the last on-disk resolution step {@link #resolve(String)} tries
     * before the system-library fallback.
     *
     * @implNote
     * This implementation bounds memory by
     * {@link Files#copy(InputStream, Path, java.nio.file.CopyOption...)}'s 8 KB
     * buffer; the full binary is never held in memory. On post-download move
     * failure the loader accepts a concurrent winner of the right size rather
     * than retrying.
     *
     * @param libraryName the library name (for diagnostics)
     * @param fileName    the platform-mapped filename
     * @param lookup      the manifest entry plus the manifest that declared it
     *                    (so the URL pins to the right commit SHA)
     * @param cachePath   the destination path
     * @return the on-disk cache path with the verified binary
     * @throws UnsatisfiedLinkError if the download fails, the size mismatches, or
     *                              the checksum does not match
     */
    private static Path downloadToCache(String libraryName, String fileName,
                                        ManifestLookup lookup, Path cachePath) {
        var entry = lookup.entry();
        var commitSha = lookup.manifest().commitSha();
        if (commitSha == null || commitSha.isEmpty()) {
            throw new UnsatisfiedLinkError(
                    "checksum manifest " + lookup.manifest().source()
                            + " is missing the commitSha field; "
                            + "the release process must set it after committing the binaries");
        }
        if (acceptIfMatchesEntry(cachePath, entry)) {
            return cachePath;
        }
        var url = REPO_BASE + commitSha + "/" + entry.path();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "downloading native library {0} from {1}", libraryName, url);
        Path tmp;
        try {
            tmp = Files.createTempFile(cachePath.getParent(), fileName + ".part-", "");
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "failed to allocate download scratch for " + cachePath, e);
        }

        try {
            String actualSha;
            long actualSize;
            try {
                var response = httpClient().send(
                        HttpRequest.newBuilder(URI.create(url))
                                .timeout(HTTP_REQUEST_TIMEOUT)
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() / 100 != 2) {
                    throw new IOException("HTTP " + response.statusCode() + " from " + url);
                }
                var digest = MessageDigest.getInstance("SHA-256");
                try (var body = response.body();
                     var digesting = new DigestInputStream(body, digest)) {
                    Files.copy(digesting, tmp, StandardCopyOption.REPLACE_EXISTING);
                }
                actualSha = HexFormat.of().formatHex(digest.digest());
                actualSize = Files.size(tmp);
            } catch (IOException e) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "download failed for " + libraryName + " from " + url, e);
                throw new UnsatisfiedLinkError(
                        "failed to download '" + url + "' for native library '" + libraryName
                                + "': " + e.getMessage()
                                + "; add the cobalt:natives-" + CLASSIFIER
                                + " classifier JAR to the classpath to skip the download.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UnsatisfiedLinkError(
                        "interrupted while downloading '" + libraryName + "' from " + url);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 unavailable", e);
            }

            if (actualSize != entry.size()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "downloaded size mismatch for {0}: expected {1} got {2}", libraryName, entry.size(), actualSize);
                throw new UnsatisfiedLinkError(
                        "downloaded size mismatch for '" + libraryName + "': expected "
                                + entry.size() + " bytes, got " + actualSize);
            }
            if (!actualSha.equalsIgnoreCase(entry.sha256())) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "downloaded checksum mismatch for {0}", libraryName);
                throw new UnsatisfiedLinkError(
                        "SHA-256 mismatch for '" + libraryName + "': expected "
                                + entry.sha256() + ", got " + actualSha);
            }
            try {
                Files.move(tmp, cachePath, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                if (acceptIfMatchesEntry(cachePath, entry)) {
                    return cachePath;
                }
                throw new UncheckedIOException(
                        "failed to commit cached native library to " + cachePath, e);
            }
            if (Log.INFO) LOGGER.log(Level.INFO, "downloaded native library {0}, {1} bytes", libraryName, actualSize);
            return cachePath;
        } finally {
            deleteQuietly(tmp);
        }
    }

    /**
     * Returns {@code true} when {@code cachePath} holds a binary whose size and
     * SHA-256 both match {@code entry}.
     *
     * <p>This is the single integrity gate for every already-on-disk binary: the
     * {@link #resolve(String)} cache fast-path, the
     * {@link #extractFromClasspath(Entry, String)} skip and post-move recovery,
     * and the {@link #downloadToCache(String, String, ManifestLookup, Path)} skip
     * and post-move recovery all defer to it. A cached binary is trusted only
     * when its bytes match the manifest, so a stale same-size file left by an
     * earlier build of the same Cobalt version is rejected and re-fetched rather
     * than loaded with a mismatched ABI.
     *
     * @implNote
     * This implementation gates on the cheap {@link Files#size(Path)} stat before
     * hashing: a wrong-size file is rejected without reading its body, and only a
     * size match triggers the {@link #fileSha256(Path)} pass. Each library is
     * resolved at most once per JVM (the {@link #CACHE} memoises the
     * {@link SymbolLookup}), so the hash runs once per library per process.
     *
     * @param cachePath the cache slot to probe
     * @param entry     the manifest entry to compare against
     * @return whether {@code cachePath} already holds a binary matching
     *         {@code entry}
     */
    private static boolean acceptIfMatchesEntry(Path cachePath, Entry entry) {
        try {
            if (!Files.exists(cachePath) || Files.size(cachePath) != entry.size()) {
                return false;
            }
            return fileSha256(cachePath).equalsIgnoreCase(entry.sha256());
        } catch (IOException _) {
            return false;
        }
    }

    /**
     * Returns the lazily-built shared {@link HttpClient}.
     *
     * <p>Held statically because the {@code java.net.http} client is heavyweight
     * (selector thread plus thread pool) and cheap to reuse across many
     * downloads.
     *
     * @return the client
     */
    private static HttpClient httpClient() {
        var c = httpClient;
        if (c != null) {
            return c;
        }
        synchronized (NativeLibLoader.class) {
            if (httpClient != null) {
                return httpClient;
            }
            httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(HTTP_CONNECT_TIMEOUT)
                    .build();
            return httpClient;
        }
    }

    /**
     * Returns the lazily-resolved on-disk cache root.
     *
     * <p>Honours the {@link #SYS_CACHE} system property; falls back to
     * {@link #DEFAULT_CACHE_ROOT} when the property is unset or blank.
     *
     * @return the cache root
     */
    private static Path cacheRoot() {
        var local = cacheRoot;
        if (local != null) {
            return local;
        }
        synchronized (NativeLibLoader.class) {
            if (cacheRoot != null) {
                return cacheRoot;
            }
            var override = System.getProperty(SYS_CACHE);
            cacheRoot = override != null && !override.isEmpty()
                    ? Paths.get(override)
                    : DEFAULT_CACHE_ROOT;
            return cacheRoot;
        }
    }

    /**
     * Ensures the per-classifier directory inside the cache root exists, even
     * when the manifest version is unknown.
     *
     * <p>{@link #extractFromClasspath(Entry, String)} uses the directory as the
     * extraction destination for classpath-bundled binaries; the path falls back
     * to a {@code dev} version segment when no manifest declares one (development
     * tree).
     *
     * @return the directory path
     * @throws IOException if the directory cannot be created
     */
    private static Path ensureCacheDir() throws IOException {
        var version = anyManifestVersion();
        var dir = cacheRoot().resolve(version != null ? version : "dev").resolve(CLASSIFIER);
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * Returns a one-line description of why the binary could not be found.
     *
     * <p>The final {@link UnsatisfiedLinkError} surfaced to the caller embeds
     * this description so the message points at the resolution leg that failed.
     *
     * @param libraryName the library name
     * @return the diagnostic
     */
    private static String describeFailure(String libraryName) {
        if (manifestEntry(libraryName, CLASSIFIER).isEmpty()) {
            return "no classpath bundle and no manifest entry; "
                    + "this classifier is not published";
        }
        return "no classpath bundle and download/cache failed";
    }

    /**
     * Returns the conventional library extension for {@code classifier}.
     *
     * <p>{@link #verifyNoForeignBundle(String, Entry)} uses the extension to
     * cross-translate filenames across classifiers when probing for
     * wrong-classifier bundles.
     *
     * @param classifier the classifier
     * @return the extension (with leading dot), or empty string when the
     *         classifier is unknown
     */
    private static String extensionFor(String classifier) {
        if (classifier.startsWith("linux-")) return ".so";
        if (classifier.startsWith("darwin-")) return ".dylib";
        if (classifier.startsWith("windows-")) return ".dll";
        return "";
    }

    /**
     * Best-effort delete of {@code path}.
     *
     * <p>Cleans up the {@code .part} file on any download failure so a retry is
     * not poisoned by partial state.
     *
     * @param path the path to delete
     */
    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException _) {
        }
    }

    /**
     * Looks up an entry for {@code libraryName} / {@code classifier} across every
     * loaded manifest.
     *
     * <p>Returns the entry plus the manifest that declared it; the manifest is
     * needed by {@link #downloadToCache(String, String, ManifestLookup, Path)} so
     * it can pin the URL to the right commit SHA (different modules ship
     * different binaries and may have been committed at different SHAs).
     *
     * @param libraryName the library name
     * @param classifier  the classifier
     * @return the lookup, or empty when no manifest declares the key
     */
    private static Optional<ManifestLookup> lookupEntry(String libraryName, String classifier) {
        var key = libraryName + "/" + classifier;
        for (var module : loadManifests()) {
            var entry = module.entries().get(key);
            if (entry != null) {
                return Optional.of(new ManifestLookup(module, entry));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns just the {@link Entry} for {@code libraryName} /
     * {@code classifier}, dropping the owning manifest.
     *
     * <p>A convenience wrapper for call sites that do not need the owning
     * manifest (the diagnostic path).
     *
     * @param libraryName the library name
     * @param classifier  the classifier
     * @return the entry, or empty when absent
     */
    private static Optional<Entry> manifestEntry(String libraryName, String classifier) {
        return lookupEntry(libraryName, classifier).map(ManifestLookup::entry);
    }

    /**
     * Returns the version of any loaded manifest.
     *
     * <p>Scopes the cache directory. When multiple modules ship manifests at
     * different versions, the cache scoping picks the first manifest's version;
     * collisions across versions are unlikely in practice (binary filenames are
     * unique per lib) and would just cause redundant downloads, not corruption.
     *
     * @return some loaded manifest's version, or {@code null} when no manifest is
     *         on the classpath
     */
    private static String anyManifestVersion() {
        var loaded = loadManifests();
        return loaded.isEmpty() ? null : loaded.get(0).version();
    }

    /**
     * Loads (and caches) every {@link #MANIFEST_RESOURCE} on the classpath.
     *
     * <p>Returns an empty list when no module ships a manifest; the loader then
     * treats every {@link #load(String, Arena)} call as
     * classpath-or-system-only with no download leg.
     *
     * @implNote
     * This implementation accepts identical entries across modules (two modules
     * shipping the same opus binary share an SHA) and rejects conflicting entries
     * (same key, different SHA) via {@link #verifyNoConflicts(List)}, naming both
     * manifests in the {@link IllegalStateException} message.
     *
     * @return the loaded manifests, or empty when none ship
     */
    private static List<ModuleManifest> loadManifests() {
        var local = manifests;
        if (local != null) {
            return local;
        }
        synchronized (NativeLibLoader.class) {
            if (manifests != null) {
                return manifests;
            }
            var loaded = new ArrayList<ModuleManifest>();
            try {
                var urls = NativeLibLoader.class.getClassLoader().getResources(MANIFEST_RESOURCE);
                while (urls.hasMoreElements()) {
                    var url = urls.nextElement();
                    try (var stream = url.openStream()) {
                        loaded.add(parseManifest(url.toString(), stream));
                    }
                }
            } catch (IOException e) {
                if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to enumerate native checksum manifests", e);
                throw new UncheckedIOException("failed to enumerate " + MANIFEST_RESOURCE, e);
            }
            verifyNoConflicts(loaded);
            manifests = Collections.unmodifiableList(loaded);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded {0} native checksum manifests", manifests.size());
            return manifests;
        }
    }

    /**
     * Throws an {@link IllegalStateException} if two manifests declare the same
     * {@code <lib>/<classifier>} key with different SHA-256.
     *
     * <p>A divergent SHA means somebody shipped a tampered binary or two
     * unrelated builds collided on a name. Identical entries are silently
     * allowed: a transitive dep plus a direct dep could each pull the same
     * module's natives.
     *
     * @param loaded the parsed manifests
     */
    static void verifyNoConflicts(List<ModuleManifest> loaded) {
        var seen = new HashMap<String, ModuleManifest>();
        for (var module : loaded) {
            for (var key : module.entries().keySet()) {
                var existingModule = seen.get(key);
                if (existingModule == null) {
                    seen.put(key, module);
                    continue;
                }
                var existingEntry = existingModule.entries().get(key);
                var newEntry = module.entries().get(key);
                if (!existingEntry.sha256().equalsIgnoreCase(newEntry.sha256())) {
                    if (Log.ERROR) LOGGER.log(Level.ERROR, "conflicting native checksum entries for key {0}", key);
                    throw new IllegalStateException(
                            "conflicting native-checksum entries for '" + key + "': "
                                    + existingModule.source() + " declares sha=" + existingEntry.sha256()
                                    + ", " + module.source() + " declares sha=" + newEntry.sha256());
                }
            }
        }
    }

    /**
     * Parses one manifest's JSON body into a {@link ModuleManifest}.
     *
     * <p>Used by {@link #loadManifests()} and by the test suite to parse
     * synthetic manifest fixtures.
     *
     * @param source human-readable origin (the resource URL string), used only
     *               in conflict messages
     * @param json   the manifest body
     * @return the parsed manifest
     */
    static ModuleManifest parseManifest(String source, InputStream json) {
        var root = JSON.parseObject(json, StandardCharsets.UTF_8);
        var version = root.getString("version");
        var commitSha = root.getString("commitSha");
        var entries = new LinkedHashMap<String, Entry>();
        var binaries = root.getJSONObject("binaries");
        if (binaries != null) {
            for (var key : binaries.keySet()) {
                var obj = (JSONObject) binaries.get(key);
                entries.put(key, new Entry(
                        obj.getString("sha256"),
                        obj.getLongValue("size"),
                        obj.getString("path")));
            }
        }
        return new ModuleManifest(source, version, commitSha, Map.copyOf(entries));
    }

    /**
     * Computes the Maven-style classifier of the running JVM from
     * {@code os.name} and {@code os.arch}.
     *
     * <p>Runs once per JVM at class init to populate {@link #CLASSIFIER}.
     *
     * @return the classifier string (for example {@code "linux-x86_64"})
     * @throws IllegalStateException if the running OS or architecture is not
     *                               recognised
     */
    private static String computeClassifier() {
        var name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        var arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return osToken(name) + "-" + archToken(arch);
    }

    /**
     * Maps {@code os.name} to a stable OS token.
     *
     * <p>Drives {@link #computeClassifier()}. The token alphabet matches the
     * {@code linux-} / {@code darwin-} / {@code windows-} prefixes of
     * {@link #KNOWN_CLASSIFIERS}.
     *
     * @param name the lower-cased {@code os.name} value
     * @return one of {@code "linux"}, {@code "darwin"}, {@code "windows"}
     * @throws IllegalStateException if the OS is not supported
     */
    private static String osToken(String name) {
        if (name.contains("linux")) return "linux";
        if (name.contains("mac") || name.contains("darwin")) return "darwin";
        if (name.contains("windows")) return "windows";
        throw new IllegalStateException("unsupported os.name: " + name);
    }

    /**
     * Maps {@code os.arch} to a stable CPU token.
     *
     * <p>Drives {@link #computeClassifier()}. Folds the various aliases
     * ({@code amd64} / {@code x86_64}, {@code aarch64} / {@code arm64}) into the
     * canonical form used in {@link #KNOWN_CLASSIFIERS}.
     *
     * @param arch the lower-cased {@code os.arch} value
     * @return one of {@code "x86_64"}, {@code "aarch64"}
     * @throws IllegalStateException if the architecture is not supported
     */
    private static String archToken(String arch) {
        if (arch.equals("amd64") || arch.equals("x86_64") || arch.equals("x64")) return "x86_64";
        if (arch.equals("aarch64") || arch.equals("arm64")) return "aarch64";
        throw new IllegalStateException("unsupported os.arch: " + arch);
    }

    /**
     * Drops every cached {@link SymbolLookup}, forcing a fresh resolve on the
     * next {@link #load(String, Arena)} for any given library.
     *
     * <p>Useful for tests that swap the classpath or cache between runs, and for
     * applications that want to reclaim the native handles after a long-lived
     * call session ends. Dropping the {@link SymbolLookup} does not unload the
     * native library from the process address space; the OS loader continues to
     * hold any DLLs the prior {@link System#load(String)} call mapped in.
     */
    public static void clearCache() {
        synchronized (NativeLibLoader.class) {
            CACHE.clear();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cleared native library symbol lookup cache");
        }
    }

    /**
     * One manifest entry: the integrity and locator data for a single
     * platform-specific native binary.
     *
     * <p>Constructed by {@link #parseManifest(String, InputStream)} from one JSON
     * object under the {@code binaries.<lib>/<classifier>} key.
     *
     * @param sha256 lower-case hex SHA-256 of the binary's bytes
     * @param size   the binary's size in bytes
     * @param path   the binary's path inside the Cobalt repository (for example
     *               {@code "modules/lib/natives/bin/linux-x86_64/libcobalt-native.so"})
     */
    record Entry(String sha256, long size, String path) {
        /**
         * Validates the SHA-256 length, the non-negativity of {@code size}, and
         * rejects {@code null} for {@code sha256} and {@code path}.
         *
         * @throws NullPointerException     if {@code sha256} or {@code path} is
         *                                  {@code null}
         * @throws IllegalArgumentException if {@code sha256} length is not
         *                                  {@link #SHA256_HEX_LENGTH} or
         *                                  {@code size} is negative
         */
        Entry {
            Objects.requireNonNull(sha256, "sha256 cannot be null");
            Objects.requireNonNull(path, "path cannot be null");
            if (sha256.length() != SHA256_HEX_LENGTH) {
                throw new IllegalArgumentException(
                        "sha256 must be " + SHA256_HEX_LENGTH + " hex chars, got " + sha256.length());
            }
            if (size < 0) {
                throw new IllegalArgumentException("size must be non-negative");
            }
        }
    }

    /**
     * One module's parsed manifest.
     *
     * <p>The {@code cobalt} module ships one manifest, the
     * {@code cobalt-call-toolkit} companion ships another. Each declares its own
     * {@code version} and {@code commitSha} (since the binaries each module ships
     * were committed at potentially different repository revisions) and a map of
     * {@code <lib>/<classifier> -> Entry}.
     *
     * @param source    human-readable origin (the resource URL string), used only
     *                  in conflict messages
     * @param version   the module version that produced these binaries
     * @param commitSha the immutable commit SHA the binaries were committed at
     *                  (drives the download URL)
     * @param entries   the {@code <lib>/<classifier> -> Entry} map
     */
    record ModuleManifest(String source, String version, String commitSha,
                          Map<String, Entry> entries) {
        /**
         * Validates that {@code source} and {@code entries} are non-{@code null}.
         *
         * @throws NullPointerException if {@code source} or {@code entries} is
         *                              {@code null}
         */
        ModuleManifest {
            Objects.requireNonNull(source, "source cannot be null");
            Objects.requireNonNull(entries, "entries cannot be null");
        }
    }

    /**
     * The result of looking a {@code <lib>/<classifier>} key up across every
     * loaded manifest: the matching entry plus the manifest that declared it.
     *
     * <p>The manifest pointer lets
     * {@link #downloadToCache(String, String, ManifestLookup, Path)} pin the URL
     * to the right commit SHA; each module ships its natives at the commit that
     * introduced them, and the pointer carries that commit through to the URL
     * builder.
     *
     * @param manifest the manifest declaring this entry
     * @param entry    the entry itself
     */
    private record ManifestLookup(ModuleManifest manifest, Entry entry) {
        /**
         * Validates that both fields are non-{@code null}.
         *
         * @throws NullPointerException if {@code manifest} or {@code entry} is
         *                              {@code null}
         */
        ManifestLookup {
            Objects.requireNonNull(manifest, "manifest cannot be null");
            Objects.requireNonNull(entry, "entry cannot be null");
        }
    }
}
