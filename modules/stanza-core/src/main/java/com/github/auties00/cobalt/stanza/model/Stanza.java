package com.github.auties00.cobalt.stanza.model;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.model.ProtobufString;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Represents an immutable stanza in a WhatsApp stanza tree.
 *
 * <p>Every message exchanged with the WhatsApp server is a tree of nodes
 * serialised through the WAWap binary protocol. A stanza carries a tag name (the
 * {@code description}), an insertion-ordered map of attributes, and an
 * optional content slot that holds either plain text, a single {@link Jid}, a
 * binary blob, or a list of child nodes. The sealed hierarchy mirrors the five
 * wire shapes a stanza can take and the read surface is exhaustive: nothing in
 * the protocol surfaces a stanza value that this interface cannot return.
 *
 * <p>The five concrete variants are:
 * <ul>
 *   <li>{@link EmptyStanza} for nodes without a content slot
 *   <li>{@link TextStanza} for nodes whose content is a UTF-8 string
 *   <li>{@link JidStanza} for nodes whose content is a single JID
 *   <li>{@link BytesStanza} for nodes whose content is a binary blob
 *   <li>{@link ContainerStanza} for nodes whose content is a child list
 * </ul>
 *
 * <p>Attribute and content readers come in three flavours: the {@code getXxx}
 * family returns an {@link Optional} or
 * {@link OptionalInt}/{@link OptionalLong}/{@link OptionalDouble} when absent;
 * the {@code getXxx(key, default)} family returns the value or a
 * caller-supplied fallback; the {@code getRequiredXxx} family throws when the
 * value is absent. Each family has a {@code streamXxx} mirror that yields a
 * single-element or empty stream so callers can compose conversions inside a
 * pipeline.
 *
 * <p>Inbound nodes are produced by {@code StanzaReader} from socket bytes;
 * outbound nodes are produced through {@link StanzaBuilder} and serialised by
 * {@code StanzaWriter}.
 *
 * <p>Reading attributes from an inbound stanza:
 * {@snippet :
 *     String id = stanza.getRequiredAttributeAsString("id");
 *     Jid from = stanza.getAttributeAsJid("from").orElse(null);
 *     boolean retry = stanza.getAttributeAsBool("retry", false);
 * }
 *
 * <p>Walking children:
 * {@snippet :
 *     Stanza body = stanza.getRequiredChild("body");
 *     List<Stanza> participants = stanza.streamChildren("participant").toList();
 *}
 *
 * @see StanzaBuilder
 * @see StanzaAttribute
 */
@WhatsAppWebModule(moduleName = "WAWap")
@WhatsAppWebModule(moduleName = "WAXmlNode")
@WhatsAppWebModule(moduleName = "WASmaxChildren")
public sealed interface Stanza extends LogRedactable {
    /**
     * Returns the description (tag name) of this stanza.
     *
     * <p>Identifies the stanza shape (for example {@code "iq"},
     * {@code "message"}, {@code "presence"}, {@code "receipt"}). Never
     * {@code null} for a valid stanza.
     *
     * @return the non null tag name
     */
    String description();

    /**
     * Returns whether this stanza's description equals the supplied value.
     *
     * <p>Lets routing code (children walkers, dispatch tables) branch on the
     * tag without calling {@link Objects#equals(Object, Object)} explicitly.
     *
     * @param description the description to compare against
     * @return {@code true} when the descriptions match
     */
    default boolean hasDescription(String description) {
        return Objects.equals(description(), description);
    }

    /**
     * Returns a redacted structural summary of this stanza for the logging pipeline.
     *
     * <p>The summary keeps the description, the attribute keys, the child count, and whether a content slot is
     * present, all of which are protocol structure. Attribute values, textual and binary content, and the
     * recursive contents of child nodes are omitted entirely, since any of them can carry a JID, a token, or
     * message payload.
     *
     * @return a summary of the form {@code stanza(description attrs=[k1, k2] children=N)}
     */
    @Override
    default String toRedactedLog() {
        var result = new StringBuilder("stanza(");
        result.append(description());
        var attributes = attributes();
        if (!attributes.isEmpty()) {
            result.append(" attrs=").append(attributes.keySet());
        }
        var children = children().size();
        if (children > 0) {
            result.append(" children=").append(children);
        } else if (hasContent()) {
            result.append(" content=<redacted>");
        }
        return result.append(')').toString();
    }

    /**
     * Returns the attributes attached to this stanza in insertion order.
     *
     * <p>The map is unmodifiable; callers must not attempt to add or remove
     * entries. Insertion order is preserved so re-encoded stanzas round-trip
     * stably even though the server is order-agnostic on the semantic level.
     *
     * @return an unmodifiable sequenced map of attribute names to values
     */
    SequencedMap<String, StanzaAttribute> attributes();

    /**
     * Returns the attribute associated with the supplied key, if any.
     *
     * <p>The base lookup; every other {@code getAttributeAs...} helper is a
     * typed view over the result of this call.
     *
     * @param key the attribute key to look up
     * @return an {@link Optional} that holds the matching attribute, or empty
     *         when absent
     * @throws NullPointerException if {@code key} is {@code null}
     */
    default Optional<StanzaAttribute> getAttribute(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return Optional.ofNullable(attributes().get(key));
    }

    /**
     * Returns the value of the supplied attribute as a string.
     *
     * <p>Delegates to {@link StanzaAttribute#toString()}; usable for every
     * attribute variant.
     *
     * @param key the attribute key
     * @return an {@link Optional} that holds the string value, or empty when
     *         the attribute is absent
     */
    default Optional<String> getAttributeAsString(String key) {
        return getAttribute(key)
                .map(StanzaAttribute::toString);
    }

    /**
     * Returns the value of the supplied attribute parsed as a boolean under the
     * {@link StanzaBooleanFormat#LENIENT} format.
     *
     * <p>Convenience for {@link #getAttributeAsBool(String, StanzaBooleanFormat)}
     * with the lenient format, which reads both the {@code "true"}/{@code "false"}
     * and the {@code "1"}/{@code "0"} wire conventions. Pass an explicit format
     * when a stanza family must reject one of those conventions.
     *
     * @param key the attribute key
     * @return an {@link Optional} that holds the parsed boolean, or empty when
     *         the attribute is absent
     */
    default Optional<Boolean> getAttributeAsBool(String key) {
        return getAttributeAsBool(key, StanzaBooleanFormat.LENIENT);
    }

    /**
     * Returns the value of the supplied attribute parsed as a boolean under the
     * supplied {@link StanzaBooleanFormat}.
     *
     * <p>Decodes the attribute's textual view through
     * {@link StanzaBooleanFormat#decode(String)}; the WhatsApp wire protocol
     * carries booleans as strings and the literal denoting truth differs by
     * stanza family, so the format selects which convention applies.
     *
     * @param key    the attribute key
     * @param format the format used to decode the textual view
     * @return an {@link Optional} that holds the parsed boolean, or empty when
     *         the attribute is absent
     * @throws NullPointerException if {@code format} is {@code null}
     */
    default Optional<Boolean> getAttributeAsBool(String key, StanzaBooleanFormat format) {
        Objects.requireNonNull(format, "format cannot be null");
        return getAttribute(key)
                .map(StanzaAttribute::toString)
                .map(format::decode);
    }

    /**
     * Returns the value of the supplied attribute as a string, falling back to
     * {@code defaultValue} when absent.
     *
     * @param key          the attribute key
     * @param defaultValue the fallback returned when the attribute is absent
     * @return the attribute value or the fallback
     */
    default String getAttributeAsString(String key, String defaultValue) {
        return getAttributeAsString(key)
                .orElse(defaultValue);
    }

    /**
     * Returns the value of the supplied attribute parsed as a boolean under the
     * {@link StanzaBooleanFormat#LENIENT} format, falling back to
     * {@code defaultValue} when absent.
     *
     * @param key          the attribute key
     * @param defaultValue the fallback returned when the attribute is absent
     * @return the parsed boolean or the fallback
     */
    default boolean getAttributeAsBool(String key, boolean defaultValue) {
        return getAttributeAsBool(key, defaultValue, StanzaBooleanFormat.LENIENT);
    }

    /**
     * Returns the value of the supplied attribute parsed as a boolean under the
     * supplied {@link StanzaBooleanFormat}, falling back to {@code defaultValue}
     * when absent.
     *
     * @param key          the attribute key
     * @param defaultValue the fallback returned when the attribute is absent
     * @param format       the format used to decode the textual view
     * @return the parsed boolean or the fallback
     * @throws NullPointerException if {@code format} is {@code null}
     */
    default boolean getAttributeAsBool(String key, boolean defaultValue, StanzaBooleanFormat format) {
        return getAttributeAsBool(key, format)
                .orElse(defaultValue);
    }

