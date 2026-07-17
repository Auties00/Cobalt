package com.github.auties00.cobalt.client.linked;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.github.auties00.qr.QrTerminal;

import java.awt.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.zxing.client.j2se.MatrixToImageWriter.writeToPath;
import static java.nio.file.Files.createTempFile;

/**
 * A pluggable strategy for completing the authentication ceremony that
 * links or registers a {@link LinkedWhatsAppClient}.
 *
 * @apiNote
 * Two sub-hierarchies cover the supported flavours: {@link Web} drives
 * the companion linking ceremony for {@link LinkedWhatsAppClientType#WEB}
 * clients (QR scan or pairing-code entry), and {@link Mobile} drives
 * the registration ceremony for {@link LinkedWhatsAppClientType#MOBILE}
 * clients (SMS, voice, or in-app verification code). Implementations
 * are wired in via {@link LinkedWhatsAppClientBuilder}.
 *
 * @see LinkedWhatsAppClientBuilder
 */
public sealed interface LinkedWhatsAppClientVerificationHandler {
    /**
     * A verification handler for WhatsApp Web companion-device linking.
     *
     * @apiNote
     * Implementations surface the value the user must authorise on the
     * primary device: either a QR code payload (for {@link QrCode}) or
     * a short pairing code (for {@link PairingCode}).
     */
    sealed interface Web extends LinkedWhatsAppClientVerificationHandler {
        /**
         * Surfaces the verification value produced by the client to the
         * user.
         *
         * @apiNote
         * The value is either a QR code payload (for {@link QrCode}
         * handlers) or a short pairing code (for {@link PairingCode}
         * handlers).
         *
         * @param value the verification value produced by the client
         */
        void handle(String value);

        /**
         * Returns the passkey authenticator this handler carries for answering a server-pushed
         * integrity checkpoint on the connected session.
         *
         * @apiNote
         * A QR-scan or pairing-code link completes without a passkey, but the connected session can
         * still be challenged with an integrity checkpoint that only a passkey assertion (or a logout)
         * satisfies. Every factory-built handler carries an authenticator to answer that checkpoint: the
         * no-argument {@code toTerminal}/{@code toFile} factories default it to
         * {@link LinkedWhatsAppClientPasskeyAuthenticator#toTerminal()}, so any web client can answer a
         * checkpoint out of the box by scanning it with the phone that holds the passkey, while the
         * overloads that take one carry the given authenticator (for example a relay to a browser). Use
         * {@link QrCode#withPasskeyAuthenticator} or {@link PairingCode#withPasskeyAuthenticator} to swap
         * the authenticator on an existing handler.
         *
         * @return the passkey authenticator; never {@code null}
         */
        LinkedWhatsAppClientPasskeyAuthenticator passkeyAuthenticator();

        /**
         * Encodes a QR payload into a {@link BitMatrix} suitable
         * for rendering.
         *
         * @param qr     the payload to encode
         * @param size   the side length, in pixels, of the rendered
         *               square
         * @param margin the white margin around the rendered code,
         *               in modules
         * @return the encoded bit matrix
         * @throws UnsupportedOperationException if the payload
         *                                       cannot be encoded
         *                                       as a QR code
         */
        private static BitMatrix createMatrix(String qr, int size, int margin) {
            try {
                var writer = new MultiFormatWriter();
                return writer.encode(qr, BarcodeFormat.QR_CODE, size, size, Map.of(EncodeHintType.MARGIN, margin, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L));
            } catch (WriterException exception) {
                throw new UnsupportedOperationException("Cannot create QR code", exception);
            }
        }

        /**
         * Renders a QR payload as a JPEG image at the given path.
         *
         * <p>The QR matrix is generated at 500 pixels with a 5-module margin for scan-friendliness.
         *
         * @param qr   the payload to encode
         * @param path the destination path where the QR code image is saved
         * @throws UncheckedIOException if the image cannot be written to the path
         */
        private static void renderQrToFile(String qr, Path path) {
            try {
                var matrix = createMatrix(qr, 500, 5);
                writeToPath(matrix, "jpg", path);
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot save QR code to file", exception);
            }
        }

        /**
         * Creates the temporary JPEG file a QR image is rendered into when no path is supplied.
         *
         * <p>The file is created up front so the write happens off the verification path.
         *
         * @return the created temporary file path
         * @throws UncheckedIOException if the temporary file cannot be created
         */
        private static Path createQrTempFile() {
            try {
                return createTempFile("qr", ".jpg");
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot create temp file for QR handler", exception);
            }
        }

