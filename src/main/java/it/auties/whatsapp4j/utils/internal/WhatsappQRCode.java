package it.auties.whatsapp4j.utils.internal;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;

/**
 * A utility class used to generate QR codes to authenticate with whatsapp
 */
public class WhatsappQRCode {
    private String ref;
    private static final int SIZE = 50;
    private static final MultiFormatWriter WRITER = new MultiFormatWriter();

    /**
     * Generates and prints the QR code to initialize the connection with WhatsappWeb's WebSocket
     *
     * @param ref       the ref to generate the qr code, might be null
     * @param publicKey the non null raw publicKey
     * @param clientId  the non null client id
     */
    @SneakyThrows
    public void generateAndPrint(String ref,byte @NotNull [] publicKey, @NotNull String clientId) {
        this.ref = Objects.requireNonNullElse(ref, this.ref);
        var qr = "%s,%s,%s".formatted(this.ref, Base64.getEncoder().encodeToString(publicKey), clientId);
        System.out.println(WRITER.encode(qr, BarcodeFormat.QR_CODE, SIZE, SIZE, Map.of(EncodeHintType.MARGIN, 0)).toString("\033[40m  \033[0m", "\033[47m  \033[0m"));
    }
}