    /**
     * Returns the value of the supplied attribute as a byte array.
     *
     * <p>Delegates to {@link StanzaAttribute#toBytes()}; the result is a fresh
     * array for {@link StanzaAttribute.TextAttribute} and
     * {@link StanzaAttribute.JidAttribute} but is shared with the underlying
     * {@link StanzaAttribute.BytesAttribute} instance, which the caller must not
     * mutate.
     *
     * @param key the attribute key
     * @return an {@link Optional} that holds the byte array, or empty when the
     *         attribute is absent
     */
    default Optional<byte[]> getAttributeAsBytes(String key) {
        return getAttribute(key)
                .map(StanzaAttribute::toBytes);
    }

    /**
     * Returns the value of the supplied attribute as a byte array, falling
     * back to {@code defaultValue} when absent.
     *
     * @param key          the attribute key
     * @param defaultValue the fallback returned when the attribute is absent
     * @return the byte array or the fallback
     */
    default byte[] getAttributeAsBytes(String key, byte[] defaultValue) {
        return getAttribute(key)
                .map(StanzaAttribute::toBytes)
                .orElse(defaultValue);
    }

    /**
     * Returns the value of the supplied attribute parsed as a {@link Jid}.
     *
     * <p>Delegates to {@link StanzaAttribute#toJid()}; always succeeds for
     * {@link StanzaAttribute.JidAttribute} and tries a parse for the textual and
     * binary variants.
     *
     * @param key the attribute key
     * @return an {@link Optional} that holds the parsed JID, or empty when the
     *         attribute is absent or unparseable
     */
    default Optional<Jid> getAttributeAsJid(String key) {
        return getAttribute(key)
                .flatMap(StanzaAttribute::toJid);
    }

    /**
     * Returns the value of the supplied attribute parsed as a {@link Jid},
     * falling back to {@code defaultValue} when absent or unparseable.
     *
     * @param key          the attribute key
     * @param defaultValue the fallback returned when the attribute is absent
     *                     or unparseable
     * @return the parsed JID or the fallback
     */
    default Jid getAttributeAsJid(String key, Jid defaultValue) {
        return getAttribute(key)
                .flatMap(StanzaAttribute::toJid)
                .orElse(defaultValue);
    }

    /**
     * Returns the value of the supplied attribute parsed as a {@code long}.
     *
     * @param key the attribute key
     * @return an {@link OptionalLong} that holds the parsed value, or empty
     *         when the attribute is absent or unparseable
     */
    default OptionalLong getAttributeAsLong(String key) {
        var result = getAttribute(key);
        return result.isEmpty() ? OptionalLong.empty() : result.get().toLong();
    }

    /**
     * Returns the value of the supplied attribute parsed as a {@code long},
     * falling back to {@code defaultValue} when absent or unparseable.
     *
     * @param key          the attribute key
     * @param defaultValue the fallback returned when the attribute is absent
     *                     or unparseable
     * @return the parsed long or the fallback
     */
    default long getAttributeAsLong(String key, long defaultValue) {
        var result = getAttribute(key);
        return result.isEmpty() ? defaultValue : result.get().toLong().orElse(defaultValue);
    }

    /**
     * Returns the value of the supplied attribute parsed as a boxed
     * {@link Long}, falling back to {@code defaultValue} when absent or
     * unparseable.
     *
     * <p>Distinct from the {@code long}-returning overload because the
     * fallback itself may be {@code null}, which is impossible to express with
     * a primitive return type.
     *
     * @param key          the attribute key
     * @param defaultValue the fallback, which may be {@code null}
     * @return the parsed {@link Long} or the fallback
     */
    default Long getAttributeAsLong(String key, Long defaultValue) {
        var result = getAttribute(key);
        if (result.isEmpty()) {
            return defaultValue;
        }

        var converted = result.get().toLong();
        if(converted.isEmpty()) {
            return defaultValue;
        }

        return converted.getAsLong();
    }

    /**
     * Returns the value of the supplied attribute parsed as an {@code int}.
     *
     * @param key the attribute key
     * @return an {@link OptionalInt} that holds the parsed value, or empty
     *         when the attribute is absent or unparseable
     */
    default OptionalInt getAttributeAsInt(String key) {
        var result = getAttribute(key);
        return result.isEmpty() ? OptionalInt.empty() : result.get().toInt();
    }

    /**
     * Returns the value of the supplied attribute parsed as an {@code int},
     * falling back to {@code defaultValue} when absent or unparseable.
     *
     * @param key          the attribute key
     * @param defaultValue the fallback returned when the attribute is absent
     *                     or unparseable
     * @return the parsed int or the fallback
     */
    default int getAttributeAsInt(String key, int defaultValue) {
        var result = getAttribute(key);
        return result.isEmpty() ? defaultValue : result.get().toInt().orElse(defaultValue);
    }

    /**
     * Returns the value of the supplied attribute parsed as a boxed
     * {@link Integer}, falling back to {@code defaultValue} when absent or
     * unparseable.
     *
     * <p>Distinct from the {@code int}-returning overload because the fallback
     * itself may be {@code null}.
     *
     * @param key          the attribute key
     * @param defaultValue the fallback, which may be {@code null}
     * @return the parsed {@link Integer} or the fallback
     */
    default Integer getAttributeAsInt(String key, Integer defaultValue) {
        var result = getAttribute(key);
        if (result.isEmpty()) {
            return defaultValue;
        }

        var converted = result.get().toInt();
        if(converted.isEmpty()) {
            return defaultValue;
        }

        return converted.getAsInt();
    }

    /**
     * Returns the value of the supplied attribute parsed as a {@code double}.
     *
     * @param key the attribute key
     * @return an {@link OptionalDouble} that holds the parsed value, or empty
     *         when the attribute is absent or unparseable
     */
    default OptionalDouble getAttributeAsDouble(String key) {
        var result = getAttribute(key);
        return result.isEmpty() ? OptionalDouble.empty() : result.get().toDouble();
    }

    /**
     * Returns the value of the supplied attribute parsed as a {@code double},
     * falling back to {@code defaultValue} when absent or unparseable.
     *
     * @param key          the attribute key
     * @param defaultValue the fallback returned when the attribute is absent
     *                     or unparseable
     * @return the parsed double or the fallback
     */
    default double getAttributeAsDouble(String key, double defaultValue) {
        var result = getAttribute(key);
        return result.isEmpty() ? defaultValue : result.get().toDouble().orElse(defaultValue);
    }

    /**
     * Returns the value of the supplied attribute parsed as a boxed
     * {@link Double}, falling back to {@code defaultValue} when absent or
     * unparseable.
     *
     * <p>Distinct from the {@code double}-returning overload because the
     * fallback itself may be {@code null}.
     *
     * @param key          the attribute key
     * @param defaultValue the fallback, which may be {@code null}
     * @return the parsed {@link Double} or the fallback
     */
    default Double getAttributeAsDouble(String key, Double defaultValue) {
        var result = getAttribute(key);
        if (result.isEmpty()) {
            return defaultValue;
        }

        var converted = result.get().toDouble();
        if(converted.isEmpty()) {
            return defaultValue;
        }

        return converted.getAsDouble();
    }

    /**
     * Returns a single-element stream that yields the matching attribute, or
     * an empty stream when absent.
     *
     * <p>The stream form is convenient when a caller already has a pipeline
     * that wants to {@link Stream#flatMap(java.util.function.Function)} into an
     * attribute lookup without breaking the stream.
     *
     * @param key the attribute key
     * @return a {@link Stream} that yields the attribute or nothing
     * @throws NullPointerException if {@code key} is {@code null}
     */
    default Stream<StanzaAttribute> streamAttribute(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return Stream.ofNullable(attributes().get(key));
    }

    /**
     * Returns a single-element stream that yields the attribute as a string,
     * or an empty stream when absent.
     *
     * @param key the attribute key
     * @return a {@link Stream} that yields the string value or nothing
     */
    default Stream<String> streamAttributeAsString(String key) {
        return streamAttribute(key)
                .map(StanzaAttribute::toString);
    }

    /**
     * Returns a single-element stream that yields the attribute parsed as a
     * boolean under the {@link StanzaBooleanFormat#LENIENT} format, or an empty
     * stream when absent.
     *
     * @param key the attribute key
     * @return a {@link Stream} that yields the boolean value or nothing
     */
    default Stream<Boolean> streamAttributeAsBool(String key) {
        return streamAttributeAsBool(key, StanzaBooleanFormat.LENIENT);
    }

