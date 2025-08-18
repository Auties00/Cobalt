package it.auties.whatsapp.registration.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.signal.auth.Version;

import java.net.URI;
import java.util.List;

record KaiOsCatalogResponse(List<App> apps) {
    record App(String name, Version version, @JsonProperty("package_path") URI uri) {

    }
}
