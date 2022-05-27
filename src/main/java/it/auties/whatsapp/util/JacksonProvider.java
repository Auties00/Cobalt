package it.auties.whatsapp.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.auties.map.SimpleMapModule;
import it.auties.protobuf.api.jackson.ProtobufMapper;

public interface JacksonProvider {
    ProtobufMapper PROTOBUF = (ProtobufMapper) new ProtobufMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true)
            .registerModule(new Jdk8Module())
            .registerModule(new SimpleMapModule());

    ObjectMapper JSON = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new SimpleMapModule())
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(Include.NON_DEFAULT)
            .enable(SerializationFeature.WRITE_ENUMS_USING_INDEX)
            .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
}
