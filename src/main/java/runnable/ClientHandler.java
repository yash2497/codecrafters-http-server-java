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

public class ClientHandler implements Runnable{
    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();
            Map<String, String> headers = readHeaders(in);
            String headerVal = headers.get("User-Agent");

            headers.forEach((key, value) -> System.out.println(key + ": " + value));

            if(headerVal != null) {
                out.write(
                        ("HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: " + headerVal.length() + "\r\n" +
                                "\r\n" +
                                headerVal).getBytes()
                );
            }
            else {
                handleRequestAndSendResp(headers.get("response-body"), out, headers);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private void handleRequestAndSendResp(String requestLine, OutputStream out, Map<String, String> headers) throws IOException {

        if(requestLine != null && requestLine.startsWith("GET")) {
            handleGetRequest(requestLine, out);
        }
        else if(requestLine != null && requestLine.startsWith("POST")) {
            handlePostRequest(requestLine, out, headers);

        }
    }

    private void handlePostRequest(String requestLine, OutputStream out, Map<String, String> headers) throws IOException {
        if (requestLine.startsWith("/files/")) {
            String filename = requestLine.substring(7);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
            }
        }
    }

    private void handleGetRequest(String requestLine, OutputStream out) throws IOException {
        String resp;
        if (requestLine == null) {
            resp = null;
        }
        else {
            // Example: GET echo/abcdefg HTTP/1.1
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                resp = null;
            }

            // Extract path
            String path = parts[1];
            if(path.startsWith("/echo")) {
                path = path.startsWith("/") ? path.substring(1) : path;
                String[] params = path.split("/");
                resp = params[1].startsWith("/") ? path.substring(1) : params[1];
            }
            else if(path.equals("/")) {
                resp = "/";
            }
            else if(path.startsWith("/files/")) {
                String filename = path.substring(7);
                resp = getFileSizeAndContent(filename);
            }
            else {
                resp = null;
            }
        }
        if(resp != null && !resp.startsWith("HTTP/1.1")) {
            out.write(
                    ("HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: "+ resp.length() + "\r\n" +
                            "\r\n"+resp).getBytes()
            );
        }
        else if(resp != null && resp.startsWith("HTTP/1.1")) {
            out.write((resp).getBytes());
        }
        else {
            out.write(
                    "HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
        }
    }

    private String getFileSizeAndContent(String filename) throws IOException {
        Path filePath = Paths.get("/tmp/data/codecrafters.io/http-server-tester", filename);
        if(Files.exists(filePath)) {
            byte[] fileContent = Files.readAllBytes(filePath);
            String fileContentString = new String(fileContent);
            return "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/octet-stream\r\n" +
                    "Content-Length: " + fileContent.length + "\r\n" +
                    "\r\n"+ fileContentString;
        }
        return null;
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

