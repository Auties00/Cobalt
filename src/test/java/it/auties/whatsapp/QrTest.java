package it.auties.whatsapp;

import it.auties.qr.QrTerminal;
import it.auties.whatsapp.api.QrHandler;
import org.junit.jupiter.api.Test;

public class QrTest {
    @Test
    public void print(){
        var qr = "2@g9Qjh2RXYBINWpoxyA/23VVjgPWRU6DYeV0SO6kkLnD1vzIwZHy0oKSGO4XOScK5nNF889fOCnDYeg==,h9Np8rYA7DkAwFTqeD/Sdzs6I+tKxDVjUzLEyAf1zGw=,Raw2OtobnuOW4dRlCnFXsCvyEEEH7nAJCRrYwQNJxRY=,UFhUneixx+JSKpCyU0/a1Hrivc98wo2Tk+N2KhYfp3Q=";
        var matrix = QrHandler.createMatrix(qr, 10, 0);
        QrTerminal.print(matrix, true);
    }
}
