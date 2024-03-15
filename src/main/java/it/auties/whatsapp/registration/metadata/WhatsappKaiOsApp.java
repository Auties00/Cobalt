package it.auties.whatsapp.registration.metadata;

import it.auties.whatsapp.model.signal.auth.Version;

record WhatsappKaiOsApp(Version version, byte[] indexHtml, byte[] backendJs) {

}
