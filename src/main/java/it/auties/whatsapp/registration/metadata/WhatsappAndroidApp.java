package it.auties.whatsapp.registration.metadata;

import it.auties.whatsapp.model.signal.auth.Version;

import java.util.List;

record WhatsappAndroidApp(Version version, byte[] md5Hash, byte[] secretKey, List<byte[]> certificates,
                          boolean business) {

}
