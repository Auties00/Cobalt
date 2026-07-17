package com.github.auties00.cobalt.media.transcode;

import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.media.MediaConnectionService;
import com.github.auties00.cobalt.media.MediaPayload;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.wire.linked.message.text.ExtendedTextMessage;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Prepares outgoing media for upload to the WhatsApp CDN by dispatching to the matching
 * per-format pipeline.
 *
 * <p>This service is consulted from two call sites. The byte-level
 * {@link #transcode(MediaProvider, Path)} and its {@link InputStream} adapter encode the
 * user-supplied source into the wire format WhatsApp expects, apply the resulting codec metadata
 * to the {@link MediaProvider} in place, and return the {@link MediaPayload} that the upload
 * layer ({@link MediaConnectionService#upload(MediaProvider, MediaPayload)}) reads from. The
 * orchestration entry point {@link #decorate(Jid, ExtendedTextMessage)} enriches a text message
 * with a rich link-preview card.
 *
 * @implSpec
 * Implementations must apply the codec-derived fields to the supplied {@link MediaProvider} in
 * place and must route every {@link MediaProvider} variant to the format that matches it.
 */
public interface MediaTranscoderService {
    /**
     * Transcodes the source file for the upload slot encoded by {@code provider}
     * and applies the resulting codec metadata to {@code provider} in place.
     *
     * <p>This is the primary entry point: when the caller already holds a file on
     * disk this variant avoids any source-side spill. The returned
     * {@link MediaPayload} carries the encoded plaintext; the caller must invoke
     * {@link MediaPayload#close()} after the upload completes to release any owned temp file.
     * {@link MediaProvider} variants without a dedicated byte-level pipeline pass through as a
     * non-owning payload that references {@code source}, so {@link MediaPayload#close()} leaves
     * the caller's file alone.
     *
     * @implSpec
     * Implementations must apply codec-derived fields to {@code provider} and must return a
     * payload whose {@link MediaPayload#close()} releases only resources the payload owns.
     *
     * @param provider the upload target; codec-derived fields are applied to this
     *                 instance; must not be {@code null}
     * @param source   the raw user-provided file path; must not be {@code null}
     * @return the encoded payload
     * @throws WhatsAppMediaException.Processing if the selected pipeline fails to
     *                                           decode, encode, or buffer the
     *                                           source
     * @throws NullPointerException              if {@code provider} or
     *                                           {@code source} is {@code null}
     */
    MediaPayload transcode(MediaProvider provider, Path source)
            throws WhatsAppMediaException.Processing;

    /**
     * Transcodes the source stream for the upload slot encoded by
     * {@code provider} and applies the resulting codec metadata to
     * {@code provider} in place.
     *
     * <p>This convenience entry point serves callers that hold a stream rather
     * than a file. The stream is closed on every exit path.
     *
     * @implSpec
     * Implementations must close {@code source} before returning and must apply codec-derived
     * fields to {@code provider}.
     *
     * @param provider the upload target; codec-derived fields are applied to this
     *                 instance; must not be {@code null}
     * @param source   the raw user-provided stream; closed before this method
     *                 returns; must not be {@code null}
     * @return the encoded payload
     * @throws WhatsAppMediaException.Processing if buffering, decoding, encoding,
     *                                           or muxing fails
     * @throws NullPointerException              if {@code provider} or
     *                                           {@code source} is {@code null}
     */
    MediaPayload transcode(MediaProvider provider, InputStream source)
            throws WhatsAppMediaException.Processing;

    /**
     * Resolves the first URL in {@code message}'s body into a rich link-preview
     * card and stamps the result onto {@code message}.
     *
     * <p>Mutates {@code message} in place with the resolved preview metadata. When no resolvable
     * URL is present the message is left unchanged.
     *
     * @implSpec
     * Implementations must mutate {@code message} in place and must leave it unchanged when no
     * resolvable URL is present.
     *
     * @param chatJid the target chat JID
     * @param message the outgoing message, mutated in place
     */
    void decorate(Jid chatJid, ExtendedTextMessage message);
}
