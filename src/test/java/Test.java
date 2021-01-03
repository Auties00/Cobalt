import it.auties.whatsapp4j.utils.BinaryMessengerReader;
import it.auties.whatsapp4j.utils.BytesArray;
import jakarta.xml.bind.DatatypeConverter;

import java.util.Arrays;

public class Test {
    public static void main(String[] args) {
        final var data = DatatypeConverter.parseHexBinary("89dc453be579c4232c3a735a044bd14c775d28b6286f4e48182fe80566c455f9d74871e6b7c3082feca8e2fefb748da0adc5277d57b55e8db3422a1da5f948062701563525d05a5e126b95c7f8025e8b3d3c7cfbe08ec9e1f5b9ff9bec0e74f495905912fa5d8642fbc4");
        final var builder = new StringBuilder();
        for(var entry : data){
            builder.append((char) entry);
        }

        System.out.println(builder);
    }
}
