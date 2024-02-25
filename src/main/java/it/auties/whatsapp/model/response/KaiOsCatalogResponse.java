package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.signal.auth.Version;

import java.net.URI;
import java.util.List;

public record KaiOsCatalogResponse(List<App> apps) {
    public record App(String name, Version version, @JsonProperty("package_path") URI uri) {

    }
}
