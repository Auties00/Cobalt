package com.github.auties00.cobalt.wire.linked.message.media;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A piece of music metadata embedded inside another message.
 *
 * <p>This variant of {@link EmbeddedContent} carries track information such as
 * title, author, album artwork and playback windows for songs attached to link
 * previews or status updates. The artwork is referenced via its encrypted CDN
 * location and integrity hashes so it can be downloaded and decrypted like any other
 * media attachment.
 */
@ProtobufMessage(name = "EmbeddedMusic")
public final class EmbeddedMusic implements EmbeddedContentVariant {
    /**
     * Identifier of the media item within the music content catalog.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String musicContentMediaId;

    /**
     * Identifier of the song in the music provider catalog.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String songId;

    /**
     * Name of the song's author or performer.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String author;

    /**
     * Display title of the song.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String title;

    /**
     * CDN direct path to the encrypted album artwork image.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String artworkDirectPath;

    /**
     * SHA-256 digest of the decrypted artwork image.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BYTES)
    byte[] artworkSha256;

    /**
     * SHA-256 digest of the encrypted artwork image.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BYTES)
    byte[] artworkEncSha256;

    /**
     * Attribution string describing who owns or contributed the song.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String artistAttribution;

    /**
     * Encoded list of country codes in which this song is not allowed to be played,
     * enforced for copyright reasons.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.BYTES)
    byte[] countryBlocklist;

    /**
     * Whether this song is flagged as containing explicit content.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.BOOL)
    Boolean isExplicit;

    /**
     * Symmetric key used to decrypt the artwork image.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BYTES)
    byte[] artworkMediaKey;

    /**
     * Offset in milliseconds within the song at which playback should start.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.INT64)
    Long musicSongStartTimeInMs;

    /**
     * Offset in milliseconds within the derived content at which playback should
     * start.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.INT64)
    Long derivedContentStartTimeInMs;

    /**
     * Duration in milliseconds of the overlap between the song and the content on
     * which it is overlaid.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.INT64)
    Long overlapDurationInMs;


    /**
     * Constructs a new embedded music entry.
     *
     * @param musicContentMediaId         the music catalog media id
     * @param songId                      the music provider song id
     * @param author                      the song author
     * @param title                       the song title
     * @param artworkDirectPath           the CDN path of the artwork
     * @param artworkSha256               the hash of the decrypted artwork
     * @param artworkEncSha256            the hash of the encrypted artwork
     * @param artistAttribution           the artist attribution string
     * @param countryBlocklist            the encoded country blocklist
     * @param isExplicit                  whether the song is explicit
     * @param artworkMediaKey             the artwork decryption key
     * @param musicSongStartTimeInMs      the playback start offset within the song
     * @param derivedContentStartTimeInMs the playback start offset within the derived content
     * @param overlapDurationInMs         the overlap duration in milliseconds
     */
    EmbeddedMusic(String musicContentMediaId, String songId, String author, String title, String artworkDirectPath, byte[] artworkSha256, byte[] artworkEncSha256, String artistAttribution, byte[] countryBlocklist, Boolean isExplicit, byte[] artworkMediaKey, Long musicSongStartTimeInMs, Long derivedContentStartTimeInMs, Long overlapDurationInMs) {
        this.musicContentMediaId = musicContentMediaId;
        this.songId = songId;
        this.author = author;
        this.title = title;
        this.artworkDirectPath = artworkDirectPath;
        this.artworkSha256 = artworkSha256;
        this.artworkEncSha256 = artworkEncSha256;
        this.artistAttribution = artistAttribution;
        this.countryBlocklist = countryBlocklist;
        this.isExplicit = isExplicit;
        this.artworkMediaKey = artworkMediaKey;
        this.musicSongStartTimeInMs = musicSongStartTimeInMs;
        this.derivedContentStartTimeInMs = derivedContentStartTimeInMs;
        this.overlapDurationInMs = overlapDurationInMs;
    }

    /**
     * Returns the music catalog media identifier.
     *
     * @return the media id, or empty if unset
     */
    public Optional<String> musicContentMediaId() {
        return Optional.ofNullable(musicContentMediaId);
    }

    /**
     * Returns the song identifier within the music provider catalog.
     *
     * @return the song id, or empty if unset
     */
    public Optional<String> songId() {
        return Optional.ofNullable(songId);
    }

    /**
     * Returns the song's author or performer.
     *
     * @return the author, or empty if unset
     */
    public Optional<String> author() {
        return Optional.ofNullable(author);
    }

    /**
     * Returns the song title.
     *
     * @return the title, or empty if unset
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the CDN direct path to the encrypted artwork.
     *
     * @return the path, or empty if unset
     */
    public Optional<String> artworkDirectPath() {
        return Optional.ofNullable(artworkDirectPath);
    }

