package com.github.auties00.cobalt.wam;
import com.github.auties00.cobalt.wire.wam.event.*;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wam.WamFixtures;
import com.github.auties00.cobalt.wam.annotation.WamEnumConstant;
import com.github.auties00.cobalt.wam.binary.WamEventDecoder;
import com.github.auties00.cobalt.wam.binary.WamEventEncoder;
import com.github.auties00.cobalt.wam.binary.WamTags;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wire.wam.type.PsIdAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Byte-identical agreement tests for Cobalt's generated
 * {@code *EventBuilder} → {@code *Impl.encode} pipeline against
 * representative multi-field event vectors captured from the live
 * WhatsApp Web bundle.
 *
 * <p>Each scenario fixes a specific (eventId, weight, fields[]) tuple
 * and captures the bytes that {@code WAWebWamLibProtocol.writeEvent}
 * plus {@code .writeField} emits when called with that field sequence
 * in the order the Cobalt {@code *Impl.encode} method walks its
 * declared properties.
 *
 * <p>Scenarios with a named {@code cobaltClass} build the event
 * through the matching public Cobalt builder, exercising the full
 * code path Cobalt code uses in production. Scenarios with
 * {@code cobaltClass = "<synthetic>"} drive the encoder directly to
 * cover WIDE_ID / weight permutations that no shipping Cobalt event
 * happens to exercise.
 *
 * <p>Vectors live in {@code fixtures/wam/wam-events-multi-field.json}
 */
@DisplayName("WAM generated *Impl encode KAT")
class WamGeneratedImplKatTest {
    /**
     * Snapshot revision the vectors were captured against.
     */
    private static final long PINNED_SNAPSHOT_REVISION = 1039260921L;

    /**
     * Output buffer size, conservatively large for the synthetic
     * WIDE_ID + 4-field scenario.
     */
    private static final int MAX_BUFFER = 4_096;

    /**
     * Returns one dynamic test per captured scenario asserting the
     * encode direction (Cobalt {@code *EventBuilder} →
     * {@code *Impl.encode} → bytes).
     *
     * @return the test factory stream
     */
    @TestFactory
    List<DynamicTest> implBytesAgreeWithLiveBundle() {
        var fixture = WamFixtures.loadOracle("wam-events-multi-field");
        WamFixtures.requireSnapshotRevision(fixture, PINNED_SNAPSHOT_REVISION);
        var vectors = fixture.getJSONArray("vectors");
        var tests = new ArrayList<DynamicTest>(vectors.size());
        for (var entry : vectors) {
            var vector = (JSONObject) entry;
            tests.add(dynamicTest(vector.getString("name"), () -> assertScenarioAgrees(vector)));
        }
        return tests;
    }

    /**
     * Returns one dynamic test per Cobalt-backed scenario asserting
     * the decode direction (captured bytes →
     * {@link WamEventRegistry#decode}
     * → {@code *Impl} → re-encode → byte-identical with the input).
     *
     * <p>Synthetic scenarios are skipped because they don't have a
     * matching {@code *Impl} and the encoder direction has already
     * exercised the wire bytes directly.
     *
     * @return the test factory stream
     */
    @TestFactory
    List<DynamicTest> implRoundTripsBytes() {
        var fixture = WamFixtures.loadOracle("wam-events-multi-field");
        WamFixtures.requireSnapshotRevision(fixture, PINNED_SNAPSHOT_REVISION);
        var vectors = fixture.getJSONArray("vectors");
        var tests = new ArrayList<DynamicTest>();
        for (var entry : vectors) {
            var vector = (JSONObject) entry;
            if ("<synthetic>".equals(vector.getString("cobaltClass"))) {
                continue;
            }
            tests.add(dynamicTest("round-trip " + vector.getString("name"),
                    () -> assertScenarioRoundTrips(vector)));
        }
        return tests;
    }

