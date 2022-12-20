package it.auties.whatsapp.model.business;

import it.auties.whatsapp.model.request.Node;
import lombok.NonNull;

public record BusinessHoursEntry(@NonNull String day, @NonNull String mode, long openTime, long closeTime) {
    public static BusinessHoursEntry of(@NonNull Node node) {
        return new BusinessHoursEntry(node.attributes()
                                              .getString("day_of_week"), node.attributes()
                                              .getString("mode"), node.attributes()
                                              .getLong("open_time"), node.attributes()
                                              .getLong("close_time"));
    }

    public boolean isAlwaysOpen() {
        return openTime == 0 && closeTime == 0;
    }
}
