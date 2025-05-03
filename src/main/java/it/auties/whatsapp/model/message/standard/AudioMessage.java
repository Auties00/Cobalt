package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.media.AttachmentType;
import it.auties.whatsapp.model.message.model.MediaMessage;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Medias;

import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

@ProtobufMessage(name = "Message.AudioMessage")
public final class AudioMessage extends MediaMessage<AudioMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String mediaUrl;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String mimetype;

    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] mediaSha256;

    @ProtobufProperty(index = 4, type = ProtobufType.UINT64)
    Long mediaSize;

    @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
    final Integer duration;

    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    final boolean voiceMessage;

    @ProtobufProperty(index = 7, type = ProtobufType.BYTES)
    byte[] mediaKey;

    @ProtobufProperty(index = 8, type = ProtobufType.BYTES)
    byte[] mediaEncryptedSha256;

    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    String mediaDirectPath;

    @ProtobufProperty(index = 10, type = ProtobufType.INT64)
    Long mediaKeyTimestampSeconds;

    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    @ProtobufProperty(index = 18, type = ProtobufType.BYTES)
    final byte[] streamingSidecar;

    @ProtobufProperty(index = 19, type = ProtobufType.BYTES)
    final byte[] waveform;

    @ProtobufProperty(index = 20, type = ProtobufType.FIXED32)
    final Integer backgroundArgb;

    AudioMessage(String mediaUrl, String mimetype, byte[] mediaSha256, Long mediaSize, Integer duration, boolean voiceMessage, byte[] mediaKey, byte[] mediaEncryptedSha256, String mediaDirectPath, Long mediaKeyTimestampSeconds, ContextInfo contextInfo, byte[] streamingSidecar, byte[] waveform, Integer backgroundArgb) {
        this.mediaUrl = mediaUrl;
        this.mimetype = mimetype;
        this.mediaSha256 = mediaSha256;
        this.mediaSize = mediaSize;
        this.duration = duration;
        this.voiceMessage = voiceMessage;
        this.mediaKey = mediaKey;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
        this.mediaDirectPath = mediaDirectPath;
        this.mediaKeyTimestampSeconds = mediaKeyTimestampSeconds;
        this.contextInfo = contextInfo;
        this.streamingSidecar = streamingSidecar;
        this.waveform = waveform;
        this.backgroundArgb = backgroundArgb;
    }

    @ProtobufBuilder(className = "AudioMessageSimpleBuilder")
    static AudioMessage customBuilder(ByteBuffer media, ContextInfo contextInfo, String mimeType, boolean voiceMessage) {
        return new AudioMessageBuilder()
                .contextInfo(Objects.requireNonNullElseGet(contextInfo, ContextInfo::empty))
                .duration(Medias.getDuration(media))
                .mimetype(getMimeType(media, mimeType))
                .voiceMessage(voiceMessage)
                .waveform(Medias.getAudioWaveForm(media))
                .build()
                .setDecodedMedia(media);
    }

    private static String getMimeType(ByteBuffer media, String mimeType) {
        return Optional.ofNullable(mimeType)
                .or(() -> Medias.getMimeType(media))
                .orElseGet(Type.AUDIO::mimeType);
    }

    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    public OptionalInt duration() {
        return duration == null ? OptionalInt.empty() : OptionalInt.of(duration);
    }

    public boolean voiceMessage() {
        return voiceMessage;
    }

    public Optional<byte[]> streamingSidecar() {
        return Optional.ofNullable(streamingSidecar);
    }

    public Optional<byte[]> waveform() {
        return Optional.ofNullable(waveform);
    }

    public Integer backgroundArgb() {
        return backgroundArgb;
    }

    @Override
    public Optional<String> mediaUrl() {
        return Optional.ofNullable(mediaUrl);
    }

    @Override
    public AudioMessage setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
        return this;
    }

    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    @Override
    public AudioMessage setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
        return this;
    }

    @Override
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    @Override
    public AudioMessage setMediaKey(byte[] bytes) {
        this.mediaKey = bytes;
        return this;
    }

    @Override
    public AudioMessage setMediaKeyTimestamp(Long timestamp) {
        this.mediaKeyTimestampSeconds = timestamp;
        return this;
    }

    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    @Override
    public AudioMessage setMediaSha256(byte[] bytes) {
        this.mediaSha256 = bytes;
        return this;
    }

    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    @Override
    public AudioMessage setMediaEncryptedSha256(byte[] bytes) {
        this.mediaEncryptedSha256 = bytes;
        return this;
    }

    @Override
    public OptionalLong mediaSize() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    @Override
    public OptionalLong mediaKeyTimestampSeconds() {
        return Clock.parseTimestamp(mediaKeyTimestampSeconds);
    }

    @Override
    public Optional<ZonedDateTime> mediaKeyTimestamp() {
        return Clock.parseSeconds(mediaKeyTimestampSeconds);
    }

    @Override
    public AudioMessage setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
        return this;
    }

    @Override
    public AttachmentType attachmentType() {
        return AttachmentType.AUDIO;
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public AudioMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    @Override
    public Type mediaType() {
        return Type.AUDIO;
    }
}