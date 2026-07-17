package com.github.auties00.cobalt.wam.processor.generator;

import com.github.auties00.cobalt.wam.binary.WamEventDecoder;
import com.github.auties00.cobalt.wam.binary.WamEventEncoder;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.processor.element.WamEventElement;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.List;

/**
 * Generates the {@code WamEventRegistry} class containing a single
 * static {@code decode} switch over event ids and a mirror
 * {@code encode} that delegates to the polymorphic
 * {@link WamEventSpec#encode}.
 *
 * <p>The registry is emitted once per compilation unit, into the
 * package shared by all {@code @WamEvent} interfaces and their
 * package-private {@code *Impl} companions, so that the switch can
 * reference each generated impl directly.
 *
 * @see WamEventDecoder
 * @see WamEventEncoder
 */
public final class WamRegistryGenerator {
    private static final ClassName WAM_EVENT_DECODER = ClassName.get(WamEventDecoder.class);
    private static final ClassName WAM_EVENT_ENCODER = ClassName.get(WamEventEncoder.class);
    private static final ClassName WAM_EVENT_SPEC = ClassName.get(WamEventSpec.class);

    private WamRegistryGenerator() {
        throw new AssertionError();
    }

    /**
     * Generates the registry class for the given events.
     *
     * @param packageName the package to emit the registry into; must be
     *                    the same package as every {@code *Impl}
     *                    referenced from the switch
     * @param events      every discovered {@code @WamEvent} element,
     *                    sorted by event id for deterministic output
     * @return the generated Java file ready to be written
     */
    public static JavaFile generate(String packageName, List<WamEventElement> events) {
        var typeBuilder = TypeSpec.classBuilder("WamEventRegistry")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("Generated dispatch table for the WAM event hierarchy.\n");

        typeBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addStatement("throw new $T()", AssertionError.class)
                .build());

        typeBuilder.addMethod(buildDecode(packageName, events));
        typeBuilder.addMethod(buildEncode());

        return JavaFile.builder(packageName, typeBuilder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
    }

    private static MethodSpec buildDecode(String packageName, List<WamEventElement> events) {
        var method = MethodSpec.methodBuilder("decode")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(WAM_EVENT_SPEC)
                .addParameter(WAM_EVENT_DECODER, "decoder")
                .addJavadoc("Reads the next event from the decoder and dispatches to the\n")
                .addJavadoc("matching impl based on the wire event id.\n\n")
                .addJavadoc("@param decoder the source decoder, must not be {@code null}\n")
                .addJavadoc("@return the decoded event spec\n")
                .addJavadoc("@throws IllegalArgumentException if the event id is unknown\n");

        method.addStatement("int marker = decoder.readHeader()");
        method.addStatement("int eventId = $T.fieldIdOf(marker)", WAM_EVENT_DECODER);
        method.addComment("Consume the weight payload but discard the value: weight is");
        method.addComment("re-derived from the live sampling override on resume.");
        method.addStatement("decoder.readInt(marker)");
        method.addStatement("boolean hasFields = !$T.isLast(marker)", WAM_EVENT_DECODER);

        var body = CodeBlock.builder().add("return switch (eventId) {\n");
        for (var event : events) {
            var implName = ClassName.get(packageName, event.className().simpleName() + "Impl");
            body.add("    case $L -> $T.decode(decoder, hasFields);\n", event.eventId(), implName);
        }
        body.add("    default -> throw new $T(\"Unknown WAM event id: \" + eventId);\n",
                IllegalArgumentException.class);
        body.add("};\n");
        method.addCode(body.build());

        return method.build();
    }

    private static MethodSpec buildEncode() {
        return MethodSpec.methodBuilder("encode")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addParameter(WAM_EVENT_ENCODER, "encoder")
                .addParameter(WAM_EVENT_SPEC, "event")
                .addParameter(int.class, "weight")
                .addJavadoc("Writes an event into the encoder using the polymorphic encode\n")
                .addJavadoc("path. Provided as a mirror of {@link #decode} for symmetry; new\n")
                .addJavadoc("call sites should usually call {@code event.encode(encoder, weight)}\n")
                .addJavadoc("directly.\n\n")
                .addJavadoc("@param encoder the destination encoder, must not be {@code null}\n")
                .addJavadoc("@param event   the event to encode, must not be {@code null}\n")
                .addJavadoc("@param weight  the resolved sampling weight\n")
                .addStatement("event.encode(encoder, weight)")
                .build();
    }
}
