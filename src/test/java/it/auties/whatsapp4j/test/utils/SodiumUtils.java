package it.auties.whatsapp4j.test.utils;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import it.auties.whatsapp4j.test.sodium.Sodium;
import it.auties.whatsapp4j.utils.internal.Validate;
import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.util.Objects;

@UtilityClass
public class SodiumUtils {
    private final String SODIUM_WIN32 = "sodium/windows/libsodium.dll";
    private final String SODIUM_WIN64 = "sodium/windows64/libsodium.dll";
    private final String SODIUM_LINUX = "sodium/linux64/libsodium.so";
    private Sodium sodium;

    public Sodium loadLibrary() {
        if(sodium == null){
            var library = setupJavaProperty();
            sodium = Native.load(library, Sodium.class);
        }

        return sodium;
    }

    private String setupJavaProperty() {
        try {
            var path = getSodiumPath();
            var directory = Objects.requireNonNull(SodiumUtils.class.getClassLoader().getResource(path));
            var filePath = Path.of(directory.toURI()).toRealPath();
            System.load(filePath.toString());
            return filePath.getFileName().toString();
        }catch (Exception exception){
            throw new RuntimeException("Cannot setup sodium", exception);
        }
    }

    private String getSodiumPath(){
        Validate.isTrue(!Platform.isMac(), "Missing support for mac os");
        Validate.isTrue(!Platform.isARM(), "Missing support for 32bit/64bit ARM");
        var is64Bit = Platform.is64Bit();
        if(Platform.isWindows()){
            return is64Bit ? SODIUM_WIN64 : SODIUM_WIN32;
        }

        if(Platform.isLinux()){
            Validate.isTrue(is64Bit, "Missing support for 32bit Linux");
            return SODIUM_LINUX;
        }

        throw new IllegalArgumentException("Missing support for unknown architecture");
    }
}
