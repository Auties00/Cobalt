package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A model clas that represents the time format used by the companion
 */
@ProtobufMessage(name = "SyncActionValue.TimeFormatAction")
public final class TimeFormatAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean twentyFourHourFormatEnabled;

    TimeFormatAction(boolean twentyFourHourFormatEnabled) {
        this.twentyFourHourFormatEnabled = twentyFourHourFormatEnabled;
    }

    @Override
    public String indexName() {
        return "time_format";
    }

    @Override
    public int actionVersion() {
        return 7;
    }

    public boolean twentyFourHourFormatEnabled() {
        return twentyFourHourFormatEnabled;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TimeFormatAction that
                && twentyFourHourFormatEnabled == that.twentyFourHourFormatEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(twentyFourHourFormatEnabled);
    }

    @Override
    public String toString() {
        return "TimeFormatAction[" +
                "twentyFourHourFormatEnabled=" + twentyFourHourFormatEnabled + ']';
    }
}