        /**
         * A consumer that reacts to the file path where a QR code has been rendered.
         *
         * @apiNote
         * Combine with a rendering target such as
         * {@link java.nio.file.Files#createTempFile(String, String, java.nio.file.attribute.FileAttribute[])}
         * to decide what to do with the resulting image: ignore it, log its location, or open it in a
         * desktop viewer.
         */
        interface ToFile extends Consumer<Path> {
            /**
             * Returns a consumer that ignores the rendered file path and takes no action.
             *
             * @apiNote
             * Useful when the application owns the file lifecycle elsewhere (for example a separate
             * thread that polls the path).
             *
             * @return the no-op consumer
             */
            static ToFile discard() {
                return ignored -> {};
            }

            /**
             * Returns a consumer that logs the rendered file path through the system logger at
             * {@link System.Logger.Level#INFO}.
             *
             * @return the logging consumer
             */
            static ToFile toTerminal() {
                return path -> {};
            }

            /**
             * Returns a consumer that opens the rendered file with the default desktop image viewer.
             *
             * @apiNote
             * Silently no-ops on hosts where {@link Desktop} is not supported (typical headless
             * servers). Throws if the viewer fails to launch on a supported host.
             *
             * @return the desktop-opening consumer
             * @throws RuntimeException if the file cannot be opened via {@link Desktop}
             */
            static ToFile toDesktop() {
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

        /**
         * A verification handler that renders the QR code produced during the companion-linking flow.
         *
         * @apiNote
         * The handler receives the raw QR payload as a string. The static factory methods provide common
         * renderers (terminal, temporary file, fixed path); each carries a passkey authenticator for
         * answering a server-pushed integrity checkpoint, defaulting to a terminal QR when none is given.
         */
        non-sealed interface QrCode extends Web {
            /**
             * Returns a handler that renders the QR code as ASCII art on standard output and carries the
             * given passkey authenticator for answering a server-pushed integrity checkpoint.
             *
             * @apiNote
             * Useful in headless or CI environments. Terminals that do not support UTF block-drawing
             * characters render the output as garbled symbols.
             *
             * @param authenticator the passkey authenticator to attach; never {@code null}
             * @return the terminal-rendering handler carrying the authenticator
             * @throws NullPointerException if {@code authenticator} is {@code null}
             */
            static QrCode toTerminal(LinkedWhatsAppClientPasskeyAuthenticator authenticator) {
                Objects.requireNonNull(authenticator, "authenticator must not be null");
                return of(qr -> {
                    var matrix = createMatrix(qr, 10, 0);
                    QrTerminal.print(matrix, true);
                }, authenticator);
            }

            /**
             * Returns a handler that renders the QR code as ASCII art on standard output, carrying a
             * terminal-QR passkey authenticator for answering a server-pushed integrity checkpoint.
             *
             * @return the terminal-rendering handler
             */
            static QrCode toTerminal() {
                return toTerminal(LinkedWhatsAppClientPasskeyAuthenticator.toTerminal());
            }

            /**
             * Returns a handler that writes the QR code to the supplied path, forwards it to the supplied
             * consumer, and carries the given passkey authenticator for answering a server-pushed
             * integrity checkpoint.
             *
             * @apiNote
             * Use when the rendering target is a fixed path (for example a shared volume or a web-served
             * asset). The QR matrix is generated at 500 pixels with a 5-module margin for
             * scan-friendliness.
             *
             * @param path          the destination path where the QR code image is saved
             * @param fileConsumer  the consumer that receives the path of the rendered file
             * @param authenticator the passkey authenticator to attach; never {@code null}
             * @return the file-rendering handler carrying the authenticator
             * @throws NullPointerException if {@code path}, {@code fileConsumer}, or {@code authenticator}
             *                              is {@code null}
             */
            static QrCode toFile(Path path, ToFile fileConsumer, LinkedWhatsAppClientPasskeyAuthenticator authenticator) {
                Objects.requireNonNull(path, "path must not be null");
                Objects.requireNonNull(fileConsumer, "fileConsumer must not be null");
                Objects.requireNonNull(authenticator, "authenticator must not be null");
                return of(qr -> {
                    renderQrToFile(qr, path);
                    fileConsumer.accept(path);
                }, authenticator);
            }

            /**
             * Returns a handler that writes the QR code to a temporary JPEG file, forwards the file path
             * to the supplied consumer, and carries the given passkey authenticator for answering a
             * server-pushed integrity checkpoint.
             *
             * @apiNote
             * Useful when the host process can render an image but not ASCII art. The temporary file is
             * created up front so the write happens off the verification path.
             *
             * @param fileConsumer  the consumer that receives the path of the rendered file
             * @param authenticator the passkey authenticator to attach; never {@code null}
             * @return the file-rendering handler carrying the authenticator
             * @throws NullPointerException if {@code fileConsumer} or {@code authenticator} is
             *                              {@code null}
             * @throws UncheckedIOException if the temporary file cannot be created
             */
            static QrCode toFile(ToFile fileConsumer, LinkedWhatsAppClientPasskeyAuthenticator authenticator) {
                return toFile(createQrTempFile(), fileConsumer, authenticator);
            }

            /**
             * Returns a handler that writes the QR code to the supplied path, forwards it to the supplied
             * consumer, and carries a terminal-QR passkey authenticator for answering a server-pushed
             * integrity checkpoint.
             *
             * @param path         the destination path where the QR code image is saved
             * @param fileConsumer the consumer that receives the path of the rendered file
             * @return the file-rendering handler
             * @throws NullPointerException if {@code path} or {@code fileConsumer} is {@code null}
             */
            static QrCode toFile(Path path, ToFile fileConsumer) {
                return toFile(path, fileConsumer, LinkedWhatsAppClientPasskeyAuthenticator.toTerminal());
            }

            /**
             * Returns a handler that writes the QR code to a temporary JPEG file, forwards the file path
             * to the supplied consumer, and carries a terminal-QR passkey authenticator for answering a
             * server-pushed integrity checkpoint.
             *
             * @param fileConsumer the consumer that receives the path of the rendered file
             * @return the file-rendering handler
             * @throws NullPointerException if {@code fileConsumer} is {@code null}
             * @throws UncheckedIOException if the temporary file cannot be created
             */
            static QrCode toFile(ToFile fileConsumer) {
                return toFile(fileConsumer, LinkedWhatsAppClientPasskeyAuthenticator.toTerminal());
            }

            /**
             * Returns a handler that renders the QR code exactly as this one does but carries the given
             * passkey authenticator instead, so the linked session answers a server-pushed integrity
             * checkpoint with it rather than this handler's authenticator.
             *
             * @param authenticator the passkey authenticator to attach; never {@code null}
             * @return a handler that renders as this one and carries the authenticator
             * @throws NullPointerException if {@code authenticator} is {@code null}
             */
            default QrCode withPasskeyAuthenticator(LinkedWhatsAppClientPasskeyAuthenticator authenticator) {
                Objects.requireNonNull(authenticator, "authenticator must not be null");
                return of(this::handle, authenticator);
            }

            /**
             * Builds a QR handler that presents the raw QR payload through the given renderer and carries
             * the given passkey authenticator.
             *
             * @param renderer      the action that presents the raw QR payload
             * @param authenticator the passkey authenticator the handler carries
             * @return the composed handler
             */
            private static QrCode of(Consumer<String> renderer, LinkedWhatsAppClientPasskeyAuthenticator authenticator) {
                return new QrCode() {
                    @Override
                    public void handle(String value) {
                        renderer.accept(value);
                    }

                    @Override
                    public LinkedWhatsAppClientPasskeyAuthenticator passkeyAuthenticator() {
                        return authenticator;
                    }
                };
            }
        }

        /**
         * A verification handler that surfaces the short pairing code produced during the
         * companion-linking flow.
         *
         * @apiNote
         * Pairing codes are typed into the Linked Devices screen on the primary device instead of
         * scanning a QR code. The handler receives the code as a plain string and is responsible for
         * presenting it; it also carries a passkey authenticator for answering a server-pushed integrity
         * checkpoint, defaulting to a terminal QR when none is given.
         */
        non-sealed interface PairingCode extends Web {
            /**
             * Returns a handler that prints the pairing code on standard output and carries the given
             * passkey authenticator for answering a server-pushed integrity checkpoint.
             *
             * @param authenticator the passkey authenticator to attach; never {@code null}
             * @return the terminal-printing handler carrying the authenticator
             * @throws NullPointerException if {@code authenticator} is {@code null}
             */
            static PairingCode toTerminal(LinkedWhatsAppClientPasskeyAuthenticator authenticator) {
                Objects.requireNonNull(authenticator, "authenticator must not be null");
                return of(System.out::println, authenticator);
            }

            /**
             * Returns a handler that prints the pairing code on standard output, carrying a terminal-QR
             * passkey authenticator for answering a server-pushed integrity checkpoint.
             *
             * @return the terminal-printing handler
             */
            static PairingCode toTerminal() {
                return toTerminal(LinkedWhatsAppClientPasskeyAuthenticator.toTerminal());
            }

            /**
             * Returns a handler that prints the pairing code exactly as this one does but carries the
             * given passkey authenticator instead, so the linked session answers a server-pushed
             * integrity checkpoint with it rather than this handler's authenticator.
             *
             * @param authenticator the passkey authenticator to attach; never {@code null}
             * @return a handler that prints as this one and carries the authenticator
             * @throws NullPointerException if {@code authenticator} is {@code null}
             */
            default PairingCode withPasskeyAuthenticator(LinkedWhatsAppClientPasskeyAuthenticator authenticator) {
                Objects.requireNonNull(authenticator, "authenticator must not be null");
                return of(this::handle, authenticator);
            }

            /**
             * Builds a pairing-code handler that presents the code through the given renderer and carries
             * the given passkey authenticator.
             *
             * @param renderer      the action that presents the pairing code
             * @param authenticator the passkey authenticator the handler carries
             * @return the composed handler
             */
            private static PairingCode of(Consumer<String> renderer, LinkedWhatsAppClientPasskeyAuthenticator authenticator) {
                return new PairingCode() {
                    @Override
                    public void handle(String value) {
                        renderer.accept(value);
                    }

                    @Override
                    public LinkedWhatsAppClientPasskeyAuthenticator passkeyAuthenticator() {
                        return authenticator;
                    }
                };
            }
        }

        /**
         * A verification handler that links the companion through a passkey instead of a QR code or
         * pairing code.
         *
         * @apiNote
         * Passkey linking authenticates the companion with a WebAuthn assertion (driven by the
         * {@link #passkeyAuthenticator()} this handler carries) and then runs a key-agreement handshake
         * with the primary device that yields a short verification code. The inherited
         * {@link #handle(String)} surfaces that code for the user to compare against the one shown on
         * the primary, and {@link #confirmVerificationCode(String)} reports whether the codes matched.
         * The same {@link #passkeyAuthenticator()} also answers a server-pushed integrity checkpoint on
         * the connected session. A client linked through {@link QrCode} or {@link PairingCode} answers a
         * checkpoint with its own {@link #passkeyAuthenticator()} too, which its no-argument factories
         * default to a terminal QR.
         */
        non-sealed interface Passkey extends Web {
            /**
             * Reports whether the verification code the companion derived matches the one shown on
             * the primary device, gating completion of the link.
             *
             * @apiNote
             * Implementations typically block on user input to let the user compare the code against
             * their primary device. The handshake completes only when this returns {@code true}; a
             * {@code false} return aborts the link.
             *
             * @implSpec
             * The default implementation returns {@code true}, auto-confirming the code. This is the
             * right behaviour for an unattended session that trusts the handshake; implementations
             * that want a human to compare the codes should override it.
             *
             * @param verificationCode the verification code the companion derived
             * @return {@code true} to complete the link, {@code false} to abort it
             */
            default boolean confirmVerificationCode(String verificationCode) {
                return true;
            }

            /**
             * Returns a handler whose authenticator drives Warden's cross-device hybrid transport,
             * rendering each ceremony's QR code as a scannable QR on standard output.
             *
             * <p>Shows a {@code FIDO:/...} QR code (printed to standard output, the same way
             * {@link Web.QrCode#toTerminal()} renders the companion-linking QR); the user scans it with
             * the phone that holds the {@code whatsapp.com} passkey, which then produces the assertion
             * over an encrypted tunnel. Use {@link #toQr(Consumer)} to render the QR yourself.
             *
             * @return the terminal-rendering handler
             */
            static Passkey toTerminal() {
                return of(LinkedWhatsAppClientPasskeyAuthenticator.toTerminal());
            }

            /**
             * Returns a handler whose authenticator drives Warden's cross-device hybrid transport,
             * handing each ceremony's {@code FIDO:/...} QR payload to the given consumer to render,
             * backed by {@link LinkedWhatsAppClientPasskeyAuthenticator#toQr(Consumer)}.
             *
             * <p>The user scans the rendered QR with the phone that holds the {@code whatsapp.com}
             * passkey, which then produces the assertion over an encrypted tunnel. The returned handler
             * auto-confirms the short verification code and prints it on standard output; implement
             * {@link Passkey} directly and override {@link #confirmVerificationCode(String)} to compare
             * it by hand.
             *
             * @param onQrCode the consumer the {@code FIDO:/...} QR payload is handed to for rendering;
             *                 never {@code null}
             * @return the QR-rendering handler
             * @throws NullPointerException if {@code onQrCode} is {@code null}
             */
            static Passkey toQr(Consumer<String> onQrCode) {
                Objects.requireNonNull(onQrCode, "onQrCode must not be null");
                return of(LinkedWhatsAppClientPasskeyAuthenticator.toQr(onQrCode));
            }

            /**
             * Returns a handler whose authenticator drives Warden's cross-device hybrid transport,
             * rendering each ceremony's {@code FIDO:/...} QR code as a JPEG image at the supplied path and
             * forwarding the path to the given consumer.
             *
             * <p>The user scans the rendered QR with the phone that holds the {@code whatsapp.com}
             * passkey, which then produces the assertion over an encrypted tunnel. The returned handler
             * auto-confirms the short verification code and prints it on standard output; implement
             * {@link Passkey} directly and override {@link #confirmVerificationCode(String)} to compare
             * it by hand.
             *
             * @param path         the destination path where the ceremony QR image is saved
             * @param fileConsumer the consumer that receives the path of the rendered file
             * @return the file-rendering handler
             * @throws NullPointerException if {@code path} or {@code fileConsumer} is {@code null}
             */
            static Passkey toFile(Path path, ToFile fileConsumer) {
                Objects.requireNonNull(path, "path must not be null");
                Objects.requireNonNull(fileConsumer, "fileConsumer must not be null");
                return of(LinkedWhatsAppClientPasskeyAuthenticator.toQr(fidoUrl -> {
                    renderQrToFile(fidoUrl, path);
                    fileConsumer.accept(path);
                }));
            }

            /**
             * Returns a handler whose authenticator drives Warden's cross-device hybrid transport,
             * rendering each ceremony's {@code FIDO:/...} QR code as a temporary JPEG file and forwarding
             * the file path to the given consumer.
             *
             * <p>The user scans the rendered QR with the phone that holds the {@code whatsapp.com}
             * passkey, which then produces the assertion over an encrypted tunnel. The returned handler
             * auto-confirms the short verification code and prints it on standard output; implement
             * {@link Passkey} directly and override {@link #confirmVerificationCode(String)} to compare
             * it by hand.
             *
             * @param fileConsumer the consumer that receives the path of the rendered file
             * @return the file-rendering handler
             * @throws NullPointerException if {@code fileConsumer} is {@code null}
             * @throws UncheckedIOException if the temporary file cannot be created
             */
            static Passkey toFile(ToFile fileConsumer) {
                return toFile(createQrTempFile(), fileConsumer);
            }

            /**
             * Returns a handler whose authenticator relays each ceremony to an external WebAuthn
             * authenticator as JSON, backed by
             * {@link LinkedWhatsAppClientPasskeyAuthenticator#toWebAuthnJson(Function)}.
             *
             * <p>The {@code ceremony} handler forwards the {@code navigator.credentials.get} options JSON
             * to an authenticator it controls (a browser served from {@code *.whatsapp.com}, the user's
             * phone, a hardware key, a REST endpoint) and returns the resulting {@code PublicKeyCredential}
             * JSON. The returned handler auto-confirms the short verification code and prints it on
             * standard output; implement {@link Passkey} directly and override
             * {@link #confirmVerificationCode(String)} to compare it by hand.
             *
             * @param ceremony maps the {@code navigator.credentials.get} options JSON to the resulting
             *                 {@code PublicKeyCredential} JSON; never {@code null}
             * @return the relay handler
             * @throws NullPointerException if {@code ceremony} is {@code null}
             */
            static Passkey toWebAuthnJson(Function<String, String> ceremony) {
                Objects.requireNonNull(ceremony, "ceremony must not be null");
                return of(LinkedWhatsAppClientPasskeyAuthenticator.toWebAuthnJson(ceremony));
            }

            /**
             * Wraps an authenticator in an auto-confirming passkey handler that prints the verification
             * code on standard output.
             *
             * @param authenticator the authenticator the handler carries
             * @return the auto-confirming handler
             */
            private static Passkey of(LinkedWhatsAppClientPasskeyAuthenticator authenticator) {
                return new Passkey() {
                    @Override
                    public LinkedWhatsAppClientPasskeyAuthenticator passkeyAuthenticator() {
                        return authenticator;
                    }

                    @Override
                    public void handle(String value) {
                        System.out.println(value);
                    }
                };
            }
        }
    }