    /**
     * Returns a single-element stream that yields the attribute parsed as a
     * boolean under the supplied {@link StanzaBooleanFormat}, or an empty stream
     * when absent.
     *
     * @param key    the attribute key
     * @param format the format used to decode the textual view
     * @return a {@link Stream} that yields the boolean value or nothing
     * @throws NullPointerException if {@code format} is {@code null}
     */
    default Stream<Boolean> streamAttributeAsBool(String key, StanzaBooleanFormat format) {
        Objects.requireNonNull(format, "format cannot be null");
        return streamAttribute(key)
                .map(StanzaAttribute::toString)
                .map(format::decode);
    }

    /**
     * Returns a single-element stream that yields the attribute as a byte
     * array, or an empty stream when absent.
     *
     * @param key the attribute key
     * @return a {@link Stream} that yields the byte array or nothing
     */
    default Stream<byte[]> streamAttributeAsBytes(String key) {
        return streamAttribute(key)
                .map(StanzaAttribute::toBytes);
    }

    /**
     * Returns a single-element stream that yields the attribute parsed as a
     * {@link Jid}, or an empty stream when absent or unparseable.
     *
     * <p>The unparseable case folds into the absent case here; callers that
     * need to distinguish the two should use the {@code getAttributeAsJid}
     * variants instead.
     *
     * @param key the attribute key
     * @return a {@link Stream} that yields the JID or nothing
     * @throws NullPointerException if {@code key} is {@code null}
     */
    default Stream<Jid> streamAttributeAsJid(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        var attributeValue = attributes().get(key);
        return attributeValue != null
                ? attributeValue.toJid().stream()
                : Stream.empty();
    }

    /**
     * Returns a single-element stream that yields the attribute parsed as a
     * {@code long}, or an empty stream when absent or unparseable.
     *
     * @param key the attribute key
     * @return a {@link LongStream} that yields the long value or nothing
     * @throws NullPointerException if {@code key} is {@code null}
     */
    default LongStream streamAttributeAsLong(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        var attributeValue = attributes().get(key);
        return attributeValue != null
                ? attributeValue.toLong().stream()
                : LongStream.empty();
    }

    /**
     * Returns a single-element stream that yields the attribute parsed as an
     * {@code int}, or an empty stream when absent or unparseable.
     *
     * @param key the attribute key
     * @return an {@link IntStream} that yields the int value or nothing
     * @throws NullPointerException if {@code key} is {@code null}
     */
    default IntStream streamAttributeAsInt(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        var attributeValue = attributes().get(key);
        return attributeValue != null
                ? attributeValue.toInt().stream()
                : IntStream.empty();
    }

    /**
     * Returns a single-element stream that yields the attribute parsed as a
     * {@code double}, or an empty stream when absent or unparseable.
     *
     * @param key the attribute key
     * @return a {@link DoubleStream} that yields the double value or nothing
     * @throws NullPointerException if {@code key} is {@code null}
     */
    default DoubleStream streamAttributeAsDouble(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        var attributeValue = attributes().get(key);
        return attributeValue != null
                ? attributeValue.toDouble().stream()
                : DoubleStream.empty();
    }

    /**
     * Returns the attribute associated with the supplied key, throwing when
     * absent.
     *
     * <p>Lets inbound handlers treat a missing attribute as a malformed-stanza
     * error rather than as a recoverable absence; the exception message names
     * the missing key and the containing stanza's description and attribute map
     * so log scans can locate the source stanza.
     *
     * @param key the attribute key
     * @return the matching attribute
     * @throws NoSuchElementException if the attribute is absent
     */
    default StanzaAttribute getRequiredAttribute(String key) {
        var result = attributes().get(key);
        if(result == null) {
            throw new NoSuchElementException("No attribute with key " + key + " found in stanza " + description() + " with attributes " + attributes());
        }

        return result;
    }

    /**
     * Returns the value of the supplied attribute as a string, throwing when
     * absent.
     *
     * @param key the attribute key
     * @return the string value
     * @throws NoSuchElementException if the attribute is absent
     */
    default String getRequiredAttributeAsString(String key) {
        return getRequiredAttribute(key)
                .toString();
    }

    /**
     * Returns the value of the supplied attribute parsed as a boolean under the
     * {@link StanzaBooleanFormat#LENIENT} format, throwing when absent.
     *
     * @param key the attribute key
     * @return the parsed boolean
     * @throws NoSuchElementException if the attribute is absent
     */
    default boolean getRequiredAttributeAsBool(String key) {
        return getRequiredAttributeAsBool(key, StanzaBooleanFormat.LENIENT);
    }

    /**
     * Returns the value of the supplied attribute parsed as a boolean under the
     * supplied {@link StanzaBooleanFormat}, throwing when absent.
     *
     * @param key    the attribute key
     * @param format the format used to decode the textual view
     * @return the parsed boolean
     * @throws NoSuchElementException if the attribute is absent
     * @throws NullPointerException   if {@code format} is {@code null}
     */
    default boolean getRequiredAttributeAsBool(String key, StanzaBooleanFormat format) {
        Objects.requireNonNull(format, "format cannot be null");
        var result = getRequiredAttribute(key)
                .toString();
        return format.decode(result);
    }

    /**
     * Returns the value of the supplied attribute as a byte array, throwing
     * when absent.
     *
     * @param key the attribute key
     * @return the byte array
     * @throws NoSuchElementException if the attribute is absent
     */
    default byte[] getRequiredAttributeAsBytes(String key) {
        return getRequiredAttribute(key)
                .toBytes();
    }

    /**
     * Returns the value of the supplied attribute parsed as a {@link Jid},
     * throwing when absent or unparseable.
     *
     * <p>Distinguishes absence ({@link NoSuchElementException}) from
     * unparseability ({@link IllegalArgumentException}) so inbound handlers
     * can route the two failure modes differently.
     *
     * @param key the attribute key
     * @return the parsed JID
     * @throws NoSuchElementException   if the attribute is absent
     * @throws IllegalArgumentException if the attribute does not parse to a
     *                                  valid JID
     */
    default Jid getRequiredAttributeAsJid(String key) {
        var requiredAttribute = getRequiredAttribute(key);
        return requiredAttribute
                .toJid()
                .orElseThrow(() -> new IllegalArgumentException("Cannot convert required attribute " + key + " to JID. Attribute value: " + requiredAttribute));
    }

    /**
     * Returns the value of the supplied attribute parsed as a {@code long},
     * throwing when absent or unparseable.
     *
     * @param key the attribute key
     * @return the parsed long
     * @throws NoSuchElementException   if the attribute is absent
     * @throws IllegalArgumentException if the attribute does not parse to a
     *                                  valid long
     */
    default long getRequiredAttributeAsLong(String key) {
        var requiredAttribute = getRequiredAttribute(key);
        return requiredAttribute
                .toLong()
                .orElseThrow(() -> new IllegalArgumentException("Cannot convert required attribute " + key + " to long. Attribute value: " + requiredAttribute));
    }

    /**
     * Returns the value of the supplied attribute parsed as an {@code int},
     * throwing when absent or unparseable.
     *
     * @param key the attribute key
     * @return the parsed int
     * @throws NoSuchElementException   if the attribute is absent
     * @throws IllegalArgumentException if the attribute does not parse to a
     *                                  valid int
     */
    default int getRequiredAttributeAsInt(String key) {
        var requiredAttribute = getRequiredAttribute(key);
        return requiredAttribute
                .toInt()
                .orElseThrow(() -> new IllegalArgumentException("Cannot convert required attribute " + key + " to int. Attribute value: " + requiredAttribute));
    }

    /**
     * Returns the value of the supplied attribute parsed as a {@code double},
     * throwing when absent or unparseable.
     *
     * @param key the attribute key
     * @return the parsed double
     * @throws NoSuchElementException   if the attribute is absent
     * @throws IllegalArgumentException if the attribute does not parse to a
     *                                  valid double
     */
    default double getRequiredAttributeAsDouble(String key) {
        var requiredAttribute = getRequiredAttribute(key);
        return requiredAttribute
                .toDouble()
                .orElseThrow(() -> new IllegalArgumentException("Cannot convert required attribute " + key + " to double. Attribute value: " + requiredAttribute));
    }

    /**
     * Returns whether this stanza carries an attribute with the supplied key.
     *
     * @param key the attribute key
     * @return {@code true} when the attribute is present
     * @throws NullPointerException if {@code key} is {@code null}
     */
    default boolean hasAttribute(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return attributes().containsKey(key);
    }

    /**
     * Returns whether this stanza carries an attribute with the supplied key
     * whose textual view equals {@code value}.
     *
     * <p>Lets dispatch chains gate on a specific string literal, for example
     * {@code type == "set"} on an iq.
     *
     * @param key   the attribute key
     * @param value the expected string value
     * @return {@code true} when the attribute exists and matches
     * @throws NullPointerException if {@code key} is {@code null}
     */
    default boolean hasAttribute(String key, String value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        return attribute != null
               && attribute.toString().equals(value);
    }