    /**
     * Returns the SHA-256 digest of the decrypted artwork.
     *
     * @return the hash, or empty if unset
     */
    public Optional<byte[]> artworkSha256() {
        return Optional.ofNullable(artworkSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted artwork.
     *
     * @return the hash, or empty if unset
     */
    public Optional<byte[]> artworkEncSha256() {
        return Optional.ofNullable(artworkEncSha256);
    }

    /**
     * Returns the artist attribution string.
     *
     * @return the attribution, or empty if unset
     */
    public Optional<String> artistAttribution() {
        return Optional.ofNullable(artistAttribution);
    }

    /**
     * Returns the encoded country blocklist.
     *
     * @return the blocklist bytes, or empty if unset
     */
    public Optional<byte[]> countryBlocklist() {
        return Optional.ofNullable(countryBlocklist);
    }

    /**
     * Returns whether the song contains explicit content.
     *
     * @return {@code true} if the song is flagged as explicit
     */
    public boolean isExplicit() {
        return isExplicit != null && isExplicit;
    }

    /**
     * Returns the symmetric key used to decrypt the artwork image.
     *
     * @return the key, or empty if unset
     */
    public Optional<byte[]> artworkMediaKey() {
        return Optional.ofNullable(artworkMediaKey);
    }

    /**
     * Returns the playback start offset within the source song, in milliseconds.
     *
     * @return the offset, or empty if unset
     */
    public OptionalLong musicSongStartTimeInMs() {
        return musicSongStartTimeInMs == null ? OptionalLong.empty() : OptionalLong.of(musicSongStartTimeInMs);
    }

    /**
     * Returns the playback start offset within the derived content, in milliseconds.
     *
     * @return the offset, or empty if unset
     */
    public OptionalLong derivedContentStartTimeInMs() {
        return derivedContentStartTimeInMs == null ? OptionalLong.empty() : OptionalLong.of(derivedContentStartTimeInMs);
    }

    /**
     * Returns the overlap duration in milliseconds.
     *
     * @return the duration, or empty if unset
     */
    public OptionalLong overlapDurationInMs() {
        return overlapDurationInMs == null ? OptionalLong.empty() : OptionalLong.of(overlapDurationInMs);
    }

    /**
     * Updates the music catalog media identifier.
     *
     * @param musicContentMediaId the new media id, or {@code null} to clear
     */
    public void setMusicContentMediaId(String musicContentMediaId) {
        this.musicContentMediaId = musicContentMediaId;
    }

    /**
     * Updates the song identifier.
     *
     * @param songId the new song id, or {@code null} to clear
     */
    public void setSongId(String songId) {
        this.songId = songId;
    }

    /**
     * Updates the song's author.
     *
     * @param author the new author, or {@code null} to clear
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Updates the song title.
     *
     * @param title the new title, or {@code null} to clear
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Updates the CDN path of the artwork.
     *
     * @param artworkDirectPath the new path, or {@code null} to clear
     */
    public void setArtworkDirectPath(String artworkDirectPath) {
        this.artworkDirectPath = artworkDirectPath;
    }

    /**
     * Updates the hash of the decrypted artwork.
     *
     * @param artworkSha256 the new hash, or {@code null} to clear
     */
    public void setArtworkSha256(byte[] artworkSha256) {
        this.artworkSha256 = artworkSha256;
    }

    /**
     * Updates the hash of the encrypted artwork.
     *
     * @param artworkEncSha256 the new hash, or {@code null} to clear
     */
    public void setArtworkEncSha256(byte[] artworkEncSha256) {
        this.artworkEncSha256 = artworkEncSha256;
    }

    /**
     * Updates the artist attribution string.
     *
     * @param artistAttribution the new attribution, or {@code null} to clear
     */
    public void setArtistAttribution(String artistAttribution) {
        this.artistAttribution = artistAttribution;
    }

    /**
     * Updates the encoded country blocklist.
     *
     * @param countryBlocklist the new blocklist, or {@code null} to clear
     */
    public void setCountryBlocklist(byte[] countryBlocklist) {
        this.countryBlocklist = countryBlocklist;
    }

    /**
     * Updates the explicit content flag.
     *
     * @param isExplicit {@code true} if the song is explicit, {@code false} or {@code null} otherwise
     */
    public void setExplicit(Boolean isExplicit) {
        this.isExplicit = isExplicit;
    }

    /**
     * Updates the artwork decryption key.
     *
     * @param artworkMediaKey the new key, or {@code null} to clear
     */
    public void setArtworkMediaKey(byte[] artworkMediaKey) {
        this.artworkMediaKey = artworkMediaKey;
    }

    /**
     * Updates the song playback start offset.
     *
     * @param musicSongStartTimeInMs the new offset in milliseconds, or {@code null} to clear
     */
    public void setMusicSongStartTimeInMs(Long musicSongStartTimeInMs) {
        this.musicSongStartTimeInMs = musicSongStartTimeInMs;
    }

    /**
     * Updates the derived content start offset.
     *
     * @param derivedContentStartTimeInMs the new offset in milliseconds, or {@code null} to clear
     */
    public void setDerivedContentStartTimeInMs(Long derivedContentStartTimeInMs) {
        this.derivedContentStartTimeInMs = derivedContentStartTimeInMs;
    }

    /**
     * Updates the overlap duration.
     *
     * @param overlapDurationInMs the new duration in milliseconds, or {@code null} to clear
     */
    public void setOverlapDurationInMs(Long overlapDurationInMs) {
        this.overlapDurationInMs = overlapDurationInMs;
    }
}
