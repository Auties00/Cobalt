package it.auties.whatsapp.util;

import it.auties.whatsapp.controller.ControllerProvider;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.stream.Collectors;

@UtilityClass
public class ControllerProviderLoader {
    private final ServiceLoader<ControllerProvider> LOADER = ServiceLoader.load(ControllerProvider.class);
    private final DefaultControllerProvider DEFAULT_SERIALIZER = new DefaultControllerProvider();

    public List<ControllerProvider> providers(boolean useDefault) {
        var modules = LOADER.stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toCollection(ArrayList::new));
        if (useDefault) {
            modules.add(DEFAULT_SERIALIZER);
        }

        return modules;
    }

    public LinkedList<Integer> allIds(boolean useDefault){
        return providers(useDefault)
                .stream()
                .map(ControllerProvider::ids)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedList::new));
    }
}
