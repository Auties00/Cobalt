package it.auties.whatsapp.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import it.auties.whatsapp.manager.WhatsappKeys;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.awt.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.google.zxing.client.j2se.MatrixToImageWriter.writeToPath;
import static it.auties.whatsapp.binary.BinaryArray.of;
import static java.awt.Desktop.getDesktop;
import static java.awt.Desktop.isDesktopSupported;
import static java.nio.file.Files.createTempFile;

/**
 * A utility class used to generate QR codes to authenticate with whatsapp
 */
@UtilityClass
public class Qr {
    /**
     * Margin between the qr code and the border
     */
    private final int MARGIN = 5;

    /**
     * Size of the QR code, only affects files
     */
    private final int SIZE = 500;

    /**
     * QR code writer
     */
    private final MultiFormatWriter WRITER = new MultiFormatWriter();

    /**
     * Generates a QR code to initialize the connection with WhatsappWeb's WebSocket
     *
     * @param keys the non-null keys to use to generate the QR code
     * @param ref  the non-null string reference to generate the qr code
     * @return a non-null {@link BitMatrix}
     */
    @SneakyThrows
    public BitMatrix generate(@NonNull WhatsappKeys keys, @NonNull String ref) {
        var qr = "%s,%s,%s,%s".formatted(
                ref,
                of(keys.companionKeyPair().publicKey()).toBase64(),
                of(keys.identityKeyPair().publicKey()).toBase64(),
                of(keys.companionKey()).toBase64()
        );

        return WRITER.encode(qr, BarcodeFormat.QR_CODE, SIZE, SIZE, Map.of(EncodeHintType.MARGIN, MARGIN, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L));
    }
}
