package com.github.auties00.cobalt.stanza.model;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;

import java.util.*;

/**
 * Fluent builder for assembling {@link Stanza} stanzas.
 *
 * <p>Stanzas are the unit of communication with the WhatsApp server. Every
 * outbound stanza in Cobalt is produced through a builder: a tag name is set
 * with {@link #description(String)}, attributes are accumulated through one of
 * the {@code attribute} overloads, and the payload is set through one of the
 * {@code content} overloads. A final call to {@link #build()} freezes the
 * accumulated state into an immutable {@link Stanza} of the variant matching the
 * populated content slot.
 *
 * <p>The {@code attribute} overloads are null-safe: a {@code null} value skips
 * the attribute instead of writing a literal {@code "null"}, and the
 * {@code condition} overload skips when the guard is {@code false}. This
 * removes the null check that every caller would otherwise need at every
 * attribute site.
 *
 * <p>Building an iq stanza for a profile-picture upload:
 * {@snippet :
 *     Stanza iq = new StanzaBuilder()
 *             .description("iq")
 *             .attribute("id", "12345")
 *             .attribute("to", recipient)
 *             .attribute("type", "set")
 *             .attribute("xmlns", "w:profile:picture")
 *             .content(new StanzaBuilder()
 *                     .description("picture")
 *                     .attribute("type", "image")
 *                     .content(imageBytes)
 *                     .build())
 *             .build();
 *}
 *
 * <p>Building a simple presence stanza without content:
 * {@snippet :
 *     Stanza presence = new StanzaBuilder()
 *             .description("presence")
 *             .attribute("type", "available")
 *             .build();
 *}
 *
 * @implNote
 * The drop-on-null guard mirrors the {@code DROP_ATTR} sentinel handling in
 * the {@code WAWap.X} factory ({@code n!==F&&(u[t]=n)} where
 * {@code F = DROP_ATTR}).
 *
 * @see Stanza
 * @see StanzaAttribute
 */
@WhatsAppWebModule(moduleName = "WAWap")
public final class StanzaBuilder {
    /**
     * Holds the pending tag name of the stanza under construction.
     *
     * <p>Set by {@link #description(String)}. When {@code null} at
     * {@link #build()} time the resulting stanza carries an empty tag name.
     */
    private String description;

    /**
     * Holds the pending attributes accumulated in insertion order.
     *
     * <p>Insertion order is preserved so that the encoder emits attributes in
     * the order they were added; the server is order-agnostic on the semantic
     * level, so the only requirement is that the stanza round-trips stably.
     *
     * @implNote
     * This implementation backs the map with a {@link LinkedHashMap} to give
     * the deterministic iteration order the encoder relies on.
     */
    private final SequencedMap<String, StanzaAttribute> attributes;

    /**
     * Holds the pending text content, or {@code null} when not set.
     *
     * <p>One of the four content slots; {@link #build()} dispatches on the
     * first non-null slot in the order text, JID, bytes, children.
     */
    private String textContent;

    /**
     * Holds the pending JID content, or {@code null} when not set.
     *
     * <p>Stored as a {@link JidProvider} so callers can pass a {@link Jid}
     * directly or any provider that resolves to one; the resolution to a
     * concrete {@link Jid} is deferred to {@link #build()}.
     */
    private JidProvider jidContent;

    /**
     * Holds the pending binary content, or {@code null} when not set.
     *
     * <p>Carries Signal ciphertext, media thumbnails, and any other payload
     * that must traverse the wire as opaque bytes.
     */
    private byte[] bytesContent;

    /**
     * Holds the pending child stanza content, or {@code null} when not set.
     *
     * <p>Lazily allocated to a fresh {@link ArrayList} the first time a
     * {@code content(Stanza...)} or {@code content(SequencedCollection)}
     * overload is invoked; subsequent calls append.
     */
    private SequencedCollection<Stanza> childrenContent;

    /**
     * Holds the pending streamed binary content, or {@code null} when not set.
     *
     * <p>Set by {@link #content(SizedInputStream)} so a large payload can be
     * streamed to the wire lazily. Checked after the eager content slots in
     * {@link #build()}, so an eager content call always wins over a stale
     * stream slot.
     */
    private SizedInputStream streamContent;

