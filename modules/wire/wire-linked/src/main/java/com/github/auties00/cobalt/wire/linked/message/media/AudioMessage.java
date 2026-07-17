package com.github.auties00.cobalt.wire.linked.message.media;

import com.github.auties00.cobalt.wire.linked.media.MediaPath;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveMessage;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * A message whose payload is an encrypted audio file.
 *
 * <p>Audio messages are used for two purposes in WhatsApp:
 * <ul>
 *     <li>Regular audio file attachments (music, recordings, podcasts)</li>
 *     <li>Push-to-talk (PTT) voice notes recorded directly in a chat</li>
 * </ul>
 *
 * <p>The audio content itself is encrypted and uploaded to WhatsApp servers; this
 * message only carries the metadata needed to download and decrypt it, together with
 * optional playback aids such as the waveform preview used for voice notes.
 */
@ProtobufMessage(name = "Message.AudioMessage")
public final class AudioMessage implements InteractiveMessage.Media, MediaMessage {
    /**
     * URL of the encrypted audio file on WhatsApp's media servers.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String mediaUrl;

    /**
     * MIME type of the audio payload, for example {@code audio/ogg; codecs=opus} for
     * voice notes or {@code audio/mpeg} for music files.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String mimetype;

    /**
     * SHA-256 digest of the decrypted audio bytes, used to verify integrity after
     * download and decryption.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] mediaSha256;

    /**
     * Size in bytes of the decrypted audio payload.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
    Long mediaSize;

    /**
     * Duration of the audio in whole seconds.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    Integer seconds;

    /**
     * Whether this audio is a push-to-talk voice note as opposed to an attached
     * audio file.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    Boolean ptt;

    /**
     * Symmetric key used to decrypt the audio payload once downloaded.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BYTES)
    byte[] mediaKey;

    /**
     * SHA-256 digest of the encrypted audio bytes as stored on the server, used to
     * detect tampering during transit.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BYTES)
    byte[] mediaEncryptedSha256;

    /**
     * Server-relative path used to locate the encrypted payload on WhatsApp's CDN.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String mediaDirectPath;

    /**
     * Moment at which the {@link #mediaKey} was generated, used to detect and rotate
     * stale keys.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant mediaKeyTimestamp;

    /**
     * Contextual information attached to the audio message, such as a quoted message
     * or forwarding metadata.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * Sidecar data used to enable streaming playback of encrypted audio before the
     * full download completes.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.BYTES)
    byte[] streamingSidecar;

    /**
     * Compact preview of audio amplitudes used to render the waveform visual for
     * voice notes.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.BYTES)
    byte[] waveform;

    /**
     * Background color to render behind the waveform, packed as an ARGB integer.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.FIXED32)
    Integer backgroundArgb;

    /**
     * Whether the audio can only be played once by the recipient before being
     * discarded.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.BOOL)
    Boolean viewOnce;

    /**
     * Accessibility label describing the audio content for screen readers.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.STRING)
    String accessibilityLabel;

    /**
     * Domain identifier that scopes how the {@link #mediaKey} was derived.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.ENUM)
    MediaMessageKeyDomain mediaKeyDomain;


    /**
     * Constructs a new audio message with the given metadata.
     *
     * @param mediaUrl             the CDN URL of the encrypted payload
     * @param mimetype             the MIME type of the audio
     * @param mediaSha256          the hash of the decrypted bytes
     * @param mediaSize            the size of the decrypted payload
     * @param seconds              the duration in seconds
     * @param ptt                  whether this is a push-to-talk voice note
     * @param mediaKey             the decryption key
     * @param mediaEncryptedSha256 the hash of the encrypted bytes
     * @param mediaDirectPath      the CDN direct path
     * @param mediaKeyTimestamp    when the key was generated
     * @param contextInfo          the context information
     * @param streamingSidecar     sidecar data enabling streaming decryption
     * @param waveform             the amplitude waveform preview
     * @param backgroundArgb       the waveform background color
     * @param viewOnce             whether the audio is single-play
     * @param accessibilityLabel   the accessibility description
     * @param mediaKeyDomain       the key derivation domain
     */
    AudioMessage(String mediaUrl, String mimetype, byte[] mediaSha256, Long mediaSize, Integer seconds, Boolean ptt, byte[] mediaKey, byte[] mediaEncryptedSha256, String mediaDirectPath, Instant mediaKeyTimestamp, ContextInfo contextInfo, byte[] streamingSidecar, byte[] waveform, Integer backgroundArgb, Boolean viewOnce, String accessibilityLabel, MediaMessageKeyDomain mediaKeyDomain) {
        this.mediaUrl = mediaUrl;
        this.mimetype = mimetype;
        this.mediaSha256 = mediaSha256;
        this.mediaSize = mediaSize;
        this.seconds = seconds;
        this.ptt = ptt;
        this.mediaKey = mediaKey;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
        this.mediaDirectPath = mediaDirectPath;
        this.mediaKeyTimestamp = mediaKeyTimestamp;
        this.contextInfo = contextInfo;
        this.streamingSidecar = streamingSidecar;
        this.waveform = waveform;
        this.backgroundArgb = backgroundArgb;
        this.viewOnce = viewOnce;
        this.accessibilityLabel = accessibilityLabel;
        this.mediaKeyDomain = mediaKeyDomain;
    }

