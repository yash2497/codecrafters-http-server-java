import runnable.ClientHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

    // Uncomment this block to pass the first stage
      try{
          ServerSocket serverSocket = new ServerSocket(4221);

          // Since the tester restarts your program quite often, setting SO_REUSEADDR
          // ensures that we don't run into 'Address already in use' errors
          serverSocket.setReuseAddress(true);
          Socket clientSocket = serverSocket.accept();
          System.out.println("Accepted connection from " + clientSocket);
          ClientHandler clientHandler = new ClientHandler(clientSocket);

          Thread thread = new Thread(clientHandler);
          thread.start();
      } catch(IOException e) {
          System.out.println("IOException: " + e.getMessage());
      }

  }

}