    /**
     * Returns whether this stanza carries an attribute with the supplied key
     * whose byte view equals {@code value}.
     *
     * @param key   the attribute key
     * @param value the expected byte array value
     * @return {@code true} when the attribute exists and matches
     * @throws NullPointerException if {@code key} is {@code null}
     */
    default boolean hasAttribute(String key, byte[] value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        return attribute != null
               && Arrays.equals(attribute.toBytes(), value);
    }

    /**
     * Returns whether this stanza carries an attribute with the supplied key
     * that parses to the supplied {@link Jid}.
     *
     * @param key   the attribute key
     * @param value the expected JID
     * @return {@code true} when the attribute exists and parses to
     *         {@code value}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    default boolean hasAttribute(String key, Jid value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        if(attribute == null) {
            return false;
        }

        var attributeValue = attribute.toJid();
        return attributeValue.isPresent()
               && Objects.equals(attributeValue.get(), value);
    }

    /**
     * Returns whether this stanza carries an attribute with the supplied key
     * that parses to the supplied {@code long}.
     *
     * @param key   the attribute key
     * @param value the expected long value
     * @return {@code true} when the attribute exists and parses to
     *         {@code value}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    default boolean hasAttribute(String key, long value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        if(attribute == null) {
            return false;
        }

        var attributeValue = attribute.toLong();
        return attributeValue.isPresent()
               && attributeValue.getAsLong() == value;
    }

    /**
     * Returns whether this stanza carries an attribute with the supplied key
     * that parses to the supplied {@code int}.
     *
     * @param key   the attribute key
     * @param value the expected int value
     * @return {@code true} when the attribute exists and parses to
     *         {@code value}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    default boolean hasAttribute(String key, int value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        if(attribute == null) {
            return false;
        }

        var attributeValue = attribute.toInt();
        return attributeValue.isPresent()
               && attributeValue.getAsInt() == value;
    }

    /**
     * Returns whether this stanza carries an attribute with the supplied key
     * that parses to the supplied {@code double}.
     *
     * @param key   the attribute key
     * @param value the expected double value
     * @return {@code true} when the attribute exists and parses to
     *         {@code value}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    default boolean hasAttribute(String key, double value) {
        Objects.requireNonNull(key, "key cannot be null");
        var attribute = attributes().get(key);
        if(attribute == null) {
            return false;
        }

        var attributeValue = attribute.toDouble();
        return attributeValue.isPresent()
               && attributeValue.getAsDouble() == value;
    }

    /**
     * Returns whether this stanza carries an attribute with the supplied key that
     * parses to the supplied boolean under the {@link StanzaBooleanFormat#LENIENT}
     * format.
     *
     * @param key   the attribute key
     * @param value the expected boolean value
     * @return {@code true} when the attribute exists and parses to
     *         {@code value}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    default boolean hasAttribute(String key, boolean value) {
        return hasAttribute(key, value, StanzaBooleanFormat.LENIENT);
    }

    /**
     * Returns whether this stanza carries an attribute with the supplied key that
     * parses to the supplied boolean under the supplied {@link StanzaBooleanFormat}.
     *
     * @param key    the attribute key
     * @param value  the expected boolean value
     * @param format the format used to decode the textual view
     * @return {@code true} when the attribute exists and parses to
     *         {@code value}
     * @throws NullPointerException if {@code key} or {@code format} is
     *                              {@code null}
     */
    default boolean hasAttribute(String key, boolean value, StanzaBooleanFormat format) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(format, "format cannot be null");
        var attribute = attributes().get(key);
        if(attribute == null) {
            return false;
        }

