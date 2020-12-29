import it.auties.whatsapp4j.api.WhatsappAPI;
import jakarta.websocket.DeploymentException;

import java.io.IOException;
import java.util.Scanner;

public class Main {
  public static void main(String[] args) throws IOException, DeploymentException {
    final var api = new WhatsappAPI();
    new Scanner(System.in).nextLine();
  }
}
