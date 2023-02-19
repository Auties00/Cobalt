package it.auties.whatsapp.model.sync;

import it.auties.whatsapp.binary.PatchType;
import it.auties.whatsapp.model.sync.RecordSync.Operation;
import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.Specification;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record PatchRequest(PatchType type, ActionValueSync sync, String index, int version,
                           Operation operation) implements JacksonProvider {
    public static PatchRequest of(PatchType type, ActionValueSync sync, Operation operation) {
        return of(type, sync, operation, Specification.Signal.CURRENT_VERSION);
    }

    @SneakyThrows
    public static PatchRequest of(PatchType type, ActionValueSync sync, Operation operation, int version, String... args) {
        var index = JSON.writeValueAsString(createArguments(sync, args));
        return new PatchRequest(type, sync, index, version, operation);
    }

    private static List<String> createArguments(ActionValueSync sync, String... args) {
        var action = sync.action();
        if (action != null) {
            var index = new ArrayList<String>();
            index.add(action.indexName());
            index.addAll(Arrays.asList(args));
            return index;
        }
        var setting = sync.setting();
        if (setting != null) {
            return List.of(setting.indexName());
        }
        throw new IllegalArgumentException("Cannot encode %s".formatted(sync));
    }
}
