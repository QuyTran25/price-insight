package com.pricetracker.server.websocket;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple SSE broadcaster. Keeps a list of connected clients and writes
 * Server-Sent Events (text/event-stream) messages to them.
 */
public class SSEBroadcaster implements Broadcaster {

    private final List<Client> clients = new CopyOnWriteArrayList<>();

    @Override
    public void broadcast(String message) {
        String sse = "data: " + message.replace("\n", "\ndata: ") + "\n\n";

        Iterator<Client> it = clients.iterator();
        while (it.hasNext()) {
            Client c = it.next();
            try {
                c.writer.write(sse);
                c.writer.flush();
            } catch (IOException e) {
                // Remove client on error
                try { c.exchange.close(); } catch (Exception ex) { }
                clients.remove(c);
            }
        }
    }

    @Override
    public int getClientCount() {
        return clients.size();
    }

    /**
     * Register a new SSE client via its HttpExchange. This method will
     * set response headers and keep the connection open.
     */
    public void addClient(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        // CORS for EventSource (allow Vercel frontend to connect)
        if (!headers.containsKey("Access-Control-Allow-Origin")) {
            headers.add("Access-Control-Allow-Origin", "*");
        }
        if (!headers.containsKey("Access-Control-Allow-Methods")) {
            headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
        }
        if (!headers.containsKey("Access-Control-Allow-Headers")) {
            headers.add("Access-Control-Allow-Headers", "Content-Type, Cache-Control, Pragma, Expires");
        }

        headers.add("Content-Type", "text/event-stream; charset=UTF-8");
        headers.add("Cache-Control", "no-cache");
        headers.add("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(exchange.getResponseBody(), StandardCharsets.UTF_8));

        // Send a comment to establish the stream
        writer.write(": connected\n\n");
        writer.flush();

        clients.add(new Client(exchange, writer));
    }

    private static class Client {
        final HttpExchange exchange;
        final BufferedWriter writer;

        Client(HttpExchange exchange, BufferedWriter writer) {
            this.exchange = exchange;
            this.writer = writer;
        }
    }
}
