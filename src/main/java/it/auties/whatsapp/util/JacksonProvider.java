package it.auties.whatsapp.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.auties.map.SimpleMapModule;
import it.auties.protobuf.api.jackson.ProtobufMapper;

public interface JacksonProvider {
    ObjectMapper PROTOBUF = configureMapper(new ProtobufMapper());
    ObjectMapper JSON = configureMapper(new ObjectMapper());

    private static ObjectMapper configureMapper(ObjectMapper mapper){
        return mapper.setSerializationInclusion(Include.NON_NULL)
                .setSerializationInclusion(Include.NON_DEFAULT)
                .registerModule(new Jdk8Module())
                .registerModule(new SimpleMapModule())
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.WRITE_ENUMS_USING_INDEX)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
