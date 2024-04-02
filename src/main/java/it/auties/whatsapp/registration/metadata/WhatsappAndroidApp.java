package it.auties.whatsapp.registration.metadata;

import it.auties.whatsapp.model.signal.auth.Version;

record WhatsappAndroidApp(
        String packageName,
        Version version,
        byte[] apkSha256,
        byte[] apkShatr,
        int apkSize,
        byte[] classesMd5,
        byte[] secretKey,
        byte[] signature,
        byte[] signatureSha1
) {

}
