package it.auties.whatsapp.model.sync;

import it.auties.whatsapp.util.JacksonProvider;
import it.auties.whatsapp.util.SignalSpecification;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;

public record PatchRequest(String type, ActionValueSync sync, String index, int version, RecordSync.Operation operation)
        implements JacksonProvider, SignalSpecification {

    @SneakyThrows
    public static PatchRequest newRequest(String type, ActionValueSync sync, RecordSync.Operation operation, int version){
        var index = JSON.writeValueAsString(createArguments(sync));
        return new PatchRequest(type, sync, index, version, operation);
    }

    public static PatchRequest newRequest(String type, ActionValueSync sync, RecordSync.Operation operation){
        return newRequest(type, sync, operation, CURRENT_VERSION);
    }

    private static List<String> createArguments(ActionValueSync sync) {
        var action = sync.action();
        if(action != null) {
            var index = new ArrayList<String>();
            index.add(action.indexName());
            index.addAll(action.indexArguments());
            return index;
        }

        var setting = sync.setting();
        if(setting != null){
            return List.of(setting.indexName());
        }

        throw new IllegalArgumentException("Cannot encode %s".formatted(sync));
    }
}
