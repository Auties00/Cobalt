package it.auties.whatsapp.api;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import it.auties.qr.QrTerminal;

import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.zxing.client.j2se.MatrixToImageWriter.writeToPath;
import static java.lang.System.Logger.Level.INFO;
import static java.nio.file.Files.createTempFile;

public sealed interface WhatsappVerificationHandler {
    /**
     * A utility sealed interface to represent methods that can be used to verify a WhatsApp Web Client
     */
    sealed interface Web extends WhatsappVerificationHandler {
        void handle(String value);
    
        /**
         * This interface allows consuming a qr code sent by WhatsApp Web
         */
        non-sealed interface QrCode extends Web {
            /**
             * Prints the QR code to the terminal. If your terminal doesn't support utf, you may see random
             * characters.
             */
            static QrCode toTerminal() {
                return qr -> {
                    var matrix = createMatrix(qr, 10, 0);
                    System.out.println(QrTerminal.toString(matrix, true));
                };
            }
    
            /**
             * Utility method to create a matrix from a qr countryCode
             *
             * @param qr     the non-null source
             * @param size   the size of the qr countryCode
             * @param margin the margin for the qr countryCode
             * @return a non-null matrix
             */
            static BitMatrix createMatrix(String qr, int size, int margin) {
                try {
                    var writer = new MultiFormatWriter();
                    return writer.encode(qr, BarcodeFormat.QR_CODE, size, size, Map.of(EncodeHintType.MARGIN, margin, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L));
                } catch (WriterException exception) {
                    throw new UnsupportedOperationException("Cannot create qr countryCode", exception);
                }
            }
    
            /**
             * Saves the QR code to a temp file
             *
             * @param fileConsumer the consumer to digest the created file
             */
            static QrCode toFile(QrCode.ToFile fileConsumer) {
                try {
                    var file = createTempFile("qr", ".jpg");
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
            static QrCode toFile(Path path, QrCode.ToFile fileConsumer) {
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
             * This interface allows consuming a file's path easily
             */
            interface ToFile extends Consumer<Path> {
                /**
                 * Discard the newly created file
                 */
                static QrCode.ToFile discard() {
                    return ignored -> {
                    };
                }
    
                /**
                 * Prints the location of the file on the terminal using the system logger
                 */
                static QrCode.ToFile toTerminal() {
                    return path -> System.getLogger(QrCode.class.getName())
                            .log(INFO, "Saved qr code at %s".formatted(path));
                }
    
                /**
                 * Opens the file if a Desktop environment is available
                 */
                static QrCode.ToFile toDesktop() {
                    return path -> {
                        try {
                            if (!Desktop.isDesktopSupported()) {
                                return;
                            }
                            Desktop.getDesktop().open(path.toFile());
                        } catch (Throwable throwable) {
                            throw new RuntimeException("Cannot open file with desktop", throwable);
                        }
                    };
                }
            }
        }
    
        /**
         * This interface allows consuming a pairing code sent by WhatsappWeb
         */
        non-sealed interface PairingCode extends Web {
            /**
             * Prints the pairing code to the terminal
             */
            static PairingCode toTerminal() {
                return System.out::println;
            }
        }
    }

    /**
     * A utility sealed interface to represent methods that can be used to verify a WhatsApp Mobile Client
     */
    non-sealed interface Mobile extends WhatsappVerificationHandler {
        Optional<String> requestMethod();
        String verificationCode();
    
        static Mobile none(Supplier<String> supplier) {
            return new Mobile() {
                @Override
                public Optional<String> requestMethod() {
                    return Optional.empty();
                }
    
                @Override
                public String verificationCode() {
                    var value = supplier.get();
                    if(value == null) {
                        throw new IllegalArgumentException("Cannot send verification code: no value");
                    }
                    return value;
                }
            };
        }
    
        static Mobile sms(Supplier<String> supplier) {
            return new Mobile() {
                @Override
                public Optional<String> requestMethod() {
                    return Optional.of("sms");
                }
    
                @Override
                public String verificationCode() {
                    var value = supplier.get();
                    if(value == null) {
                        throw new IllegalArgumentException("Cannot send verification code: no value");
                    }
                    return value;
                }
            };
        }
    
        static Mobile call(Supplier<String> supplier) {
            return new Mobile() {
                @Override
                public Optional<String> requestMethod() {
                    return Optional.of("voice");
                }
    
                @Override
                public String verificationCode() {
                    var value = supplier.get();
                    if(value == null) {
                        throw new IllegalArgumentException("Cannot send verification code: no value");
                    }
                    return value;
                }
            };
        }
    
        static Mobile whatsapp(Supplier<String> supplier) {
            return new Mobile() {
                @Override
                public Optional<String> requestMethod() {
                    return Optional.of("wa_old");
                }
    
                @Override
                public String verificationCode() {
                    var value = supplier.get();
                    if(value == null) {
                        throw new IllegalArgumentException("Cannot send verification code: no value");
                    }
                    return value;
                }
            };
        }
    }
}