    /**
     * Returns the CDN URL of the encrypted audio payload.
     *
     * @return the URL, or empty if not yet uploaded
     */
    public Optional<String> url() {
        return Optional.ofNullable(mediaUrl);
    }

    /**
     * Returns the CDN URL of the encrypted audio payload.
     *
     * @return the URL, or empty if not yet uploaded
     */
    @Override
    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    /**
     * Returns the MIME type of the audio payload.
     *
     * @return the MIME type, or empty if unknown
     */
    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    /**
     * Returns the SHA-256 digest of the decrypted audio.
     *
     * @return the hash, or empty if unknown
     */
    public Optional<byte[]> fileSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    /**
     * Returns the SHA-256 digest of the decrypted audio.
     *
     * @return the hash, or empty if unknown
     */
    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    /**
     * Returns the size in bytes of the decrypted audio.
     *
     * @return the size, or empty if unknown
     */
    public OptionalLong fileLength() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns the size in bytes of the decrypted audio.
     *
     * @return the size, or empty if unknown
     */
    @Override
    public OptionalLong mediaSize() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns the duration of the audio in seconds.
     *
     * @return the duration, or empty if unknown
     */
    public OptionalInt seconds() {
        return seconds == null ? OptionalInt.empty() : OptionalInt.of(seconds);
    }

    /**
     * Returns whether this audio is a push-to-talk voice note.
     *
     * @return {@code true} if this is a voice note, {@code false} for a regular audio file
     */
    public boolean ptt() {
        return ptt != null && ptt;
    }

    /**
     * Returns the symmetric key used to decrypt the audio payload.
     *
     * @return the key, or empty if the media has not been prepared for upload
     */
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    /**
     * Returns the SHA-256 digest of the encrypted audio as stored on the server.
     *
     * @return the hash, or empty if unknown
     */
    public Optional<byte[]> fileEncSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted audio as stored on the server.
     *
     * @return the hash, or empty if unknown
     */
    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the CDN direct path used to fetch the encrypted payload.
     *
     * @return the path, or empty if not yet uploaded
     */
    public Optional<String> directPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    /**
     * Returns the CDN direct path used to fetch the encrypted payload.
     *
     * @return the path, or empty if not yet uploaded
     */
    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    /**
     * Returns the moment at which the media key was generated.
     *
     * @return the key timestamp, or empty if unknown
     */
    public Optional<Instant> mediaKeyTimestamp() {
        return Optional.ofNullable(mediaKeyTimestamp);
    }

    /**
     * Returns the context information attached to this audio message.
     *
     * @return the context info, or empty if none is attached
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the streaming sidecar used to decrypt the audio progressively as it
     * downloads.
     *
     * @return the sidecar, or empty if not available
     */
    public Optional<byte[]> streamingSidecar() {
        return Optional.ofNullable(streamingSidecar);
    }

    /**
     * Returns the compact amplitude waveform used to render a voice note preview.
     *
     * @return the waveform bytes, or empty if not present
     */
    public Optional<byte[]> waveform() {
        return Optional.ofNullable(waveform);
    }

    /**
     * Returns the background color to render behind the waveform preview.
     *
     * @return the packed ARGB color, or empty if unset
     */
    public OptionalInt backgroundArgb() {
        return backgroundArgb == null ? OptionalInt.empty() : OptionalInt.of(backgroundArgb);
    }

    /**
     * Returns whether this audio can only be played once by the recipient.
     *
     * @return {@code true} if single-play, {@code false} otherwise
     */
    public boolean viewOnce() {
        return viewOnce != null && viewOnce;
    }

