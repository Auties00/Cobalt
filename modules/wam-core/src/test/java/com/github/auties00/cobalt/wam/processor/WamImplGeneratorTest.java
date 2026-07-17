package com.github.auties00.cobalt.wam.processor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compile-time test for {@link WamEventProcessor}. Runs the
 * processor against a synthetic single-event source in-memory via
 * the {@link JavaCompiler} ToolProvider, captures the generated
 * {@code *Impl} and {@code *Builder} source files, and asserts
 * structural invariants the consumer-side bytecode tests can't see
 * directly (method signatures, field ordering, switch tables).
 *
 * <p>This catches regressions in the annotation-processor codegen
 * shape (e.g. an accidental rename of {@code markCommitted}, a
 * missing {@code @Override}, a dropped {@code decode} case) that
 * would otherwise only surface at the byte-output layer through
 * {@code WamGeneratedImplKatTest} — and only then if a captured
 * fixture happens to exercise the broken path.
 *
 * <p>The test is intentionally narrow: one event covering INTEGER,
 * STRING, and ENUM property kinds plus the {@code WamEventSpec}
 * mandatory members. Broader coverage is provided by the
 * consumer-side KATs.
 */
@DisplayName("WamEventProcessor codegen shape")
class WamImplGeneratorTest {
    /**
     * Synthetic event source: minimal {@code @WamEvent} interface
     * covering INTEGER, STRING, and ENUM property kinds.
     */
    private static final String SYNTHETIC_EVENT_SOURCE = """
            package generatortest;

            import com.github.auties00.cobalt.wam.annotation.WamEvent;
            import com.github.auties00.cobalt.wam.annotation.WamProperty;
            import com.github.auties00.cobalt.wam.model.WamChannel;
            import com.github.auties00.cobalt.wam.model.WamEventSpec;
            import com.github.auties00.cobalt.wam.model.WamType;
            import java.util.Optional;
            import java.util.OptionalLong;

            @WamEvent(id = 9001, channel = WamChannel.REGULAR)
            public interface SyntheticEvent extends WamEventSpec {
                @WamProperty(index = 1, type = WamType.INTEGER)
                OptionalLong counter();

                @WamProperty(index = 2, type = WamType.STRING)
                Optional<String> label();

                @WamProperty(index = 3, type = WamType.ENUM)
                Optional<SyntheticEnum> shape();
            }
            """;

    /**
     * Synthetic enum source used by the {@code shape()} property of
     * the synthetic event. Mirrors the
     * {@code @WamEnum}/{@code @WamEnumConstant} annotation contract
     * the processor relies on.
     */
    private static final String SYNTHETIC_ENUM_SOURCE = """
            package generatortest;

            import com.github.auties00.cobalt.wam.annotation.WamEnum;
            import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;

            @WamEnum
            public enum SyntheticEnum {
                @WamEnumConstant(1) CIRCLE,
                @WamEnumConstant(2) SQUARE,
                @WamEnumConstant(3) TRIANGLE
            }
            """;

    /**
     * Verifies that the processor generates a {@code SyntheticEventImpl}
     * whose source contains the documented public surface: {@code id()},
     * {@code channel()}, {@code releaseWeight()}, {@code sizeOf(int)},
     * {@code encode(WamEventEncoder, int)}, and the static {@code decode}
     * entry point.
     *
     * @throws IOException if the in-memory compilation fails to read
     *                     or write a source/resource entry
     */
    @Test
    @DisplayName("processor emits *Impl with the documented public surface")
    void implHasDocumentedSurface() throws IOException {
        var generated = runProcessor();
        var impl = requireGenerated(generated, "generatortest.SyntheticEventImpl");
        assertContains(impl, "public int id()");
        assertContains(impl, "return 9001");
        assertContains(impl, "public WamChannel channel()");
        assertContains(impl, "WamChannel.REGULAR");
        assertContains(impl, "public int releaseWeight()");
        assertContains(impl, "public int sizeOf(int weight)");
        assertContains(impl, "public void encode(");
        assertContains(impl, "static SyntheticEventImpl decode(");
        // markCommitted() one-shot guard
        assertContains(impl, "public boolean markCommitted()");
        // WamEventSpec id/channel match the annotation; not paranoid
        // about every line but the eventId integer must appear in the
        // event-marker emission too.
        assertContains(impl, "writeEventMarker(9001");
    }

    /**
     * Verifies that the processor emits a builder with a fluent
     * setter per property and a terminal {@code build()} method.
     *
     * @throws IOException if compilation fails
     */
    @Test
    @DisplayName("processor emits *Builder with one setter per @WamProperty + build()")
    void builderHasFluentSetters() throws IOException {
        var generated = runProcessor();
        var builder = requireGenerated(generated, "generatortest.SyntheticEventBuilder");
        assertContains(builder, "public SyntheticEventBuilder counter(");
        assertContains(builder, "public SyntheticEventBuilder label(");
        assertContains(builder, "public SyntheticEventBuilder shape(");
        assertContains(builder, "public SyntheticEvent build()");
    }

