package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class GlobalSettings {

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("WallpaperSettings")
  private WallpaperSettings darkThemeWallpaper;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("MediaVisibility")
  private MediaVisibility mediaVisibility;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("WallpaperSettings")
  private WallpaperSettings lightThemeWallpaper;
}
