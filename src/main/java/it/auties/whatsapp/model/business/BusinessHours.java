package it.auties.whatsapp.model.business;

import lombok.NonNull;

import java.util.List;

public record BusinessHours(@NonNull String timeZone, @NonNull List<BusinessHoursEntry> entries) {
}
