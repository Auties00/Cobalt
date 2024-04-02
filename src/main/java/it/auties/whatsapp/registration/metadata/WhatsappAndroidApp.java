package it.auties.whatsapp.registration.metadata;

import it.auties.whatsapp.model.signal.auth.Version;

import java.util.List;

record WhatsappAndroidApp(
        String packageName,
        Version version,
        byte[] apkSha256,
        byte[] apkCompactSha256,
        int apkSize,
        byte[] classesMD5,
        byte[] secretKey,
        List<byte[]> certificates,
        byte[] certificatesSha1,
        boolean business
) {

}
