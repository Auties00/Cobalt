package it.auties.whatsapp.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import it.auties.whatsapp.manager.WhatsappKeys;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.lang.constant.Constable;
import java.util.Map;

import static it.auties.buffer.ByteBuffer.of;

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

        return WRITER.encode(qr, BarcodeFormat.QR_CODE,
                SIZE, SIZE, createOptions());
    }

    private Map<EncodeHintType, ? extends Constable> createOptions() {
        return Map.of(
                EncodeHintType.MARGIN, MARGIN,
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L
        );
    }
}
