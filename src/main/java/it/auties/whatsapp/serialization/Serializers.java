package it.auties.whatsapp.serialization;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Accessors(fluent = true, chain = true, makeFinal = true)
public class Serializers {
    /**
     * Serializer provider
     * @param serializer new serializer
     * @return the serializer
     */
    @Getter
    @Setter
    @NonNull
    private ControllerSerializerProvider serializer ;

    /**
     * Deserializer provider
     * @param deserializer new deserializer
     * @return the deserializer
     */
    @Getter
    @Setter
    @NonNull
    private ControllerDeserializerProvider deserializer ;
}
