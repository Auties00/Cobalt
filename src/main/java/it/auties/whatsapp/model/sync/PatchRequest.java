package it.auties.whatsapp.model.sync;

import it.auties.whatsapp.model.sync.RecordSync.Operation;
import it.auties.whatsapp.util.Json;
import it.auties.whatsapp.util.Spec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record PatchRequest(ActionValueSync sync, String index, int version, Operation operation) {
    public static PatchRequest of(ActionValueSync sync, Operation operation) {
        return of(sync, operation, Spec.Signal.CURRENT_VERSION);
    }

    public static PatchRequest of(ActionValueSync sync, Operation operation, int version, String... args) {
        var index = Json.writeValueAsString(createArguments(sync, args));
        return new PatchRequest(sync, index, version, operation);
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
