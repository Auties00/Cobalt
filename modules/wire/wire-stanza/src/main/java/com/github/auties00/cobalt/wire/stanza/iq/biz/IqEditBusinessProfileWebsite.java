package com.github.auties00.cobalt.wire.stanza.iq.biz;

import java.util.Objects;

/**
 * Models one typed website slot carried inside the {@code <website/>} children of an {@link IqEditBusinessProfileRequest}.
 *
 * <p>A slot expresses one of the at-most-two website URLs the SMB profile editor shows alongside
 * the merchant's contact details. An empty URL clears the slot while preserving the
 * {@code <website/>} envelope structure in the edit-profile mutation.
 *
 * @implNote
 * This implementation mirrors the wire shape produced by {@code WAWebBusinessProfileJob.editBusinessProfile}:
 * the request consumes at most the first two entries of the website list and emits one
 * {@code <website/>} child per entry, with the URL routed verbatim into the child content.
 */
public final class IqEditBusinessProfileWebsite {
    /**
     * Holds the website URL routed verbatim into the {@code <website/>} child content.
     *
     * <p>An empty string clears the slot.
     */
    private final String url;

    /**
     * Constructs a typed website slot from the given URL.
     *
     * <p>An empty string clears the slot while preserving the {@code <website/>} envelope structure.
     *
     * @param url the website URL; never {@code null}
     * @throws NullPointerException if {@code url} is {@code null}
     */
    public IqEditBusinessProfileWebsite(String url) {
        this.url = Objects.requireNonNull(url, "url cannot be null");
    }

    /**
     * Returns the website URL this slot stamps.
     *
     * <p>An empty string clears the slot.
     *
     * @return the URL; never {@code null}
     */
    public String url() {
        return url;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqEditBusinessProfileWebsite) obj;
        return Objects.equals(this.url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "IqEditBusinessProfileWebsite[url=" + url + ']';
    }
}
