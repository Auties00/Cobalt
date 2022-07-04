package it.auties.whatsapp.model.sync;

import it.auties.whatsapp.crypto.LTHash;

import java.util.List;

public record MutationsRecord(LTHashState state, LTHash.Result result, List<ActionDataSync> records) {
}
