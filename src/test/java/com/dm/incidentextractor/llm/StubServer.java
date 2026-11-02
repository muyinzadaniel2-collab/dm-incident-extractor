package com.dm.incidentextractor.llm;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal local HTTP server that records the last request and replies with a canned response.
 */
public final class StubServer implements AutoCloseable {

    private final HttpServer server;
    private volatile String lastPath;
    private volatile String lastBody;
    private final Map<String, String> lastHeaders = new ConcurrentHashMap<>();

    public StubServer(int status, String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastPath = exchange.getRequestURI().getPath();
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastHeaders.clear();
            exchange.getRequestHeaders().forEach((k, v) -> lastHeaders.put(k.toLowerCase(), v.getFirst()));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
    }

    public URI baseUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    public String lastPath() {
        return lastPath;
    }

    public String lastBody() {
        return lastBody;
    }

    public String header(String name) {
        return lastHeaders.get(name.toLowerCase());
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
