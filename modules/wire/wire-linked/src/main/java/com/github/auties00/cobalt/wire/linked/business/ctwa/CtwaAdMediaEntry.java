package com.github.auties00.cobalt.wire.linked.business.ctwa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * Single Click-to-WhatsApp ad-media entry.
 *
 * <p>Each ad creative the WhatsApp Business client uploads to the
 * native-ad media service is identified by an opaque relay-allocated
 * identifier and one of the documented {@link CtwaAdMediaType media
 * kinds}. Both the primary creative slot and the additional 0..10
 * media-list slots use the same {@code (id, type)} shape.
 */
@ProtobufMessage(name = "CtwaAdMediaEntry")
public final class CtwaAdMediaEntry {
    /**
     * The relay-allocated media identifier.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * The media kind.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    CtwaAdMediaType type;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param id   the media identifier
     * @param type the media kind
     */
    CtwaAdMediaEntry(String id, CtwaAdMediaType type) {
        this.id = id;
        this.type = type;
    }

    /**
     * Returns the relay-allocated media identifier.
     *
     * @return the identifier; never {@code null} for a parsed entry
     */
    public String id() {
        return id;
    }

    /**
     * Returns the media kind.
     *
     * @return the kind; never {@code null} for a parsed entry
     */
    public CtwaAdMediaType type() {
        return type;
    }
}