    /**
     * Builds a fresh builder with no description, no attributes, and no
     * content.
     *
     * <p>The default starting point for every outbound stanza in Cobalt.
     */
    public StanzaBuilder() {
        this.attributes = new LinkedHashMap<>();
    }

    /**
     * Sets the description (tag name) of the stanza under construction.
     *
     * <p>Mandatory in practice, since every meaningful stanza has a tag, but
     * not enforced by the builder; an unset description produces a stanza with
     * an empty tag name.
     *
     * @param description the tag name
     * @return this builder
     */
    public StanzaBuilder description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Adds a text attribute when the value is non null.
     *
     * <p>Passing {@code null} silently drops the attribute; use the
     * {@code condition}-taking overload for an explicit guard. Every other
     * {@code attribute} overload follows the same drop-on-null convention.
     *
     * @param key   the attribute key
     * @param value the attribute value, or {@code null} to skip
     * @return this builder
     */
    @WhatsAppWebExport(moduleName = "WAWap", exports = "DROP_ATTR",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder attribute(String key, String value) {
        if(value != null) {
            this.attributes.put(key, new StanzaAttribute.TextAttribute(value));
        }
        return this;
    }

    /**
     * Adds a text attribute when the value is non null and the condition is
     * {@code true}.
     *
     * <p>Removes the {@code if (cond) builder.attribute(key, value);}
     * boilerplate at every conditional call site.
     *
     * @param key       the attribute key
     * @param value     the attribute value, or {@code null} to skip
     * @param condition guard that must hold for the attribute to be written
     * @return this builder
     */
    public StanzaBuilder attribute(String key, String value, boolean condition) {
        if(value != null && condition) {
            this.attributes.put(key, new StanzaAttribute.TextAttribute(value));
        }
        return this;
    }

    /**
     * Adds a numeric attribute when the value is non null.
     *
     * <p>The number is serialised through {@link Number#toString()} into a
     * decimal string before the encoder runs the standard string compression
     * ladder.
     *
     * @param key   the attribute key
     * @param value the numeric value, or {@code null} to skip
     * @return this builder
     */
    public StanzaBuilder attribute(String key, Number value) {
        if(value != null) {
            this.attributes.put(key, new StanzaAttribute.TextAttribute(value.toString()));
        }
        return this;
    }

    /**
     * Adds a numeric attribute when the value is non null and the condition is
     * {@code true}.
     *
     * <p>The number is serialised through {@link Number#toString()}.
     *
     * @param key       the attribute key
     * @param value     the numeric value, or {@code null} to skip
     * @param condition guard that must hold for the attribute to be written
     * @return this builder
     */
    public StanzaBuilder attribute(String key, Number value, boolean condition) {
        if(value != null && condition) {
            this.attributes.put(key, new StanzaAttribute.TextAttribute(value.toString()));
        }
        return this;
    }

    /**
     * Adds a boolean attribute serialised under the
     * {@link StanzaBooleanFormat#LENIENT} format.
     *
     * <p>The lenient format encodes through the {@code "true"}/{@code "false"}
     * pair, so this overload preserves the historical wire shape. Use
     * {@link #attribute(String, boolean, StanzaBooleanFormat)} for a stanza family
     * that carries booleans as {@code "1"}/{@code "0"}.
     *
     * @param key   the attribute key
     * @param value the boolean value
     * @return this builder
     */
    public StanzaBuilder attribute(String key, boolean value) {
        return attribute(key, value, StanzaBooleanFormat.LENIENT);
    }

    /**
     * Adds a boolean attribute serialised under the supplied
     * {@link StanzaBooleanFormat}.
     *
     * <p>The value is encoded through {@link StanzaBooleanFormat#encode(boolean)};
     * the WhatsApp wire protocol carries booleans as strings whose literal form
     * differs by stanza family, so the format selects which convention to emit.
     *
     * @param key    the attribute key
     * @param value  the boolean value
     * @param format the format used to encode the value
     * @return this builder
     * @throws NullPointerException if {@code format} is {@code null}
     */
    public StanzaBuilder attribute(String key, boolean value, StanzaBooleanFormat format) {
        Objects.requireNonNull(format, "format cannot be null");
        this.attributes.put(key, new StanzaAttribute.TextAttribute(format.encode(value)));
        return this;
    }

    /**
     * Adds a boolean attribute serialised under the
     * {@link StanzaBooleanFormat#LENIENT} format when the condition is
     * {@code true}.
     *
     * @param key       the attribute key
     * @param value     the boolean value
     * @param condition guard that must hold for the attribute to be written
     * @return this builder
     */
    public StanzaBuilder attribute(String key, boolean value, boolean condition) {
        return attribute(key, value, condition, StanzaBooleanFormat.LENIENT);
    }

    /**
     * Adds a boolean attribute serialised under the supplied
     * {@link StanzaBooleanFormat} when the condition is {@code true}.
     *
     * @param key       the attribute key
     * @param value     the boolean value
     * @param condition guard that must hold for the attribute to be written
     * @param format    the format used to encode the value
     * @return this builder
     * @throws NullPointerException if {@code format} is {@code null}
     */
    public StanzaBuilder attribute(String key, boolean value, boolean condition, StanzaBooleanFormat format) {
        Objects.requireNonNull(format, "format cannot be null");
        if(condition) {
            this.attributes.put(key, new StanzaAttribute.TextAttribute(format.encode(value)));
        }
        return this;
    }

    /**
     * Adds a JID attribute when the provider is non null.
     *
     * <p>The {@link JidProvider} is resolved eagerly into a concrete
     * {@link Jid} so the builder snapshot does not change if the provider's
     * state mutates later. {@code null} is accepted and skips the attribute.
     *
     * @param key   the attribute key
     * @param value the JID provider, or {@code null} to skip
     * @return this builder
     * @see Jid
     */
    public StanzaBuilder attribute(String key, JidProvider value) {
        if(value != null) {
            this.attributes.put(key, new StanzaAttribute.JidAttribute(value.toJid()));
        }
        return this;
    }

    /**
     * Adds a JID attribute when the provider is non null and the condition is
     * {@code true}.
     *
     * @param key       the attribute key
     * @param value     the JID provider, or {@code null} to skip
     * @param condition guard that must hold for the attribute to be written
     * @return this builder
     * @see Jid
     */
    public StanzaBuilder attribute(String key, JidProvider value, boolean condition) {
        if(value != null && condition) {
            this.attributes.put(key, new StanzaAttribute.JidAttribute(value.toJid()));
        }
        return this;
    }

    /**
     * Adds a binary attribute when the value is non null.
     *
     * <p>The blob is stored by reference; callers must not mutate the array
     * after handing it off. Carries already-encoded payloads (Signal
     * ciphertext, base64-decoded hashes, and similar) that should not pass
     * through the string compression ladder.
     *
     * @param key   the attribute key
     * @param value the binary value, or {@code null} to skip
     * @return this builder
     */
    public StanzaBuilder attribute(String key, byte[] value) {
        if(value != null) {
            this.attributes.put(key, new StanzaAttribute.BytesAttribute(value));
        }
        return this;
    }

    /**
     * Adds a binary attribute when the value is non null and the condition is
     * {@code true}.
     *
     * @param key       the attribute key
     * @param value     the binary value, or {@code null} to skip
     * @param condition guard that must hold for the attribute to be written
     * @return this builder
     */
    public StanzaBuilder attribute(String key, byte[] value, boolean condition) {
        if(value != null && condition) {
            this.attributes.put(key, new StanzaAttribute.BytesAttribute(value));
        }
        return this;
    }

    /**
     * Adds a binary attribute streamed lazily from the given sized stream.
     *
     * <p>The streaming counterpart of {@link #attribute(String, byte[])}: the
     * stream is not read here, so a large attribute payload can be streamed to
     * the wire without first materialising it into a {@code byte[]}. The wire
     * encoder emits the length prefix from the advertised
     * {@link SizedInputStream#length()} and drains a fresh stream once at
     * serialisation time.
     *
     * @param key   the attribute key
     * @param value the sized stream over the value, or {@code null} to skip
     * @return this builder
     */
    public StanzaBuilder attribute(String key, SizedInputStream value) {
        if(value != null) {
            this.attributes.put(key, new StanzaAttribute.StreamAttribute(value));
        }
        return this;
    }

    /**
     * Copies every entry from the supplied map into the pending attribute set,
     * preserving any prior attributes and overwriting on key collision.
     *
     * <p>Lets callers splice a pre-built attribute map into a fresh stanza,
     * for example forwarding a captured attribute set from one stanza to
     * another. A {@code null} map is treated as a no-op.
     *
     * @param attributes the entries to copy, or {@code null} to skip
     * @return this builder
     */
    public StanzaBuilder attributes(Map<String, ? extends StanzaAttribute> attributes) {
        if(attributes != null) {
            this.attributes.putAll(attributes);
        }
        return this;
    }

    /**
     * Sets the stanza content to a text value, clearing any previously set
     * content slot.
     *
     * <p>Carries free-form textual payloads (status blurbs, plain message
     * bodies) whose wire format passes through the string compression ladder.
     *
     * @param value the textual content
     * @return this builder
     */
    public StanzaBuilder content(String value) {
        this.textContent = value;
        this.jidContent = null;
        this.bytesContent = null;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the stanza content to a numeric value, clearing any previously set
     * content slot.
     *
     * <p>The number is serialised through {@link Objects#toString(Object)}.
     *
     * @param value the numeric content
     * @return this builder
     */
    public StanzaBuilder content(Number value) {
        this.textContent = Objects.toString(value);
        this.jidContent = null;
        this.bytesContent = null;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the stanza content to a boolean value serialised under the
     * {@link StanzaBooleanFormat#LENIENT} format, clearing any previously set
     * content slot.
     *
     * <p>The lenient format encodes through the {@code "true"}/{@code "false"}
     * pair. Use {@link #content(boolean, StanzaBooleanFormat)} for a stanza family
     * that carries booleans as {@code "1"}/{@code "0"}.
     *
     * @param value the boolean content
     * @return this builder
     */
    public StanzaBuilder content(boolean value) {
        return content(value, StanzaBooleanFormat.LENIENT);
    }

    /**
     * Sets the stanza content to a boolean value serialised under the supplied
     * {@link StanzaBooleanFormat}, clearing any previously set content slot.
     *
     * <p>The value is encoded through {@link StanzaBooleanFormat#encode(boolean)};
     * the WhatsApp wire protocol has no dedicated boolean shape, so the format
     * selects which literal convention to emit.
     *
     * @param value  the boolean content
     * @param format the format used to encode the value
     * @return this builder
     * @throws NullPointerException if {@code format} is {@code null}
     */
    public StanzaBuilder content(boolean value, StanzaBooleanFormat format) {
        Objects.requireNonNull(format, "format cannot be null");
        this.textContent = format.encode(value);
        this.jidContent = null;
        this.bytesContent = null;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the stanza content to a JID, clearing any previously set content
     * slot.
     *
     * <p>The provider is captured by reference and resolved lazily at
     * {@link #build()} time.
     *
     * @param value the JID provider
     * @return this builder
     * @see Jid
     */
    public StanzaBuilder content(JidProvider value) {
        this.textContent = null;
        this.jidContent = value;
        this.bytesContent = null;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the stanza content to a binary blob, clearing any previously set
     * content slot.
     *
     * <p>The blob is stored by reference; do not mutate the array after
     * passing it in.
     *
     * @param value the binary content
     * @return this builder
     */
    public StanzaBuilder content(byte[] value) {
        this.textContent = null;
        this.jidContent = null;
        this.bytesContent = value;
        this.childrenContent = null;
        return this;
    }

    /**
     * Sets the content of the stanza to a binary blob streamed lazily from the
     * given sized stream, clearing any content previously set.
     *
     * <p>Unlike {@link #content(byte[])}, the stream is not read here: the stanza
     * carries the sized stream and the wire encoder drains a fresh stream once
     * at serialisation time, emitting the length prefix from the advertised
     * {@link SizedInputStream#length()}. This lets callers stream a file or
     * network resource straight to the wire without materialising it into a
     * {@code byte[]}.
     *
     * @param content the sized stream over the content
     * @return this builder
     * @throws NullPointerException if {@code content} is {@code null}
     */
    public StanzaBuilder content(SizedInputStream content) {
        Objects.requireNonNull(content, "content cannot be null");
        this.textContent = null;
        this.jidContent = null;
        this.bytesContent = null;
        this.childrenContent = null;
        this.streamContent = content;
        return this;
    }

    /**
     * Appends a collection of child stanzas to the pending children list,
     * clearing any non-child content previously set.
     *
     * <p>Additive: a second call merges its argument into the children already
     * accumulated, preserving order. {@code null} entries inside the supplied
     * collection are skipped.
     *
     * @implNote
     * This implementation filters out {@code null} entries to match the
     * {@code WAWap.X} {@code Array.isArray(n)} branch, which applies
     * {@code filter(Boolean)}.
     *
     * @param stanzas the children to append, or {@code null} to skip
     * @return this builder
     */
    public StanzaBuilder content(SequencedCollection<Stanza> stanzas) {
        this.textContent = null;
        this.jidContent = null;
        this.bytesContent = null;
        if(childrenContent == null) {
            this.childrenContent = new ArrayList<>();
        }
        if(stanzas != null) {
            for(var stanza : stanzas) {
                if(stanza != null) {
                    this.childrenContent.add(stanza);
                }
            }
        }
        return this;
    }

    /**
     * Appends a varargs sequence of child stanzas to the pending children list,
     * clearing any non-child content previously set.
     *
     * <p>Additive: a second call merges its arguments into the children
     * already accumulated, preserving order. {@code null} entries are skipped.
     *
     * @param stanzas the children to append
     * @return this builder
     */
    public StanzaBuilder content(Stanza... stanzas) {
        this.textContent = null;
        this.jidContent = null;
        this.bytesContent = null;
        if(childrenContent == null) {
            this.childrenContent = new ArrayList<>();
        }
        if(stanzas != null) {
            for(var stanza : stanzas) {
                if(stanza != null) {
                    this.childrenContent.add(stanza);
                }
            }
        }
        return this;
    }

    /**
     * Returns whether an attribute with the supplied key is currently set on
     * this builder.
     *
     * <p>Lets callers that compose a stanza in branches decide whether to
     * override or skip based on what an earlier branch already added.
     *
     * @param key the attribute key
     * @return {@code true} when the attribute is present
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * Returns whether any content slot has been populated on this builder.
     *
     * <p>Lets callers conditionally add children or content depending on
     * whether the stanza is meant to remain empty.
     *
     * @return {@code true} when text, JID, binary, or child content has been
     *         set
     */
    public boolean hasContent() {
        return textContent != null
               || jidContent != null
               || bytesContent != null
               || childrenContent != null
               || streamContent != null;
    }

    /**
     * Returns the constructed immutable {@link Stanza}.
     *
     * <p>The concrete variant is selected from the first populated content
     * slot in the order text, JID, bytes, children, falling back to
     * {@link Stanza.EmptyStanza} when no slot is set. If no description was
     * supplied the stanza carries an empty tag name. The builder is not reset;
     * callers must construct a fresh {@code StanzaBuilder} for the next stanza.
     *
     * <ul>
     *   <li>{@link Stanza.TextStanza} when text content is set
     *   <li>{@link Stanza.JidStanza} when JID content is set
     *   <li>{@link Stanza.BytesStanza} when binary content is set
     *   <li>{@link Stanza.ContainerStanza} when child nodes are set
     *   <li>{@link Stanza.EmptyStanza} when no content slot is populated
     * </ul>
     *
     * @return the freshly built stanza
     * @see Stanza
     */
    @WhatsAppWebExport(moduleName = "WAWap", exports = "makeWapNode",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWap", exports = "wap",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public Stanza build() {
        var description = Objects.requireNonNullElse(this.description, "");
        if(textContent != null) {
            return new Stanza.TextStanza(description, attributes, textContent);
        }else if(jidContent != null){
            return new Stanza.JidStanza(description, attributes, jidContent.toJid());
        }else if(bytesContent != null){
            return new Stanza.BytesStanza(description, attributes, bytesContent);
        }else if(childrenContent != null){
            return new Stanza.ContainerStanza(description, attributes, childrenContent);
        }else if(streamContent != null){
            return new Stanza.StreamStanza(description, attributes, streamContent);
        }else {
            return new Stanza.EmptyStanza(description, attributes);
        }
    }
}
