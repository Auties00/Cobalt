package it.auties.whatsapp.api;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.google.zxing.client.j2se.MatrixToImageWriter.writeToPath;
import static java.awt.Desktop.getDesktop;
import static java.awt.Desktop.isDesktopSupported;
import static java.nio.file.Files.createTempFile;

/**
 * The constants of this enumerated type describe a set of consumers to handle a QR code.
 * If you want to implement a custom functionality, return CUSTOM and implement your own logic
 */
@FunctionalInterface
public interface QrHandler extends Consumer<String> {
    /**
     * Prints the QR code to the terminal.
     * If your terminal doesn't support utf, you may see random characters.
     */
    static QrHandler toTerminal() {
        return qr -> {
            var matrix = createMatrix(qr, 10, 0);
            var writer = new StringBuilder();

            var ww = "█";
            var bb = " ";
            var wb = "▀";
            var bw = "▄";
            var qz = 2;

            var header = ww.repeat(matrix.getWidth() + qz * 2);
            writer.append((header + "\n").repeat(qz / 2));
            for (var i = 0; i <= matrix.getWidth(); i += 2) {
                writer.append(ww.repeat(qz));
                for (var j = 0; j <= matrix.getWidth(); j++) {
                    var nextBlack = i + 1 < matrix.getWidth() && matrix.get(j, i + 1);
                    var currentBlack = matrix.get(j, i);
                    if (currentBlack && nextBlack) {
                        writer.append(bb);
                    } else if (currentBlack) {
                        writer.append(bw);
                    } else if (!nextBlack) {
                        writer.append(ww);
                    } else {
                        writer.append(wb);
                    }
                }

                writer.append(ww.repeat(qz - 1));
                writer.append("\n");
            }

            writer.append(wb.repeat(matrix.getWidth() + qz * 2));
            writer.append("\n");
            System.out.println(writer);
        };
    }

    /**
     * Saves the QR code to a file and opens it if a Desktop environment is available
     */
    static QrHandler toFile() {
        return qr -> {
            try {
                var matrix = createMatrix(qr, 500, 5);
                var path = createTempFile(UUID.randomUUID()
                        .toString(), ".jpg");
                writeToPath(matrix, "jpg", path);
                if (!isDesktopSupported()) {
                    return;
                }

                getDesktop().open(path.toFile());
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot save qr to file", exception);
            }
        };
    }

    private static BitMatrix createMatrix(String qr, int size, int margin) {
        try {
            var writer = new MultiFormatWriter();
            return writer.encode(qr, BarcodeFormat.QR_CODE, size, size,
                    Map.of(EncodeHintType.MARGIN, margin, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L));
        } catch (WriterException exception) {
            throw new UnsupportedOperationException("Cannot create qr code", exception);
        }
    }
}