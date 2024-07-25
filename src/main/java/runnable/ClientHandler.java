package runnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable{
    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            Map<String, String> headers = readHeaders(in);
            String headerVal = headers.get("User-Agent");

            System.out.println("Map-Values###########");
            headers.forEach((key, value) -> System.out.println(key + ": " + value));
            System.out.println("----------header value: "+ headerVal);

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

    private String handleRequest(String requestLine) {
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
        System.out.println("path: " + path);
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

    private Map<String, String> readHeaders(BufferedReader in) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String headerLine;
        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
            System.out.println("header line: " + headerLine);
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

