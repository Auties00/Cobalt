package it.auties.whatsapp.model.sync;

import it.auties.whatsapp.model.sync.RecordSync.Operation;
import it.auties.whatsapp.util.Json;
import it.auties.whatsapp.util.Spec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record PatchRequest(PatchType type, List<PatchEntry> entries) {
    public record PatchEntry(ActionValueSync sync, String index, int version, Operation operation) {
        public static PatchEntry of(ActionValueSync sync, Operation operation) {
            return of(sync, operation, Spec.Signal.CURRENT_VERSION);
        }

        public static PatchEntry of(ActionValueSync sync, Operation operation, int version, String... args) {
            var index = Json.writeValueAsString(createArguments(sync, args));
            return new PatchEntry(sync, index, version, operation);
        }

        private static List<String> createArguments(ActionValueSync sync, String... args) {
            var action = sync.action();
            if (action.isPresent()) {
                var index = new ArrayList<String>();
                index.add(action.get().indexName());
                index.addAll(Arrays.asList(args));
                return index;
            }
            var setting = sync.setting();
            if (setting.isPresent()) {
                return List.of(setting.get().indexName());
            }
            throw new IllegalArgumentException("Cannot encode %s".formatted(sync));
        }
    }
}