    /**
     * Verifies that the processor emits the per-field wire writes in
     * a deterministic alphabetical-getter-name order (which the
     * consumer-side KATs assume — see
     * {@code wam-events-multi-field.json}'s documentation comment).
     *
     * @throws IOException if compilation fails
     */
    @Test
    @DisplayName("encode emits fields in alphabetical-getter-name order")
    void encodeOrderIsAlphabetical() throws IOException {
        var generated = runProcessor();
        var impl = requireGenerated(generated, "generatortest.SyntheticEventImpl");

        // Names in alphabetical order: counter (1), label (2), shape (3).
        // The encode method's per-field `writeXxxField(<index>, ...)` calls
        // must therefore reference index 1 before 2 before 3.
        var counterIdx = impl.indexOf("writeIntField(1");
        var labelIdx = impl.indexOf("writeStringField(2");
        var shapeIdx = impl.indexOf("writeIntField(3");

        assertTrue(counterIdx >= 0, "counter field emit must be present");
        assertTrue(labelIdx >= 0, "label field emit must be present");
        assertTrue(shapeIdx >= 0, "shape field emit must be present");
        assertTrue(counterIdx < labelIdx,
                "counter (index 1) must encode before label (index 2)");
        assertTrue(labelIdx < shapeIdx,
                "label (index 2) must encode before shape (index 3)");
    }

    /**
     * Verifies that the registry's switch table includes the event's
     * numeric id mapped to its {@code *Impl.decode} entry.
     *
     * @throws IOException if compilation fails
     */
    @Test
    @DisplayName("WamEventRegistry switch table includes the synthetic event")
    void registryIncludesSyntheticEvent() throws IOException {
        var generated = runProcessor();
        var registry = requireGenerated(generated, "generatortest.WamEventRegistry");
        assertContains(registry, "case 9001");
        assertContains(registry, "SyntheticEventImpl.decode");
    }

    /**
     * Returns the source of the generated class, or fails with a
     * self-describing message if the processor skipped it.
     *
     * @param generated the captured source map
     * @param fqcn      the fully-qualified class name
     * @return the generated source code
     */
    private static String requireGenerated(Map<String, String> generated, String fqcn) {
        var src = generated.get(fqcn);
        if (src == null) {
            throw new AssertionError("processor did not generate " + fqcn
                    + "; generated keys: " + generated.keySet());
        }
        return src;
    }

    /**
     * Asserts the given source contains {@code expected} as a
     * substring, with a diagnostic message that helps locate the
     * miss.
     *
     * @param source   the captured generated source
     * @param expected the expected substring
     */
    private static void assertContains(String source, String expected) {
        if (!source.contains(expected)) {
            throw new AssertionError("generated source missing expected substring:\n  '"
                    + expected + "'\nsource:\n" + source);
        }
    }