    /**
     * A verification handler for the WhatsApp mobile registration
     * flow.
     *
     * @apiNote
     * Mobile registration requires the user to receive a one-time code
     * on the phone number being registered and to feed it back into
     * the client. Implementations expose two decisions: which delivery
     * channel to request (SMS, voice call, in-app WhatsApp, or
     * server-chosen) and how to obtain the code once it has been
     * delivered. Optional callbacks handle CAPTCHA challenges and
     * two-factor PIN prompts.
     */
    non-sealed interface Mobile extends LinkedWhatsAppClientVerificationHandler {
        /**
         * Returns the preferred delivery channel for the verification
         * code.
         *
         * @apiNote
         * Supported values mirror the WhatsApp server-side method
         * identifiers: {@code sms}, {@code voice}, and {@code wa_old}.
         * Returning {@link Optional#empty()} lets the server pick a
         * default channel.
         *
         * @return the preferred delivery method, or empty to defer the
         *         choice to the server
         */
        Optional<String> requestMethod();

        /**
         * Returns the verification code supplied by the user.
         *
         * @apiNote
         * Implementations typically block on user input (for example
         * reading a console line) and return the code once it has been
         * entered.
         *
         * @return the verification code
         */
        String verificationCode();

        /**
         * Solves a server-issued CAPTCHA challenge and returns the
         * user's answer.
         *
         * @apiNote
         * Called by the registration code when {@code /v2/code} or
         * {@code /v2/exist} returns an {@code image_blob} or
         * {@code audio_blob} payload, which happens whenever the
         * server places the request in the low-trust lane (typically
         * because no Play Integrity or App Attest token was submitted).
         * Returning {@link Optional#empty()} aborts registration with a
         * failure.
         *
         * @implSpec
         * The default implementation returns {@link Optional#empty()},
         * which the registration code treats as "caller cannot solve
         * challenges". Implementations that can prompt the user should
         * override this method.
         *
         * @param imagePng the PNG image challenge bytes, or
         *                 {@code null} if the server did not include
         *                 one
         * @param audioOgg the Ogg-encoded audio challenge bytes, or
         *                 {@code null} if the server did not include
         *                 one
         * @return the user-supplied answer, or empty to abort
         */
        default Optional<String> solveCaptcha(byte[] imagePng, byte[] audioOgg) {
            return Optional.empty();
        }

        /**
         * Returns the two-factor authentication PIN configured on the
         * account being registered.
         *
         * @apiNote
         * Called by the registration code when {@code /v2/register}
         * returns a {@code 2fa_required} reason. Returning
         * {@link Optional#empty()} aborts registration with a failure.
         *
         * @implSpec
         * The default implementation returns {@link Optional#empty()},
         * which the registration code treats as "caller cannot supply
         * a PIN". Implementations that can prompt the user should
         * override this method.
         *
         * @return the PIN, or empty to abort
         */
        default Optional<String> twoFactorPin() {
            return Optional.empty();
        }

        /**
         * Returns a verification handler that doesn't request a new
         * verification code.
         *
         * @apiNote
         * Use when the calling application is already in possession of a verification
         * code and doesn't need a new one
         *
         * @param supplier the supplier that produces the verification code
         * @return the verification handler
         * @throws NullPointerException if {@code supplier} is
         *                              {@code null}
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
         * Returns a verification handler that requests SMS delivery
         * and reads the verification code from the supplied supplier.
         *
         * @param supplier the supplier that produces the verification
         *                 code once the user has received it
         * @return the verification handler
         * @throws NullPointerException if {@code supplier} is
         *                              {@code null}
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
         * Returns a verification handler that requests voice-call
         * delivery and reads the verification code from the supplied
         * supplier.
         *
         * @param supplier the supplier that produces the verification
         *                 code once the user has received it
         * @return the verification handler
         * @throws NullPointerException if {@code supplier} is
         *                              {@code null}
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
         * Returns a verification handler that requests in-app WhatsApp
         * delivery and reads the verification code from the supplied
         * supplier.
         *
         * @apiNote
         * Maps to the server-side {@code wa_old} method, which delivers
         * the code via an existing WhatsApp install on the same number.
         *
         * @param supplier the supplier that produces the verification
         *                 code once the user has received it
         * @return the verification handler
         * @throws NullPointerException if {@code supplier} is
         *                              {@code null}
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
