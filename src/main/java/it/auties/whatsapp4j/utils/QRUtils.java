package it.auties.whatsapp4j.utils;

import lombok.experimental.UtilityClass;
import net.glxn.qrgen.javase.QRCode;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@UtilityClass
public class QRUtils {
    public BufferedImage generateQRCodeImage(@NotNull String barcodeText) throws IOException {
        final var stream = QRCode
                .from(barcodeText)
                .withSize(250, 250)
                .stream();

        return ImageIO.read(new ByteArrayInputStream(stream.toByteArray()));
    }

    public void saveAndOpenQrCode(@NotNull BufferedImage image) throws IOException {
        final var file = File.createTempFile(UUID.randomUUID().toString(), ".jpg");
        ImageIO.write(image, "jpg", file);
        Desktop.getDesktop().open(file);
    }
}