    /**
     * Decodes the captured bytes via
     * {@link WamEventRegistry},
     * re-encodes the decoded spec at the same weight, and asserts
     * the re-encoded bytes are byte-identical to the captured input.
     *
     * <p>Catches a class of bugs where a generated
     * {@code *Impl.decode}'s switch table omits or misinterprets a
     * field id, or where {@code *Impl.encode} emits fields in a
     * different order than its own {@code decode} expects.
     *
     * @param scenario the captured scenario
     */
    private static void assertScenarioRoundTrips(JSONObject scenario) {
        var expectedHex = scenario.getString("bytes");
        var weight = scenario.getIntValue("weight");
        var bytes = HexFormat.of().parseHex(expectedHex);

        var decoder = WamEventDecoder.fromBytes(bytes, 0, bytes.length);
        var decoded = WamEventRegistry.decode(decoder);

        var reencodeBuffer = new byte[MAX_BUFFER];
        var encoder = WamEventEncoder.toBytes(reencodeBuffer);
        // The JS-emitted bytes carry the negated weight on the wire.
        // WamEventRegistry.decode consumes that payload but discards
        // the value; the re-encode must use the same captured weight
        // (negated to match writeEventMarker's sign convention).
        decoded.encode(encoder, -weight);

        var actualHex = HexFormat.of().formatHex(reencodeBuffer, 0, encoder.written());
        assertEquals(expectedHex, actualHex,
                () -> "decode → re-encode mismatch for " + scenario.getString("name"));
    }

    /**
     * Builds the scenario's event through its matching Cobalt builder
     * (or directly through the encoder for synthetic rows), encodes
     * it, and asserts the produced hex matches the captured bytes.
     *
     * @param scenario the captured scenario
     */
    private static void assertScenarioAgrees(JSONObject scenario) {
        var cobaltClass = scenario.getString("cobaltClass");
        var weight = scenario.getIntValue("weight");
        var expectedHex = scenario.getString("bytes");
        var fields = scenario.getJSONArray("fields");

        var buffer = new byte[MAX_BUFFER];
        var encoder = WamEventEncoder.toBytes(buffer);

        if ("<synthetic>".equals(cobaltClass)) {
            encodeSyntheticDirectly(encoder, scenario.getIntValue("eventId"), weight, fields);
        } else {
            var event = buildCobaltEvent(cobaltClass, fields);
            // The Cobalt encoder negates weight to write -weight on the
            // wire, matching the JS encoder's convention. The captured
            // bytes already reflect the negation, so pass the captured
            // value directly negated back.
            event.encode(encoder, -weight);
        }

        var written = encoder.written();
        var actualHex = HexFormat.of().formatHex(buffer, 0, written);
        assertEquals(expectedHex, actualHex,
                () -> "encoded bytes mismatch for " + scenario.getString("name"));
    }

    /**
     * Encodes the synthetic scenario by driving the encoder directly,
     * mirroring the per-field {@code writeEvent} + {@code writeField}
     * sequence the JS bundle produced.
     *
     * @param encoder the destination encoder
     * @param eventId the captured event id
     * @param weight  the captured weight (JS wire convention)
     * @param fields  the captured field list
     */
    private static void encodeSyntheticDirectly(WamEventEncoder encoder, int eventId, int weight, JSONArray fields) {
        var hasFields = fields != null && !fields.isEmpty();
        encoder.writeEventMarker(eventId, -weight, hasFields);
        if (!hasFields) {
            return;
        }
        var count = fields.size();
        for (var i = 0; i < count; i++) {
            var field = (JSONObject) fields.get(i);
            var hasMore = i < count - 1;
            var index = field.getIntValue("index");
            var type = field.getString("type");
            var rawValue = field.get("value");
            switch (type) {
                case "null" -> encoder.writeNull(index, WamTags.FIELD | (hasMore ? 0 : WamTags.LAST));
                case "int", "enum", "bool" -> encoder.writeIntField(index, ((Number) rawValue).longValue(), hasMore);
                case "float" -> encoder.writeFloatField(index, ((Number) rawValue).doubleValue(), hasMore);
                case "str" -> encoder.writeStringField(index, (String) rawValue, hasMore);
                default -> throw new IllegalStateException("unsupported synthetic field type: " + type);
            }
        }
    }

    /**
     * Constructs the matching Cobalt event by instantiating its
     * {@code <CobaltClass>Builder} via reflection, calling one setter
     * per captured field (keyed by {@code name}), and calling
     * {@code build()}.
     *
     * @param cobaltClass the simple Cobalt class name (e.g.
     *                    {@code "PsIdUpdateEvent"})
     * @param fields      the captured field list
     * @return the built event spec
     */
    private static WamEventSpec buildCobaltEvent(String cobaltClass, JSONArray fields) {
        try {
            var builderClassName = "com.github.auties00.cobalt.wire.wam.event." + cobaltClass + "Builder";
            var builderClass = Class.forName(builderClassName);
            var builder = builderClass.getConstructor().newInstance();
            for (var entry : fields) {
                var field = (JSONObject) entry;
                applyField(builder, builderClass, field);
            }
            var built = builderClass.getMethod("build").invoke(builder);
            return (WamEventSpec) built;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("failed to build Cobalt event " + cobaltClass + ": " + error, error);
        }
    }

