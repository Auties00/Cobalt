package it.auties.whatsapp4j.utils;

import lombok.SneakyThrows;
import net.glxn.qrgen.javase.QRCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.util.Base64;

/**
 * A utility class used to generate QR codes to authenticate with whatsapp
 */
public class WhatsappQRCode {
    private @Nullable File qr;
    private @Nullable String ref;
    private static final int SIZE = 250;

    /**
     * Generates a QR code to initialize the connection with WhatsappWeb's WebSocket
     *
     * @param ref the ref to generate the qr code, might be null
     * @param publicKey the non null publicKey
     * @param clientId the non null client id
     * @return the same object it was called on
     */
    @SneakyThrows
    public @NotNull WhatsappQRCode generateQRCodeImage(@Nullable String ref, byte @NotNull [] publicKey, @NotNull String clientId) {
        if(ref != null) this.ref = ref;
        this.qr = QRCode
                .from("%s,%s,%s".formatted(this.ref, Base64.getEncoder().encodeToString(publicKey), clientId))
                .withSize(SIZE, SIZE)
                .file();
        return this;
    }

    /**
     * Opens the qr code stored by this object using the default image editor.
     * This will probably not work on Linux distros that don't provide a GUI.
     * Looking for alternatives.
     */
    @SneakyThrows
    public void open() {
        if(qr == null) return;
        Desktop.getDesktop().open(qr);
    }
}
