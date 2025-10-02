package it.auties.whatsapp.stream.webAppState;

import it.auties.whatsapp.model.sync.MutationSync;
import it.auties.whatsapp.model.sync.RecordSync;

record MutationResult(MutationSync sync, byte[] indexMac, byte[] valueMac, RecordSync.Operation operation) {

}
