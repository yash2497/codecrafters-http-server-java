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

     try {
       ServerSocket serverSocket = new ServerSocket(4221);

       // Since the tester restarts your program quite often, setting SO_REUSEADDR
       // ensures that we don't run into 'Address already in use' errors
       serverSocket.setReuseAddress(true);

       Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
       System.out.println("accepted new connection");

       BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));


       Map<String, String> headers = readHeaders(in);
       String headerVal = headers.get("User-Agent");

       headers.forEach((key, value) -> System.out.println(key + ": " + value));

       if(headerVal != null) {
           clientSocket.getOutputStream().write(
                   ("HTTP/1.1 200 OK\r\n" +
                           "Content-Type: text/plain\r\n" +
                           "Content-Length: " + headerVal.length() + "\r\n" +
                           "\r\n" +
                           headerVal).getBytes()
           );
       }
       else {
           String resp = handleRequest(headers.get("response-body"));
           if(resp != null) {
               clientSocket.getOutputStream().write(
                       ("HTTP/1.1 200 OK\r\n" +
                               "Content-Type: text/plain\r\n" +
                               "Content-Length: "+ resp.length() + "\r\n" +
                               "\r\n"+resp).getBytes()
               );
           }
           else {
               clientSocket.getOutputStream().write(
                       "HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
           }

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
        else if(path.equals("/")) {
            return "/";
        }
        else {
            return null;
        }
    }

    private static Map<String, String> readHeaders(BufferedReader in) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String headerLine;
        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
            String[] headerParts = headerLine.split(": ", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0], headerParts[1]);
            }
            else {
                if(headerParts[0].startsWith("GET")) {
                    headers.put("response-body", headerParts[0]);
                }
            }
        }
        return headers;
    }
}
