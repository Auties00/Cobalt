package it.auties.whatsapp.api;

import static com.google.zxing.client.j2se.MatrixToImageWriter.writeToPath;
import static java.lang.System.Logger.Level.INFO;
import static java.nio.file.Files.createTempFile;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import it.auties.qr.QrTerminal;
import java.awt.Desktop;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.NonNull;

/**
 * This interface allows to consume a qr code and provides default common implementations to do so
 */
@FunctionalInterface
@SuppressWarnings("unused")
public interface QrHandler
    extends Consumer<String> {

  /**
   * Prints the QR code to the terminal. If your terminal doesn't support utf, you may see random
   * characters.
   */
  static QrHandler toTerminal() {
    return toString(System.out::println);
  }

  /**
   * Transforms the qr code in a UTF-8 string and accepts a consumer for the latter
   *
   * @param smallQrConsumer the non-null consumer
   */
  static QrHandler toString(@NonNull Consumer<String> smallQrConsumer) {
    return qr -> {
      var matrix = createMatrix(qr, 10, 0);
      smallQrConsumer.accept(QrTerminal.toString(matrix, true));
    };
  }

  /**
   * Saves the QR code to a temp file
   *
   * @param fileConsumer the consumer to digest the created file
   */
  static QrHandler toFile(@NonNull ToFileConsumer fileConsumer) {
    try {
      var file = createTempFile(UUID.randomUUID()
          .toString(), ".jpg");
      return toFile(file, fileConsumer);
    } catch (IOException exception) {
      throw new UncheckedIOException("Cannot create temp file for qr handler", exception);
    }
  }

  /**
   * Saves the QR code to a specified file
   *
   * @param path         the location where the qr will be written
   * @param fileConsumer the consumer to digest the created file
   */
  static QrHandler toFile(@NonNull Path path, @NonNull ToFileConsumer fileConsumer) {
    return qr -> {
      try {
        var matrix = createMatrix(qr, 500, 5);
        writeToPath(matrix, "jpg", path);
        fileConsumer.accept(path);
      } catch (IOException exception) {
        throw new UncheckedIOException("Cannot save qr to file", exception);
      }
    };
  }

  /**
   * Utility method to create a matrix from a qr code
   *
   * @param qr     the non-null source
   * @param size   the size of the qr code
   * @param margin the margin for the qr code
   * @return a non-null matrix
   */
  static BitMatrix createMatrix(@NonNull String qr, int size, int margin) {
    try {
      var writer = new MultiFormatWriter();
      return writer.encode(qr, BarcodeFormat.QR_CODE, size, size,
          Map.of(EncodeHintType.MARGIN, margin, EncodeHintType.ERROR_CORRECTION,
              ErrorCorrectionLevel.L));
    } catch (WriterException exception) {
      throw new UnsupportedOperationException("Cannot create qr code", exception);
    }
  }

  /**
   * This interface allows to consume a file created by
   * {@link QrHandler#toFile(Path, ToFileConsumer)} easily
   */
  interface ToFileConsumer
      extends Consumer<Path> {

    /**
     * Discard the newly created file
     */
    static ToFileConsumer discarding() {
      return ignored -> {
      };
    }

    /**
     * Prints the location of the file on the terminal using the system logger
     */
    static ToFileConsumer toTerminal() {
      return path -> System.getLogger(QrHandler.class.getName())
          .log(INFO, "Saved qr code at %s".formatted(path));
    }

    /**
     * Opens the file if a Desktop environment is available
     */
    static ToFileConsumer toDesktop() {
      return path -> {
        try {
          if (!Desktop.isDesktopSupported()) {
            return;
          }
          Desktop.getDesktop()
              .open(path.toFile());
        } catch (IOException exception) {
          throw new UncheckedIOException("Cannot open file with desktop", exception);
        }
      };
    }
  }
}