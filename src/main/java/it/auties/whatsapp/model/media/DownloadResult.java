package it.auties.whatsapp.model.media;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * An immutable model class that represents a downloaded media
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Jacksonized
@Accessors(fluent = true)
@Value
public class DownloadResult {

  private static final DownloadResult MISSING = new DownloadResult(null, null, Status.MISSING);

  byte[] media;

  Throwable error;

  @NonNull Status status;

  /**
   * Constructs a new successful download result
   *
   * @param media the non-null media that was downloaded
   * @return a non-null download result
   */
  public static DownloadResult success(byte @NonNull [] media) {
    return new DownloadResult(media, null, Status.SUCCESS);
  }

  /**
   * Constructs a new download result from an erroneous download
   *
   * @param throwable the non-null error
   * @return a non-null download result
   */
  public static DownloadResult error(@NonNull Throwable throwable) {
    return new DownloadResult(null, throwable, Status.ERROR);
  }

  /**
   * Constructs a new download result from a missing media
   *
   * @return a non-null download result
   */
  public static DownloadResult missing() {
    return MISSING;
  }

  /**
   * Returns the downloaded wrapped in an optional. Use {@link DownloadResult#status()} to check the
   * status of this result.
   *
   * @return a non-null optional
   */
  public Optional<byte[]> media() {
    return Optional.ofNullable(media);
  }

  /**
   * Returns the error thrown when downloading wrapped in an optional. Use
   * {@link DownloadResult#status()} to check the status of this result.
   *
   * @return a non-null optional
   */
  public Optional<Throwable> error() {
    return Optional.ofNullable(error);
  }

  /**
   * The constants of this enumerated type describe type various outcomes that a media download can
   * have
   */
  public enum Status {
    /**
     * The download was downloaded successfully
     */
    SUCCESS,
    /**
     * A media re-upload is required
     */
    MISSING,
    /**
     * An unknown error made the download fail
     */
    ERROR
  }
}
