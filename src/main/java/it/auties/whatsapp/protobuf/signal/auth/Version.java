package it.auties.whatsapp.protobuf.signal.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.util.Validate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Version {
    @JsonProperty("1")
    @JsonPropertyDescription("uint32")
    private int primary;

    @JsonProperty("2")
    @JsonPropertyDescription("uint32")
    private int secondary;

    @JsonProperty("3")
    @JsonPropertyDescription("uint32")
    private int tertiary;

    @JsonProperty("4")
    @JsonPropertyDescription("uint32")
    private int quaternary;

    @JsonProperty("5")
    @JsonPropertyDescription("uint32")
    private int quinary;

    public Version(int primary) {
        this.primary = primary;
    }

    public Version(int primary, int secondary, int tertiary) {
        this.primary = primary;
        this.secondary = secondary;
        this.tertiary = tertiary;
    }

    public Version(int[] version) {
        Validate.isTrue(version.length == 3 || version.length == 5,
                "Invalid encoded version %s: expected 3 or 5 entries", Arrays.toString(version));
        this.primary = version[0];
        this.secondary = version[1];
        this.tertiary = version[2];
        if (version.length != 5) {
            return;
        }

        this.quaternary = version[3];
        this.quinary = version[4];
    }
}
