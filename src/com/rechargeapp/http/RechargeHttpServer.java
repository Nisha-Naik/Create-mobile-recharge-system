package com.rechargeapp.http;

import com.rechargeapp.model.Recharge;
import com.rechargeapp.model.RechargePlan;
import com.rechargeapp.model.RechargeRequest;
import com.rechargeapp.service.RechargeService;
import com.rechargeapp.util.JsonUtil;
import com.rechargeapp.util.ValidationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RechargeHttpServer {
    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private final int port;
    private final Path webRoot;
    private final RechargeService rechargeService;
    private final ExecutorService executor = Executors.newFixedThreadPool(12);
    private ServerSocket serverSocket;

    public RechargeHttpServer(int port, Path webRoot, Path historyFile) {
        this.port = port;
        this.webRoot = webRoot.toAbsolutePath().normalize();
        this.rechargeService = new RechargeService(historyFile);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(port));

        Thread acceptThread = new Thread(this::acceptLoop, "recharge-http-server");
        acceptThread.setDaemon(false);
        acceptThread.start();
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                executor.submit(() -> handleClient(socket));
            } catch (IOException ex) {
                if (!serverSocket.isClosed()) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket; InputStream input = socket.getInputStream(); OutputStream output = socket.getOutputStream()) {
            socket.setSoTimeout(2500);
            HttpRequest request = readRequest(input, output);
            HttpResponse response = route(request);
            writeResponse(output, response);
        } catch (Exception ex) {
            try {
                writeResponse(socket.getOutputStream(), jsonResponse(500,
                        "{\"status\":\"error\",\"message\":\"Internal server error\"}"));
            } catch (IOException ignored) {
            }
        }
    }

    private HttpResponse route(HttpRequest request) throws IOException {
        if (request == null) {
            return textResponse(400, "Bad request", "text/plain; charset=utf-8");
        }
        if ("OPTIONS".equalsIgnoreCase(request.method())) {
            return noContentResponse();
        }

        String path = request.uri().getPath();
        if ("/api/recharge".equals(path)) {
            return handleRecharge(request);
        }
        if ("/api/history".equals(path)) {
            return handleHistory(request);
        }
        if ("/api/plans".equals(path)) {
            return handlePlans(request);
        }
        return handleStaticFile(request);
    }

    private HttpResponse handleRecharge(HttpRequest request) {
        if (!"POST".equalsIgnoreCase(request.method())) {
            return jsonResponse(405, "{\"status\":\"error\",\"message\":\"Method not allowed\"}");
        }

        try {
            Map<String, String> payload = JsonUtil.parseObject(request.bodyAsText());
            RechargeRequest rechargeRequest = new RechargeRequest(
                    payload.get("mobileNumber"),
                    payload.get("customerName"),
                    payload.get("operator"),
                    payload.get("rechargeAmount"),
                    payload.get("rechargePlan"),
                    payload.get("paymentMethod"),
                    payload.get("currentBalance")
            );

            Recharge recharge = rechargeService.processRecharge(rechargeRequest);
            return jsonResponse(200, "{\"status\":\"success\",\"message\":\"Recharge Successful\",\"recharge\":"
                    + rechargeToJson(recharge) + "}");
        } catch (ValidationException ex) {
            return jsonResponse(400, "{\"status\":\"error\",\"message\":" + JsonUtil.quote(ex.getMessage()) + "}");
        } catch (IllegalStateException ex) {
            return jsonResponse(500, "{\"status\":\"error\",\"message\":\"Recharge could not be saved. Please try again.\"}");
        } catch (Exception ex) {
            return jsonResponse(400, "{\"status\":\"error\",\"message\":\"Invalid recharge request\"}");
        }
    }

    private HttpResponse handleHistory(HttpRequest request) {
        if (!"GET".equalsIgnoreCase(request.method())) {
            return jsonResponse(405, "{\"status\":\"error\",\"message\":\"Method not allowed\"}");
        }

        List<Recharge> history = rechargeService.getRechargeHistory();
        StringBuilder json = new StringBuilder();
        json.append("{\"status\":\"success\",\"history\":[");
        for (int i = 0; i < history.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(rechargeToJson(history.get(i)));
        }
        json.append("]}");
        return jsonResponse(200, json.toString());
    }

    private HttpResponse handlePlans(HttpRequest request) {
        if (!"GET".equalsIgnoreCase(request.method())) {
            return jsonResponse(405, "{\"status\":\"error\",\"message\":\"Method not allowed\"}");
        }

        Map<String, String> query = parseQuery(request.uri());
        String operator = query.getOrDefault("operator", "Jio");
        List<RechargePlan> plans = rechargeService.getPlansForOperator(operator);

        StringBuilder json = new StringBuilder();
        json.append("{\"status\":\"success\",\"plans\":[");
        for (int i = 0; i < plans.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(planToJson(plans.get(i)));
        }
        json.append("]}");
        return jsonResponse(200, json.toString());
    }

    private HttpResponse handleStaticFile(HttpRequest request) throws IOException {
        if (!"GET".equalsIgnoreCase(request.method())) {
            return textResponse(405, "Method not allowed", "text/plain; charset=utf-8");
        }

        String requestPath = request.uri().getPath();
        if ("/".equals(requestPath)) {
            requestPath = "/index.html";
        }

        Path file = webRoot.resolve(requestPath.substring(1)).normalize();
        if (!file.startsWith(webRoot) || !Files.exists(file) || Files.isDirectory(file)) {
            return textResponse(404, "Not found", "text/plain; charset=utf-8");
        }

        return new HttpResponse(200, contentType(file), Files.readAllBytes(file), commonHeaders());
    }

    private HttpRequest readRequest(InputStream input, OutputStream output) throws IOException {
        String requestLine = readLine(input);
        if (requestLine == null || requestLine.isBlank()) {
            return null;
        }

        String[] requestParts = requestLine.split(" ", 3);
        if (requestParts.length < 2) {
            return null;
        }

        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String line;
        while ((line = readLine(input)) != null && !line.isEmpty()) {
            int separator = line.indexOf(':');
            if (separator > 0) {
                headers.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
            }
        }

        if ("100-continue".equalsIgnoreCase(headers.get("Expect"))) {
            output.write("HTTP/1.1 100 Continue\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            output.flush();
        }

        int contentLength = parseContentLength(headers.get("Content-Length"));
        byte[] body = contentLength > 0 ? readBody(input, contentLength) : new byte[0];
        return new HttpRequest(requestParts[0], URI.create(requestParts[1]), headers, body);
    }

    private byte[] readBody(InputStream input, int contentLength) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(contentLength);
        byte[] chunk = new byte[Math.min(1024, contentLength)];
        int remaining = contentLength;

        while (remaining > 0) {
            int read;
            try {
                read = input.read(chunk, 0, Math.min(chunk.length, remaining));
            } catch (SocketTimeoutException ex) {
                break;
            }

            if (read == -1) {
                break;
            }
            buffer.write(chunk, 0, read);
            remaining -= read;
        }

        return buffer.toByteArray();
    }

    private String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int current;
        while ((current = input.read()) != -1) {
            if (current == '\r') {
                int next = input.read();
                if (next == '\n') {
                    break;
                }
                if (next != -1) {
                    buffer.write(next);
                }
                continue;
            }
            if (current == '\n') {
                break;
            }
            buffer.write(current);
        }

        if (current == -1 && buffer.size() == 0) {
            return null;
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private void writeResponse(OutputStream output, HttpResponse response) throws IOException {
        byte[] body = response.body();
        StringBuilder header = new StringBuilder();
        header.append("HTTP/1.1 ")
                .append(response.statusCode())
                .append(' ')
                .append(reasonPhrase(response.statusCode()))
                .append("\r\n");
        header.append("Content-Type: ").append(response.contentType()).append("\r\n");
        header.append("Content-Length: ").append(body.length).append("\r\n");
        header.append("Connection: close\r\n");

        for (Map.Entry<String, String> entry : response.headers().entrySet()) {
            header.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }

        header.append("\r\n");
        output.write(header.toString().getBytes(StandardCharsets.UTF_8));
        output.write(body);
        output.flush();
    }

    private int parseContentLength(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private HttpResponse jsonResponse(int statusCode, String json) {
        return new HttpResponse(statusCode, "application/json; charset=utf-8",
                json.getBytes(StandardCharsets.UTF_8), commonHeaders());
    }

    private HttpResponse textResponse(int statusCode, String body, String contentType) {
        return new HttpResponse(statusCode, contentType, body.getBytes(StandardCharsets.UTF_8), commonHeaders());
    }

    private HttpResponse noContentResponse() {
        return new HttpResponse(204, "text/plain; charset=utf-8", new byte[0], commonHeaders());
    }

    private Map<String, String> commonHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type");
        headers.put("X-Content-Type-Options", "nosniff");
        return headers;
    }

    private String rechargeToJson(Recharge recharge) {
        return "{"
                + "\"mobileNumber\":" + JsonUtil.quote(recharge.getUser().getMobileNumber()) + ","
                + "\"customerName\":" + JsonUtil.quote(recharge.getUser().getCustomerName()) + ","
                + "\"operator\":" + JsonUtil.quote(recharge.getOperator().getDisplayName()) + ","
                + "\"amount\":" + money(recharge.getRechargeAmount()) + ","
                + "\"rechargePlan\":" + JsonUtil.quote(recharge.getRechargePlan()) + ","
                + "\"paymentMethod\":" + JsonUtil.quote(recharge.getPaymentMethod()) + ","
                + "\"transactionId\":" + JsonUtil.quote(recharge.getTransactionId()) + ","
                + "\"dateTime\":" + JsonUtil.quote(recharge.getDateTime().toString()) + ","
                + "\"dateTimeDisplay\":" + JsonUtil.quote(recharge.getDateTime().format(DISPLAY_FORMATTER)) + ","
                + "\"remainingBalance\":" + money(recharge.getRemainingBalance())
                + "}";
    }

    private String planToJson(RechargePlan plan) {
        return "{"
                + "\"name\":" + JsonUtil.quote(plan.getName()) + ","
                + "\"amount\":" + money(plan.getAmount()) + ","
                + "\"validity\":" + JsonUtil.quote(plan.getValidity()) + ","
                + "\"data\":" + JsonUtil.quote(plan.getData()) + ","
                + "\"badge\":" + JsonUtil.quote(plan.getBadge())
                + "}";
    }

    private String money(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private Map<String, String> parseQuery(URI uri) {
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return Map.of();
        }

        Map<String, String> values = new LinkedHashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (name.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private String reasonPhrase(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            default -> "OK";
        };
    }

    private record HttpRequest(String method, URI uri, Map<String, String> headers, byte[] body) {
        private String bodyAsText() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    private record HttpResponse(int statusCode, String contentType, byte[] body, Map<String, String> headers) {
    }
}
