package it.auties.whatsapp.model.sync;

public sealed interface GenericSync permits ActionDataSync, ActionValueSync, ParsableMutation {
}
