package it.auties.whatsapp.util;

import it.auties.whatsapp.controller.ControllerSerializer;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.ServiceLoader;

@UtilityClass
public class ControllerSerializerLoader {
    private final ServiceLoader<ControllerSerializer> LOADER = ServiceLoader.load(ControllerSerializer.class);
    private final DefaultControllerSerializer DEFAULT_SERIALIZER = new DefaultControllerSerializer();

    public List<ControllerSerializer> providers(boolean useDefault) {
        if (useDefault) {
            return List.of(DEFAULT_SERIALIZER);
        }

        return LOADER.stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }
}
