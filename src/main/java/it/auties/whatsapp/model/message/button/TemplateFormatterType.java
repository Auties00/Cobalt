package it.auties.whatsapp.model.message.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;

/**
 * The constant of this enumerated type define the various of types of visual formats for a
 * {@link TemplateMessage}
 */
@AllArgsConstructor
@Accessors(fluent = true)
public enum TemplateFormatterType {
    /**
     * No format
     */
    NONE(0),
    /**
     * Four row template
     */
    FOUR_ROW(1),
    /**
     * Hydrated four row template
     */
    HYDRATED_FOUR_ROW(2),
    /**
     * Interactive message
     */
    INTERACTIVE(3);

    @Getter
    private final int index;

    @JsonCreator
    public static TemplateFormatterType of(int index) {
        return Arrays.stream(values())
                .filter(entry -> entry.index() == index)
                .findFirst()
                .orElse(TemplateFormatterType.NONE);
    }
}
