package com.github.auties00.cobalt.sync.handlers;

import com.github.auties00.cobalt.sync.model.DecryptedMutation;
import com.github.auties00.cobalt.store.WhatsappStore;
import com.github.auties00.cobalt.sync.WebAppStateActionHandler;

/**
 * Handles time format actions.
 *
 * <p>This handler processes mutations that change the time format preference (12/24 hour).
 */
public final class TimeFormatHandler implements WebAppStateActionHandler {
    public static final TimeFormatHandler INSTANCE = new TimeFormatHandler();

    private TimeFormatHandler() {

    }

    @Override
    public String actionName() {
        return "timeFormatAction";
    }

    @Override
    public boolean applyMutation(WhatsappStore store, DecryptedMutation.Trusted mutation) {
        var action = mutation.value()
                .timeFormatAction()
                .orElseThrow(() -> new IllegalArgumentException("Missing timeFormatAction"));

        store.setTwentyFourHourFormat(action.twentyFourHourFormatEnabled());

        return true;
    }
}
