package it.auties.whatsapp.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import it.auties.whatsapp.controller.WhatsappKeys;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.lang.constant.Constable;
import java.util.Map;

import static it.auties.bytes.Bytes.of;

@UtilityClass
public class QrGenerator {
    private final int MARGIN = 5;
    private final int SIZE = 500;
    private final MultiFormatWriter WRITER = new MultiFormatWriter();

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
