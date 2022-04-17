package it.auties.whatsapp;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

import it.auties.protobuf.api.model.ProtobufProperty;
import java.util.*;
import lombok.*;
import lombok.experimental.*;
import lombok.extern.jackson.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class GlobalSettings {

  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = WallpaperSettings.class)
  private WallpaperSettings lightThemeWallpaper;

  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = MediaVisibility.class)
  private MediaVisibility mediaVisibility;

  @ProtobufProperty(index = 3, type = MESSAGE, concreteType = WallpaperSettings.class)
  private WallpaperSettings darkThemeWallpaper;
}
