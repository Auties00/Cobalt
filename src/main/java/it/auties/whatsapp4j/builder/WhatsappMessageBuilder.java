package it.auties.whatsapp4j.builder;

import jakarta.validation.constraints.NotNull;

interface WhatsappMessageBuilder<R> {
    /**
     * Builds a WhatsappMessage from the data provided
     *
     * @return a non null object
     */
    @NotNull R create();
}
