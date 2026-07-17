package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;

/**
 * The {@code (id, type)} pair carried on the CTWA native-ad upload-media stanzas.
 * Backs both the outbound primary {@code <media/>} child and each of the {@code 0..10} {@code <media_list/>}
 * children built by {@link SmaxUploadAdMediaRequest#toStanza()}, and the matching inbound echoes parsed by
 * {@link SmaxUploadAdMediaResponse.Success#of(Stanza, Stanza)}.
 * The {@code id} is the relay-allocated media identifier produced by a prior upload; the {@code type}
 * classifies the asset as {@link SmaxUploadAdMediaMediaType#IMAGE} or {@link SmaxUploadAdMediaMediaType#VIDEO}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBizCtwaNativeAdUploadAdMediaRequest")
@WhatsAppWebModule(moduleName = "WASmaxInBizCtwaNativeAdUploadAdMediaResponseSuccess")
public final class SmaxUploadAdMediaMediaEntry {
    /**
     * The relay-allocated media identifier.
     */
    private final String id;

    /**
     * The asset kind classifier.
     */
    private final SmaxUploadAdMediaMediaType type;

    /**
     * Constructs a new entry from the supplied identifier and type.
     *
     * @param id   the relay-allocated media identifier; never {@code null}
     * @param type the asset kind; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public SmaxUploadAdMediaMediaEntry(String id, SmaxUploadAdMediaMediaType type) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
    }

    /**
     * Returns the relay-allocated media identifier.
     * Surfaces as the {@code id} attribute on the corresponding {@code <media/>} or {@code <media_list/>} child.
     *
     * @return the identifier; never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Returns the asset kind classifier.
     * Surfaces as the {@code type} attribute on the corresponding {@code <media/>} or {@code <media_list/>}
     * child via {@link SmaxUploadAdMediaMediaType#wire()}.
     *
     * @return the type; never {@code null}
     */
    public SmaxUploadAdMediaMediaType type() {
        return type;
    }

    /**
     * Compares this entry with another object for equality.
     * Two entries are equal when both the {@code id} and the {@code type} match.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an entry with the same {@code id} and {@code type}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxUploadAdMediaMediaEntry) obj;
        return Objects.equals(this.id, that.id) && this.type == that.type;
    }

    /**
     * Returns a hash code derived from the {@code id} and the {@code type}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    /**
     * Returns a debug representation listing the {@code id} and the {@code type}.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "SmaxUploadAdMediaMediaEntry[id=" + id + ", type=" + type + ']';
    }
}
