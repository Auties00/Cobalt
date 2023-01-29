package it.auties.whatsapp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class WebSocketExample {
  public static void main(String[] args) {
    try(var socket = new Socket()) {
      socket.connect(new InetSocketAddress("g.whatsapp.net", 443));
      System.out.println("Open");
      while (!socket.isClosed()){

      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
