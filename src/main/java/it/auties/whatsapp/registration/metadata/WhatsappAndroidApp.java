package it.auties.whatsapp.registration.metadata;

import it.auties.whatsapp.model.signal.auth.Version;

import java.util.List;

record WhatsappAndroidApp(
        String packageName,
        Version version,
        byte[] sha256Hash,
        byte[] compactSha256Hash,
        byte[] md5Hash,
        byte[] secretKey,
        List<byte[]> certificates,
        int size,
        boolean business
) {

}
