package it.auties.whatsapp.listener;

import it.auties.whatsapp.model.call.Call;

public interface OnCall extends Listener {
    /**
     * Called when a phone call arrives
     *
     * @param call the non-null phone call
     */
    @Override
    void onCall(Call call);
}
