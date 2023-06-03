package it.auties.whatsapp;

import it.auties.qr.QrTerminal;
import it.auties.whatsapp.api.QrHandler;
import org.junit.jupiter.api.Test;

public class QrTest {
    @Test
    public void print(){
        var matrix = QrHandler.createMatrix("2@/d3q5ERjD6q34FfshzIO94BuprqwlOTTaMjnnqkld/Xuk+FPvGzuwzr8kCCzUUBisUY2fDWySl0Xzw==,7B+Hv4LN5dyiaAXqSnPFPpJEFBfS4lCncDc/va8KXiY=,HqVarXxHQMAJGE8uk8u5aEHbtbj8mXb4gsmIm5ofRk0=,VoHpLZFpwQMnw19u7DmFD8HNweIU+zl4uRQQ8f6viQ8=", 10, 0);
        QrTerminal.print(matrix, true);
    }
}
