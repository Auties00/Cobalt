package it.auties.whatsapp.model.business;

import java.util.List;
import lombok.NonNull;

public record BusinessHours(@NonNull String timeZone, @NonNull List<BusinessHoursEntry> entries) {

}
