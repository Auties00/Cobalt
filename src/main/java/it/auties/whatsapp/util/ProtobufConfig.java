package it.auties.whatsapp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import it.auties.map.MapDeserializerModifier;
import it.auties.protobuf.annotation.ProtobufConfigurator;
import it.auties.protobuf.decoder.ProtobufDecoderConfigurator;

@ProtobufConfigurator
@SuppressWarnings("unused") // Used by protobuf
public class ProtobufConfig implements ProtobufDecoderConfigurator {
    @Override
    public ObjectMapper createMapper() {
        return ProtobufDecoderConfigurator.super
                .createMapper()
                .registerModule(new SimpleModule().setDeserializerModifier(new MapDeserializerModifier()));
    }
}
