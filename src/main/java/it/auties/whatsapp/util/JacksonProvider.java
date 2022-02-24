package it.auties.whatsapp.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import it.auties.map.SimpleMapModule;

public interface JacksonProvider {
    /**
     * An instance of Jackson
     */
    ObjectMapper JACKSON = new ObjectMapper()
            .setSerializationInclusion(Include.NON_NULL)
            .setSerializationInclusion(Include.NON_DEFAULT)
            .registerModule(new Jdk8Module())
            .registerModule(new SimpleMapModule())
            .enable(SerializationFeature.WRITE_ENUMS_USING_INDEX)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
}
