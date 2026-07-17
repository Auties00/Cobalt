package com.github.auties00.cobalt.wam.processor.generator;

import com.github.auties00.cobalt.wam.binary.WamEventDecoder;
import com.github.auties00.cobalt.wam.binary.WamEventEncoder;
import com.github.auties00.cobalt.wam.binary.WamEventSizes;
import com.github.auties00.cobalt.wam.binary.WamWireValue;
import com.github.auties00.cobalt.wam.processor.element.WamEnumElement;
import com.github.auties00.cobalt.wam.processor.element.WamEventElement;
import com.github.auties00.cobalt.wam.processor.element.WamPropertyElement;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.palantir.javapoet.*;

import javax.lang.model.element.Modifier;
import java.time.Instant;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

/**
 * Generates the companion {@code *Impl} class for a {@code @WamEvent}-annotated
 * interface, implementing the interface methods and providing
 * high-performance {@code sizeOf}, {@code encode}, and {@code decode}
 * methods.
 *
 * <p>The generated code makes direct calls to {@link WamEventEncoder}
 * and {@link WamEventDecoder} instance methods with hardcoded field
 * identifiers and pre-determined types. Sizes come from
 * {@link WamEventSizes}. No reflection is involved at runtime.
 */
public final class WamImplGenerator {
    private static final ClassName WAM_EVENT_ENCODER = ClassName.get(WamEventEncoder.class);
    private static final ClassName WAM_EVENT_DECODER = ClassName.get(WamEventDecoder.class);
    private static final ClassName WAM_EVENT_SIZES = ClassName.get(WamEventSizes.class);
    private static final ClassName WAM_CHANNEL = ClassName.get(WamChannel.class);
    private static final ClassName OPTIONAL = ClassName.get(Optional.class);
    private static final ClassName OPTIONAL_LONG = ClassName.get(OptionalLong.class);
    private static final ClassName OPTIONAL_DOUBLE = ClassName.get(OptionalDouble.class);
    private static final ClassName INTEGER = ClassName.get(Integer.class);
    private static final ClassName LONG = ClassName.get(Long.class);
    private static final ClassName BOOLEAN = ClassName.get(Boolean.class);
    private static final ClassName STRING = ClassName.get(String.class);
    private static final ClassName DOUBLE = ClassName.get(Double.class);
    private static final ClassName INSTANT = ClassName.get(Instant.class);
    private static final ClassName NAVIGABLE_MAP = ClassName.get(NavigableMap.class);
    private static final ClassName WAM_WIRE_VALUE = ClassName.get(WamWireValue.class);
    private static final ClassName WAM_WIRE_INT = WAM_WIRE_VALUE.nestedClass("WamInt");
    private static final ClassName WAM_WIRE_FLOAT = WAM_WIRE_VALUE.nestedClass("WamFloat");
    private static final ClassName WAM_WIRE_STRING = WAM_WIRE_VALUE.nestedClass("WamString");

    /**
     * Field-count ceiling for the efficient per-field implementation. Events
     * with more properties than this generate a compact map-based impl
     * instead, because a per-field constructor would exceed the JVM's
     * 255-parameter limit and the inline {@code encode}/{@code decode} bodies
     * would exceed the 64KB per-method bytecode limit.
     */
    public static final int MAP_THRESHOLD = 100;

    private WamImplGenerator() {
        throw new AssertionError();
    }

