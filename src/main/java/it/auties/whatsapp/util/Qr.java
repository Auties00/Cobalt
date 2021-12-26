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

import java.util.Map;

import static it.auties.whatsapp.binary.BinaryArray.of;

/**
 * A utility class used to generate QR codes to authenticate with whatsapp
 */
@UtilityClass
public class Qr {
    private final int MARGIN = 0;
    private final int SIZE = 50;
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

    /**
     * Prints thr provided QR code to the console
     *
     * @param qr the qr code to print
     */
    public void print(@NonNull BitMatrix qr){
        System.out.println(qr.toString("\033[40m  \033[0m", "\033[47m  \033[0m"));
    }
}
