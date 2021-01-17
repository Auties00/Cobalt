package it.auties.whatsapp4j.utils;

import lombok.SneakyThrows;
import net.glxn.qrgen.javase.QRCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.util.Base64;

public class WhatsappQRCode {
    private static final int SIZE = 250;
    private @Nullable File qr;
    private @Nullable String ref;

    @SneakyThrows
    public @NotNull WhatsappQRCode generateQRCodeImage(@Nullable String ref, byte[] publicKey, @NotNull String clientId) {
        Validate.ifTrue(ref != null, () -> this.ref = ref);
        this.qr = QRCode
                .from("%s,%s,%s".formatted(this.ref, Base64.getEncoder().encodeToString(publicKey), clientId))
                .withSize(SIZE, SIZE)
                .file();

        return this;
    }

    public void open() {
        Validate.ifTrue(qr != null, this::openImageWithDesktop);
    }

    @SneakyThrows
    private void openImageWithDesktop(){
        Desktop.getDesktop().open(qr);
    }
}