    /**
     * Returns the accessibility label describing the audio content.
     *
     * @return the label, or empty if not provided
     */
    public Optional<String> accessibilityLabel() {
        return Optional.ofNullable(accessibilityLabel);
    }

    /**
     * Returns the domain identifier that scopes how the media key was derived.
     *
     * @return the key domain, or empty if unset
     */
    public Optional<MediaMessageKeyDomain> mediaKeyDomain() {
        return Optional.ofNullable(mediaKeyDomain);
    }

    /**
     * Returns the {@link MediaPath} classification for audio media, used to select
     * the correct CDN upload/download endpoint.
     *
     * @return {@link MediaPath#AUDIO}
     */
    @Override
    public MediaPath mediaPath() {
        return MediaPath.AUDIO;
    }

    /**
     * Updates the CDN URL of the encrypted audio payload.
     *
     * @param mediaUrl the new URL, or {@code null} to clear
     */
    @Override
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    /**
     * Updates the audio MIME type.
     *
     * @param mimetype the new MIME type, or {@code null} to clear
     */
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * Updates the SHA-256 digest of the decrypted audio.
     *
     * @param mediaSha256 the new hash, or {@code null} to clear
     */
    @Override
    public void setMediaSha256(byte[] mediaSha256) {
        this.mediaSha256 = mediaSha256;
    }

    /**
     * Updates the size in bytes of the decrypted audio.
     *
     * @param mediaSize the new size
     */
    @Override
    public void setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
    }

    /**
     * Updates the duration in seconds.
     *
     * @param seconds the new duration, or {@code null} to clear
     */
    public void setSeconds(Integer seconds) {
        this.seconds = seconds;
    }

    /**
     * Updates the push-to-talk flag.
     *
     * @param ptt {@code true} to mark this as a voice note, {@code false} or {@code null} otherwise
     */
    public void setPtt(Boolean ptt) {
        this.ptt = ptt;
    }

    /**
     * Updates the symmetric key used to decrypt the audio payload.
     *
     * @param mediaKey the new key, or {@code null} to clear
     */
    @Override
    public void setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
    }

    /**
     * Updates the SHA-256 digest of the encrypted audio.
     *
     * @param mediaEncryptedSha256 the new hash, or {@code null} to clear
     */
    @Override
    public void setMediaEncryptedSha256(byte[] mediaEncryptedSha256) {
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
    }

    /**
     * Updates the CDN direct path.
     *
     * @param mediaDirectPath the new path, or {@code null} to clear
     */
    @Override
    public void setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
    }

    /**
     * Updates the moment at which the media key was generated.
     *
     * @param mediaKeyTimestamp the new timestamp, or {@code null} to clear
     */
    @Override
    public void setMediaKeyTimestamp(Instant mediaKeyTimestamp) {
        this.mediaKeyTimestamp = mediaKeyTimestamp;
    }

    /**
     * Updates the context information attached to this audio message.
     *
     * @param contextInfo the new context info, or {@code null} to clear
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Updates the streaming sidecar.
     *
     * @param streamingSidecar the new sidecar, or {@code null} to clear
     */
    public void setStreamingSidecar(byte[] streamingSidecar) {
        this.streamingSidecar = streamingSidecar;
    }

    /**
     * Updates the waveform preview bytes.
     *
     * @param waveform the new waveform, or {@code null} to clear
     */
    public void setWaveform(byte[] waveform) {
        this.waveform = waveform;
    }

    /**
     * Updates the ARGB background color for the waveform preview.
     *
     * @param backgroundArgb the new color, or {@code null} to clear
     */
    public void setBackgroundArgb(Integer backgroundArgb) {
        this.backgroundArgb = backgroundArgb;
    }

    /**
     * Updates the single-play flag.
     *
     * @param viewOnce {@code true} to mark as single-play, {@code false} or {@code null} otherwise
     */
    public void setViewOnce(Boolean viewOnce) {
        this.viewOnce = viewOnce;
    }

    /**
     * Updates the accessibility label.
     *
     * @param accessibilityLabel the new label, or {@code null} to clear
     */
    public void setAccessibilityLabel(String accessibilityLabel) {
        this.accessibilityLabel = accessibilityLabel;
    }

    /**
     * Updates the key derivation domain.
     *
     * @param mediaKeyDomain the new domain, or {@code null} to clear
     */
    public void setMediaKeyDomain(MediaMessageKeyDomain mediaKeyDomain) {
        this.mediaKeyDomain = mediaKeyDomain;
    }
}
