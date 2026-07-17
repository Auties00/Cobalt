package com.github.auties00.cobalt.wam.processor.generator;

import com.github.auties00.cobalt.wam.binary.WamWireValue;
import com.github.auties00.cobalt.wam.processor.element.WamEventElement;
import com.github.auties00.cobalt.wam.processor.element.WamPropertyElement;
import com.github.auties00.cobalt.wam.model.WamType;
import com.palantir.javapoet.*;

import javax.lang.model.element.Modifier;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Generates the companion {@code *Builder} class for a {@code @WamEvent}-annotated
 * interface, providing a fluent API for constructing event instances.
 *
 * <p>For {@link WamType#TIMER TIMER} properties the builder additionally generates
 * {@code startXxx()} and {@code stopXxx()} methods that measure elapsed
 * milliseconds using {@link Instant#now()}.
 *
 * <p>The generated {@code build()} method returns the event interface type,
 * constructing the processor-generated {@code *Impl} class internally.
 */
public final class WamBuilderGenerator {
    private static final ClassName INTEGER = ClassName.get(Integer.class);
    private static final ClassName LONG = ClassName.get(Long.class);
    private static final ClassName BOOLEAN = ClassName.get(Boolean.class);
    private static final ClassName STRING = ClassName.get(String.class);
    private static final ClassName DOUBLE = ClassName.get(Double.class);
    private static final ClassName INSTANT = ClassName.get(Instant.class);
    private static final ClassName NAVIGABLE_MAP = ClassName.get(NavigableMap.class);
    private static final ClassName CONCURRENT_SKIP_LIST_MAP = ClassName.get(ConcurrentSkipListMap.class);
    private static final ClassName MAP = ClassName.get(Map.class);
    private static final ClassName HASH_MAP = ClassName.get(HashMap.class);
    private static final ClassName WAM_WIRE_VALUE = ClassName.get(WamWireValue.class);
    private static final ClassName WAM_WIRE_INT = WAM_WIRE_VALUE.nestedClass("WamInt");
    private static final ClassName WAM_WIRE_FLOAT = WAM_WIRE_VALUE.nestedClass("WamFloat");
    private static final ClassName WAM_WIRE_STRING = WAM_WIRE_VALUE.nestedClass("WamString");

    private WamBuilderGenerator() {
        throw new AssertionError();
    }

    /**
     * Generates a {@code JavaFile} containing the {@code *Builder} class for
     * the given event element.
     *
     * @param event the parsed event metadata
     * @return the generated Java file ready to be written
     */
    public static JavaFile generate(WamEventElement event) {
        if (event.properties().size() > WamImplGenerator.MAP_THRESHOLD) {
            return generateMapBased(event);
        }

        var builderName = event.className().simpleName() + "Builder";
        var builderClassName = ClassName.get(event.packageName(), builderName);
        var builderBuilder = TypeSpec.classBuilder(builderName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        // Fields
        for (var prop : event.properties()) {
            var fieldType = builderFieldType(prop);
            builderBuilder.addField(FieldSpec.builder(fieldType, prop.fieldName(), Modifier.PRIVATE).build());
        }

        // Transient start markers for TIMER fields
        for (var prop : event.properties()) {
            if (prop.wamType() == WamType.TIMER) {
                builderBuilder.addField(
                        FieldSpec.builder(INSTANT, prop.fieldName() + "Start", Modifier.PRIVATE).build()
                );
            }
        }

        // Fluent setter methods
        for (var prop : event.properties()) {
            var fieldType = resolveFieldType(prop);
            builderBuilder.addMethod(
                    MethodSpec.methodBuilder(prop.fieldName())
                            .addModifiers(Modifier.PUBLIC)
                            .returns(builderClassName)
                            .addParameter(fieldType, prop.fieldName())
                            .addStatement("this.$N = $N", prop.fieldName(), prop.fieldName())
                            .addStatement("return this")
                            .build()
            );

            // Timer start/stop helpers
            if (prop.wamType() == WamType.TIMER) {
                builderBuilder.addMethod(
                        MethodSpec.methodBuilder("start" + capitalize(prop.fieldName()))
                                .addModifiers(Modifier.PUBLIC)
                                .returns(builderClassName)
                                .addStatement("this.$NStart = $T.now()", prop.fieldName(), INSTANT)
                                .addStatement("return this")
                                .build()
                );
                builderBuilder.addMethod(
                        MethodSpec.methodBuilder("stop" + capitalize(prop.fieldName()))
                                .addModifiers(Modifier.PUBLIC)
                                .returns(builderClassName)
                                .beginControlFlow("if (this.$NStart != null)", prop.fieldName())
                                .addStatement("this.$N = $T.ofEpochMilli($T.now().toEpochMilli() - this.$NStart.toEpochMilli())",
                                        prop.fieldName(), INSTANT, INSTANT, prop.fieldName())
                                .addStatement("this.$NStart = null", prop.fieldName())
                                .endControlFlow()
                                .addStatement("return this")
                                .build()
                );
            }
        }

        builderBuilder.addMethod(generateBuildMethod(event));

        return JavaFile.builder(event.packageName(), builderBuilder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
    }

    private static MethodSpec generateBuildMethod(WamEventElement event) {
        var implClassName = ClassName.get(event.packageName(), event.className().simpleName() + "Impl");
        var method = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(event.className());

        var args = new StringBuilder();
        for (var i = 0; i < event.properties().size(); i++) {
            if (i > 0) {
                args.append(",\n        ");
            }
            args.append(event.properties().get(i).fieldName());
        }

        method.addStatement("return new $T(\n        $L\n)", implClassName, args.toString());
        return method.build();
    }

    /**
     * Generates a map-backed {@code *Builder} for an event whose field count
     * exceeds {@link WamImplGenerator#MAP_THRESHOLD}.
     *
     * <p>Setters write directly into a sorted {@code NavigableMap} of decoded
     * wire values rather than into per-field instance fields, so neither the
     * setters nor {@code build()} grow with the field count. The populated map
     * is handed to the matching map-based {@code *Impl} constructor.
     *
     * @param event the parsed event metadata
     * @return the generated Java file ready to be written
     */
    private static JavaFile generateMapBased(WamEventElement event) {
        var builderName = event.className().simpleName() + "Builder";
        var builderClassName = ClassName.get(event.packageName(), builderName);
        var implClassName = ClassName.get(event.packageName(), event.className().simpleName() + "Impl");
        var builderBuilder = TypeSpec.classBuilder(builderName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        var valuesType = ParameterizedTypeName.get(NAVIGABLE_MAP, INTEGER, WAM_WIRE_VALUE);
        builderBuilder.addField(FieldSpec.builder(valuesType, "values", Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", CONCURRENT_SKIP_LIST_MAP)
                .build());

        var hasTimer = event.properties().stream().anyMatch(prop -> prop.wamType() == WamType.TIMER);
        if (hasTimer) {
            var startsType = ParameterizedTypeName.get(MAP, INTEGER, INSTANT);
            builderBuilder.addField(FieldSpec.builder(startsType, "timerStarts", Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("new $T<>()", HASH_MAP)
                    .build());
        }

        for (var prop : event.properties()) {
            var fieldType = resolveFieldType(prop);
            var setter = MethodSpec.methodBuilder(prop.fieldName())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(builderClassName)
                    .addParameter(fieldType, prop.fieldName());
            addMapSetterBody(setter, prop);
            setter.addStatement("return this");
            builderBuilder.addMethod(setter.build());

            if (prop.wamType() == WamType.TIMER) {
                builderBuilder.addMethod(
                        MethodSpec.methodBuilder("start" + capitalize(prop.fieldName()))
                                .addModifiers(Modifier.PUBLIC)
                                .returns(builderClassName)
                                .addStatement("this.timerStarts.put($L, $T.now())", prop.index(), INSTANT)
                                .addStatement("return this")
                                .build()
                );
                builderBuilder.addMethod(
                        MethodSpec.methodBuilder("stop" + capitalize(prop.fieldName()))
                                .addModifiers(Modifier.PUBLIC)
                                .returns(builderClassName)
                                .addStatement("$T start = this.timerStarts.remove($L)", INSTANT, prop.index())
                                .beginControlFlow("if (start != null)")
                                .addStatement("this.values.put($L, new $T($T.now().toEpochMilli() - start.toEpochMilli()))",
                                        prop.index(), WAM_WIRE_INT, INSTANT)
                                .endControlFlow()
                                .addStatement("return this")
                                .build()
                );
            }
        }

        builderBuilder.addMethod(MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(event.className())
                .addStatement("return new $T(this.values)", implClassName)
                .build());

        WamImplGenerator.addEnumEncoders(builderBuilder, event);

        return JavaFile.builder(event.packageName(), builderBuilder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
    }

    private static void addMapSetterBody(MethodSpec.Builder setter, WamPropertyElement prop) {
        var name = prop.fieldName();
        // INTEGER setters take a primitive long, which cannot be null-checked, so
        // the value is stored unconditionally; the nullable boxed types only store
        // when a non-null value is supplied.
        var nullable = prop.wamType() != WamType.INTEGER;
        if (nullable) {
            setter.beginControlFlow("if ($N != null)", name);
        }
        switch (prop.wamType()) {
            case INTEGER -> setter.addStatement("this.values.put($L, new $T($N))", prop.index(), WAM_WIRE_INT, name);
            case BOOLEAN -> setter.addStatement("this.values.put($L, new $T($N ? 1L : 0L))", prop.index(), WAM_WIRE_INT, name);
            case STRING -> setter.addStatement("this.values.put($L, new $T($N))", prop.index(), WAM_WIRE_STRING, name);
            case FLOAT -> setter.addStatement("this.values.put($L, new $T($N))", prop.index(), WAM_WIRE_FLOAT, name);
            case TIMER -> setter.addStatement("this.values.put($L, new $T($N.toEpochMilli()))", prop.index(), WAM_WIRE_INT, name);
            case ENUM -> setter.addStatement("this.values.put($L, new $T($N($N)))",
                    prop.index(), WAM_WIRE_INT, WamImplGenerator.enumEncoderName(prop), name);
        }
        if (nullable) {
            setter.endControlFlow();
        }
    }

    /**
     * Returns the type of the builder's private backing field for the given
     * property.
     *
     * <p>This matches {@link #resolveFieldType(WamPropertyElement)} for every
     * type except {@link WamType#INTEGER}: the INTEGER setter parameter is a
     * primitive {@code long} for caller ergonomics, but its backing field stays
     * a boxed {@link Long} so that a property the caller never sets remains
     * {@code null} (absent on the wire) instead of defaulting to {@code 0} and
     * being encoded as a present zero. The boxed field also matches the boxed
     * {@code Long} constructor parameter of the generated {@code *Impl}.
     *
     * @param prop the parsed property metadata
     * @return the boxed backing-field type
     */
    private static TypeName builderFieldType(WamPropertyElement prop) {
        return prop.wamType() == WamType.INTEGER ? LONG : resolveFieldType(prop);
    }

    private static TypeName resolveFieldType(WamPropertyElement prop) {
        return switch (prop.wamType()) {
            case INTEGER -> TypeName.LONG;
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

    private static String capitalize(String name) {
        if (name.isEmpty()) {
            return name;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