        var attributeValue = attribute.toString();
        return format.decode(attributeValue) == value;
    }

    /**
     * Returns whether this stanza carries any content slot at all.
     *
     * <p>{@code false} only for {@link EmptyStanza}; every other variant carries
     * some content. The serialiser uses this to decide whether to allocate a
     * content token in the stanza's size header.
     *
     * @return {@code false} for {@link EmptyStanza}, {@code true} for every
     *         other variant
     */
    boolean hasContent();

    /**
     * Returns whether this stanza's content equals the supplied text.
     *
     * <p>Variant-specific: {@link TextStanza} compares strings, {@link JidStanza}
     * compares the canonical JID string, {@link BytesStanza} decodes the blob
     * and compares as text. The empty and container variants always return
     * {@code false}.
     *
     * @param content the expected text content
     * @return {@code true} when the stanza carries textual content equal to
     *         {@code content}
     */
    boolean hasContent(String content);

    /**
     * Returns whether this stanza's content equals the supplied JID.
     *
     * <p>Variant-specific: {@link JidStanza} compares JIDs directly;
     * {@link TextStanza} and {@link BytesStanza} compare the JID's canonical
     * string against the textual view. The empty and container variants always
     * return {@code false}.
     *
     * @param content the expected JID content
     * @return {@code true} when the stanza carries JID content equal to
     *         {@code content}
     */
    boolean hasContent(Jid content);

    /**
     * Returns whether this stanza's content equals the supplied byte array.
     *
     * <p>Variant-specific: {@link BytesStanza} compares blobs directly;
     * {@link TextStanza} and {@link JidStanza} compare the textual view to a UTF-8
     * decoded version of {@code content}. The empty and container variants
     * always return {@code false}.
     *
     * @param content the expected binary content
     * @return {@code true} when the stanza carries binary content equal to
     *         {@code content}
     */
    boolean hasContent(byte[] content);

    /**
     * Returns the encoded size of this stanza measured in token slots.
     *
     * <p>{@code StanzaWriter} uses the count to emit the
     * {@code StanzaTags.LIST_8} or
     * {@code StanzaTags.LIST_16} size
     * header at the start of the encoded stanza. The count is one for the
     * description, two for every attribute (key plus value), and one when a
     * content slot is populated.
     *
     * @return the number of token slots required by the stanza
     */
    default int size() {
        return 1
               + (attributes().size() * 2)
               + (hasContent() ? 1 : 0);
    }

    /**
     * Returns the content of this stanza as a byte array when applicable.
     *
     * <p>Always succeeds for {@link BytesStanza}; produces a UTF-8 encoding of
     * the textual view for {@link TextStanza} and {@link JidStanza}; returns
     * {@link Optional#empty()} for {@link EmptyStanza} and {@link ContainerStanza}.
     *
     * @return an {@link Optional} that holds the content as bytes, or empty
     *         when the variant has no convertible content
     */
    Optional<byte[]> toContentBytes();

    /**
     * Returns a single-element stream that yields the content as a byte array,
     * or an empty stream when no conversion is possible.
     *
     * @return a {@link Stream} that yields the byte array or nothing
     */
    default Stream<byte[]> streamContentBytes() {
        return toContentBytes()
                .stream();
    }

    /**
     * Returns the content of this stanza as an {@link InputStream} when
     * applicable.
     *
     * <p>Wraps the same bytes that {@link #toContentBytes()} would return in a
     * {@link ByteArrayInputStream}; useful for callers that want to decode a
     * payload incrementally.
     *
     * @return an {@link Optional} that holds the content as a stream, or empty
     *         when the variant has no convertible content
     */
    Optional<InputStream> toContentStream();

    /**
     * Returns a single-element stream that yields the content as an
     * {@link InputStream}, or an empty stream when no conversion is possible.
     *
     * @return a {@link Stream} that yields the input stream or nothing
     */
    default Stream<InputStream> streamContentStream() {
        return toContentStream()
                .stream();
    }

    /**
     * Returns the content of this stanza as a string when applicable.
     *
     * <p>Always succeeds for {@link TextStanza}; produces the canonical string
     * for {@link JidStanza}; decodes the blob under the platform default charset
     * for {@link BytesStanza}; returns {@link Optional#empty()} for
     * {@link EmptyStanza} and {@link ContainerStanza}.
     *
     * @return an {@link Optional} that holds the content as text, or empty
     *         when the variant has no convertible content
     */
    Optional<String> toContentString();

    /**
     * Returns a single-element stream that yields the content as a string, or
     * an empty stream when no conversion is possible.
     *
     * @return a {@link Stream} that yields the string or nothing
     */
    default Stream<String> streamContentString() {
        return toContentString()
                .stream();
    }

    /**
     * Returns the content of this stanza parsed as a boolean under the
     * {@link StanzaBooleanFormat#LENIENT} format when applicable.
     *
     * <p>Convenience for {@link #toContentBool(StanzaBooleanFormat)} with the
     * lenient format.
     *
     * @return an {@link Optional} that holds the parsed boolean, or empty when
     *         no conversion is possible
     */
    default Optional<Boolean> toContentBool() {
        return toContentBool(StanzaBooleanFormat.LENIENT);
    }

    /**
     * Returns the content of this stanza parsed as a boolean under the supplied
     * {@link StanzaBooleanFormat} when applicable.
     *
     * <p>Delegates to {@link #toContentString()} and then to
     * {@link StanzaBooleanFormat#decode(String)}.
     *
     * @param format the format used to decode the content's textual view
     * @return an {@link Optional} that holds the parsed boolean, or empty when
     *         no conversion is possible
     * @throws NullPointerException if {@code format} is {@code null}
     */
    default Optional<Boolean> toContentBool(StanzaBooleanFormat format) {
        Objects.requireNonNull(format, "format cannot be null");
        return toContentString()
                .map(format::decode);
    }

    /**
     * Returns a single-element stream that yields the content parsed as a
     * boolean under the {@link StanzaBooleanFormat#LENIENT} format, or an empty
     * stream when no conversion is possible.
     *
     * @return a {@link Stream} that yields the parsed boolean or nothing
     */
    default Stream<Boolean> streamContentBool() {
        return toContentBool()
                .stream();
    }

    /**
     * Returns a single-element stream that yields the content parsed as a
     * boolean under the supplied {@link StanzaBooleanFormat}, or an empty stream
     * when no conversion is possible.
     *
     * @param format the format used to decode the content's textual view
     * @return a {@link Stream} that yields the parsed boolean or nothing
     * @throws NullPointerException if {@code format} is {@code null}
     */
    default Stream<Boolean> streamContentBool(StanzaBooleanFormat format) {
        return toContentBool(format)
                .stream();
    }

    /**
     * Returns the content of this stanza parsed as an {@link Integer} when
     * applicable.
     *
     * <p>A non-integer textual payload returns {@link Optional#empty()} rather
     * than throwing, so callers composing inside a stream pipeline can chain
     * on it without a try/catch.
     *
     * @return an {@link Optional} that holds the parsed int, or empty when the
     *         content does not represent a valid int
     */
    default Optional<Integer> toContentInt() {
        return toContentString().map(str -> {
            try {
                return Integer.parseInt(str);
            }catch (NumberFormatException _) {
                return null;
            }
        });
    }

    /**
     * Returns a single-element stream that yields the content parsed as an
     * {@link Integer}, or an empty stream when parsing fails.
     *
     * @return a {@link Stream} that yields the parsed int or nothing
     */
    default Stream<Integer> streamContentInt() {
        return toContentInt()
                .stream();
    }

    /**
     * Returns the content of this stanza parsed as a {@link Long} when
     * applicable.
     *
     * <p>A non-long textual payload returns {@link Optional#empty()} rather
     * than throwing.
     *
     * @return an {@link Optional} that holds the parsed long, or empty when
     *         the content does not represent a valid long
     */
    default Optional<Long> toContentLong() {
        return toContentString().map(str -> {
            try {
                return Long.parseLong(str);
            }catch (NumberFormatException _) {
                return null;
            }
        });
    }

    /**
     * Returns a single-element stream that yields the content parsed as a
     * {@link Long}, or an empty stream when parsing fails.
     *
     * @return a {@link Stream} that yields the parsed long or nothing
     */
    default Stream<Long> streamContentLong() {
        return toContentLong()
                .stream();
    }

    /**
     * Returns the content of this stanza as a {@link Jid} when applicable.
     *
     * <p>Always succeeds for {@link JidStanza}; tries a parse for
     * {@link TextStanza} and {@link BytesStanza}; returns {@link Optional#empty()}
     * for {@link EmptyStanza} and {@link ContainerStanza}.
     *
     * @return an {@link Optional} that holds the JID, or empty when the
     *         content does not parse to a valid JID
     */
    Optional<Jid> toContentJid();

    /**
     * Returns a single-element stream that yields the content as a
     * {@link Jid}, or an empty stream when parsing fails.
     *
     * @return a {@link Stream} that yields the JID or nothing
     */
    default Stream<Jid> streamContentJid() {
        return toContentJid()
                .stream();
    }

    /**
     * Returns the children of this stanza in declaration order.
     *
     * <p>Non-empty only for {@link ContainerStanza}; every other variant returns
     * an empty list. The returned collection is unmodifiable for the container
     * variant.
     *
     * @return a sequenced collection of child nodes, possibly empty
     */
    SequencedCollection<Stanza> children();

    /**
     * Returns the children of this stanza as a {@link Stream}.
     *
     * @return a non null stream over the children
     */
    default Stream<Stanza> streamChildren() {
        return children()
                .stream();
    }

    /**
     * Returns the first child of this stanza, if any.
     *
     * <p>Used by walkers that consume a known wrapper stanza, for example
     * {@code <result><body>...</body></result>}, and want the singular inner
     * child without filtering by description.
     *
     * @return an {@link Optional} that holds the first child, or empty when
     *         the stanza has none
     */
    default Optional<Stanza> getChild() {
        var children = children();
        return children.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(children.getFirst());
    }

    /**
     * Returns a single-element stream that yields the first child, or an empty
     * stream when the stanza has none.
     *
     * @return a {@link Stream} that yields the first child or nothing
     */
    default Stream<Stanza> streamChild() {
        var children = children();
        return children.isEmpty()
                ? Stream.empty()
                : Stream.of(children.getFirst());
    }

    /**
     * Returns the first child whose description matches {@code description}.
     *
     * <p>The canonical lookup for unwrapping a known sub-element from a
     * container stanza, for example the {@code <body>} child of an iq result.
     *
     * @param description the description to match
     * @return an {@link Optional} that holds the matching child, or empty when
     *         none matches
     * @throws NullPointerException if {@code description} is {@code null}
     */
    default Optional<Stanza> getChild(String description) {
        Objects.requireNonNull(description, "description cannot be null");
        return streamChildren(description)
                .findFirst();
    }

    /**
     * Returns the first child whose description matches any of the supplied
     * descriptions, tested in order.
     *
     * <p>Used when a stanza may carry one of several alternative inner shapes,
     * for example {@code <body>} or {@code <error>} inside an iq result; the
     * order of {@code descriptions} encodes the caller's preference.
     *
     * @param descriptions the descriptions to match
     * @return an {@link Optional} that holds the first match, or empty when
     *         none matches
     * @throws NullPointerException if {@code descriptions} or any element is
     *                              {@code null}
     */
    default Optional<Stanza> getChild(String... descriptions) {
        Objects.requireNonNull(descriptions, "descriptions cannot be null");
        for (var description : descriptions) {
            Objects.requireNonNull(description, "description cannot be null");
            var child = getChild(description);
            if (child.isPresent()) {
                return child;
            }
        }

        return Optional.empty();
    }

    /**
     * Returns the first child whose description matches {@code description}, or
     * {@code defaultValue} when none matches.
     *
     * @param description  the description to match
     * @param defaultValue the fallback returned when no child matches
     * @return the matching child or the fallback
     * @throws NullPointerException if {@code description} is {@code null}
     */
    default Stanza getChild(String description, Stanza defaultValue) {
        return getChild(description)
                .orElse(defaultValue);
    }

    /**
     * Returns the first child whose description matches {@code description},
     * throwing when none matches.
     *
     * <p>Lets inbound handlers treat a missing child as a malformed-stanza
     * error.
     *
     * @param description the description to match
     * @return the first matching child
     * @throws NullPointerException     if {@code description} is {@code null}
     * @throws IllegalArgumentException if no child matches
     */
    default Stanza getRequiredChild(String description) {
        Objects.requireNonNull(description, "description cannot be null");
        return streamChildren(description)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No child stanza found with description: " + description));
    }

    /**
     * Returns the first child whose description matches any of the supplied
     * descriptions, throwing when none matches.
     *
     * <p>The order of {@code descriptions} encodes the caller's preference;
     * the first match wins.
     *
     * @param descriptions the descriptions to match
     * @return the first matching child
     * @throws NullPointerException     if {@code descriptions} or any element
     *                                  is {@code null}
     * @throws IllegalArgumentException if no child matches
     */
    default Stanza getRequiredChild(String... descriptions) {
        return getChild(descriptions)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No child stanza found with descriptions: " + Arrays.toString(descriptions)
                ));
    }

    /**
     * Returns a single-element stream that yields the first child whose
     * description matches {@code description}, or an empty stream when none
     * matches.
     *
     * @param description the description to match
     * @return a {@link Stream} that yields the matching child or nothing
     * @throws NullPointerException if {@code description} is {@code null}
     */
    default Stream<Stanza> streamChild(String description) {
        Objects.requireNonNull(description, "description cannot be null");
        return streamChildren(description)
                .findFirst()
                .stream();
    }

    /**
     * Returns a single-element stream that yields the first child whose
     * description matches any of the supplied descriptions, or an empty stream
     * when none matches.
     *
     * @param descriptions the descriptions to match
     * @return a {@link Stream} that yields the matching child or nothing
     * @throws NullPointerException if {@code descriptions} or any element is
     *                              {@code null}
     */
    default Stream<Stanza> streamChild(String... descriptions) {
        return getChild(descriptions)
                .stream();
    }

    /**
     * Returns every child whose description matches {@code description},
     * preserving declaration order.
     *
     * <p>Used to iterate repeated sub-elements, for example
     * {@code <participant>} entries inside a group iq result.
     *
     * @param description the description to match
     * @return a sequenced collection of matching children, possibly empty
     * @throws NullPointerException if {@code description} is {@code null}
     */
    default SequencedCollection<Stanza> getChildren(String description) {
        Objects.requireNonNull(description, "description cannot be null");
        return children()
                .stream()
                .filter(node -> node.hasDescription(description))
                .toList();
    }

    /**
     * Returns every child whose description matches any of the supplied
     * descriptions, preserving declaration order.
     *
     * @param descriptions the descriptions to match
     * @return a sequenced collection of matching children, possibly empty
     * @throws NullPointerException if {@code descriptions} or any element is
     *                              {@code null}
     */
    default SequencedCollection<Stanza> getChildren(String... descriptions) {
        return streamChildren(descriptions)
                .toList();
    }

    /**
     * Returns a stream of every child whose description matches
     * {@code description}, preserving declaration order.
     *
     * @param description the description to match
     * @return a {@link Stream} over the matching children
     * @throws NullPointerException if {@code description} is {@code null}
     */
    default Stream<Stanza> streamChildren(String description) {
        Objects.requireNonNull(description, "description cannot be null");
        return children()
                .stream()
                .filter(node -> node.hasDescription(description));
    }

    /**
     * Returns a stream of every child whose description matches any of the
     * supplied descriptions, preserving declaration order.
     *
     * @implNote
     * This implementation pre-builds a {@link LinkedHashSet} of the requested
     * descriptions so each child's membership check is a single hash lookup.
     *
     * @param descriptions the descriptions to match
     * @return a {@link Stream} over the matching children
     * @throws NullPointerException if {@code descriptions} or any element is
     *                              {@code null}
     */
    default Stream<Stanza> streamChildren(String... descriptions) {
        var descriptionSet = new LinkedHashSet<String>();
        Objects.requireNonNull(descriptions, "descriptions cannot be null");
        for (var description : descriptions) {
            descriptionSet.add(Objects.requireNonNull(description, "description cannot be null"));
        }

        return children()
                .stream()
                .filter(node -> descriptionSet.contains(node.description()));
    }

    /**
     * Returns whether this stanza has at least one child whose description
     * matches {@code description}.
     *
     * @param description the description to match
     * @return {@code true} when at least one child matches
     * @throws NullPointerException if {@code description} is {@code null}
     */
    default boolean hasChild(String description) {
        Objects.requireNonNull(description, "description cannot be null");
        return children()
                .stream()
                .anyMatch(node -> node.hasDescription(description));
    }

    /**
     * Returns whether this stanza has at least one child whose description
     * matches any of the supplied descriptions.
     *
     * @param descriptions the descriptions to match
     * @return {@code true} when at least one child matches
     * @throws NullPointerException if {@code descriptions} or any element is
     *                              {@code null}
     */
    default boolean hasChild(String... descriptions) {
        return getChild(descriptions).isPresent();
    }

    /**
     * Stanza variant that carries no content slot.
     *
     * <p>Carries stanzas whose meaning is conveyed entirely by the tag and
     * attributes; presence updates, acknowledgements, and the
     * {@code <pair-device>} family of handshake nodes are typical examples.
     *
     * @param description the tag name
     * @param attributes  the attribute map
     */
    record EmptyStanza(String description, SequencedMap<String, StanzaAttribute> attributes) implements Stanza {
        /**
         * Builds an empty stanza, rejecting {@code null} arguments.
         *
         * @throws NullPointerException if any argument is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WAXmlNode", exports = "XmlNode",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public EmptyStanza {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation wraps the underlying map with
         * {@link Collections#unmodifiableSequencedMap(SequencedMap)} on every
         * call rather than freezing it once; the cost is a single object
         * allocation and the read surface stays truly read-only.
         */
        @Override
        public SequencedMap<String, StanzaAttribute> attributes() {
            return Collections.unmodifiableSequencedMap(attributes);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent(String content) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent(Jid content) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> toContentString() {
            return Optional.empty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent(byte[] content) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<Jid> toContentJid() {
            return Optional.empty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SequencedCollection<Stanza> children() {
            return List.of();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<byte[]> toContentBytes() {
            return Optional.empty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<InputStream> toContentStream() {
            return Optional.empty();
        }

        /**
         * Returns whether {@code o} is a {@link Stanza} that compares
         * structurally equal to this one.
         *
         * <p>Cross-variant equality is supported: an {@link EmptyStanza} equals
         * a {@link TextStanza} with an empty payload, and equals a
         * {@link BytesStanza} whose payload {@link #hasContent(byte[])} treats as
         * empty. This matches the wire-level reality that a stanza with no
         * content and a stanza with an empty payload decode to indistinguishable
         * bytes.
         *
         * @param o the object to compare against
         * @return {@code true} when the two nodes match structurally
         */
        @Override
        public boolean equals(Object o) {
            return switch (o) {
                case EmptyStanza(var thatDescription, var thatAttributes) -> Objects.equals(description, thatDescription) && Objects.equals(attributes, thatAttributes);
                case TextStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription) && Objects.equals(attributes, thatAttributes) && hasContent(thatContent);
                case BytesStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription) && Objects.equals(attributes, thatAttributes) && hasContent(thatContent);
                case null, default -> false;
            };
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash of the description and attribute map
         */
        @Override
        public int hashCode() {
            return Objects.hash(description, attributes);
        }

        /**
         * Returns a debug-oriented string for this empty stanza.
         *
         * <p>The output is not part of the wire format and exists only for
         * logs and stack traces.
         *
         * @return the debug string
         */
        @Override
        @WhatsAppWebExport(moduleName = "WAXmlNode",
                exports = {"XmlNode", "attrsToString"},
                adaptation = WhatsAppAdaptation.ADAPTED)
        public String toString() {
            var result = new StringBuilder();
            result.append("Stanza[description=");
            result.append(description);

            if(!attributes.isEmpty()) {
                result.append(", attributes=");
                result.append(attributes);
            }

            result.append("]");

            return result.toString();
        }
    }

    /**
     * Stanza variant whose content is a UTF-8 string.
     *
     * <p>Carries stanzas whose payload is textual: status blurbs, identifiers
     * serialised as text, free-form message bodies, and any inbound stanza
     * whose content slot decoded as a dictionary token, packed nibble or hex
     * string, or length-prefixed UTF-8 blob.
     *
     * @param description the tag name
     * @param attributes  the attribute map
     * @param content     the textual payload
     */
    record TextStanza(String description, SequencedMap<String, StanzaAttribute> attributes, String content) implements Stanza {
        /**
         * Builds a text stanza, rejecting {@code null} arguments.
         *
         * @throws NullPointerException if any argument is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WAXmlNode", exports = "XmlNode",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public TextStanza {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
            Objects.requireNonNull(content, "content cannot be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SequencedMap<String, StanzaAttribute> attributes() {
            return Collections.unmodifiableSequencedMap(attributes);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent(String content) {
            return Objects.equals(this.content, content);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation compares the textual view of the supplied JID
         * against the stored string; a {@code null} {@code content} returns
         * {@code false} without dereferencing.
         */
        @Override
        public boolean hasContent(Jid content) {
            return content != null && Objects.equals(this.content, content.toString());
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation decodes the supplied bytes under the platform
         * default charset, matching {@link #toContentBytes()}'s encoding
         * direction.
         */
        @Override
        public boolean hasContent(byte[] content) {
            return content != null && Objects.equals(this.content, new String(content));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<byte[]> toContentBytes() {
            return Optional.of(content.getBytes());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<InputStream> toContentStream() {
            return Optional.of(new ByteArrayInputStream(content.getBytes()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<Jid> toContentJid() {
            try {
                var result = Jid.of(content);
                return Optional.of(result);
            }catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> toContentString() {
            return Optional.of(content);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SequencedCollection<Stanza> children() {
            return List.of();
        }

        /**
         * Returns whether {@code o} is a {@link Stanza} that compares
         * structurally equal to this one.
         *
         * <p>Cross-variant equality is supported: an {@link EmptyStanza} equals
         * this stanza when this stanza's payload is the empty string;
         * {@link JidStanza} and {@link BytesStanza} match through their textual
         * views via {@link #hasContent(Jid)} and {@link #hasContent(byte[])}.
         *
         * @param o the object to compare against
         * @return {@code true} when the two nodes match structurally
         */
        @Override
        public boolean equals(Object o) {
            return switch (o) {
                case EmptyStanza(var thatDescription, var thatAttributes) -> Objects.equals(description, thatDescription)
                                                                             && Objects.equals(attributes, thatAttributes)
                                                                             && content.isEmpty();
                case TextStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                                                                                             && Objects.equals(attributes, thatAttributes)
                                                                                             && hasContent(thatContent);
                case JidStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                                                                                            && Objects.equals(attributes, thatAttributes)
                                                                                            && hasContent(thatContent);
                case BytesStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                                                                                              && Objects.equals(attributes, thatAttributes)
                                                                                              && hasContent(thatContent);
                case null, default -> false;
            };
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash of the description, attribute map, and textual
         *         content
         */
        @Override
        public int hashCode() {
            return Objects.hash(description, attributes, content);
        }

        /**
         * Returns a debug-oriented string for this text stanza.
         *
         * <p>The output is not part of the wire format and exists only for
         * logs and stack traces.
         *
         * @return the debug string
         */
        @Override
        @WhatsAppWebExport(moduleName = "WAXmlNode",
                exports = {"XmlNode", "attrsToString"},
                adaptation = WhatsAppAdaptation.ADAPTED)
        public String toString() {
            var result = new StringBuilder();
            result.append("Stanza[description=");
            result.append(description());

            if(!attributes.isEmpty()) {
                result.append(", attributes=");
                result.append(attributes);
            }

            if(content != null) {
                result.append(", content=");
                result.append(content);
            }

            result.append("]");

            return result.toString();
        }
    }

    /**
     * Stanza variant whose content is a single {@link Jid}.
     *
     * <p>Carries stanzas that address a user, group, or device. The writer
     * encodes the JID under one of the four JID wire shapes
     * ({@code StanzaTags.JID_PAIR},
     * {@code StanzaTags.AD_JID},
     * {@code StanzaTags.JID_INTEROP},
     * {@code StanzaTags.JID_FB}), picked
     * from the JID's server and device.
     *
     * @param description the tag name
     * @param attributes  the attribute map
     * @param content     the JID payload
     */
    record JidStanza(String description, SequencedMap<String, StanzaAttribute> attributes, Jid content) implements Stanza {
        /**
         * Builds a JID stanza, rejecting {@code null} arguments.
         *
         * @throws NullPointerException if any argument is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WAXmlNode", exports = "XmlNode",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public JidStanza {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
            Objects.requireNonNull(content, "content cannot be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SequencedMap<String, StanzaAttribute> attributes() {
            return Collections.unmodifiableSequencedMap(attributes);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent() {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation compares against the JID's canonical string
         * form.
         */
        @Override
        public boolean hasContent(String content) {
            return Objects.equals(this.content.toString(), content);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent(Jid content) {
            return Objects.equals(this.content, content);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation decodes the supplied bytes under the platform
         * default charset and compares against the JID's canonical string
         * form.
         */
        @Override
        public boolean hasContent(byte[] content) {
            return content != null && Objects.equals(this.content.toString(), new String(content));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> toContentString() {
            return Optional.of(content.toString());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<Jid> toContentJid() {
            return Optional.of(content);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<byte[]> toContentBytes() {
            return Optional.of(content.toString().getBytes());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<InputStream> toContentStream() {
            return Optional.of(new ByteArrayInputStream(content.toString().getBytes()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SequencedCollection<Stanza> children() {
            return List.of();
        }

        /**
         * Returns whether {@code o} is a {@link Stanza} that compares
         * structurally equal to this one.
         *
         * <p>Matches structurally against {@link TextStanza}, {@link JidStanza},
         * and {@link BytesStanza} through their textual views.
         *
         * @param o the object to compare against
         * @return {@code true} when the two nodes match structurally
         */
        @Override
        public boolean equals(Object o) {
            return switch (o) {
                case TextStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                                                                                             && Objects.equals(attributes, thatAttributes)
                                                                                             && hasContent(thatContent);
                case JidStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                                                                                            && Objects.equals(attributes, thatAttributes)
                                                                                            && hasContent(thatContent);
                case BytesStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                                                                                              && Objects.equals(attributes, thatAttributes)
                                                                                              && hasContent(thatContent);
                case null, default -> false;
            };
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash of the description, attribute map, and JID content
         */
        @Override
        public int hashCode() {
            return Objects.hash(description, attributes, content);
        }

        /**
         * Returns a debug-oriented string for this JID stanza.
         *
         * <p>The output is not part of the wire format and exists only for
         * logs and stack traces.
         *
         * @return the debug string
         */
        @Override
        @WhatsAppWebExport(moduleName = "WAXmlNode",
                exports = {"XmlNode", "attrsToString"},
                adaptation = WhatsAppAdaptation.ADAPTED)
        public String toString() {
            var result = new StringBuilder();
            result.append("Stanza[description=");
            result.append(description());

            if(!attributes.isEmpty()) {
                result.append(", attributes=");
                result.append(attributes);
            }

            if(content != null) {
                result.append(", content=");
                result.append(content);
            }

            result.append("]");

            return result.toString();
        }
    }

    /**
     * Stanza variant whose content is an opaque binary blob.
     *
     * <p>Carries stanzas whose payload is raw bytes: Signal ciphertext, media
     * thumbnails, identity blobs, and any inbound stanza whose content slot
     * decoded as a length-prefixed binary shape
     * ({@code StanzaTags.BINARY_8},
     * {@code StanzaTags.BINARY_20}, or
     * {@code StanzaTags.BINARY_32}).
     *
     * @param description the tag name
     * @param attributes  the attribute map
     * @param content     the binary payload
     */
    record BytesStanza(String description, SequencedMap<String, StanzaAttribute> attributes, byte[] content) implements Stanza {
        /**
         * Builds a bytes stanza, rejecting {@code null} arguments.
         *
         * @throws NullPointerException if any argument is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WAXmlNode", exports = "XmlNode",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public BytesStanza {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
            Objects.requireNonNull(content, "content cannot be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SequencedMap<String, StanzaAttribute> attributes() {
            return Collections.unmodifiableSequencedMap(attributes);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<byte[]> toContentBytes() {
            return Optional.of(content);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<InputStream> toContentStream() {
            return Optional.of(new ByteArrayInputStream(content));
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation routes the parse through the protobuf
         * {@link ProtobufString#lazy(byte[])} fast path so the JID parser walks
         * the underlying bytes directly, avoiding the intermediate UTF-8
         * string allocation that {@code Jid.of(new String(content))} would
         * incur.
         */
        @Override
        public Optional<Jid> toContentJid() {
            try {
                var result = Jid.of(ProtobufString.lazy(content));
                return Optional.of(result);
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation decodes the blob under the platform default
         * charset; non-UTF-8 bytes produce a string that is faithful to the
         * platform decoder but may not round-trip.
         */
        @Override
        public Optional<String> toContentString() {
            var decoded = new String(content);
            return Optional.of(decoded);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent() {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation decodes the stored blob under the platform
         * default charset and compares against the supplied string.
         */
        @Override
        public boolean hasContent(String content) {
            return Objects.equals(new String(this.content), content);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation decodes the stored blob under the platform
         * default charset and compares against the canonical JID string of the
         * supplied JID.
         */
        @Override
        public boolean hasContent(Jid content) {
            return content != null && Objects.equals(new String(this.content), content.toString());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent(byte[] content) {
            return Arrays.equals(this.content, content);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SequencedCollection<Stanza> children() {
            return List.of();
        }

        /**
         * Returns whether {@code o} is a {@link Stanza} that compares
         * structurally equal to this one.
         *
         * <p>Matches structurally against {@link TextStanza}, {@link JidStanza},
         * and {@link BytesStanza}; cross-variant matches use the textual or byte
         * views as appropriate.
         *
         * @param o the object to compare against
         * @return {@code true} when the two nodes match structurally
         */
        @Override
        public boolean equals(Object o) {
            return switch (o) {
                case TextStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                                                                                             && Objects.equals(attributes, thatAttributes)
                                                                                             && hasContent(thatContent);
                case JidStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                                                                                            && Objects.equals(attributes, thatAttributes)
                                                                                            && hasContent(thatContent);
                case BytesStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                                                                                              && Objects.equals(attributes, thatAttributes)
                                                                                              && hasContent(thatContent);
                case null, default -> false;
            };
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash of the description, attribute map, and the hash of
         *         the byte content
         */
        @Override
        public int hashCode() {
            return Objects.hash(description, attributes, Arrays.hashCode(content));
        }

        /**
         * Returns a debug-oriented string for this bytes stanza.
         *
         * <p>The output is not part of the wire format and exists only for
         * logs and stack traces.
         *
         * @implNote
         * This implementation decodes the payload as a string for the
         * descriptions that conventionally carry textual data ({@code "result"},
         * {@code "query"}, {@code "body"}) so the log line stays readable; for
         * every other description it renders the raw byte array through
         * {@link Arrays#toString(byte[])}.
         *
         * @return the debug string
         */
        @Override
        @WhatsAppWebExport(moduleName = "WAXmlNode",
                exports = {"XmlNode", "attrsToString", "uint8ArrayToDebugString"},
                adaptation = WhatsAppAdaptation.ADAPTED)
        public String toString() {
            var result = new StringBuilder();
            result.append("Stanza[description=");
            result.append(description());

            if(!attributes.isEmpty()) {
                result.append(", attributes=");
                result.append(attributes);
            }

            if(content != null) {
                if(hasDescription("result") || hasDescription("query") || hasDescription("body")) {
                    result.append(", content=");
                    result.append(new String(content));
                }else {
                    result.append(", content=");
                    result.append(Arrays.toString(content));
                }
            }

            result.append("]");

            return result.toString();
        }
    }

    /**
     * Stanza variant whose content is a binary blob streamed lazily from a
     * {@link SizedInputStream}.
     *
     * <p>Built for outbound stanzas that carry a large payload (for example a
     * profile or group picture) without first materialising it into a
     * {@code byte[]}: the {@linkplain SizedInputStream#length() length} is
     * advertised up front so the wire encoder emits the length prefix from
     * metadata, then the body is streamed once from {@link #content()} at
     * serialisation time. Inbound binary content is never produced as a
     * {@code StreamStanza}; the decoder always surfaces it as a {@link BytesStanza}.
     *
     * <p>Because the payload is a stream rather than a value, the
     * content-reading accessors ({@link #toContentBytes()} and friends) drain a
     * fresh stream from {@link #content()} on each call, and equality is
     * identity-based on the sized stream; two stream nodes are never
     * structurally equal across to a {@link BytesStanza}.
     *
     * @param description the tag name
     * @param attributes  the attribute map
     * @param content     the sized stream over the payload
     */
    record StreamStanza(String description, SequencedMap<String, StanzaAttribute> attributes,
                        SizedInputStream content) implements Stanza {
        /**
         * Builds a stream stanza, rejecting {@code null} arguments.
         *
         * @throws NullPointerException if any argument is {@code null}
         */
        public StreamStanza {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
            Objects.requireNonNull(content, "content cannot be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SequencedMap<String, StanzaAttribute> attributes() {
            return Collections.unmodifiableSequencedMap(attributes);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation drains a fresh stream from {@link #content()} into
         * a byte array on each call.
         */
        @Override
        public Optional<byte[]> toContentBytes() {
            try (var stream = content.openStream()) {
                return Optional.of(stream.readAllBytes());
            } catch (IOException exception) {
                throw new UncheckedIOException("Failed to read stanza content stream", exception);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<InputStream> toContentStream() {
            return Optional.of(content.openStream());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<Jid> toContentJid() {
            try {
                return Optional.of(Jid.of(ProtobufString.lazy(toContentBytes().orElseThrow())));
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> toContentString() {
            return toContentBytes().map(String::new);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent(String content) {
            return toContentString().map(decoded -> decoded.equals(content)).orElse(false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent(Jid content) {
            return content != null && hasContent(content.toString());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent(byte[] content) {
            return Arrays.equals(toContentBytes().orElse(null), content);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SequencedCollection<Stanza> children() {
            return List.of();
        }

        /**
         * Returns whether {@code o} is a {@code StreamStanza} with the same
         * description, attributes, and sized-stream identity.
         *
         * @implNote
         * This implementation compares the sized stream by identity rather than
         * draining it, so equality never reads the stream.
         *
         * @param o the object to compare against
         * @return {@code true} when {@code o} is an identical stream stanza
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof StreamStanza(var thatDescription, var thatAttributes, var thatContent)
                    && Objects.equals(description, thatDescription)
                    && Objects.equals(attributes, thatAttributes)
                    && content == thatContent;
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash of the description, attribute map, and content length
         */
        @Override
        public int hashCode() {
            return Objects.hash(description, attributes, content.length());
        }

        /**
         * Returns a debug-oriented string that records the content length
         * without draining the stream.
         *
         * @return the debug string
         */
        @Override
        public String toString() {
            var result = new StringBuilder();
            result.append("Stanza[description=");
            result.append(description());
            if (!attributes.isEmpty()) {
                result.append(", attributes=");
                result.append(attributes);
            }
            result.append(", content=stream(");
            result.append(content.length());
            result.append(" bytes)]");
            return result.toString();
        }
    }

    /**
     * Stanza variant whose content is a sequence of child nodes.
     *
     * <p>The recursive case of the stanza tree: a container stanza groups a list
     * of children under a common tag, matching the XML element-with-children
     * shape used pervasively across the WhatsApp protocol (iq results, group
     * payloads, syncd collections, and so on).
     *
     * @param description the tag name
     * @param attributes  the attribute map
     * @param children    the child nodes in declaration order
     */
    record ContainerStanza(String description, SequencedMap<String, StanzaAttribute> attributes, SequencedCollection<Stanza> children) implements Stanza {
        /**
         * Builds a container stanza, rejecting {@code null} arguments.
         *
         * @throws NullPointerException if any argument is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WAXmlNode", exports = "XmlNode",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public ContainerStanza {
            Objects.requireNonNull(description, "description cannot be null");
            Objects.requireNonNull(attributes, "attributes cannot be null");
            Objects.requireNonNull(children, "children cannot be null");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SequencedMap<String, StanzaAttribute> attributes() {
            return Collections.unmodifiableSequencedMap(attributes);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SequencedCollection<Stanza> children() {
            return Collections.unmodifiableSequencedCollection(children);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent(Jid content) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent(byte[] content) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasContent(String content) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<byte[]> toContentBytes() {
            return Optional.empty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<String> toContentString() {
            return Optional.empty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<Jid> toContentJid() {
            return Optional.empty();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Optional<InputStream> toContentStream() {
            return Optional.empty();
        }

        /**
         * Returns whether {@code o} is a {@link Stanza} that compares
         * structurally equal to this one.
         *
         * <p>Cross-variant equality matches the wire-level reality: a container
         * with an empty child list compares equal to an {@link EmptyStanza} of
         * the same description and attributes; matches against {@link TextStanza},
         * {@link JidStanza}, and {@link BytesStanza} use the appropriate
         * {@code hasContent} variant on the other side of the comparison.
         *
         * @param o the object to compare against
         * @return {@code true} when the two nodes match structurally
         */
        @Override
        public boolean equals(Object o) {
            return switch (o) {
                case EmptyStanza(var thatDescription, var thatAttributes) -> Objects.equals(description, thatDescription)
                                                                             && Objects.equals(attributes, thatAttributes);
                case TextStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                                                                                             && Objects.equals(attributes, thatAttributes)
                                                                                             && hasContent(thatContent);
                case JidStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                                                                                            && Objects.equals(attributes, thatAttributes)
                                                                                            && hasContent(thatContent);
                case BytesStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                                                                                              && Objects.equals(attributes, thatAttributes)
                                                                                              && hasContent(thatContent);
                case ContainerStanza(var thatDescription, var thatAttributes, var thatContent) -> Objects.equals(description, thatDescription)
                                                                                                  && Objects.equals(attributes, thatAttributes)
                                                                                                  && Objects.equals(children, thatContent);
                case null, default -> false;
            };
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash of the description, attribute map, and child list
         */
        @Override
        public int hashCode() {
            return Objects.hash(description, attributes, children);
        }

        /**
         * Returns a debug-oriented string for this container stanza.
         *
         * <p>The output is not part of the wire format and exists only for
         * logs and stack traces; children are rendered recursively through
         * their own {@code toString}.
         *
         * @return the debug string
         */
        @Override
        @WhatsAppWebExport(moduleName = "WAXmlNode",
                exports = {"XmlNode", "attrsToString"},
                adaptation = WhatsAppAdaptation.ADAPTED)
        public String toString() {
            var result = new StringBuilder();
            result.append("Stanza[description=");
            result.append(description());

            if(!attributes.isEmpty()) {
                result.append(", attributes=");
                result.append(attributes);
            }

            if(!children.isEmpty()) {
                result.append(", children=");
                result.append(children);
            }

            result.append("]");

            return result.toString();
        }
    }
}
