package runnable;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;


public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private static final String BASE_DIRECTORY = "/tmp/data/codecrafters.io/http-server-tester/";

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

            // Handle the request
            handleRequest(requestLine, headers, in, out);

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

    private void handleRequest(String requestLine, Map<String, String> headers, BufferedReader in, OutputStream out) throws IOException {
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            sendNotFoundResponse(out);
            return;
        }

        String method = parts[0];
        String path = parts[1];
        if (method.equals("GET")) {
            handleGetRequest(path, headers, out);
        } else if (method.equals("POST")) {
            handlePostRequest(path, headers, in, out);
        } else {
            sendNotFoundResponse(out);
        }
    }

    private void handleGetRequest(String path, Map<String, String> headers, OutputStream out) throws IOException {
        if (path.startsWith("/echo/")) {
            // Extract the {str} part
            String echoString = path.substring(6);
            if(headers.containsKey("Accept-Encoding")) {
                sendEchoResponseEncoding(out, echoString, headers.get("Accept-Encoding"));
            } else {
                sendEchoResponse(out, echoString);
            }
        } else if (path.equals("/")) {
            sendOkResponse(out);
        } else if (path.startsWith("/files/")) {
            String filename = path.substring(7);  // Extract the {filename} part
            sendFileResponse(out, filename);
        } else if (path.equals("/user-agent")) {
            sendUserAgentResponse(out, headers.get("User-Agent"));
        } else {
            sendNotFoundResponse(out);
        }
    }

    private void handlePostRequest(String path, Map<String, String> headers, BufferedReader in, OutputStream out) throws IOException {
        if (path.startsWith("/files/")) {
            String filename = path.substring(7);  // Extract the {filename} part

            // Read the content length
            int contentLength = Integer.parseInt(headers.get("Content-Length"));
            char[] content = new char[contentLength];
            in.read(content, 0, contentLength);

            // Write the content to the file
            Path filePath = Paths.get(BASE_DIRECTORY, filename);
            Files.createDirectories(filePath.getParent());  // Ensure the directory exists
            Files.write(filePath, new String(content).getBytes());

            // Send 201 Created response
            String response = "HTTP/1.1 201 Created\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: 0\r\n" +
                    "\r\n";
            out.write(response.getBytes());
            out.flush();
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
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + echoString.length() + "\r\n" +
                "\r\n" +
                echoString;
        out.write(response.getBytes());
        out.flush();
    }

    private void sendEchoResponseEncoding(OutputStream out, String echoString, String encoding) throws IOException {
        String[] encodings = encoding.split(",");
        List<String> encodingsList = Arrays.asList(encodings);
        encodingsList.replaceAll(String::trim);
        if(encodingsList.contains("gzip")) {
            // Compress the string
            byte[] compressedData = compress(echoString);
            String hexDump = getHexDump(compressedData);
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Encoding: gzip\r\n" +
                    "Content-Length: " + compressedData.length + "\r\n" +
                    "\r\n" +
                    hexDump;
            out.write(response.getBytes());
            out.flush();
        }
        else {
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: " + echoString.length() + "\r\n" +
                    "\r\n" +
                    echoString;
            out.write(response.getBytes());
            out.flush();
        }
    }

    public static byte[] compress(String data) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try(GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(data.getBytes(StandardCharsets.UTF_8));
        }
        return byteArrayOutputStream.toByteArray();
    }

    private String getHexDump(byte[] data) {
        StringBuilder hexDump = new StringBuilder();
        for (int i = 0; i < data.length; i += 16) {
            hexDump.append(String.format("%08X  ", i));
            for (int j = 0; j < 16; j++) {
                if (i + j < data.length) {
                    hexDump.append(String.format("%02X ", data[i + j]));
                } else {
                    hexDump.append("   ");
                }
            }
            hexDump.append(" ");
            for (int j = 0; j < 16 && i + j < data.length; j++) {
                if (data[i + j] >= 32 && data[i + j] <= 126) {
                    hexDump.append((char) data[i + j]);
                } else {
                    hexDump.append(".");
                }
            }
            hexDump.append("\n");
        }
        return hexDump.toString();
    }

    private void sendFileResponse(OutputStream out, String filename) throws IOException {
        Path filePath = Paths.get(BASE_DIRECTORY, filename);
        if (Files.exists(filePath)) {
            byte[] fileContent = Files.readAllBytes(filePath);
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

    private void sendUserAgentResponse(OutputStream out, String userAgent) throws IOException {
        String body = userAgent == null ? "" : userAgent;
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
        out.write(response.getBytes());
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