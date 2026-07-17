package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.Arrays;
import java.util.Objects;

/**
 * Builds the {@code <iq xmlns="w:biz" type="set">} stanza that attaches a previously-uploaded cover
 * photo to the current merchant's business profile.
 *
 * <p>The stanza is sent from the cover-photo edit surface after the binary upload has succeeded and
 * the media upload service has returned the {@code (id, ts, token)} triple identifying the artefact;
 * the relay uses the token to validate that the upload still exists and belongs to the calling user.
 */
@WhatsAppWebModule(moduleName = "WAWebBusinessProfileJob")
public final class IqSendCoverPhotoRequest implements IqStanza.Request {
    /**
     * Holds the upload id stamped into the {@code id} attribute of the {@code <cover_photo/>}
     * grandchild.
     */
    private final long id;

    /**
     * Holds the upload timestamp in Unix seconds, stamped into the {@code ts} attribute of the
     * {@code <cover_photo/>} grandchild.
     */
    private final long ts;

    /**
     * Holds the opaque upload token stamped into the {@code token} attribute of the
     * {@code <cover_photo/>} grandchild; the relay uses it to validate that the upload artefact
     * still exists and belongs to the calling user.
     */
    private final byte[] token;

    /**
     * Constructs a request from the {@code (id, ts, token)} triple returned by the media upload
     * service.
     *
     * <p>The token is defensively cloned so the caller's buffer is not retained.
     *
     * @param id    the upload id
     * @param ts    the upload timestamp
     * @param token the upload token; never {@code null}
     * @throws NullPointerException if {@code token} is {@code null}
     */
    public IqSendCoverPhotoRequest(long id, long ts, byte[] token) {
        this.id = id;
        this.ts = ts;
        Objects.requireNonNull(token, "token cannot be null");
        this.token = token.clone();
    }

    /**
     * Returns the upload id, taken verbatim from the media upload service response.
     *
     * @return the id
     */
    public long id() {
        return id;
    }

    /**
     * Returns the upload timestamp, taken verbatim from the media upload service response.
     *
     * @return the timestamp
     */
    public long ts() {
        return ts;
    }

    /**
     * Returns a defensive copy of the upload token.
     *
     * <p>The returned array is a fresh clone so callers may mutate it freely without disturbing this
     * request.
     *
     * @return the token; never {@code null}
     */
    public byte[] token() {
        return token.clone();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation materialises a {@code <cover_photo op="update" id ts token/>} grandchild
     * wrapped in a {@code <business_profile v="3" mutation_type="delta"/>} envelope and a
     * {@code w:biz set} IQ frame routed to the WhatsApp service.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBusinessProfileJob",
            exports = "sendCoverPhoto", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var coverPhotoNode = new StanzaBuilder()
                .description("cover_photo")
                .attribute("op", "update")
                .attribute("id", String.valueOf(id))
                .attribute("ts", String.valueOf(ts))
                .attribute("token", token)
                .build();
        var businessProfileNode = new StanzaBuilder()
                .description("business_profile")
                .attribute("v", "3")
                .attribute("mutation_type", "delta")
                .content(coverPhotoNode)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(businessProfileNode);
    }

    /**
     * Compares this request with another for value equality across the id, timestamp and token.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an equal request
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSendCoverPhotoRequest) obj;
        return this.id == that.id
                && this.ts == that.ts
                && Arrays.equals(this.token, that.token);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        var h = Objects.hash(id, ts);
        return 31 * h + Arrays.hashCode(token);
    }

    /**
     * Returns a diagnostic string naming the upload id and timestamp; the token is elided.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "IqSendCoverPhotoRequest[id=" + id + ", ts=" + ts + ']';
    }
}