    /**
     * Invokes the builder setter named {@code field.name} with the
     * value coerced to the setter's parameter type.
     *
     * @param builder      the builder instance
     * @param builderClass the builder's class
     * @param field        the captured field descriptor
     * @throws ReflectiveOperationException if the setter cannot be
     *                                      located or invoked
     */
    private static void applyField(Object builder, Class<?> builderClass, JSONObject field) throws ReflectiveOperationException {
        var setterName = field.getString("name");
        if (setterName == null) {
            throw new IllegalStateException("captured field has no 'name': " + field);
        }
        var setter = findSetter(builderClass, setterName);
        var paramType = setter.getParameterTypes()[0];
        var rawValue = field.get("value");
        var coerced = coerceValue(paramType, field.getString("type"), rawValue);
        setter.invoke(builder, coerced);
    }

    /**
     * Locates the single-arg builder setter named {@code name}.
     *
     * @param builderClass the builder's class
     * @param name         the setter name
     * @return the matching {@link Method}
     */
    private static Method findSetter(Class<?> builderClass, String name) {
        for (var method : builderClass.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) {
                return method;
            }
        }
        throw new IllegalStateException("no setter '" + name + "' on " + builderClass.getName());
    }

    /**
     * Coerces a captured value into the builder setter's declared
     * parameter type, handling the {@link PsIdAction} enum and the
     * {@code Boolean} / {@code Integer} boxing conventions.
     *
     * @param paramType the setter's parameter type
     * @param type      the captured field type label
     * @param rawValue  the captured raw value
     * @return the coerced value
     */
    private static Object coerceValue(Class<?> paramType, String type, Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (paramType == Boolean.class || paramType == boolean.class) {
            if (rawValue instanceof Number n) {
                return n.intValue() != 0;
            }
            return (Boolean) rawValue;
        }
        if (paramType == Integer.class || paramType == int.class) {
            return ((Number) rawValue).intValue();
        }
        if (paramType == Long.class || paramType == long.class) {
            return ((Number) rawValue).longValue();
        }
        if (paramType == Double.class || paramType == double.class) {
            return ((Number) rawValue).doubleValue();
        }
        if (paramType == Float.class || paramType == float.class) {
            return ((Number) rawValue).floatValue();
        }
        if (paramType == String.class) {
            return (String) rawValue;
        }
        if (paramType == PsIdAction.class) {
            return decodePsIdAction(((Number) rawValue).intValue());
        }
        if (paramType.isEnum() && "enum".equals(type)) {
            return decodeEnum(paramType, ((Number) rawValue).intValue());
        }
        throw new IllegalStateException("unsupported builder parameter type: " + paramType.getName() + " for value " + rawValue);
    }

    /**
     * Decodes a {@link PsIdAction} enum from its WAM wire value.
     *
     * @param wireValue the wire integer
     * @return the matching enum constant
     */
    private static PsIdAction decodePsIdAction(int wireValue) {
        return switch (wireValue) {
            case 1 -> PsIdAction.CREATED;
            case 2 -> PsIdAction.ROTATED;
            case 3 -> PsIdAction.DELETED;
            default -> throw new IllegalStateException("unknown PsIdAction wire value: " + wireValue);
        };
    }

    /**
     * Decodes a generic enum from a wire integer by scanning the
     * enum's {@code @WamEnumConstant}-annotated constants.
     *
     * <p>Used as a fallback when an enum type is encountered that this
     * test doesn't recognise by name; the
     * {@code @WamEnumConstant.value()} attribute holds the wire integer.
     *
     * @param enumType  the enum class
     * @param wireValue the wire integer
     * @return the matching enum constant
     */
    private static Object decodeEnum(Class<?> enumType, int wireValue) {
        for (var constant : enumType.getEnumConstants()) {
            try {
                var field = enumType.getField(((Enum<?>) constant).name());
                var ann = field.getAnnotation(WamEnumConstant.class);
                if (ann != null && ann.value() == wireValue) {
                    return constant;
                }
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new IllegalStateException("unknown enum wire value " + wireValue + " for " + enumType.getName());
    }
}
