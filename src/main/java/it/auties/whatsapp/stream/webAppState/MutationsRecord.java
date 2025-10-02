package it.auties.whatsapp.stream.webAppState;

import it.auties.whatsapp.model.sync.ActionDataSync;

import java.util.List;

record MutationsRecord(LTHash.Result result, List<ActionDataSync> records) {

}
