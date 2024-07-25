package runnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            // Read the request line
            String requestLine = in.readLine();
            System.out.println("Request: " + requestLine);

            if (requestLine == null) {
                sendBadRequestResponse(out);
                return;
            }

            // Read headers
            Map<String, String> headers = readHeaders(in);
            headers.forEach((key, value) -> System.out.println(key + ": " + value));

            // Extract User-Agent
            String userAgent = headers.get("User-Agent");
            if (userAgent != null) {
                System.out.println("User-Agent: " + userAgent);
            } else {
                System.out.println("User-Agent header not found.");
            }

            // Handle the request
            handleRequest(requestLine, out);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> readHeaders(BufferedReader in) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String headerLine;
        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
            String[] headerParts = headerLine.split(": ", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0], headerParts[1]);
            }
        }
        return headers;
    }

    private void handleRequest(String requestLine, OutputStream out) throws IOException {
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            sendNotFoundResponse(out);
            return;
        }

        String path = parts[1];
        if (path.startsWith("/echo/")) {
            String echoString = path.substring(6);  // Extract the {str} part
            sendEchoResponse(out, echoString);
        } else if (path.equals("/")) {
            sendOkResponse(out);
        } else if (path.startsWith("/files/")) {
            String filename = path.substring(7);  // Extract the {filename} part
            sendFileResponse(out, filename);
        } else {
            sendNotFoundResponse(out);
        }
    }

    private void sendOkResponse(OutputStream out) throws IOException {
        String body = "Hello, World!";
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
        out.write(response.getBytes());
        out.flush();
    }

    private void sendEchoResponse(OutputStream out, String echoString) throws IOException {
        String body = echoString;
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
        out.write(response.getBytes());
        out.flush();
    }

    private void sendFileResponse(OutputStream out, String filename) throws IOException {
        Path filePath = Paths.get("/tmp", filename);
        if (Files.exists(filePath)) {
            byte[] fileContent = Files.readAllBytes(filePath);
            String fileContentString = new String(fileContent);
            System.out.println("File content: " + fileContentString);
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/octet-stream\r\n" +
                    "Content-Length: " + fileContent.length + "\r\n" +
                    "\r\n";
            out.write(response.getBytes());
            out.write(fileContent);
        } else {
            sendNotFoundResponse(out);
        }
        out.flush();
    }

    private void sendNotFoundResponse(OutputStream out) throws IOException {
        String body = "404 Not Found";
        String response = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
        out.write(response.getBytes());
        out.flush();
    }

    private void sendBadRequestResponse(OutputStream out) throws IOException {
        String body = "400 Bad Request";
        String response = "HTTP/1.1 400 Bad Request\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
        out.write(response.getBytes());
        out.flush();
    }
}

