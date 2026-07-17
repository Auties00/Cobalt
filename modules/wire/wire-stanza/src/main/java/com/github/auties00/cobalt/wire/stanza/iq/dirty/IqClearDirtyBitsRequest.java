package com.github.auties00.cobalt.wire.stanza.iq.dirty;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds the outbound {@code <iq xmlns="urn:xmpp:whatsapp:dirty" type="set">} stanza that
 * acknowledges resynchronisation of a batch of dirty-bit resources.
 *
 * <p>When the relay raises an {@code <ib>} broadcast that one of the supported resource types
 * ({@code account_sync}, {@code groups}, {@code blocklist}, {@code syncd_app_state}, and
 * similar) needs a refresh, the client performs the corresponding sync and then sends this
 * request to clear the relay-side dirty marker so the broadcast is not repeated. Each
 * {@link DirtyEntry} in the batch becomes one {@code <clean/>} child of the {@code <iq>}
 * envelope produced by {@link #toStanza()}.
 */
@WhatsAppWebModule(moduleName = "WAWebClearDirtyBitsJob")
public final class IqClearDirtyBitsRequest implements IqStanza.Request {
    /**
     * Holds the dirty-bit entries to clear.
     *
     * <p>Never {@code null} or empty; the {@link #IqClearDirtyBitsRequest(List)} constructor
     * rejects an empty batch so the empty-batch invariant fails fast on the caller side.
     */
    private final List<DirtyEntry> entries;

    /**
     * Constructs a new clear-dirty-bits request from the given batch of entries.
     *
     * <p>The supplied list is defensively copied so subsequent mutation by the caller does not
     * affect the dispatched stanza.
     *
     * @param entries the dirty entries to clear
     * @throws NullPointerException     if {@code entries} is {@code null}
     * @throws IllegalArgumentException if {@code entries} is empty
     */
    public IqClearDirtyBitsRequest(List<DirtyEntry> entries) {
        Objects.requireNonNull(entries, "entries cannot be null");
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("entries cannot be empty");
        }
        this.entries = List.copyOf(entries);
    }

    /**
     * Returns the unmodifiable list of dirty entries being cleared.
     *
     * @return the entries, never {@code null} or empty
     */
    public List<DirtyEntry> entries() {
        return entries;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces a {@code <iq xmlns="urn:xmpp:whatsapp:dirty" type="set">} envelope addressed
     * to {@link Jid#userServer()} and wrapping one {@code <clean type="..." timestamp="..."/>}
     * child per {@link DirtyEntry}.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <iq>} envelope and the
     *         {@code <clean/>} payload children
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebClearDirtyBitsJob",
            exports = "clearDirtyBits", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var cleanNodes = new ArrayList<Stanza>(entries.size());
        for (var entry : entries) {
            var cleanNode = new StanzaBuilder()
                    .description("clean")
                    .attribute("type", entry.type())
                    .attribute("timestamp", entry.timestamp())
                    .build();
            cleanNodes.add(cleanNode);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "urn:xmpp:whatsapp:dirty")
                .attribute("to", Jid.userServer())
                .attribute("type", "set")
                .content(cleanNodes);
    }

    /**
     * Compares this request to another object for equality.
     *
     * <p>Two requests are equal when their {@link #entries()} lists are equal.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is an {@link IqClearDirtyBitsRequest} with equal
     *         entries, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqClearDirtyBitsRequest) obj;
        return Objects.equals(this.entries, that.entries);
    }

    /**
     * Returns a hash code derived from the {@link #entries()} list.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }

    /**
     * Returns a debug string describing this request and its entries.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "IqClearDirtyBitsRequest[entries=" + entries + ']';
    }

    /**
     * Carries the per-resource dirty-bit data that materialises as a single {@code <clean/>}
     * child of the outbound request.
     *
     * <p>Each entry pairs a resource {@code type} with the high-water-mark {@code timestamp}
     * the client reached during the corresponding sync; the relay only clears markers whose
     * own timestamp is at most this value.
     */
    @WhatsAppWebModule(moduleName = "WAWebClearDirtyBitsJob")
    public static final class DirtyEntry {
        /**
         * Holds the resource type for the dirty marker.
         *
         * <p>One of the documented resource keys ({@code account_sync}, {@code groups},
         * {@code blocklist}, {@code syncd_app_state}, and similar); routed verbatim into the
         * {@code type} attribute of the {@code <clean/>} stanza.
         */
        private final String type;

        /**
         * Holds the high-water-mark timestamp the relay treats as the clean-as-of point.
         *
         * <p>Routed verbatim into the {@code timestamp} attribute of the {@code <clean/>}
         * stanza and expressed in the relay's native dirty-bit clock units.
         */
        private final long timestamp;

        /**
         * Constructs a new dirty-bit entry from a resource type and high-water-mark timestamp.
         *
         * @param type      the resource type
         * @param timestamp the high-water-mark timestamp
         * @throws NullPointerException if {@code type} is {@code null}
         */
        public DirtyEntry(String type, long timestamp) {
            this.type = Objects.requireNonNull(type, "type cannot be null");
            this.timestamp = timestamp;
        }

        /**
         * Returns the resource type.
         *
         * @return the type, never {@code null}
         */
        public String type() {
            return type;
        }

        /**
         * Returns the high-water-mark timestamp.
         *
         * @return the timestamp
         */
        public long timestamp() {
            return timestamp;
        }

        /**
         * Compares this entry to another object for equality.
         *
         * <p>Two entries are equal when both their {@link #type()} and {@link #timestamp()}
         * are equal.
         *
         * @param obj the object to compare against
         * @return {@code true} if {@code obj} is a {@link DirtyEntry} with equal type and
         *         timestamp, {@code false} otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (DirtyEntry) obj;
            return this.timestamp == that.timestamp
                    && Objects.equals(this.type, that.type);
        }

        /**
         * Returns a hash code derived from the {@link #type()} and {@link #timestamp()}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(type, timestamp);
        }

        /**
         * Returns a debug string describing this entry's type and timestamp.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "Request.DirtyEntry[type=" + type
                    + ", timestamp=" + timestamp + ']';
        }
    }
}
