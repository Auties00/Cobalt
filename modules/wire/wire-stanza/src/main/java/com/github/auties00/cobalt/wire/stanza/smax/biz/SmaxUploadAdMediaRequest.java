package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The outbound stanza that registers one or more uploaded media identifiers as CTWA native-ad assets on the
 * relay.
 * Drives the CTWA native-ad linking flow that links a freshly-uploaded media id to the Facebook ad backend so
 * it becomes available there for ad composition. Carries an optional primary {@link SmaxUploadAdMediaMediaEntry}
 * plus {@code 0..10} additional list entries; the link-in-Facebook caller passes a single primary entry.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutBizCtwaNativeAdUploadAdMediaRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutBizCtwaNativeAdHackBaseIQSetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutBizCtwaNativeAdBaseIQSetRequestMixin")
public final class SmaxUploadAdMediaRequest implements SmaxStanza.Request {
    /**
     * The optional user JID echoed onto the outbound IQ's {@code from} attribute; {@code null} omits the
     * attribute.
     */
    private final Jid iqFrom;

    /**
     * The optional primary {@code <media/>} entry; {@code null} omits the child.
     */
    private final SmaxUploadAdMediaMediaEntry media;

    /**
     * The {@code <media_list/>} entries ({@code 0..10}).
     */
    private final List<SmaxUploadAdMediaMediaEntry> mediaList;

    /**
     * Constructs a request with no {@code from} echo, no primary media child, and an empty media list.
     * Serves as a starting point in tests; production callers normally use the two- or three-argument
     * constructor with at least one entry.
     */
    public SmaxUploadAdMediaRequest() {
        this(null, null, List.of());
    }

    /**
     * Constructs a request with no {@code from} echo.
     * Matches the link-in-Facebook caller, which passes a single {@code (mediaId, "image")} primary entry and
     * no extra list entries.
     *
     * @param media     the optional primary media entry; may be {@code null}
     * @param mediaList the additional list entries; never {@code null}; at most 10 entries
     * @throws NullPointerException     if {@code mediaList} is {@code null}
     * @throws IllegalArgumentException if {@code mediaList} contains more than 10 entries
     */
    public SmaxUploadAdMediaRequest(SmaxUploadAdMediaMediaEntry media, List<SmaxUploadAdMediaMediaEntry> mediaList) {
        this(null, media, mediaList);
    }

    /**
     * Constructs a request, optionally echoing the supplied user JID onto the {@code from} attribute.
     * Lets multi-device callers make the outbound IQ look like it originated from a specific linked user JID;
     * standard callers pass {@code null} to omit the attribute.
     *
     * @param iqFrom    the optional user JID; may be {@code null}
     * @param media     the optional primary media entry; may be {@code null}
     * @param mediaList the additional list entries; never {@code null}; at most 10 entries
     * @throws NullPointerException     if {@code mediaList} is {@code null}
     * @throws IllegalArgumentException if {@code mediaList} contains more than 10 entries
     */
    public SmaxUploadAdMediaRequest(Jid iqFrom, SmaxUploadAdMediaMediaEntry media, List<SmaxUploadAdMediaMediaEntry> mediaList) {
        Objects.requireNonNull(mediaList, "mediaList cannot be null");
        if (mediaList.size() > 10) {
            throw new IllegalArgumentException("mediaList must contain at most 10 entries");
        }
        this.iqFrom = iqFrom;
        this.media = media;
        this.mediaList = List.copyOf(mediaList);
    }

    /**
     * Returns the optional {@code from} echo.
     * Returns {@link Optional#empty()} when the request was built without a {@code from} echo.
     *
     * @return an {@link Optional} carrying the user JID
     */
    public Optional<Jid> iqFrom() {
        return Optional.ofNullable(iqFrom);
    }

    /**
     * Returns the optional primary media entry.
     * Returns {@link Optional#empty()} when the request was built without a primary {@code <media/>} child.
     *
     * @return an {@link Optional} carrying the entry
     */
    public Optional<SmaxUploadAdMediaMediaEntry> media() {
        return Optional.ofNullable(media);
    }

    /**
     * Returns the additional media-list entries.
     * Returns an empty list when no extra entries were supplied.
     *
     * @return an unmodifiable list of {@code 0..10} entries; never {@code null}
     */
    public List<SmaxUploadAdMediaMediaEntry> mediaList() {
        return mediaList;
    }

    /**
     * {@inheritDoc}
     * Stamps {@code xmlns="fb:thrift_iq"}, {@code type="set"}, {@code to="s.whatsapp.net"} and the optional
     * {@code from} echo, then emits the optional primary {@code <media/>} child followed by the {@code 0..10}
     * {@code <media_list/>} children. The IQ {@code id} is assigned by the dispatcher.
     *
     * @return the outbound stanza builder; never {@code null}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutBizCtwaNativeAdUploadAdMediaRequest",
            exports = "makeUploadAdMediaRequest", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizCtwaNativeAdUploadAdMediaRequest",
            exports = "makeUploadAdMediaRequestMedia", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizCtwaNativeAdUploadAdMediaRequest",
            exports = "makeUploadAdMediaRequestMediaList", adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizCtwaNativeAdHackBaseIQSetRequestMixin",
            exports = "mergeHackBaseIQSetRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WASmaxOutBizCtwaNativeAdBaseIQSetRequestMixin",
            exports = "mergeBaseIQSetRequestMixin", adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var children = new ArrayList<Stanza>();
        if (media != null) {
            var mediaNode = new StanzaBuilder()
                    .description("media")
                    .attribute("id", media.id())
                    .attribute("type", media.type().wire())
                    .build();
            children.add(mediaNode);
        }
        for (var entry : mediaList) {
            var entryNode = new StanzaBuilder()
                    .description("media_list")
                    .attribute("id", entry.id())
                    .attribute("type", entry.type().wire())
                    .build();
            children.add(entryNode);
        }
        var builder = new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "fb:thrift_iq")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(children);
        if (iqFrom != null) {
            builder.attribute("from", iqFrom);
        }
        return builder;
    }

    /**
     * Compares this request with another object for equality.
     * Two requests are equal when the {@code from} echo, the primary media entry, and the media list all match.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is a request with the same {@code from}, {@code media}, and
     *         {@code mediaList}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxUploadAdMediaRequest) obj;
        return Objects.equals(this.iqFrom, that.iqFrom)
                && Objects.equals(this.media, that.media)
                && Objects.equals(this.mediaList, that.mediaList);
    }

    /**
     * Returns a hash code derived from the {@code from} echo, the primary media entry, and the media list.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(iqFrom, media, mediaList);
    }

    /**
     * Returns a debug representation listing the {@code from} echo, the primary media entry, and the media list.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "SmaxUploadAdMediaRequest[iqFrom=" + iqFrom
                + ", media=" + media
                + ", mediaList=" + mediaList + ']';
    }
}
