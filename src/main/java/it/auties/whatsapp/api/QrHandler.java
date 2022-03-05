package it.auties.whatsapp.api;

import com.google.zxing.common.BitMatrix;

import java.io.IOException;
import java.io.UncheckedIOException;
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
public interface QrHandler extends Consumer<BitMatrix> {
    /**
     * Prints the QR code to the terminal
     */
    static QrHandler toTerminal() {
        return matrix -> System.out.println(matrix.toString("\033[40m  \033[0m", "\033[47m  \033[0m"));
    }

    /**
     * Saves the QR code to a file and opens it if a Desktop environment is available
     */
    static QrHandler toFile(){
        return matrix -> {
            try {
                var path = createTempFile(UUID.randomUUID().toString(), ".jpg");
                writeToPath(matrix, "jpg", path);
                if(!isDesktopSupported()){
                    return;
                }

                getDesktop().open(path.toFile());
            }catch (IOException exception){
                throw new UncheckedIOException("Cannot save qr to file", exception);
            }
        };
    }
}