    /**
     * Runs {@link WamEventProcessor} against the synthetic event
     * source via the JavaC {@link ToolProvider} and returns a map
     * of generated FQCN → source text.
     *
     * @return the captured generated source map
     * @throws IOException if the in-memory file manager throws
     */
    private static Map<String, String> runProcessor() throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "system Java compiler must be available; ToolProvider returned null");

        var diagnostics = new ArrayList<Diagnostic<? extends JavaFileObject>>();
        var standardFileManager = compiler.getStandardFileManager(diagnostics::add, null, StandardCharsets.UTF_8);
        try (var captured = new CapturingFileManager(standardFileManager)) {
            var sources = List.of(
                    sourceFile("generatortest.SyntheticEvent", SYNTHETIC_EVENT_SOURCE),
                    sourceFile("generatortest.SyntheticEnum", SYNTHETIC_ENUM_SOURCE));
            var task = compiler.getTask(
                    null,
                    captured,
                    diagnostics::add,
                    List.of("--enable-preview", "--release", "25"),
                    null,
                    sources);
            task.setProcessors(List.of(new WamEventProcessor()));
            var success = task.call();
            // Compilation may report a "preview features used" warning;
            // failures of the processor's own diagnostics should surface
            // as a non-true return value.
            if (!success) {
                var errors = new StringBuilder("compilation failed:\n");
                for (var d : diagnostics) {
                    if (d.getKind() == Diagnostic.Kind.ERROR) {
                        errors.append("  ").append(d.getMessage(null)).append("\n");
                    }
                }
                throw new AssertionError(errors.toString());
            }
            return captured.generatedSources();
        }
    }

    /**
     * Returns an in-memory {@link JavaFileObject} wrapping the
     * given source.
     *
     * @param fqcn   the fully-qualified class name
     * @param source the source text
     * @return the in-memory source file
     */
    private static JavaFileObject sourceFile(String fqcn, String source) {
        var path = fqcn.replace('.', '/') + ".java";
        return new SimpleJavaFileObject(URI.create("string:///" + path), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }

            @Override
            public InputStream openInputStream() {
                return new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public Reader openReader(boolean ignoreEncodingErrors) {
                return new StringReader(source);
            }
        };
    }

    /**
     * Forwarding {@link JavaFileManager} that captures every
     * source file the processor writes to {@link StandardLocation#SOURCE_OUTPUT},
     * keyed by its fully-qualified class name. Class files
     * (bytecode) are written to an in-memory location too so the
     * compilation completes; the test only consumes the captured
     * source map.
     */
    private static final class CapturingFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        /**
         * Captured generated source files keyed by FQCN.
         */
        private final Map<String, String> generatedSources = new LinkedHashMap<>();

        /**
         * Captured class files keyed by FQCN; the values are byte
         * sinks. The processor doesn't compile against these
         * directly — they exist so the JavaC pipeline completes
         * without complaining about a missing output target.
         */
        private final Map<String, ByteArrayOutputStream> classFiles = new HashMap<>();

        /**
         * Constructs a forwarding manager that captures generated
         * outputs alongside the delegated standard manager.
         *
         * @param delegate the delegated standard file manager
         */
        CapturingFileManager(JavaFileManager delegate) {
            super(delegate);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                   JavaFileObject.Kind kind, FileObject sibling) {
            if (location == StandardLocation.SOURCE_OUTPUT && kind == JavaFileObject.Kind.SOURCE) {
                return new InMemoryGeneratedSource(className);
            }
            if (kind == JavaFileObject.Kind.CLASS) {
                var sink = classFiles.computeIfAbsent(className, _ -> new ByteArrayOutputStream());
                return new InMemoryGeneratedClass(className, sink);
            }
            return new InMemoryGeneratedClass(className, new ByteArrayOutputStream());
        }

        /**
         * Returns the captured generated source map.
         *
         * @return the FQCN → source text map
         */
        Map<String, String> generatedSources() {
            return generatedSources;
        }

        /**
         * In-memory {@link JavaFileObject} that captures the
         * generated source bytes into {@link #generatedSources}.
         *
         * <p>Also overrides the read-side methods so that JavaC's
         * round-2 pass (which parses the just-written generated
         * sources to type-check them) can replay the captured
         * bytes without falling through to the
         * {@link SimpleJavaFileObject} default
         * {@link UnsupportedOperationException} throws.
         */
        private final class InMemoryGeneratedSource extends SimpleJavaFileObject {
            private final String fqcn;
            private final ByteArrayOutputStream sink = new ByteArrayOutputStream();

            InMemoryGeneratedSource(String fqcn) {
                super(URI.create("string:///out/" + fqcn.replace('.', '/') + ".java"), JavaFileObject.Kind.SOURCE);
                this.fqcn = fqcn;
            }

            @Override
            public OutputStream openOutputStream() {
                return new FilterOutputStream(sink) {
                    @Override
                    public void close() throws IOException {
                        out.close();
                        generatedSources.put(fqcn, sink.toString(StandardCharsets.UTF_8));
                    }
                };
            }

            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return sink.toString(StandardCharsets.UTF_8);
            }

            @Override
            public InputStream openInputStream() {
                return new ByteArrayInputStream(sink.toByteArray());
            }

            @Override
            public Reader openReader(boolean ignoreEncodingErrors) {
                return new StringReader(sink.toString(StandardCharsets.UTF_8));
            }
        }

        /**
         * In-memory bytecode sink used solely to let the compiler
         * write {@code .class} files; the test never reads them.
         */
        private static final class InMemoryGeneratedClass extends SimpleJavaFileObject {
            private final ByteArrayOutputStream sink;

            InMemoryGeneratedClass(String fqcn, ByteArrayOutputStream sink) {
                super(URI.create("mem:///out/" + fqcn.replace('.', '/') + ".class"), Kind.CLASS);
                this.sink = sink;
            }

            @Override
            public OutputStream openOutputStream() {
                return sink;
            }
        }
    }

    /**
     * Convenience self-test: a generated source map for a known
     * input should always be non-empty.
     *
     * @throws IOException if compilation fails
     */
    @Test
    @DisplayName("processor generates at least one source file for the synthetic event")
    void processorGeneratesSomething() throws IOException {
        var generated = runProcessor();
        assertTrue(!generated.isEmpty(),
                "WamEventProcessor must emit at least one generated source for a single @WamEvent input");
        // Names follow the convention; assert at least the three core artefacts.
        var keys = generated.keySet();
        assertTrue(keys.contains("generatortest.SyntheticEventImpl"),
                () -> "expected SyntheticEventImpl in " + keys);
        assertTrue(keys.contains("generatortest.SyntheticEventBuilder"),
                () -> "expected SyntheticEventBuilder in " + keys);
        assertTrue(keys.contains("generatortest.WamEventRegistry"),
                () -> "expected WamEventRegistry in " + keys);
    }

}
