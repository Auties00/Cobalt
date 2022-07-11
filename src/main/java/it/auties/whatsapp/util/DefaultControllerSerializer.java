package it.auties.whatsapp.util;

import it.auties.whatsapp.controller.Controller;
import it.auties.whatsapp.controller.ControllerSerializer;

public class DefaultControllerSerializer implements ControllerSerializer {
    @Override
    public void serialize(Controller<?> controller) {
        controller.preferences()
                .writeJson(controller, true);
    }
}
