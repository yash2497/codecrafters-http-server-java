import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

    // Uncomment this block to pass the first stage

     try {
       ServerSocket serverSocket = new ServerSocket(4221);

       // Since the tester restarts your program quite often, setting SO_REUSEADDR
       // ensures that we don't run into 'Address already in use' errors
       serverSocket.setReuseAddress(true);

       Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
       System.out.println("accepted new connection");

       BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

       String requestLine = in.readLine();
       String body = handleRequest(requestLine);

       System.out.println("body: " + body);

       if(body != null) {
           clientSocket.getOutputStream().write(
                   ("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "+ body.length()+"\r\n\r\n" + Arrays.toString(body.getBytes())).getBytes()
           );
       }
       else {
           clientSocket.getOutputStream().write(
                   "HTTP/1.1 500 Internal Server Error\r\n\r\n".getBytes()
           );
       }
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }

    private static String handleRequest(String requestLine) {
        if (requestLine == null || !requestLine.startsWith("GET")) {
            return null;
        }

        // Example: GET echo/abcdefg HTTP/1.1
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            return null;
        }

        // Extract path
        String path = parts[1];
        if(path.startsWith("/echo")) {
            path = path.startsWith("/") ? path.substring(1) : path;
            String[] params = path.split("/");
            return params[1].startsWith("/") ? path.substring(1) : params[1];
        }
        else {
            return null;
        }
    }
}
