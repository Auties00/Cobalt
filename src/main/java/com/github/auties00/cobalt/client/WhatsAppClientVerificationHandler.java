package com.github.auties00.cobalt.client;

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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.zxing.client.j2se.MatrixToImageWriter.writeToPath;
import static java.lang.System.Logger.Level.INFO;
import static java.nio.file.Files.createTempFile;

/**
 * A sealed interface that defines handlers for verification methods in WhatsApp.
 * This interface provides mechanisms for handling both Web and Mobile verification processes.
 */
public sealed interface WhatsAppClientVerificationHandler {
    /**
     * A sealed interface that represents verification methods for WhatsApp Web Client.
     * Provides handling for QR codes and pairing codes used in the WhatsApp Web verification process.
     */
    sealed interface Web extends WhatsAppClientVerificationHandler {
        /**
         * Handles the verification value provided by WhatsApp Web.
         *
         * @param value The verification value to be processed (either QR code data or pairing code)
         */
        void handle(String value);

        /**
         * An interface for handling QR codes sent by WhatsApp Web during authentication.
         * Provides various methods to process and display QR codes in different formats.
         */
        @FunctionalInterface
        non-sealed interface QrCode extends Web {
            /**
             * Creates a handler that prints the QR code to the terminal.
             *
             * @return A QrCode handler that renders the QR code to the console
             * @apiNote If your terminal doesn't support UTF characters, the output may appear as random characters
             */
            static QrCode toTerminal() {
                return qr -> {
                    var matrix = createMatrix(qr, 10, 0);
                    System.out.println(QrTerminal.toString(matrix, true));
                };
            }

            /**
             * Creates a BitMatrix representation of a QR code from a value.
             *
             * @param qr     The QR code children to encode
             * @param size   The size of the QR code in pixels
             * @param margin The margin size around the QR code
             * @return A BitMatrix representing the QR code
             * @throws UnsupportedOperationException if the QR code cannot be created
             */
            static BitMatrix createMatrix(String qr, int size, int margin) {
                try {
                    var writer = new MultiFormatWriter();
                    return writer.encode(qr, BarcodeFormat.QR_CODE, size, size, Map.of(EncodeHintType.MARGIN, margin, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L));
                } catch (WriterException exception) {
                    throw new UnsupportedOperationException("Cannot create QR code", exception);
                }
            }

            /**
             * Creates a handler that saves the QR code to a temporary file and processes it with the provided consumer.
             *
             * @param fileConsumer The consumer to process the created file path
             * @return A QrCode handler that saves the QR code to a temporary file
             * @throws UncheckedIOException if the temporary file cannot be created
             */
            static QrCode toFile(QrCode.ToFile fileConsumer) {
                try {
                    var file = createTempFile("qr", ".jpg");
                    return toFile(file, fileConsumer);
                } catch (IOException exception) {
                    throw new UncheckedIOException("Cannot create temp file for QR handler", exception);
                }
            }

            /**
             * Creates a handler that saves the QR code to a specified path and processes it with the provided consumer.
             *
             * @param path The destination path where the QR code image will be saved
             * @param fileConsumer The consumer to process the file path after creation
             * @return A QrCode handler that saves the QR code to the specified path
             */
            static QrCode toFile(Path path, QrCode.ToFile fileConsumer) {
                return qr -> {
                    try {
                        var matrix = createMatrix(qr, 500, 5);
                        writeToPath(matrix, "jpg", path);
                        fileConsumer.accept(path);
                    } catch (IOException exception) {
                        throw new UncheckedIOException("Cannot save QR code to file", exception);
                    }
                };
            }

            /**
             * An interface for consuming a file path containing a saved QR code.
             * Provides various methods to process the file path.
             */
            interface ToFile extends Consumer<Path> {
                /**
                 * Creates a consumer that discards the file path, taking no action.
                 *
                 * @return A ToFile consumer that ignores the file path
                 */
                static QrCode.ToFile discard() {
                    return ignored -> {};
                }

                /**
                 * Creates a consumer that logs the file path to the terminal using the system logger.
                 *
                 * @return A ToFile consumer that prints the file location to the console
                 */
                static QrCode.ToFile toTerminal() {
                    return path -> System.getLogger(QrCode.class.getName())
                            .log(INFO, "Saved QR code at %s".formatted(path));
                }

                /**
                 * Creates a consumer that opens the QR code file using the default desktop application.
                 *
                 * @return A ToFile consumer that opens the file with the desktop
                 * @throws RuntimeException if the file cannot be opened with the desktop
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
         * An interface for handling pairing codes sent by WhatsApp Web during authentication.
         * Provides methods to process and display pairing codes.
         */
        @FunctionalInterface
        non-sealed interface PairingCode extends Web {
            /**
             * Creates a handler that prints the pairing code to the terminal.
             *
             * @return A PairingCode handler that outputs the code to the console
             */
            static PairingCode toTerminal() {
                return System.out::println;
            }
        }
    }

    /**
     * An interface that represents verification methods for WhatsApp Mobile Client.
     * Handles verification processes for mobile authentication through various channels.
     */
    non-sealed interface Mobile extends WhatsAppClientVerificationHandler {
        /**
         * Returns the preferred verification method to be requested.
         *
         * @return An Optional containing the verification method name, or empty if no specific method is requested
         */
        Optional<String> requestMethod();

        /**
         * Returns the verification code to be used for authentication.
         *
         * @return The verification code value
         */
        String verificationCode();

        /**
         * Creates a Mobile verification handler with no specific request method.
         * The verification code is obtained from the provided supplier.
         *
         * @param supplier A non-null supplier that provides the verification code
         * @return A Mobile verification handler with no specific request method
         * @throws NullPointerException if the supplier is null
         */
        static Mobile none(Supplier<String> supplier) {
            Objects.requireNonNull(supplier, "supplier cannot be null");
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

        /**
         * Creates a Mobile verification handler that requests verification via SMS.
         * The verification code is obtained from the provided supplier.
         *
         * @param supplier A non-null supplier that provides the verification code
         * @return A Mobile verification handler for SMS verification
         * @throws NullPointerException if the supplier is null
         */
        static Mobile sms(Supplier<String> supplier) {
            Objects.requireNonNull(supplier, "supplier cannot be null");
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

        /**
         * Creates a Mobile verification handler that requests verification via phone call.
         * The verification code is obtained from the provided supplier.
         *
         * @param supplier A non-null supplier that provides the verification code
         * @return A Mobile verification handler for voice call verification
         * @throws NullPointerException if the supplier is null
         */
        static Mobile call(Supplier<String> supplier) {
            Objects.requireNonNull(supplier, "supplier cannot be null");
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

        /**
         * Creates a Mobile verification handler that requests verification via WhatsApp.
         * The verification code is obtained from the provided supplier.
         *
         * @param supplier A non-null supplier that provides the verification code
         * @return A Mobile verification handler for WhatsApp verification
         * @throws NullPointerException if the supplier is null
         */
        static Mobile whatsapp(Supplier<String> supplier) {
            Objects.requireNonNull(supplier, "supplier cannot be null");
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