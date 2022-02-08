package it.auties.whatsapp.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface VersionProvider {
    /**
     * The current version
     */
    int CURRENT_VERSION = 3;
}