    /**
     * Generates a {@code JavaFile} containing the {@code *Impl} class for
     * the given event element.
     *
     * @param event the parsed event metadata
     * @return the generated Java file ready to be written
     */
    public static JavaFile generate(WamEventElement event) {
        if (event.properties().size() > MAP_THRESHOLD) {
            return generateMapBased(event);
        }

        var implName = event.className().simpleName() + "Impl";
        var implBuilder = TypeSpec.classBuilder(implName)
                .addModifiers(Modifier.FINAL)
                .addSuperinterface(event.className());

        addCommittedField(implBuilder);
        addMetadataAccessors(implBuilder, event);
        addFields(implBuilder, event);
        addConstructor(implBuilder, event);
        addGetterOverrides(implBuilder, event);
        addMarkCommitted(implBuilder);
        addSizeOf(implBuilder, event);
        addEncode(implBuilder, event);
        addDecode(implBuilder, event);
        addEnumEncoders(implBuilder, event);
        addEnumDecoders(implBuilder, event);

        return JavaFile.builder(event.packageName(), implBuilder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
    }

    /**
     * Generates a compact, map-backed {@code *Impl} for an event whose field
     * count exceeds {@link #MAP_THRESHOLD}.
     *
     * <p>Instead of one field, parameter, and inline encode/decode branch per
     * property, the event's values live in a single sorted
     * {@code NavigableMap<Integer, WamWireValue>}. The {@code sizeOf},
     * {@code encode}, and {@code decode} methods delegate to the shared
     * generic helpers, so their bytecode size is independent of the field
     * count. Typed accessors read the map and reinterpret the wire value for
     * their declared {@link WamType}.
     *
     * @param event the parsed event metadata
     * @return the generated Java file ready to be written
     */
    private static JavaFile generateMapBased(WamEventElement event) {
        var implName = event.className().simpleName() + "Impl";
        var implBuilder = TypeSpec.classBuilder(implName)
                .addModifiers(Modifier.FINAL)
                .addSuperinterface(event.className());

        addCommittedField(implBuilder);
        addMetadataAccessors(implBuilder, event);
        addValuesField(implBuilder);
        addMapConstructor(implBuilder);
        addMapGetterOverrides(implBuilder, event);
        addMarkCommitted(implBuilder);
        addMapSizeOf(implBuilder, event);
        addMapEncode(implBuilder, event);
        addMapDecode(implBuilder, event);
        addEnumDecoders(implBuilder, event);

        return JavaFile.builder(event.packageName(), implBuilder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
    }

    private static void addMetadataAccessors(TypeSpec.Builder implBuilder, WamEventElement event) {
        implBuilder.addMethod(MethodSpec.methodBuilder("id")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return $L", event.eventId())
                .build());
        implBuilder.addMethod(MethodSpec.methodBuilder("channel")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(WAM_CHANNEL)
                .addStatement("return $T.$N", WAM_CHANNEL, event.channel().name())
                .build());
        implBuilder.addMethod(MethodSpec.methodBuilder("alphaWeight")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return $L", event.alphaWeight())
                .build());
        implBuilder.addMethod(MethodSpec.methodBuilder("betaWeight")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return $L", event.betaWeight())
                .build());
        implBuilder.addMethod(MethodSpec.methodBuilder("releaseWeight")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return $L", event.releaseWeight())
                .build());
        implBuilder.addMethod(MethodSpec.methodBuilder("privateStatsId")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addStatement("return $L", event.privateStatsId())
                .build());
    }

    private static void addCommittedField(TypeSpec.Builder implBuilder) {
        implBuilder.addField(FieldSpec.builder(boolean.class, "committed", Modifier.PRIVATE, Modifier.VOLATILE).build());
    }

    private static void addMarkCommitted(TypeSpec.Builder implBuilder) {
        implBuilder.addMethod(MethodSpec.methodBuilder("markCommitted")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(boolean.class)
                .beginControlFlow("if (this.committed)")
                .addStatement("return false")
                .endControlFlow()
                .addStatement("this.committed = true")
                .addStatement("return true")
                .build());
    }

    private static void addFields(TypeSpec.Builder implBuilder, WamEventElement event) {
        for (var prop : event.properties()) {
            var fieldType = rawFieldType(prop);
            implBuilder.addField(FieldSpec.builder(fieldType, prop.fieldName(), Modifier.PRIVATE, Modifier.FINAL).build());
        }
    }

    private static void addConstructor(TypeSpec.Builder implBuilder, WamEventElement event) {
        var ctor = MethodSpec.constructorBuilder();
        for (var prop : event.properties()) {
            var fieldType = rawFieldType(prop);
            ctor.addParameter(fieldType, prop.fieldName());
            ctor.addStatement("this.$N = $N", prop.fieldName(), prop.fieldName());
        }
        implBuilder.addMethod(ctor.build());
    }

    private static void addGetterOverrides(TypeSpec.Builder implBuilder, WamEventElement event) {
        for (var prop : event.properties()) {
            var method = MethodSpec.methodBuilder(prop.fieldName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(optionalReturnType(prop));
            addGetterBody(method, prop);
            implBuilder.addMethod(method.build());
        }
    }

    private static void addGetterBody(MethodSpec.Builder method, WamPropertyElement prop) {
        switch (prop.wamType()) {
            case INTEGER -> method.addStatement(
                    "return this.$N != null ? $T.of(this.$N) : $T.empty()",
                    prop.fieldName(), OPTIONAL_LONG, prop.fieldName(), OPTIONAL_LONG);
            case FLOAT -> method.addStatement(
                    "return this.$N != null ? $T.of(this.$N) : $T.empty()",
                    prop.fieldName(), OPTIONAL_DOUBLE, prop.fieldName(), OPTIONAL_DOUBLE);
            default -> method.addStatement(
                    "return $T.ofNullable(this.$N)",
                    OPTIONAL, prop.fieldName());
        }
    }

    private static void addSizeOf(TypeSpec.Builder implBuilder, WamEventElement event) {
        var method = MethodSpec.methodBuilder("sizeOf")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addParameter(int.class, "weight");

        method.addStatement("int size = $T.eventMarkerSize($L, weight)",
                WAM_EVENT_SIZES, event.eventId());

        for (var prop : event.properties()) {
            method.beginControlFlow("if (this.$N != null)", prop.fieldName());
            addSizeForProperty(method, prop);
            method.endControlFlow();
        }

        method.addStatement("return size");
        implBuilder.addMethod(method.build());
    }

    private static void addSizeForProperty(MethodSpec.Builder method, WamPropertyElement prop) {
        switch (prop.wamType()) {
            case INTEGER -> method.addStatement("size += $T.intFieldSize($L, this.$N)",
                    WAM_EVENT_SIZES, prop.index(), prop.fieldName());
            case BOOLEAN -> method.addStatement("size += $T.boolFieldSize($L)",
                    WAM_EVENT_SIZES, prop.index());
            case STRING -> method.addStatement("size += $T.stringFieldSize($L, this.$N)",
                    WAM_EVENT_SIZES, prop.index(), prop.fieldName());
            case FLOAT -> method.addStatement("size += $T.floatFieldSize($L)",
                    WAM_EVENT_SIZES, prop.index());
            case TIMER -> {
                method.beginControlFlow("if (this.$N.toEpochMilli() <= $L)", prop.fieldName(), Integer.MAX_VALUE);
                method.addStatement("size += $T.intFieldSize($L, this.$N.toEpochMilli())",
                        WAM_EVENT_SIZES, prop.index(), prop.fieldName());
                method.endControlFlow();
            }
            case ENUM -> method.addStatement("size += $T.intFieldSize($L, $N(this.$N))",
                    WAM_EVENT_SIZES, prop.index(), enumEncoderName(prop), prop.fieldName());
        }
    }

    private static void addEncode(TypeSpec.Builder implBuilder, WamEventElement event) {
        var method = MethodSpec.methodBuilder("encode")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(WAM_EVENT_ENCODER, "encoder")
                .addParameter(int.class, "weight");

        var properties = event.properties();

        // Find last non-null field index for LAST flag computation.
        // Timer fields exceeding Integer.MAX_VALUE are skipped on the wire,
        // so they must also be excluded from the last-field determination.
        method.addStatement("int lastField = -1");
        for (var i = properties.size() - 1; i >= 0; i--) {
            var prop = properties.get(i);
            if (i == properties.size() - 1) {
                if (prop.wamType() == WamType.TIMER) {
                    method.beginControlFlow("if (this.$N != null && this.$N.toEpochMilli() <= $L)",
                            prop.fieldName(), prop.fieldName(), Integer.MAX_VALUE);
                } else {
                    method.beginControlFlow("if (this.$N != null)", prop.fieldName());
                }
            } else {
                if (prop.wamType() == WamType.TIMER) {
                    method.nextControlFlow("else if (this.$N != null && this.$N.toEpochMilli() <= $L)",
                            prop.fieldName(), prop.fieldName(), Integer.MAX_VALUE);
                } else {
                    method.nextControlFlow("else if (this.$N != null)", prop.fieldName());
                }
            }
            method.addStatement("lastField = $L", i);
        }
        if (!properties.isEmpty()) {
            method.endControlFlow();
        }

        // Event marker
        method.addStatement(
                "encoder.writeEventMarker($L, weight, lastField >= 0)",
                event.eventId()
        );

        // Fields
        for (var i = 0; i < properties.size(); i++) {
            var prop = properties.get(i);
            method.beginControlFlow("if (this.$N != null)", prop.fieldName());
            method.addStatement("boolean hasMore = $L < lastField", i);
            addWriteForProperty(method, prop);
            method.endControlFlow();
        }

        implBuilder.addMethod(method.build());
    }

    private static void addWriteForProperty(MethodSpec.Builder method, WamPropertyElement prop) {
        switch (prop.wamType()) {
            case INTEGER -> method.addStatement(
                    "encoder.writeIntField($L, this.$N, hasMore)",
                    prop.index(), prop.fieldName());
            case BOOLEAN -> method.addStatement(
                    "encoder.writeBoolField($L, this.$N, hasMore)",
                    prop.index(), prop.fieldName());
            case STRING -> method.addStatement(
                    "encoder.writeStringField($L, this.$N, hasMore)",
                    prop.index(), prop.fieldName());
            case FLOAT -> method.addStatement(
                    "encoder.writeFloatField($L, this.$N, hasMore)",
                    prop.index(), prop.fieldName());
            case TIMER -> {
                method.beginControlFlow("if (this.$N.toEpochMilli() <= $L)", prop.fieldName(), Integer.MAX_VALUE);
                method.addStatement("encoder.writeIntField($L, this.$N.toEpochMilli(), hasMore)",
                        prop.index(), prop.fieldName());
                method.endControlFlow();
            }
            case ENUM -> method.addStatement(
                    "encoder.writeIntField($L, $N(this.$N), hasMore)",
                    prop.index(), enumEncoderName(prop), prop.fieldName());
        }
    }

    private static void addDecode(TypeSpec.Builder implBuilder, WamEventElement event) {
        var implName = event.className().simpleName() + "Impl";
        var implType = ClassName.get(event.packageName(), implName);

        var method = MethodSpec.methodBuilder("decode")
                .addModifiers(Modifier.STATIC)
                .returns(implType)
                .addParameter(WAM_EVENT_DECODER, "decoder")
                .addParameter(boolean.class, "hasFields");

        for (var prop : event.properties()) {
            method.addStatement("$T $N = null", rawFieldType(prop), prop.fieldName());
        }

        method.beginControlFlow("while (hasFields)");
        method.addStatement("int header = decoder.readHeader()");
        method.addStatement("int fieldId = $T.fieldIdOf(header)", WAM_EVENT_DECODER);
        method.beginControlFlow("switch (fieldId)");
        for (var prop : event.properties()) {
            switch (prop.wamType()) {
                case INTEGER -> method.addStatement(
                        "case $L -> $N = decoder.readInt(header)",
                        prop.index(), prop.fieldName());
                case BOOLEAN -> method.addStatement(
                        "case $L -> $N = decoder.readInt(header) != 0L",
                        prop.index(), prop.fieldName());
                case STRING -> method.addStatement(
                        "case $L -> $N = decoder.readString(header)",
                        prop.index(), prop.fieldName());
                case FLOAT -> method.addStatement(
                        "case $L -> $N = decoder.readFloat(header)",
                        prop.index(), prop.fieldName());
                case TIMER -> method.addStatement(
                        "case $L -> $N = $T.ofEpochMilli(decoder.readInt(header))",
                        prop.index(), prop.fieldName(), INSTANT);
                case ENUM -> method.addStatement(
                        "case $L -> $N = $N(decoder.readInt(header))",
                        prop.index(), prop.fieldName(), enumDecoderName(prop));
            }
        }
        method.addStatement("default -> decoder.skip(header)");
        method.endControlFlow();
        method.addStatement("hasFields = !$T.isLast(header)", WAM_EVENT_DECODER);
        method.endControlFlow();

        var ctorArgs = new StringBuilder();
        for (var i = 0; i < event.properties().size(); i++) {
            if (i > 0) {
                ctorArgs.append(", ");
            }
            ctorArgs.append(event.properties().get(i).fieldName());
        }
        method.addStatement("return new $T($L)", implType, ctorArgs.toString());

        implBuilder.addMethod(method.build());
    }

    private static TypeName valuesType() {
        return ParameterizedTypeName.get(NAVIGABLE_MAP, INTEGER, WAM_WIRE_VALUE);
    }

    private static void addValuesField(TypeSpec.Builder implBuilder) {
        implBuilder.addField(FieldSpec.builder(valuesType(), "values", Modifier.PRIVATE, Modifier.FINAL).build());
    }

    private static void addMapConstructor(TypeSpec.Builder implBuilder) {
        implBuilder.addMethod(MethodSpec.constructorBuilder()
                .addParameter(valuesType(), "values")
                .addStatement("this.values = values")
                .build());
    }

    private static void addMapGetterOverrides(TypeSpec.Builder implBuilder, WamEventElement event) {
        for (var prop : event.properties()) {
            var method = MethodSpec.methodBuilder(prop.fieldName())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(optionalReturnType(prop));
            addMapGetterBody(method, prop);
            implBuilder.addMethod(method.build());
        }
    }

    private static void addMapGetterBody(MethodSpec.Builder method, WamPropertyElement prop) {
        switch (prop.wamType()) {
            case INTEGER -> method.addStatement(
                    "return this.values.get($L) instanceof $T value ? $T.of(value.value()) : $T.empty()",
                    prop.index(), WAM_WIRE_INT, OPTIONAL_LONG, OPTIONAL_LONG);
            case BOOLEAN -> method.addStatement(
                    "return this.values.get($L) instanceof $T value ? $T.of(value.value() != 0L) : $T.empty()",
                    prop.index(), WAM_WIRE_INT, OPTIONAL, OPTIONAL);
            case STRING -> method.addStatement(
                    "return this.values.get($L) instanceof $T value ? $T.of(value.value()) : $T.empty()",
                    prop.index(), WAM_WIRE_STRING, OPTIONAL, OPTIONAL);
            case FLOAT -> method.addStatement(
                    "return this.values.get($L) instanceof $T value ? $T.of(value.value()) : $T.empty()",
                    prop.index(), WAM_WIRE_FLOAT, OPTIONAL_DOUBLE, OPTIONAL_DOUBLE);
            case TIMER -> method.addStatement(
                    "return this.values.get($L) instanceof $T value ? $T.of($T.ofEpochMilli(value.value())) : $T.empty()",
                    prop.index(), WAM_WIRE_INT, OPTIONAL, INSTANT, OPTIONAL);
            case ENUM -> method.addStatement(
                    "return this.values.get($L) instanceof $T value ? $T.ofNullable($N((int) value.value())) : $T.empty()",
                    prop.index(), WAM_WIRE_INT, OPTIONAL, enumDecoderName(prop), OPTIONAL);
        }
    }

    private static void addMapSizeOf(TypeSpec.Builder implBuilder, WamEventElement event) {
        implBuilder.addMethod(MethodSpec.methodBuilder("sizeOf")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(int.class)
                .addParameter(int.class, "weight")
                .addStatement("return $T.sizeOf($L, weight, this.values)", WAM_EVENT_SIZES, event.eventId())
                .build());
    }

    private static void addMapEncode(TypeSpec.Builder implBuilder, WamEventElement event) {
        implBuilder.addMethod(MethodSpec.methodBuilder("encode")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(WAM_EVENT_ENCODER, "encoder")
                .addParameter(int.class, "weight")
                .addStatement("encoder.writeEvent($L, weight, this.values)", event.eventId())
                .build());
    }

    private static void addMapDecode(TypeSpec.Builder implBuilder, WamEventElement event) {
        var implType = ClassName.get(event.packageName(), event.className().simpleName() + "Impl");
        implBuilder.addMethod(MethodSpec.methodBuilder("decode")
                .addModifiers(Modifier.STATIC)
                .returns(implType)
                .addParameter(WAM_EVENT_DECODER, "decoder")
                .addParameter(boolean.class, "hasFields")
                .addStatement("return new $T($T.readFields(decoder, hasFields))", implType, WAM_EVENT_DECODER)
                .build());
    }

    static void addEnumEncoders(TypeSpec.Builder implBuilder, WamEventElement event) {
        var generated = new HashSet<String>();
        for (var prop : event.properties()) {
            if (prop.wamType() != WamType.ENUM || prop.enumElement() == null) {
                continue;
            }
            var methodName = enumEncoderName(prop);
            if (!generated.add(methodName)) {
                continue;
            }
            implBuilder.addMethod(generateEnumEncoder(prop.enumElement(), methodName));
        }
    }

    private static MethodSpec generateEnumEncoder(WamEnumElement enumElement, String methodName) {
        var method = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(long.class)
                .addParameter(enumElement.className(), "value");

        var switchBuilder = CodeBlock.builder()
                .add("return switch (value) {\n");
        for (var entry : enumElement.constants().entrySet()) {
            switchBuilder.add("    case $N -> $LL;\n", entry.getKey(), entry.getValue());
        }
        switchBuilder.add("};\n");
        method.addCode(switchBuilder.build());

        return method.build();
    }

    private static void addEnumDecoders(TypeSpec.Builder implBuilder, WamEventElement event) {
        var generated = new HashSet<String>();
        for (var prop : event.properties()) {
            if (prop.wamType() != WamType.ENUM || prop.enumElement() == null) {
                continue;
            }
            var methodName = enumDecoderName(prop);
            if (!generated.add(methodName)) {
                continue;
            }
            implBuilder.addMethod(generateEnumDecoder(prop.enumElement(), methodName));
        }
    }

    private static MethodSpec generateEnumDecoder(WamEnumElement enumElement, String methodName) {
        var method = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(enumElement.className())
                .addParameter(long.class, "value");

        var switchBuilder = CodeBlock.builder()
                .add("return switch ((int) value) {\n");
        for (var entry : enumElement.constants().entrySet()) {
            switchBuilder.add("    case $L -> $T.$N;\n", entry.getValue(), enumElement.className(), entry.getKey());
        }
        switchBuilder.add("    default -> null;\n");
        switchBuilder.add("};\n");
        method.addCode(switchBuilder.build());

        return method.build();
    }

    private static TypeName rawFieldType(WamPropertyElement prop) {
        return switch (prop.wamType()) {
            case INTEGER -> LONG;
            case BOOLEAN -> BOOLEAN;
            case STRING -> STRING;
            case FLOAT -> DOUBLE;
            case TIMER -> INSTANT;
            case ENUM -> {
                var enumElement = prop.enumElement();
                if (enumElement == null) {
                    throw new IllegalStateException("ENUM property missing enum element: " + prop.fieldName());
                }
                yield enumElement.className();
            }
        };
    }

    private static TypeName optionalReturnType(WamPropertyElement prop) {
        return switch (prop.wamType()) {
            case INTEGER -> OPTIONAL_LONG;
            case FLOAT -> OPTIONAL_DOUBLE;
            default -> ParameterizedTypeName.get(OPTIONAL, rawFieldType(prop));
        };
    }

    static String enumEncoderName(WamPropertyElement prop) {
        var enumElement = prop.enumElement();
        if (enumElement == null) {
            throw new IllegalStateException("No enum element for property " + prop.fieldName());
        }
        return "encode" + enumElement.className().simpleName();
    }

    private static String enumDecoderName(WamPropertyElement prop) {
        var enumElement = prop.enumElement();
        if (enumElement == null) {
            throw new IllegalStateException("No enum element for property " + prop.fieldName());
        }
        return "decode" + enumElement.className().simpleName();
    }
}
